package com.backbase.dbs.transaction.mgmt.presentation.extension.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("transaction-extension.ingestion")
public class TransactionManagerConfig {
    private boolean enabled = true;
    private int timeWaitSeconds = 15;
    private int pollIntervalSeconds = 1;
    private boolean continueAfterFailedCursorCheck = true;
}
