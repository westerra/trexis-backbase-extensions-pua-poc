package com.backbase.dbs.product.config;

import com.backbase.buildingblocks.communication.client.ApiClientConfig;
import com.backbase.dbs.arrangement.arrangement_manager.api.client.ApiClient;
import com.backbase.dbs.arrangement.arrangement_manager.api.client.v2.ArrangementsApi;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.backbase.buildingblocks.communication.http.HttpCommunicationConfiguration.INTERCEPTORS_ENABLED_HEADER;
import static java.lang.Boolean.TRUE;

@Configuration
@ConfigurationProperties("backbase.communication.services.arrangement-manager")
public class ArrangementManagerServiceConfig extends ApiClientConfig {

    public static final String SERVICE_ID = "arrangement-manager";

    public ArrangementManagerServiceConfig() {
        super(SERVICE_ID);
    }

    @Bean
    public ArrangementsApi arrangementsApiApi() {
        return new ArrangementsApi(createApiClient());
    }

    private ApiClient createApiClient() {
        return new ApiClient(getRestTemplate())
                .setBasePath(createBasePath())
                .addDefaultHeader(INTERCEPTORS_ENABLED_HEADER, TRUE.toString());
    }
}