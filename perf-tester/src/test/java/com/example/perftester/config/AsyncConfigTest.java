package com.example.perftester.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AsyncConfigTest {

    @Test
    void asyncConfigShouldBeInstantiable() {
        AsyncConfig asyncConfig = new AsyncConfig();

        assertNotNull(asyncConfig);
    }
}
