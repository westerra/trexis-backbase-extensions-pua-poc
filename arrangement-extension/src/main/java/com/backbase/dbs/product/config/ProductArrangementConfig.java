package com.backbase.dbs.product.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("arrangement-extension.product-arrangement")
public class ProductArrangementConfig {
    private boolean syncAliasToCore = false;
}
