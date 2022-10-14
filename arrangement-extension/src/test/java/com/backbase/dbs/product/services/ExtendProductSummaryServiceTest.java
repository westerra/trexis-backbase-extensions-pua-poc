package com.backbase.dbs.product.services;

import com.backbase.buildingblocks.backend.security.auth.config.SecurityContextUtil;
import com.backbase.buildingblocks.presentation.errors.BadRequestException;
import com.backbase.buildingblocks.presentation.errors.InternalServerErrorException;
import com.backbase.dbs.notifications.notification_service.api.client.v2.NotificationsApi;
import com.backbase.dbs.product.Configurations;
import com.backbase.dbs.product.ProductKindStorage;
import com.backbase.dbs.product.arrangement.ArrangementService;
import com.backbase.dbs.product.balance.BalanceService;
import com.backbase.dbs.product.clients.AccessControlClient;
import com.backbase.dbs.product.clients.JwtContext;
import com.backbase.dbs.product.config.ProductSummaryConfig;
import com.backbase.dbs.product.repository.ArrangementJpaRepository;
import com.backbase.dbs.product.summary.ProductSummaryFilter;
import com.backbase.dbs.user.api.client.v2.UserManagementApi;
import net.trexis.experts.cursor.cursor_service.api.client.v2.CursorApi;
import net.trexis.experts.cursor.cursor_service.v2.model.Cursor;
import net.trexis.experts.cursor.cursor_service.v2.model.CursorGetResponseBody;
import org.joda.time.DateTime;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class ExtendProductSummaryServiceTest {

    private final ProductKindStorage productKindStorage = mock(ProductKindStorage.class);
    private final ArrangementService arrangementService = mock(ArrangementService.class);
    private final JwtContext jwtContext = mock(JwtContext.class);
    private final AccessControlClient accessControlClient = mock(AccessControlClient.class);
    private final ArrangementJpaRepository arrangementRepository = mock(ArrangementJpaRepository.class);
    private final BalanceService balanceService = mock(BalanceService.class);
    private final SecurityContextUtil securityContextUtil = mock(SecurityContextUtil.class);
    private final CursorApi cursorApi = mock(CursorApi.class);
    private final NotificationsApi notificationsApi = mock(NotificationsApi.class);
    private final UserManagementApi userManagementApi = mock(UserManagementApi.class);

    private Configurations configurations = new Configurations();
    private ProductSummaryConfig productSummaryConfig = new ProductSummaryConfig();

    private ExtendProductSummaryService extendProductSummaryService;

    private final String MOCK_EXTERNAL_USER_ID = "mockExternalUserId";
    private final String MOCK_INTERNAL_ARRANGEMENT_ID = "mockInternalArrangementId";

    @BeforeEach
    void setup(){
        extendProductSummaryService = new ExtendProductSummaryService(
                configurations, productKindStorage, arrangementService, jwtContext, accessControlClient, arrangementRepository, balanceService, securityContextUtil,
                cursorApi, productSummaryConfig, notificationsApi, userManagementApi
        );

        when(accessControlClient.getArrangementIdsByUserIdWithViewPrivilege(any(), any()))
                .thenReturn(List.of(MOCK_INTERNAL_ARRANGEMENT_ID));

        when(securityContextUtil.getUserTokenClaim(any(), any()))
                .thenReturn(Optional.of(MOCK_EXTERNAL_USER_ID));
    }

    @Test
    void getProductSummary_BadCursor_BadRequest() {
        ProductSummaryFilter filter = ProductSummaryFilter.builder().build();

        doThrow(new BadRequestException()).when(cursorApi).getCursorWithHttpInfo(any(), any(), any());

        assertThrows(BadRequestException.class, ()->{
            extendProductSummaryService.getProductSummary(filter);
        });
    }
    @Test
    void getProductSummary_BadCursor_InternalServerErrorExceptionFail() {
        ProductSummaryFilter filter = ProductSummaryFilter.builder().build();

        doThrow(new InternalServerErrorException()).when(cursorApi).getCursorWithHttpInfo(any(), any(), any());

        //This should throw a exception, since we don't continue on a failed cursor
        productSummaryConfig.setContinueAfterFailedCursorCheck(false);
        assertThrows(InternalServerErrorException.class, ()->{
            extendProductSummaryService.getProductSummary(filter);
        });
    }
    @Test
    void getProductSummary_BadCursor_InternalServerErrorExceptionContinue() {
        ProductSummaryFilter filter = ProductSummaryFilter.builder().build();

        doThrow(new InternalServerErrorException()).when(cursorApi).getCursorWithHttpInfo(any(), any(), any());

        //This should NOT fail, since we continue on a failed cursor
        productSummaryConfig.setContinueAfterFailedCursorCheck(true);
        extendProductSummaryService.getProductSummary(filter);
    }

    @Test
    void getProductSummary_NotInProgress() {
        ProductSummaryFilter filter = ProductSummaryFilter.builder().build();

        Cursor cursor = setupUserCursor(Cursor.StatusEnum.SUCCESS, "2022-01-01T00:00:00", "2022-01-01T00:00:00");
        when(cursorApi.getCursorWithHttpInfo(any(), any(), any()))
                .thenReturn(ResponseEntity.ok(new CursorGetResponseBody().cursor(cursor)));

        extendProductSummaryService.getProductSummary(filter);
    }

    @Test
    void getProductSummary_InProgressMaxWaitExceeded() {
        ProductSummaryFilter filter = ProductSummaryFilter.builder().build();

        Cursor cursorInProgress = setupUserCursor(Cursor.StatusEnum.IN_PROGRESS, "2022-01-01T00:00:00", "2022-01-01T00:00:00");
        when(cursorApi.getCursorWithHttpInfo(any(), any(), any()))
                .thenReturn(ResponseEntity.ok(new CursorGetResponseBody().cursor(cursorInProgress)));

        extendProductSummaryService.getProductSummary(filter);
    }
    @Test
    void getProductSummary_InProgressLoop() {
        ProductSummaryFilter filter = ProductSummaryFilter.builder().build();

        String startDateTime = DateTime.now().toLocalDateTime().toString();

        Cursor cursorInProgress = setupUserCursor(Cursor.StatusEnum.IN_PROGRESS, "2022-01-01T00:00:00", startDateTime);
        Cursor cursorSuccess = setupUserCursor(Cursor.StatusEnum.SUCCESS, "2022-01-01T00:00:00", "2022-01-01T00:00:00");
        when(cursorApi.getCursorWithHttpInfo(any(), any(), any()))
                .thenReturn(ResponseEntity.ok(new CursorGetResponseBody().cursor(cursorInProgress)))
                .thenReturn(ResponseEntity.ok(new CursorGetResponseBody().cursor(cursorSuccess)));

        extendProductSummaryService.getProductSummary(filter);
    }

    private Cursor setupUserCursor(Cursor.StatusEnum status, String lastSuccessDate, String startDate){
        Cursor cursor = new Cursor();
        cursor.setStatus(status);
        cursor.setEntityId(MOCK_EXTERNAL_USER_ID);
        cursor.setType(Cursor.TypeEnum.USER);
        cursor.setLastSuccessDateTime(lastSuccessDate);
        cursor.setStartDateTime(startDate);
        return cursor;
    }
}