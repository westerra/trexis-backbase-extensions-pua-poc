package com.backbase.dbs.product.services;

import com.backbase.buildingblocks.presentation.errors.BadRequestException;
import com.finite.ApiClient;
import com.finite.api.AccountsApi;
import net.trexis.experts.finite.FiniteConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommonFiniteServiceTest {

    private final AccountsApi accountsApi = mock(AccountsApi.class);
    private FiniteConfiguration finiteConfiguration = new FiniteConfiguration();

    private final String MOCK_EXTERNAL_ARRANGEMENT_ID = "mock-id";
    private final String MOCK_TRACE_ID = "mockTraceId";

    @Test
    void updateAccountName_happyPath() {
        when(accountsApi.getApiClient()).thenReturn(new ApiClient());
        finiteConfiguration.setApiKeys(List.of(getAPIKey()));
        CommonFiniteService commonFiniteService = new CommonFiniteService(finiteConfiguration, accountsApi);
        commonFiniteService.updateAccountTitle(MOCK_EXTERNAL_ARRANGEMENT_ID, "mockAlias", MOCK_TRACE_ID);
    }

    @Test
    void updateAccountName_unhappyPath() {
        when(accountsApi.getApiClient()).thenReturn(new ApiClient());
        finiteConfiguration.setApiKeys(List.of(getAPIKey()));
        when(accountsApi.putAccount(any(), any(), any(), any(), any(), any()))
                .thenThrow(new BadRequestException());
        CommonFiniteService commonFiniteService = new CommonFiniteService(finiteConfiguration, accountsApi);
        Assertions.assertThrows(Exception.class, () -> {
            commonFiniteService.updateAccountTitle(MOCK_EXTERNAL_ARRANGEMENT_ID, "mockAlias", MOCK_TRACE_ID);
        });
    }

    private FiniteConfiguration.APIKey getAPIKey(){
        FiniteConfiguration.APIKey apiKey = new FiniteConfiguration.APIKey();
        apiKey.setName("core");
        apiKey.setEnabled(true);
        apiKey.setApikey("do-no-use");
        return apiKey;
    }
}