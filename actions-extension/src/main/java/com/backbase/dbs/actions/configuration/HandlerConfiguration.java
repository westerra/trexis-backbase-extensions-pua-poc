package com.backbase.dbs.actions.configuration;

import com.backbase.dbs.actions.extended.handler.TransactionOverThresholdHandler;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class HandlerConfiguration {

    @Primary
    @Bean(name = "transactionOverThresholdHandler")
    public TransactionOverThresholdHandler customHandler(ApplicationEventPublisher publisher) {
        return new TransactionOverThresholdHandler(publisher);
    }
}