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
import com.backbase.transaction.persistence.rest.spec.v2.transactions.TransactionItem;
import com.backbase.transaction.persistence.rest.spec.v2.transactions.TransactionsGetResponseBody;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

    public static final String DIRECTION_DESCENDING = "DESC";
    private final TransactionManagerConfig.Ingestion transactionManagerIngestionConfig;
    private final TransactionManagerConfig.AdditionOrdering transactionManagerAdditionOrderingConfig;
    private final CursorApi cursorApi;
    private final SecurityContextUtil securityContextUtil;
    private final ArrangementsApi arrangementsApi;

    private static final String ORDER_BY_BOOKING_DATE = "bookingDate";
    private static final String BILLING_STATUS_BILLED = "BILLED";
    private static final String BILLING_STATUS_UNBILLED = "UNBILLED";
    private static final String BILLING_STATUS_PENDING = "PENDING";

    public ExtendTransactionService(ApplicationContext applicationContext, TransactionRepository transactionRepository, EventBus eventBus, OriginatorContextUtil originatorContextUtil, Validator validator, @Value("${backbase.api.extensions.classes.com.backbase.transaction.persistence.rest.spec.v2.transactions.TransactionsPostRequestBody:null}") String additionalDataConfig, @Value("${backbase.transaction.additions.mode:tableAdditions}") Transaction.AdditionsMode additionsMode, TransacionPostRequestMapper transactionPostRequestMapper, TransactionPatchRequestMapper transactionPatchRequestMapper, SpecificationBuilder specificationBuilder,
                                    TransactionManagerConfig transactionManagerConfig, CursorApi cursorApi, SecurityContextUtil securityContextUtil, ArrangementsApi arrangementsApi) {
        super(applicationContext, transactionRepository, eventBus, originatorContextUtil, validator, additionalDataConfig, additionsMode, transactionPostRequestMapper, transactionPatchRequestMapper, specificationBuilder);
        this.transactionManagerIngestionConfig = transactionManagerConfig.getIngestion();
        this.transactionManagerAdditionOrderingConfig = transactionManagerConfig.getAdditionOrdering();
        this.cursorApi = cursorApi;
        this.securityContextUtil = securityContextUtil;
        this.arrangementsApi = arrangementsApi;
    }

    @Override
    @Transactional
    public TransactionsGetResponseBody getTransactions(ParameterHolder parameterHolder) {

        var response = super.getTransactions(parameterHolder);

        // Booking date flip extension
        if (transactionManagerIngestionConfig.isDateFlipEnabled()) {
            response.getTransactionItems().stream()
                    .forEach(tx -> {
                        var originalBookingDate = tx.getBookingDate();
                        tx.setBookingDate(tx.getValueDate());
                        tx.setValueDate(originalBookingDate);
                    });
        }

        // Ingestion extension
        //Only do if enabled
        if(!transactionManagerIngestionConfig.isEnabled()) {
            log.info("Ingestion cursor progress check disabled");
            return response;
        }
        //If the sub is not present, then this is likely a call from a service, and we do not want to delay responses
        if(!securityContextUtil.getUserTokenClaim("sub", String.class).isPresent())
            return response;

        List<AccountArrangementItem> arrangementsByIds = getArrangementsByIds(parameterHolder.getArrangementIds());
        arrangementsByIds.stream()
            .map(AccountArrangementItem::getExternalArrangementId)
            .forEach(arrangementExternalId -> {
                log.info("Getting arrangement cursor by arrangementExternalId: {}", arrangementExternalId);
                Cursor arrangementCursor = null;
                try {
                    arrangementCursor = getArrangementCursor(arrangementExternalId);
                } catch (InternalServerErrorException e) {
                    if (transactionManagerIngestionConfig.isContinueAfterFailedCursorCheck()) {
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
                    if (now().isAfter(ingestionStartDateTime.plusSeconds(transactionManagerIngestionConfig.getTimeWaitSeconds()))) {
                        log.info("Reached maximum waiting time of {} seconds for ingestion to complete. Ingestion still in progress, returning existing data",
                                transactionManagerIngestionConfig.getTimeWaitSeconds());
                        break;
                    }

                    log.info("Sleeping for {} seconds before checking again. Maximum waiting time is {}", transactionManagerIngestionConfig.getPollIntervalSeconds(), transactionManagerIngestionConfig.getTimeWaitSeconds());
                    try {
                        // Multiply by 1000 for millis instead of seconds
                        sleep(1000L * transactionManagerIngestionConfig.getPollIntervalSeconds());
                    } catch (InterruptedException e) {
                        log.warn("Thread sleep while waiting for in progress cursor to succeed was interrupted! Swallowing exception and allowing while loop to continue.");
                    }

                    arrangementCursor = getArrangementCursor(arrangementExternalId);
                }
        });

        if (ORDER_BY_BOOKING_DATE.equals(parameterHolder.getOrderBy()) && transactionManagerAdditionOrderingConfig.isEnabled()) {
            var pendingTransactions = response.getTransactionItems().stream()
                    .filter(it -> BILLING_STATUS_UNBILLED.equals(it.getBillingStatus()) || BILLING_STATUS_PENDING.equals(it.getBillingStatus()))
                    .collect(Collectors.toList());

            var postedTransactions = response.getTransactionItems().stream()
                    .filter(it -> BILLING_STATUS_BILLED.equals(it.getBillingStatus()))
                    .collect(Collectors.toList());

            response.getTransactionItems().clear();

            // If no billing statuses were sent as a parameter we want to sort/return all transactions
            // If it was provided, we only want to sort/return UNBILLED/PENDING if those were requested
            if (parameterHolder.getBillingStatuses() == null || parameterHolder.getBillingStatuses() != null && parameterHolder.getBillingStatuses().contains(BILLING_STATUS_UNBILLED) || parameterHolder.getBillingStatuses().contains(BILLING_STATUS_PENDING)) {
                pendingTransactions = pendingTransactions.stream()
                        .filter(it -> BILLING_STATUS_UNBILLED.equals(it.getBillingStatus()) || BILLING_STATUS_PENDING.equals(it.getBillingStatus()))
                        .sorted(Comparator.comparing(t -> t.getAdditions().get(transactionManagerAdditionOrderingConfig.getPendingAddition())))
                        .collect(Collectors.toList());

                if (DIRECTION_DESCENDING.equals(parameterHolder.getDirection())) {
                    pendingTransactions = pendingTransactions.stream()
                            .sorted(Comparator.comparing(t -> t.getAdditions().get(transactionManagerAdditionOrderingConfig.getPendingAddition()), Comparator.reverseOrder()))
                            .collect(Collectors.toList());
                }

                response.getTransactionItems().addAll(pendingTransactions);
            }

            if (parameterHolder.getBillingStatuses() == null || parameterHolder.getBillingStatuses() != null && parameterHolder.getBillingStatuses().contains(BILLING_STATUS_BILLED)) {
                postedTransactions = postedTransactions.stream()
                        .filter(it -> BILLING_STATUS_BILLED.equals(it.getBillingStatus()))
                        .sorted(Comparator.comparing(t -> t.getAdditions().get(transactionManagerAdditionOrderingConfig.getPostedAddition())))
                        .collect(Collectors.toList());

                if (DIRECTION_DESCENDING.equals(parameterHolder.getDirection())) {
                    postedTransactions = postedTransactions.stream()
                            .sorted(Comparator.comparing(t -> t.getAdditions().get(transactionManagerAdditionOrderingConfig.getPostedAddition()), Comparator.reverseOrder()))
                            .collect(Collectors.toList());
                }

                response.getTransactionItems().addAll(postedTransactions);
            }
        }

        return response;
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