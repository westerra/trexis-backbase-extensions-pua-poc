package com.backbase.dbs.transaction.mgmt.presentation.extension.service;

import com.backbase.buildingblocks.backend.communication.context.OriginatorContextUtil;
import com.backbase.buildingblocks.backend.communication.event.proxy.EventBus;
import com.backbase.buildingblocks.backend.security.auth.config.SecurityContextUtil;
import com.backbase.buildingblocks.presentation.errors.InternalServerErrorException;
import com.backbase.dbs.arrangement.client.v2.ArrangementsApi;
import com.backbase.dbs.arrangement.client.v2.model.AccountArrangementItem;
import com.backbase.dbs.transaction.mgmt.persistence.domain.ParameterHolder;
import com.backbase.dbs.transaction.mgmt.persistence.domain.Transaction;
import com.backbase.dbs.transaction.mgmt.persistence.mapper.request.patch.TransactionPatchRequestMapper;
import com.backbase.dbs.transaction.mgmt.persistence.mapper.request.post.TransacionPostRequestMapper;
import com.backbase.dbs.transaction.mgmt.persistence.repository.SpecificationBuilder;
import com.backbase.dbs.transaction.mgmt.persistence.repository.TransactionRepository;
import com.backbase.dbs.transaction.mgmt.persistence.service.TransactionService;
import com.backbase.dbs.transaction.mgmt.presentation.extension.config.TransactionManagerConfig;
import com.backbase.transaction.persistence.rest.spec.v2.transactions.TransactionsGetResponseBody;
import lombok.extern.slf4j.Slf4j;
import net.trexis.experts.cursor.cursor_service.api.client.v2.CursorApi;
import net.trexis.experts.cursor.cursor_service.v2.model.Cursor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Validator;
import java.util.List;

import static java.lang.Thread.sleep;
import static java.time.LocalDateTime.now;
import static java.time.LocalDateTime.parse;
import static net.trexis.experts.cursor.cursor_service.v2.model.Cursor.StatusEnum.IN_PROGRESS;
import static net.trexis.experts.cursor.cursor_service.v2.model.Cursor.TypeEnum.ARRANGEMENT;

@Slf4j
@Primary
@Service
public class ExtendTransactionService extends TransactionService {

    private final TransactionManagerConfig transactionManagerConfig;
    private final CursorApi cursorApi;
    private final SecurityContextUtil securityContextUtil;
    private final ArrangementsApi arrangementsApi;

    public ExtendTransactionService(ApplicationContext applicationContext, TransactionRepository transactionRepository, EventBus eventBus, OriginatorContextUtil originatorContextUtil, Validator validator, @Value("${backbase.api.extensions.classes.com.backbase.transaction.persistence.rest.spec.v2.transactions.TransactionsPostRequestBody:null}") String additionalDataConfig, @Value("${backbase.transaction.additions.mode:tableAdditions}") Transaction.AdditionsMode additionsMode, TransacionPostRequestMapper transactionPostRequestMapper, TransactionPatchRequestMapper transactionPatchRequestMapper, SpecificationBuilder specificationBuilder,
                                    TransactionManagerConfig transactionManagerConfig, CursorApi cursorApi, SecurityContextUtil securityContextUtil, ArrangementsApi arrangementsApi) {
        super(applicationContext, transactionRepository, eventBus, originatorContextUtil, validator, additionalDataConfig, additionsMode, transactionPostRequestMapper, transactionPatchRequestMapper, specificationBuilder);
        this.transactionManagerConfig = transactionManagerConfig;
        this.cursorApi = cursorApi;
        this.securityContextUtil = securityContextUtil;
        this.arrangementsApi = arrangementsApi;
    }

    @Override
    @Transactional
    public TransactionsGetResponseBody getTransactions(ParameterHolder parameterHolder) {

        //Only do if enabled
        if(!transactionManagerConfig.isEnabled()) {
            log.info("Ingestion cursor progress check disabled");
            return super.getTransactions(parameterHolder);
        }
        //If the sub is not present, then this is likely a call from a service, and we do not want to delay responses
        if(!securityContextUtil.getUserTokenClaim("sub", String.class).isPresent())
            return super.getTransactions(parameterHolder);

        List<AccountArrangementItem> arrangementsByIds = getArrangementsByIds(parameterHolder.getArrangementIds());
        arrangementsByIds.stream()
            .map(AccountArrangementItem::getExternalArrangementId)
            .forEach(arrangementExternalId -> {
                log.info("Getting arrangement cursor by arrangementExternalId: {}", arrangementExternalId);
                Cursor arrangementCursor = null;
                try {
                    arrangementCursor = getArrangementCursor(arrangementExternalId);
                } catch (InternalServerErrorException e) {
                    if (transactionManagerConfig.isContinueAfterFailedCursorCheck()) {
                        log.warn("Failed to get arrangement cursor: {}", e.getMessage(), e);
                    } else {
                        log.error("Failed to get user cursor, erroring due to configuration continue-after-failed-cursor-check", e);
                        throw e;
                    }
                }

                var ingestionStartDateTime = parse(arrangementCursor.getStartDateTime());

                log.info("Cursor status is {} for arrangementExternalId {}, started at {}", arrangementCursor.getStatus(), arrangementExternalId, ingestionStartDateTime);
                while (arrangementCursor.getStatus() == IN_PROGRESS) {
                    log.info("Ingestion is still in progress for arrangementExternalId {}, started at {}", arrangementExternalId, ingestionStartDateTime);

                    // Regardless if ingestion is still in progress, stop waiting after the configured time
                    if (now().isAfter(ingestionStartDateTime.plusSeconds(transactionManagerConfig.getTimeWaitSeconds()))) {
                        log.info("Reached maximum waiting time of {} seconds for ingestion to complete. Ingestion still in progress, returning existing data",
                                transactionManagerConfig.getTimeWaitSeconds());
                        break;
                    }

                    log.info("Sleeping for {} seconds before checking again. Maximum waiting time is {}", transactionManagerConfig.getPollIntervalSeconds(), transactionManagerConfig.getTimeWaitSeconds());
                    try {
                        // Multiply by 1000 for millis instead of seconds
                        sleep(1000L * transactionManagerConfig.getPollIntervalSeconds());
                    } catch (InterruptedException e) {
                        log.warn("Thread sleep while waiting for in progress cursor to succeed was interrupted! Swallowing exception and allowing while loop to continue.");
                    }

                    arrangementCursor = getArrangementCursor(arrangementExternalId);
                }
        });
        return super.getTransactions(parameterHolder);
    }

    private List<AccountArrangementItem> getArrangementsByIds(List<String> arrangementIds) {
        if (arrangementIds.isEmpty()) {
            return List.of();
        }
        return arrangementsApi.getArrangements(null, arrangementIds, null).getArrangementElements();
    }


    private Cursor getArrangementCursor(String arrangementExternalId) {
        var cursorResponse = cursorApi.getCursorWithHttpInfo(arrangementExternalId, ARRANGEMENT.toString(), null);

        if (!cursorResponse.getStatusCode().is2xxSuccessful()) {
            log.error("Failed to get ARRANGEMENT cursor for arrangementExternalId {} for getTransactions. Status code: {}. Returning 500 error.", arrangementExternalId,
                    cursorResponse.getStatusCode());
            throw new InternalServerErrorException("Unable to retrieve ARRANGEMENT cursor!");
        }

        return cursorResponse.getBody().getCursor();
    }
}