package com.catalog.consolidation.infrastructure.json;

import com.catalog.consolidation.domain.model.ProductInsertionResult;
import com.catalog.consolidation.domain.model.Seller;
import com.catalog.consolidation.domain.model.SellerProduct;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JsonFailedCatalogWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldWriteFailedItemsInInputFormatWithErrorMessage() throws Exception {
        SellerProduct source = new SellerProduct(
                new Seller("MegaStore", null),
                "seller-1",
                "Broken Product",
                "Acme",
                "Gadgets"
        );
        ProductInsertionResult failure = ProductInsertionResult.failure(source, "db unavailable");
        Path output = tempDir.resolve("failed-seller-products.json");

        new JsonFailedCatalogWriter().write(output, List.of(failure));

        List<FailedSellerProductOutputJson> written = new ObjectMapper().readValue(
                output.toFile(),
                new TypeReference<>() {
                }
        );

        assertThat(written).hasSize(1);
        FailedSellerProductOutputJson item = written.get(0);
        assertThat(item.getId()).isEqualTo("seller-1");
        assertThat(item.getSellerName()).isEqualTo("MegaStore");
        assertThat(item.getName()).isEqualTo("Broken Product");
        assertThat(item.getBrand()).isEqualTo("Acme");
        assertThat(item.getCategory()).isEqualTo("Gadgets");
        assertThat(item.getErrorMessage()).isEqualTo("db unavailable");
    }
}
