package com.backbase.dbs.product.config;

import com.backbase.buildingblocks.communication.client.ApiClientConfig;
import com.backbase.dbs.notifications.notification_service.api.client.ApiClient;
import com.backbase.dbs.notifications.notification_service.api.client.v2.NotificationsApi;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.backbase.buildingblocks.communication.http.HttpCommunicationConfiguration.INTERCEPTORS_ENABLED_HEADER;
import static java.lang.Boolean.TRUE;

@Configuration
@ConfigurationProperties("backbase.communication.services.notification-service")
public class NotificationServiceRestClientConfig extends ApiClientConfig {

    public static final String SERVICE_ID = "notifications-service";

    public NotificationServiceRestClientConfig() {
        super(SERVICE_ID);
    }

    @Bean
    public NotificationsApi notificationsApi() {
        return new NotificationsApi(createApiClient());
    }

    private ApiClient createApiClient() {
        return new ApiClient(getRestTemplate())
                .setBasePath(createBasePath())
                .addDefaultHeader(INTERCEPTORS_ENABLED_HEADER, TRUE.toString());
    }
}