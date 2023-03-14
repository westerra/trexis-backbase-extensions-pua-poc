package com.backbase.dbs.transaction.mgmt.presentation.extension.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("transaction-extension")
public class TransactionManagerConfig {

    private Ingestion ingestion = new Ingestion();
    private AdditionOrdering additionOrdering = new AdditionOrdering();

    @Data
    public static class Ingestion {

        private boolean enabled = true;
        private int timeWaitSeconds = 15;
        private int pollIntervalSeconds = 1;
        private boolean continueAfterFailedCursorCheck = true;
        private boolean dateFlipEnabled = false;
    }

    @Data
    public static class AdditionOrdering {

        private boolean enabled = false;
        private String postedAddition = "bookingDateTime";
        private String pendingAddition = "createdDateTime";

    }
}
