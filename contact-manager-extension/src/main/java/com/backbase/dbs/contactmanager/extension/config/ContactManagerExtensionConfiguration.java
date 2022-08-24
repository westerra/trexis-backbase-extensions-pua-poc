package com.backbase.dbs.contactmanager.extension.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("contact-manager-extension")
public class ContactManagerExtensionConfiguration {

    private int lastNameValidateFirstCharacters = 3;
    private String finiteEntityIdentifierClaim = "entityId";
}