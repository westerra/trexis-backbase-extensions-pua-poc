package com.backbase.dbs.product.services;

import com.backbase.dbs.arrangement.arrangement_manager.v2.model.AccountArrangementItem;
import com.backbase.dbs.product.AliasConfiguration;
import com.backbase.dbs.product.AliasJourney;
import com.backbase.dbs.product.Configurations;
import com.backbase.dbs.product.arrangement.UserPreferencesCommand;
import com.backbase.dbs.product.arrangement.UserPreferencesService;
import com.backbase.dbs.product.clients.AccessControlClient;
import com.backbase.dbs.product.clients.JwtContext;
import com.backbase.dbs.product.config.ProductArrangementConfig;
import com.backbase.dbs.product.persistence.Arrangement;
import com.backbase.dbs.product.persistence.UserPreferences;
import com.backbase.dbs.product.repository.ArrangementJpaRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.ArrayList;
import java.util.Optional;

import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class ExtendUserPreferencesServiceTest {

    private final JwtContext jwtContext = mock(JwtContext.class);
    private final AccessControlClient accessControlClient = mock(AccessControlClient.class);
    private final UserPreferencesService userPreferencesService = mock(UserPreferencesService.class);
    private final ArrangementJpaRepository arrangementRepository = mock(ArrangementJpaRepository.class);

    private final CommonBackbaseService backbaseService = mock(CommonBackbaseService.class);
    private final CommonFiniteService finiteService = mock(CommonFiniteService.class);

    private Configurations configurations = new Configurations();
    private ProductArrangementConfig productArrangementConfig = new ProductArrangementConfig();
    private ExtendUserPreferencesService extendUserPreferencesService;

    private final String MOCK_INTERNAL_ARRANGEMENT_ID = "mockInternalArrangementId";
    private final String MOCK_EXTERNAL_ARRANGEMENT_ID = "mockExternalArrangementId";
    private final String MOCK_INTERNAL_USER_ID = "mockInternalUserId";

    @BeforeEach
    void setup(){
        AliasConfiguration aliasConfiguration = new AliasConfiguration();
        aliasConfiguration.setJourney(AliasJourney.USER_ALIAS);
        configurations.setAlias(aliasConfiguration);

        when(arrangementRepository.findByIdAndNotDeleted(any())).thenReturn(Optional.of(new Arrangement()));
        when(arrangementRepository.findById(any())).thenReturn(Optional.of(new Arrangement()));
        when(accessControlClient.checkViewPrivileges(any(), any(), any())).thenReturn(new ArrayList<>());
        when(userPreferencesService.getUserPreferences(any(), any(), any())).thenReturn(new UserPreferences());

        extendUserPreferencesService = new ExtendUserPreferencesService(
                jwtContext, accessControlClient, userPreferencesService, arrangementRepository, configurations,
                productArrangementConfig, backbaseService,finiteService
        );
    }

    @Test
    void testGetUserPreferencesArrangementOnly() {
        //Not writing any assertions, since this is pure Backbase code.
        extendUserPreferencesService.getUserPreferences(MOCK_INTERNAL_ARRANGEMENT_ID);
    }

    @Test
    void testGetUserPreferencesArrangementAndUserId() {
        //Not writing any assertions, since this is pure Backbase code.
        extendUserPreferencesService.getUserPreferences(MOCK_INTERNAL_ARRANGEMENT_ID, MOCK_INTERNAL_USER_ID, true);
    }

    @Test
    void updateUserPreferences_happyPath_aliasOnly_syncDisabled() {
        UserPreferencesCommand userPreferencesCommand = new UserPreferencesCommand();
        userPreferencesCommand.setAlias("mockAlias");
        userPreferencesCommand.setArrangementId(MOCK_INTERNAL_ARRANGEMENT_ID);
        extendUserPreferencesService.updateUserPreferences(userPreferencesCommand);

        //Since sync is disabled by default means the finite service should not have been invoked
        verify(finiteService, times(0)).updateAccountTitle(any(), any(), any());
    }

    @Test
    void updateUserPreferences_happyPath_aliasOnly_syncEnabled() {
        when(backbaseService.getArrangementsById(any()))
                .thenReturn(new AccountArrangementItem().externalArrangementId(MOCK_EXTERNAL_ARRANGEMENT_ID));

        //Enable sync
        productArrangementConfig.setSyncAliasToCore(true);

        UserPreferencesCommand userPreferencesCommand = new UserPreferencesCommand();
        userPreferencesCommand.setAlias("mockAlias");
        userPreferencesCommand.setArrangementId(MOCK_INTERNAL_ARRANGEMENT_ID);
        extendUserPreferencesService.updateUserPreferences(userPreferencesCommand);

        //Since sync is enabled, expecting finite to be triggered once
        verify(finiteService, times(1)).updateAccountTitle(any(), any(), any());
    }

}