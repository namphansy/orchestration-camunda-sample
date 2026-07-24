package com.example.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IntegrationTestModuleTests {

    @Test
    void moduleIsReadyForFutureEndToEndTests() {
        assertThat("integration-tests").isNotBlank();
    }
}

