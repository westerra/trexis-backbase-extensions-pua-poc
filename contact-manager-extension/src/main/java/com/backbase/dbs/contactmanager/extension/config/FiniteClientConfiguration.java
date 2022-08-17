package com.backbase.dbs.contactmanager.extension.config;

import com.finite.api.EntityApi;
import com.finite.ApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trexis.experts.finite.FiniteConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@RequiredArgsConstructor
@ComponentScan("net.trexis.experts.finite")
@Slf4j
public class FiniteClientConfiguration {

    private final FiniteConfiguration finiteConfiguration;

    @Bean(name = "coreApiClient")
    public ApiClient coreApiClient() {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(finiteConfiguration.getHosturl());
        //Set default to the core mapping, but this will get overwritten per request based on the multicore support feature
        apiClient.setApiKey(finiteConfiguration.getAPIKeyValue("core"));
        return apiClient;
    }

    @Primary
    @Bean(name = "coreEntityApi")
    public EntityApi coreEntityApi(@Qualifier("coreApiClient") ApiClient coreApiClient) {
        return new EntityApi(coreApiClient);
    }
}