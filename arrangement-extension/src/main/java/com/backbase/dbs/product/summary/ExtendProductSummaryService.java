package com.backbase.dbs.product.summary;

import com.backbase.buildingblocks.backend.security.auth.config.SecurityContextUtil;
import com.backbase.buildingblocks.presentation.errors.InternalServerErrorException;
import com.backbase.dbs.notifications.notification_service.api.client.v2.NotificationsApi;
import com.backbase.dbs.notifications.notification_service.v2.model.LegalEntity;
import com.backbase.dbs.notifications.notification_service.v2.model.NotificationsPostRequestBody;
import com.backbase.dbs.notifications.notification_service.v2.model.Routing;
import com.backbase.dbs.product.Configurations;
import com.backbase.dbs.product.arrangement.ArrangementService;
import com.backbase.dbs.product.balance.BalanceService;
import com.backbase.dbs.product.clients.AccessControlClient;
import com.backbase.dbs.product.clients.JwtContext;
import com.backbase.dbs.product.repository.ArrangementJpaRepository;
import com.backbase.dbs.product.summary.config.ProductSummaryConfig;
import com.backbase.dbs.user.api.client.v2.UserManagementApi;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.trexis.experts.cursor.cursor_service.api.client.v2.CursorApi;
import net.trexis.experts.cursor.cursor_service.v2.model.Cursor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import static com.backbase.dbs.notifications.notification_service.v2.model.SeverityLevel.WARNING;
import static com.backbase.dbs.notifications.notification_service.v2.model.TargetGroup.CUSTOMER;
import static java.lang.String.format;
import static java.lang.Thread.sleep;
import static java.time.LocalDateTime.now;
import static java.time.LocalDateTime.parse;
import static java.util.Optional.ofNullable;
import static net.trexis.experts.cursor.cursor_service.v2.model.Cursor.StatusEnum.IN_PROGRESS;
import static net.trexis.experts.cursor.cursor_service.v2.model.Cursor.TypeEnum.ENTITY;

@Slf4j
@Primary
@Service
public class ExtendProductSummaryService extends ProductSummaryService {

    private final SecurityContextUtil securityContextUtil;
    private final CursorApi cursorApi;
    private final NotificationsApi notificationsApi;
    private final ProductSummaryConfig productSummaryConfig;
    private final UserManagementApi userManagementApi;

    public ExtendProductSummaryService(Configurations configurations,
            ArrangementService arrangementService,
            JwtContext jwtContext,
            AccessControlClient accessControlClient,
            ArrangementJpaRepository arrangementRepository,
            BalanceService balanceService,
            SecurityContextUtil securityContextUtil,
            CursorApi cursorApi,
            ProductSummaryConfig productSummaryConfig,
            NotificationsApi notificationsApi,
            UserManagementApi userManagementApi) {
        super(configurations, arrangementService, jwtContext, accessControlClient, arrangementRepository, balanceService);
        this.securityContextUtil = securityContextUtil;
        this.cursorApi = cursorApi;
        this.productSummaryConfig = productSummaryConfig;
        this.notificationsApi = notificationsApi;
        this.userManagementApi = userManagementApi;
        log.info("Extended service for cursor-aware read arrangements has been created");
    }

    @PostConstruct
    private void logConfigValues() {
        log.info("Configured in progress cursor time wait: {} seconds",
                productSummaryConfig.getTimeWaitSeconds());
        log.info("Configured in progress cursor poll interval: {} seconds",
                productSummaryConfig.getPollIntervalSeconds());
    }

    @Override
    public ProductSummary getProductSummary(ProductSummaryFilter filter) {
        var entityId = securityContextUtil.getUserTokenClaim("sub", String.class)
                .orElseThrow(() -> new RuntimeException("Failed to get sub claim for jwt claims while getting product summary. Cannot check cursor status, failing."));

        log.info("Getting entity cursor by entityId: {}", entityId);
        var entityCursor = getEntityCursor(entityId);

        var lastSuccess = ofNullable(entityCursor.getLastSuccessDateTime()).map(LocalDateTime::parse);
        var ingestionStartDateTime = parse(entityCursor.getStartDateTime());
        var productSummaryCallStartDateTime = now();

        if (entityCursor.getStatus() != IN_PROGRESS) {
            log.info("Ingestion not in progress, returning immediately.");
            return super.getProductSummary(filter);
        }

        while (entityCursor.getStatus() == IN_PROGRESS) {
            log.info("Ingestion is still in progress for entityId {}, started at {}", entityId, ingestionStartDateTime);

            // Regardless if ingestion is still in progress, stop waiting after the configured time
            if (now().isAfter(ingestionStartDateTime.plusSeconds(productSummaryConfig.getTimeWaitSeconds()))) {
                log.info("Reached maximum waiting time of {} seconds for ingestion to complete. Ingestion still in progress, returning existing data",
                        productSummaryConfig.getTimeWaitSeconds());
                notifyUserOfIngestionInProgress(entityId, lastSuccess);
                break;
            }

            log.info("Sleeping for {} seconds before checking again", productSummaryConfig.getPollIntervalSeconds());
            try {
                // Multiply by 1000 for millis instead of seconds
                sleep(1000L * productSummaryConfig.getPollIntervalSeconds());
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

    private void notifyUserOfIngestionInProgress(String entityId, Optional<LocalDateTime> lastSuccessDateTime) {

        try {
            var getUserResponse = userManagementApi.getUserByExternalIdWithHttpInfo(entityId, true);
            if (getUserResponse.getStatusCode().isError()) {
                log.error("Exception while getting user by external id {} to notify user of ingestion still in progress timed out",
                        entityId);
            }

            var internalLegalEntityId = getUserResponse.getBody().getLegalEntityId();
            log.info("found legal entity id {} to notify", internalLegalEntityId);

            var requestBody = new NotificationsPostRequestBody()
                    .legalEntities(List.of(new LegalEntity().leId(internalLegalEntityId)))
                    .level(WARNING)
                    .title(productSummaryConfig.getTimedOutNotifcationTitle())
                    .message(format(productSummaryConfig.getTimedOutNotifcationMessage(), lastSuccessDateTime.map(LocalDateTime::toString).orElse("")))
                    .origin("get-product-summary")
                    .targetGroup(CUSTOMER)
                    .routing(new Routing().whereTo("product-summary-widget"));

            var notificationResponse = notificationsApi.postNotificationsWithHttpInfo(requestBody);
            if (notificationResponse.getStatusCode().isError()) {
                log.error("Exception while notifying user with legal entity id {} of ingestion still in progress timed out {}",
                        internalLegalEntityId);
            }
        } catch (RuntimeException ex) {
            log.error("Exception while notifying user with external id {} of ingestion still in progress timed out",
                    entityId, ex);
        }
    }
}
