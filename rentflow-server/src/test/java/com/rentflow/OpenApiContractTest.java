package com.rentflow;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiContractTest {
    @Test
    void exposesOnlyCommerceProductAndOrderContracts() throws IOException {
        byte[] bytes;
        try (InputStream input = getClass().getResourceAsStream("/openapi/rentflow-v1.yaml")) {
            assertThat(input).isNotNull();
            bytes = input.readAllBytes();
        }
        String contract = new String(bytes, StandardCharsets.UTF_8);
        Map<String, Object> document = new Yaml().load(contract);

        assertThat(document).containsKeys("openapi", "paths", "components");
        assertThat(contract)
                .doesNotContainIgnoringCase("rental")
                .doesNotContainIgnoringCase("reservation")
                .doesNotContainIgnoringCase("quote")
                .doesNotContain("dailyRate", "fixedDeposit", "availableCount", "maxDailyRate");
        assertThat(contract)
                .contains("/api/v1/store/orders/checkout:")
                .contains("StoreOrder:")
                .contains("maxPrice");
    }
}
