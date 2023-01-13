package com.backbase.dbs.transaction.mgmt.presentation.extension.config;

import com.backbase.buildingblocks.communication.client.ApiClientConfig;
import net.trexis.experts.cursor.cursor_service.api.client.ApiClient;
import net.trexis.experts.cursor.cursor_service.api.client.v2.CursorApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.backbase.buildingblocks.communication.http.HttpCommunicationConfiguration.INTERCEPTORS_ENABLED_HEADER;
import static java.lang.Boolean.TRUE;

@Configuration
public class CursorApiRestClientConfig extends ApiClientConfig {

    public static final String SERVICE_ID = "cursor";

    public CursorApiRestClientConfig() {
        super(SERVICE_ID);
    }

    @Bean
    public CursorApi cursorApi() {
        return new CursorApi(createApiClient());
    }

    private ApiClient createApiClient() {
        return new ApiClient(getRestTemplate())
                .setBasePath(createBasePath())
                .addDefaultHeader(INTERCEPTORS_ENABLED_HEADER, TRUE.toString());
    }
}