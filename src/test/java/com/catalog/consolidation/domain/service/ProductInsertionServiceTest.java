package com.catalog.consolidation.domain.service;

import com.catalog.consolidation.domain.model.Availability;
import com.catalog.consolidation.domain.model.Product;
import com.catalog.consolidation.domain.model.ProductInsertionResult;
import com.catalog.consolidation.domain.model.Seller;
import com.catalog.consolidation.domain.model.SellerProduct;
import com.catalog.consolidation.domain.repository.ProductRepository;
import com.catalog.consolidation.domain.repository.SellerProductRepository;
import com.catalog.consolidation.domain.repository.SellerRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductInsertionServiceTest {

    @Test
    void shouldReturnFailureWhenRepositoryThrows() {
        SellerProduct input = sellerProduct("fail-1", "MegaStore", "Broken Product", "Acme", "Gadgets");
        ProductInsertionService service = new ProductInsertionService(
                new ProductNormalizationService(),
                new InMemorySellerRepository(),
                product -> {
                    throw new IllegalStateException("db unavailable");
                },
                productLinkedToSeller -> true
        );

        ProductInsertionResult result = service.insert(input);

        assertThat(result.failed()).isTrue();
        assertThat(result.errorMessage()).isEqualTo("db unavailable");
        assertThat(result.failedSellerProduct()).isEqualTo(input);
        assertThat(result.product()).isNull();
        assertThat(result.inserted()).isFalse();
        assertThat(result.productLinkedToSeller()).isFalse();
    }

    @Test
    void shouldKeepNullErrorMessageWhenExceptionHasNoMessage() {
        SellerProduct input = sellerProduct("fail-2", "MegaStore", "Broken Product", "Acme", "Gadgets");
        ProductInsertionService service = new ProductInsertionService(
                new ProductNormalizationService(),
                new InMemorySellerRepository(),
                product -> {
                    throw new RuntimeException();
                },
                productLinkedToSeller -> true
        );

        ProductInsertionResult result = service.insert(input);

        assertThat(result.failed()).isTrue();
        assertThat(result.errorMessage()).isNull();
        assertThat(result.failedSellerProduct()).isEqualTo(input);
    }

    @Test
    void shouldContinueProcessingAfterFailure() {
        SellerProduct failing = sellerProduct("fail-1", "MegaStore", "Broken Product", "Acme", "Gadgets");
        SellerProduct succeeding = sellerProduct("ok-1", "MegaStore", "Good Product", "Acme", "Gadgets");

        ProductInsertionService service = new ProductInsertionService(
                new ProductNormalizationService(),
                new InMemorySellerRepository(),
                new SelectiveProductRepository(),
                productLinkedToSeller -> true
        );

        ProductInsertionResult failure = service.insert(failing);
        ProductInsertionResult success = service.insert(succeeding);

        assertThat(failure.failed()).isTrue();
        assertThat(success.failed()).isFalse();
        assertThat(success.inserted()).isTrue();
        assertThat(success.productLinkedToSeller()).isTrue();
        assertThat(success.product().getName()).isEqualTo("Good Product");
    }

    private static SellerProduct sellerProduct(String id, String sellerName, String name, String brand, String category) {
        return new SellerProduct(new Seller(sellerName, null), id, name, brand, category);
    }

    private static final class InMemorySellerRepository implements SellerRepository {
        private long nextId = 1L;

        @Override
        public Seller insertIfNotExistsAndFetch(Seller seller) {
            Seller persisted = new Seller(seller.getName(), seller.getNormalizedName());
            persisted.setId(nextId++);
            return persisted;
        }
    }

    private static final class SelectiveProductRepository implements ProductRepository {
        private long nextId = 1L;

        @Override
        public ProductInsertionResult insertIfNotExistsAndFetch(Product product) {
            if ("Broken Product".equals(product.getName())) {
                throw new IllegalStateException("cannot insert broken product");
            }
            Product persisted = new Product(
                    product.getName(),
                    product.getBrand(),
                    product.getCategory(),
                    product.getNormalizedProductName(),
                    product.getNormalizedBrand(),
                    Availability.PENDING
            );
            persisted.setId(nextId++);
            return new ProductInsertionResult(persisted, true, false);
        }
    }
}
