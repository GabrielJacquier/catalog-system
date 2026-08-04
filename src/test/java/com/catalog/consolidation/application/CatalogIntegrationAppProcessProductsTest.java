package com.catalog.consolidation.application;

import com.catalog.consolidation.domain.model.Availability;
import com.catalog.consolidation.domain.model.Product;
import com.catalog.consolidation.domain.model.ProductInsertionResult;
import com.catalog.consolidation.domain.model.Seller;
import com.catalog.consolidation.domain.model.SellerProduct;
import com.catalog.consolidation.domain.service.ProductInsertionService;
import com.catalog.consolidation.domain.service.ProductNormalizationService;
import com.catalog.consolidation.infrastructure.json.JsonCatalogReader;
import com.catalog.consolidation.infrastructure.json.JsonFailedCatalogWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogIntegrationAppProcessProductsTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCountFailuresAndContinueProcessingRemainingItems() {
        SellerProduct failing = sellerProduct("fail-1", "StoreA", "Broken Product", "Acme", "Gadgets");
        SellerProduct succeeding = sellerProduct("ok-1", "StoreB", "Good Product", "Acme", "Gadgets");

        AtomicInteger calls = new AtomicInteger();
        ProductInsertionService service = new ProductInsertionService(
                new ProductNormalizationService(),
                seller -> {
                    Seller persisted = new Seller(seller.getName(), seller.getNormalizedName());
                    persisted.setId(1L);
                    return persisted;
                },
                product -> {
                    if (calls.getAndIncrement() == 0) {
                        throw new IllegalStateException("first item failed");
                    }
                    Product persisted = new Product(
                            product.getName(),
                            product.getBrand(),
                            product.getCategory(),
                            product.getNormalizedProductName(),
                            product.getNormalizedBrand(),
                            Availability.PENDING
                    );
                    persisted.setId(10L);
                    return new ProductInsertionResult(persisted, true, false);
                },
                (productId, sellerProduct) -> true
        );

        CatalogIntegrationApp app = new CatalogIntegrationApp(
                null,
                new JsonCatalogReader(),
                service,
                new JsonFailedCatalogWriter(),
                tempDir.resolve("failed-seller-products.json")
        );

        CatalogIntegrationResult result = app.processProducts(List.of(failing, succeeding));

        assertThat(result.totalProcessed()).isEqualTo(2);
        assertThat(result.itemsFailed()).isEqualTo(1);
        assertThat(result.productsInserted()).isEqualTo(1);
        assertThat(result.sellerLinksCreated()).isEqualTo(1);
        assertThat(result.failures()).hasSize(1);
        assertThat(result.failures().get(0).errorMessage()).isEqualTo("first item failed");
    }

    private static SellerProduct sellerProduct(String id, String sellerName, String name, String brand, String category) {
        return new SellerProduct(new Seller(sellerName, null), id, name, brand, category);
    }
}
