package com.catalog.consolidation.domain.service;

import com.catalog.consolidation.domain.model.Availability;
import com.catalog.consolidation.domain.model.Product;
import com.catalog.consolidation.domain.model.ProductInsertionResult;
import com.catalog.consolidation.domain.model.ProductLinkedToSeller;
import com.catalog.consolidation.domain.model.Seller;
import com.catalog.consolidation.domain.model.SellerProduct;
import com.catalog.consolidation.domain.repository.ProductRepository;
import com.catalog.consolidation.domain.repository.SellerProductRepository;
import com.catalog.consolidation.domain.repository.SellerRepository;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProductInsertionServiceTest {

    @Test
    void shouldInsertMatchAndPropagateLinkOutcome() {
        InMemorySellerRepository sellers = new InMemorySellerRepository();
        InMemoryProductRepository products = new InMemoryProductRepository();
        InMemorySellerProductRepository links = new InMemorySellerProductRepository();
        ProductInsertionService service = new ProductInsertionService(
                new ProductNormalizationService(),
                sellers,
                products,
                links
        );

        SellerProduct first = sellerProduct("id-1", "MegaStore", "Good Product", "Acme", "Gadgets");
        ProductInsertionResult inserted = service.insert(first);

        assertThat(inserted.failed()).isFalse();
        assertThat(inserted.inserted()).isTrue();
        assertThat(inserted.productLinkedToSeller()).isTrue();
        assertThat(inserted.product().getAvailability()).isEqualTo(Availability.PENDING);

        SellerProduct sameListing = sellerProduct("id-1", "MegaStore", "Good Product", "Acme", "Gadgets");
        ProductInsertionResult skippedLink = service.insert(sameListing);

        assertThat(skippedLink.failed()).isFalse();
        assertThat(skippedLink.inserted()).isFalse();
        assertThat(skippedLink.productLinkedToSeller()).isFalse();
        assertThat(skippedLink.product().getId()).isEqualTo(inserted.product().getId());

        SellerProduct otherSeller = sellerProduct("id-2", "OtherStore", "Good  Product", "Acme", "Other");
        ProductInsertionResult matched = service.insert(otherSeller);

        assertThat(matched.failed()).isFalse();
        assertThat(matched.inserted()).isFalse();
        assertThat(matched.productLinkedToSeller()).isTrue();
        assertThat(matched.product().getId()).isEqualTo(inserted.product().getId());
        assertThat(sellers.size()).isEqualTo(2);
        assertThat(products.size()).isEqualTo(1);
        assertThat(links.size()).isEqualTo(2);
    }

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

    private static SellerProduct sellerProduct(String id, String sellerName, String name, String brand, String category) {
        return new SellerProduct(new Seller(sellerName, null), id, name, brand, category);
    }

    private static final class InMemorySellerRepository implements SellerRepository {
        private long nextId = 1L;
        private final Map<String, Seller> byNormalizedName = new HashMap<>();

        @Override
        public Seller insertIfNotExistsAndFetch(Seller seller) {
            Seller existing = byNormalizedName.get(seller.getNormalizedName());
            if (existing != null) {
                return existing;
            }
            Seller persisted = new Seller(seller.getName(), seller.getNormalizedName());
            persisted.setId(nextId++);
            byNormalizedName.put(persisted.getNormalizedName(), persisted);
            return persisted;
        }

        int size() {
            return byNormalizedName.size();
        }
    }

    private static final class InMemoryProductRepository implements ProductRepository {
        private long nextId = 1L;
        private final Map<String, Product> byKey = new HashMap<>();

        @Override
        public ProductInsertionResult insertIfNotExistsAndFetch(Product product) {
            String key = product.getNormalizedProductName() + "|" + product.getNormalizedBrand();
            Product existing = byKey.get(key);
            if (existing != null) {
                return new ProductInsertionResult(existing, false, false);
            }
            Product persisted = new Product(
                    product.getName(),
                    product.getBrand(),
                    product.getCategory(),
                    product.getNormalizedProductName(),
                    product.getNormalizedBrand(),
                    product.getAvailability()
            );
            persisted.setId(nextId++);
            byKey.put(key, persisted);
            return new ProductInsertionResult(persisted, true, false);
        }

        int size() {
            return byKey.size();
        }
    }

    private static final class InMemorySellerProductRepository implements SellerProductRepository {
        private final Set<String> linkedKeys = new HashSet<>();

        @Override
        public boolean link(ProductLinkedToSeller productLinkedToSeller) {
            String key = productLinkedToSeller.seller().getId() + "|" + productLinkedToSeller.sellerProductId();
            return linkedKeys.add(key);
        }

        int size() {
            return linkedKeys.size();
        }
    }
}
