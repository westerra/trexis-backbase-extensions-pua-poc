package com.backbase.dbs.product.config;

import lombok.RequiredArgsConstructor;
import net.trexis.experts.finite.FiniteConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({FiniteConfiguration.class})
@RequiredArgsConstructor
public class AppConfig {
}
