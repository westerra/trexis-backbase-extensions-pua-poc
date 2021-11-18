package com.backbase.dbs.product.summary;

import com.backbase.buildingblocks.backend.security.auth.config.SecurityContextUtil;
import com.backbase.buildingblocks.presentation.errors.InternalServerErrorException;
import com.backbase.dbs.product.Configurations;
import com.backbase.dbs.product.arrangement.ArrangementService;
import com.backbase.dbs.product.balance.BalanceService;
import com.backbase.dbs.product.clients.AccessControlClient;
import com.backbase.dbs.product.clients.JwtContext;
import com.backbase.dbs.product.repository.ArrangementJpaRepository;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.trexis.experts.cursor.cursor_service.api.client.v2.CursorApi;
import net.trexis.experts.cursor.cursor_service.v2.model.Cursor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import static java.lang.Thread.sleep;
import static java.time.LocalDateTime.now;
import static java.time.LocalDateTime.parse;
import static net.trexis.experts.cursor.cursor_service.v2.model.Cursor.StatusEnum.IN_PROGRESS;
import static net.trexis.experts.cursor.cursor_service.v2.model.Cursor.TypeEnum.ENTITY;

@Slf4j
@Primary
@Service
public class ExtendProductSummaryService extends ProductSummaryService {

    private final SecurityContextUtil securityContextUtil;
    private final CursorApi cursorApi;

    @Value("${arrangement-extension.product-summary.in-progress-cursor.time-wait-seconds:60}")
    private int inProgressCursorTimeWaitSeconds;

    @Value("${arrangement-extension.product-summary.in-progress-cursor.poll-interval-seconds:2}")
    private int inProgressCursorPollIntervalSeconds;


    public ExtendProductSummaryService(Configurations configurations,
            ArrangementService arrangementService,
            JwtContext jwtContext,
            AccessControlClient accessControlClient,
            ArrangementJpaRepository arrangementRepository,
            BalanceService balanceService,
            SecurityContextUtil securityContextUtil,
            CursorApi cursorApi) {
        super(configurations, arrangementService, jwtContext, accessControlClient, arrangementRepository, balanceService);
        this.securityContextUtil = securityContextUtil;
        this.cursorApi = cursorApi;
        log.info("Extended service for cursor-aware read arrangements has been created");
    }

    @PostConstruct
    private void logConfigValues() {
        log.info("Configured in progress cursor time wait: {} seconds",
                inProgressCursorTimeWaitSeconds);
        log.info("Configured in progress cursor poll interval: {} seconds",
                inProgressCursorPollIntervalSeconds);
    }

    @Override
    public ProductSummary getProductSummary(ProductSummaryFilter filter) {
        var entityId = securityContextUtil.getUserTokenClaim("sub", String.class)
                .orElseThrow(() -> new RuntimeException("Failed to get sub claim for jwt claims while getting product summary. Cannot check cursor status, failing."));

        log.info("Getting entity cursor by entityId: {}", entityId);
        var entityCursor = getEntityCursor(entityId);

        var ingestionStartDateTime = parse(entityCursor.getStartDateTime());
        var productSummaryCallStartDateTime = now();

        if (entityCursor.getStatus() != IN_PROGRESS) {
            log.info("Ingestion not in progress, returning immediately.");
            return super.getProductSummary(filter);
        }

        while (entityCursor.getStatus() == IN_PROGRESS) {
            log.info("Ingestion is still in progress for entityId {}, started at {}", entityId, ingestionStartDateTime);

            // Regardless if ingestion is still in progress, stop waiting after the configured time
            if (now().isAfter(ingestionStartDateTime.plusSeconds(inProgressCursorTimeWaitSeconds))) {
                log.info("Reached maximum waiting time of {} seconds for ingestion to complete. Ingestion still in progress, returning existing data", inProgressCursorTimeWaitSeconds);
                break;
            }

            log.info("Sleeping for {} seconds before checking again", inProgressCursorPollIntervalSeconds);
            try {
                // Multiply by 1000 for millis instead of seconds
                sleep(1000L * inProgressCursorPollIntervalSeconds);
            } catch (InterruptedException e) {
                log.warn("Thread sleep while waiting for in progress cursor to succeed was interrupted! Swallowing exception and allowing while loop to continue.");
            }

            entityCursor = getEntityCursor(entityId);
        }

        return super.getProductSummary(filter);
    }

    private Cursor getEntityCursor(String entityId) {
        var cursorResponse = cursorApi.getCursorWithHttpInfo(entityId, ENTITY.toString(), null);

        if (!cursorResponse.getStatusCode().is2xxSuccessful()) {
            log.error("Failed to get ENTITY cursor for entityId {} for getProductSummary. Status code: {}. Returning 500 error.", entityId, cursorResponse.getStatusCode());
            throw new InternalServerErrorException("Unable to retrieve ENTITY cursor!");
        }

        return cursorResponse.getBody().getCursor();
    }
}
