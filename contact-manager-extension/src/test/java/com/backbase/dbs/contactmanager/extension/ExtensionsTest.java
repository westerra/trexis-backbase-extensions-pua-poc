package com.backbase.dbs.contactmanager.extension;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan({"com.backbase.dbs.contactmanager.extension"})
class ExtensionsTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ExtensionsTest.class);

    @Test
    void testExtensionsLoaded() {
        this.contextRunner.run((context) -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(AddContactRouteExtension.class);
        });
    }
}
