package com.backbase.dbs.product.summary.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("arrangement-extension.product-summary")
public class ProductSummaryConfig {

    private int timeWaitSeconds = 35;
    private int pollIntervalSeconds = 2;
    private String timedOutNotifcationTitle = "Loading your updated information is taking longer than expected.";
    private String timedOutNotifcationMessage = "The information displayed is accurate as of %s. Please try logging in again to view your updated information. If you need additional help, please contact us.";
}
