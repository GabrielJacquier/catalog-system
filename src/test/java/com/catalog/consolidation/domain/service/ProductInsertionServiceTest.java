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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductInsertionServiceTest {

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SellerProductRepository sellerProductRepository;

    private ProductInsertionService productInsertionService;

    @BeforeEach
    void setUp() {
        productInsertionService = new ProductInsertionService(
                new ProductNormalizationService(),
                sellerRepository,
                productRepository,
                sellerProductRepository
        );
    }

    @Test
    void shouldInsertNewProductThenSkipDuplicateLinkAndReuseMatchForAnotherSeller() {
        Seller megaStore = seller(1L, "MegaStore", "MEGASTORE");
        Seller otherStore = seller(2L, "OtherStore", "OTHERSTORE");
        Product canonical = product(10L, "Good Product", "Acme", "Gadgets", "good product", "acme");

        when(sellerRepository.insertIfNotExistsAndFetch(any(Seller.class)))
                .thenReturn(megaStore, megaStore, otherStore);
        when(productRepository.insertIfNotExistsAndFetch(any(Product.class)))
                .thenReturn(
                        new ProductInsertionResult(canonical, true, false),
                        new ProductInsertionResult(canonical, false, false),
                        new ProductInsertionResult(canonical, false, false)
                );
        when(sellerProductRepository.link(any(ProductLinkedToSeller.class)))
                .thenReturn(true, false, true);

        ProductInsertionResult inserted = productInsertionService.insert(
                sellerProduct("id-1", "MegaStore", "Good Product", "Acme", "Gadgets")
        );
        assertThat(inserted.failed()).isFalse();
        assertThat(inserted.inserted()).isTrue();
        assertThat(inserted.productLinkedToSeller()).isTrue();
        assertThat(inserted.product().getAvailability()).isEqualTo(Availability.PENDING);

        ProductInsertionResult skippedLink = productInsertionService.insert(
                sellerProduct("id-1", "MegaStore", "Good Product", "Acme", "Gadgets")
        );
        assertThat(skippedLink.failed()).isFalse();
        assertThat(skippedLink.inserted()).isFalse();
        assertThat(skippedLink.productLinkedToSeller()).isFalse();
        assertThat(skippedLink.product().getId()).isEqualTo(10L);

        ProductInsertionResult matched = productInsertionService.insert(
                sellerProduct("id-2", "OtherStore", "Good  Product", "Acme", "Other")
        );
        assertThat(matched.failed()).isFalse();
        assertThat(matched.inserted()).isFalse();
        assertThat(matched.productLinkedToSeller()).isTrue();
        assertThat(matched.product().getId()).isEqualTo(10L);

        ArgumentCaptor<ProductLinkedToSeller> linkCaptor = ArgumentCaptor.forClass(ProductLinkedToSeller.class);
        verify(sellerProductRepository, times(3)).link(linkCaptor.capture());
        assertThat(linkCaptor.getAllValues()).extracting(ProductLinkedToSeller::sellerProductId)
                .containsExactly("id-1", "id-1", "id-2");
    }

    @Test
    void shouldReturnFailureWhenRepositoryThrows() {
        SellerProduct input = sellerProduct("fail-1", "MegaStore", "Broken Product", "Acme", "Gadgets");
        when(sellerRepository.insertIfNotExistsAndFetch(any(Seller.class)))
                .thenReturn(seller(1L, "MegaStore", "MEGASTORE"));
        when(productRepository.insertIfNotExistsAndFetch(any(Product.class)))
                .thenThrow(new IllegalStateException("db unavailable"));

        ProductInsertionResult result = productInsertionService.insert(input);

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
        when(sellerRepository.insertIfNotExistsAndFetch(any(Seller.class)))
                .thenReturn(seller(1L, "MegaStore", "MEGASTORE"));
        when(productRepository.insertIfNotExistsAndFetch(any(Product.class)))
                .thenThrow(new RuntimeException());

        ProductInsertionResult result = productInsertionService.insert(input);

        assertThat(result.failed()).isTrue();
        assertThat(result.errorMessage()).isNull();
        assertThat(result.failedSellerProduct()).isEqualTo(input);
    }

    private static SellerProduct sellerProduct(String id, String sellerName, String name, String brand, String category) {
        return new SellerProduct(new Seller(sellerName, null), id, name, brand, category);
    }

    private static Seller seller(long id, String name, String normalizedName) {
        Seller seller = new Seller(name, normalizedName);
        seller.setId(id);
        return seller;
    }

    private static Product product(long id, String name, String brand, String category,
                                   String normalizedName, String normalizedBrand) {
        Product product = new Product(name, brand, category, normalizedName, normalizedBrand, Availability.PENDING);
        product.setId(id);
        return product;
    }
}
