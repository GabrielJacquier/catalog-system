package com.catalog.consolidation.infrastructure.json;

import com.catalog.consolidation.domain.model.SellerProduct;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JsonCatalogReaderTest {

    private final JsonCatalogReader reader = new JsonCatalogReader();

    @TempDir
    Path tempDir;

    @Test
    void shouldReadCompleteSellerProduct() throws Exception {
        Path input = writeJson("""
                [
                  {
                    "Id": "seller-1",
                    "SellerName": "MegaStore",
                    "Name": "Smartphone Galaxy S23",
                    "Brand": "Samsung",
                    "Category": "Electronics"
                  }
                ]
                """);

        List<SellerProduct> products = reader.read(input);

        assertThat(products).hasSize(1);
        SellerProduct product = products.get(0);
        assertThat(product.sellerProductId()).isEqualTo("seller-1");
        assertThat(product.seller().getName()).isEqualTo("MegaStore");
        assertThat(product.sellerProductName()).isEqualTo("Smartphone Galaxy S23");
        assertThat(product.sellerBrand()).isEqualTo("Samsung");
        assertThat(product.sellerCategory()).isEqualTo("Electronics");
    }

    @Test
    void shouldIgnoreUnknownFields() throws Exception {
        Path input = writeJson("""
                [
                  {
                    "Id": "seller-1",
                    "SellerName": "MegaStore",
                    "Name": "Smartphone Galaxy S23",
                    "Brand": "Samsung",
                    "Category": "Electronics",
                    "Price": 1999.90,
                    "ExtraField": "ignored"
                  }
                ]
                """);

        List<SellerProduct> products = reader.read(input);

        assertThat(products).hasSize(1);
        SellerProduct product = products.get(0);
        assertThat(product.sellerProductId()).isEqualTo("seller-1");
        assertThat(product.seller().getName()).isEqualTo("MegaStore");
        assertThat(product.sellerProductName()).isEqualTo("Smartphone Galaxy S23");
        assertThat(product.sellerBrand()).isEqualTo("Samsung");
        assertThat(product.sellerCategory()).isEqualTo("Electronics");
    }

    @Test
    void shouldAllowMissingFieldsAsNull() throws Exception {
        Path input = writeJson("""
                [
                  {
                    "Id": "seller-2",
                    "SellerName": "StoreB",
                    "Name": "Generic Product"
                  }
                ]
                """);

        List<SellerProduct> products = reader.read(input);

        assertThat(products).hasSize(1);
        SellerProduct product = products.get(0);
        assertThat(product.sellerProductId()).isEqualTo("seller-2");
        assertThat(product.seller().getName()).isEqualTo("StoreB");
        assertThat(product.sellerProductName()).isEqualTo("Generic Product");
        assertThat(product.sellerBrand()).isNull();
        assertThat(product.sellerCategory()).isNull();
    }

    private Path writeJson(String content) throws Exception {
        Path file = tempDir.resolve("seller-products.json");
        Files.writeString(file, content);
        return file;
    }
}
