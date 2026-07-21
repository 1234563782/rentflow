package com.rentflow.store.infrastructure;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class StoreMapperXmlTest {
    @Test
    void productExistenceUsesTheProductsEnabledColumn() throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("/mapper/store/StoreMapper.xml")) {
            assertThat(stream).isNotNull();
            String mapper = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(mapper)
                    .contains("FROM products WHERE id = #{productId} AND enabled = TRUE")
                    .doesNotContain("FROM products WHERE id = #{productId} AND active = TRUE");
        }
    }
}
