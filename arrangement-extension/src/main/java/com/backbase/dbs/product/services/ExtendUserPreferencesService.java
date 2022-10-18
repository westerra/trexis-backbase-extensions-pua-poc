package com.backbase.dbs.product.services;

import com.backbase.dbs.arrangement.arrangement_manager.v2.model.AccountArrangementItem;
import com.backbase.dbs.product.arrangement.PermissionAwareUserPreferencesService;
import com.backbase.dbs.product.arrangement.UserPreferencesCommand;
import com.backbase.dbs.product.arrangement.UserPreferencesService;
import com.backbase.dbs.product.config.ProductArrangementConfig;
import com.backbase.dbs.product.Configurations;
import com.backbase.dbs.product.clients.AccessControlClient;
import com.backbase.dbs.product.clients.JwtContext;
import com.backbase.dbs.product.persistence.UserPreferences;
import com.backbase.dbs.product.repository.ArrangementJpaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Slf4j
@Primary
@Service
public class ExtendUserPreferencesService extends PermissionAwareUserPreferencesService {
    private final ProductArrangementConfig productArrangementConfig;
    private final CommonBackbaseService backbaseService;
    private final CommonFiniteService finiteService;

    private JwtContext jwtContext;

    public ExtendUserPreferencesService(JwtContext jwtContext, AccessControlClient accessControlClient, UserPreferencesService userPreferencesService, ArrangementJpaRepository arrangementRepository, Configurations configurations, ProductArrangementConfig productArrangementConfig, CommonBackbaseService backbaseService, CommonFiniteService finiteService) {
        super(jwtContext, accessControlClient, userPreferencesService, arrangementRepository, configurations);
        this.productArrangementConfig = productArrangementConfig;
        this.backbaseService = backbaseService;
        this.jwtContext = jwtContext;
        this.finiteService = finiteService;
    }

    @Override
    public UserPreferences getUserPreferences(@NotNull @Size(min = 1,max = 36) String arrangementId){
        return super.getUserPreferences(arrangementId);
    }

    @Override
    public UserPreferences getUserPreferences(@NotNull @Size(min = 1,max = 36) String arrangementId, String userId, Boolean favorite){
        return super.getUserPreferences(arrangementId, userId, favorite);
    }

    @Override
    public void updateUserPreferences(UserPreferencesCommand userPreferencesCommand){
        super.updateUserPreferences(userPreferencesCommand);
        log.info("Is sync alias to core enabled: {}", productArrangementConfig.isSyncAliasToCore());
        if(productArrangementConfig.isSyncAliasToCore() && userPreferencesCommand.getAlias()!=null){
            AccountArrangementItem accountArrangementItem = backbaseService.getArrangementsById(userPreferencesCommand.getArrangementId());
            finiteService.updateAccountTitle(accountArrangementItem.getExternalArrangementId(), userPreferencesCommand.getAlias(), String.format("USER_PREFERENCE_UPDATE_%s", jwtContext.getUserId()));
        }
    }
}
