package com.net.trexis.experts.bai2_export.configuration;
import com.backbase.buildingblocks.communication.client.ApiClientConfig;
import com.backbase.dbs.arrangement.client.ApiClient;
import com.backbase.dbs.arrangement.client.v2.ArrangementsApi;
import com.backbase.dbs.arrangement.client.v2.ProductsApi;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.backbase.buildingblocks.communication.http.HttpCommunicationConfiguration;

@Configuration
@ConfigurationProperties("backbase.communication.services.arrangement-manager")
public class ArrangementManagerConfiguration extends ApiClientConfig {
    
    public static final String SERVICE_ID = "arrangement-manager";

    public ArrangementManagerConfiguration() {
        super(SERVICE_ID);
    }

    @Bean
    public ProductsApi productsApi() {
        return new ProductsApi(createApiClient());
    }

    @Bean
    public ArrangementsApi arrangementsApi() {
        return new ArrangementsApi(createApiClient());
    }

    private ApiClient createApiClient() {
        return new ApiClient(getRestTemplate())
                .setBasePath(createBasePath())
                .addDefaultHeader(HttpCommunicationConfiguration.INTERCEPTORS_ENABLED_HEADER, 
                    Boolean.TRUE.toString());
    }
}