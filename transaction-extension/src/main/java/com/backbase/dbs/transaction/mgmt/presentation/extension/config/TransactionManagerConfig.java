package com.backbase.dbs.transaction.mgmt.presentation.extension.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("transaction-extension")
public class TransactionManagerConfig {

    private Ingestion ingestion = new Ingestion();

    @Data
    public static class Ingestion {

        private boolean enabled = true;
        private int timeWaitSeconds = 15;
        private int pollIntervalSeconds = 1;
        private boolean continueAfterFailedCursorCheck = true;
        private boolean dateFlipEnabled = false;
    }
}
