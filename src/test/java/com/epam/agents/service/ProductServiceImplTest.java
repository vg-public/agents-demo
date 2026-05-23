package com.epam.agents.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.epam.agents.dto.request.BulkPriceUpdateRequest;
import com.epam.agents.dto.request.CreateProductRequest;
import com.epam.agents.dto.request.PriceUpdateEntry;
import com.epam.agents.dto.request.UpdateProductRequest;
import com.epam.agents.dto.response.BulkPriceUpdateResponse;
import com.epam.agents.dto.response.ProductResponse;
import com.epam.agents.entity.Product;
import com.epam.agents.exception.AlreadyArchivedException;
import com.epam.agents.exception.DuplicateResourceException;
import com.epam.agents.exception.InvalidSearchCriteriaException;
import com.epam.agents.exception.ResourceNotFoundException;
import com.epam.agents.mapper.ProductMapper;
import com.epam.agents.repository.ProductRepository;
import com.epam.agents.service.impl.ProductServiceImpl;

/**
 * Unit tests for {@link ProductServiceImpl}.
 *
 * <p>
 * Uses Mockito for all dependencies; no Spring context is loaded.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    private ProductServiceImpl productService;

    private Product product;
    private ProductResponse productResponse;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(productRepository, productMapper);

        product = new Product();
        product.setId(1L);
        product.setName("Test Widget");
        product.setPrice(new BigDecimal("29.99"));
        product.setSku("TEST-001");
        product.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        product.setUpdatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));

        productResponse = new ProductResponse(1L, "TEST-001", "Test Widget", new BigDecimal("29.99"), false, product.getCreatedAt(), product.getUpdatedAt());
    }

    // ─── getById ────────────────────────────────────────────────────────────

    @Test
    void getById_shouldReturnProductResponse_whenProductExists() {
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(productMapper.toResponse(product)).willReturn(productResponse);

        ProductResponse result = productService.getById(1L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.sku()).isEqualTo("TEST-001");
        assertThat(result.name()).isEqualTo("Test Widget");
    }

    @Test
    void getById_shouldThrowResourceNotFoundException_whenProductDoesNotExist() {
        given(productRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(99L)).isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("Product").hasMessageContaining("99");
    }

    // ─── getAll ─────────────────────────────────────────────────────────────

    @Test
    void getAll_shouldReturnPageOfProductResponses() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Product> productPage = new PageImpl<>(List.of(product), pageable, 1);
        given(productRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(productPage);
        given(productMapper.toResponse(product)).willReturn(productResponse);

        Page<ProductResponse> result = productService.getAll(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).sku()).isEqualTo("TEST-001");
    }

    @Test
    void getAll_shouldReturnEmptyPage_whenNoProductsExist() {
        Pageable pageable = PageRequest.of(0, 20);
        given(productRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(Page.empty(pageable));

        Page<ProductResponse> result = productService.getAll(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    // ─── create ─────────────────────────────────────────────────────────────

    @Test
    void create_shouldReturnCreatedProduct_whenSkuIsUnique() {
        CreateProductRequest request = new CreateProductRequest("TEST-001", "Test Widget", new BigDecimal("29.99"));
        given(productRepository.existsBySku("TEST-001")).willReturn(false);
        given(productMapper.toEntity(request)).willReturn(product);
        given(productRepository.save(product)).willReturn(product);
        given(productMapper.toResponse(product)).willReturn(productResponse);

        ProductResponse result = productService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.sku()).isEqualTo("TEST-001");
        then(productRepository).should().save(product);
    }

    @Test
    void create_shouldThrowDuplicateResourceException_whenSkuAlreadyExists() {
        CreateProductRequest request = new CreateProductRequest("TEST-001", "Test Widget", new BigDecimal("29.99"));
        given(productRepository.existsBySku("TEST-001")).willReturn(true);

        assertThatThrownBy(() -> productService.create(request)).isInstanceOf(DuplicateResourceException.class).hasMessageContaining("TEST-001");

        then(productRepository).should(never()).save(any());
    }

    // ─── update ─────────────────────────────────────────────────────────────

    @Test
    void update_shouldReturnUpdatedProduct_whenProductExists() {
        UpdateProductRequest request = new UpdateProductRequest("Updated Widget", new BigDecimal("39.99"));
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(productRepository.save(product)).willReturn(product);
        given(productMapper.toResponse(product)).willReturn(productResponse);

        ProductResponse result = productService.update(1L, request);

        assertThat(result).isNotNull();
        then(productMapper).should().updateEntity(request, product);
        then(productRepository).should().save(product);
    }

    @Test
    void update_shouldThrowResourceNotFoundException_whenProductDoesNotExist() {
        UpdateProductRequest request = new UpdateProductRequest("Updated Widget", new BigDecimal("39.99"));
        given(productRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(99L, request)).isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("Product").hasMessageContaining("99");

        then(productRepository).should(never()).save(any());
    }

    // ─── delete ─────────────────────────────────────────────────────────────

    @Test
    void delete_shouldDeleteProduct_whenProductExists() {
        given(productRepository.findById(1L)).willReturn(Optional.of(product));

        productService.delete(1L);

        then(productRepository).should().delete(product);
    }

    @Test
    void delete_shouldThrowResourceNotFoundException_whenProductDoesNotExist() {
        given(productRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete(99L)).isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("Product").hasMessageContaining("99");

        then(productRepository).should(never()).delete(any(Product.class));
    }

    // ─── search ─────────────────────────────────────────────────────────────

    @Nested
    class Search {

        private final Pageable pageable = PageRequest.of(0, 20);

        @Test
        void search_shouldReturnMatchingProducts_whenAllFiltersProvided() {
            Page<Product> productPage = new PageImpl<>(List.of(product));
            given(productRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(productPage);
            given(productMapper.toResponse(product)).willReturn(productResponse);

            Page<ProductResponse> result = productService.search("Widget", new BigDecimal("10.00"), new BigDecimal("50.00"), pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).name()).isEqualTo("Test Widget");
        }

        @Test
        void search_shouldReturnAllProducts_whenNoFiltersProvided() {
            Page<Product> productPage = new PageImpl<>(List.of(product));
            given(productRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(productPage);
            given(productMapper.toResponse(product)).willReturn(productResponse);

            Page<ProductResponse> result = productService.search(null, null, null, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        void search_shouldReturnEmptyPage_whenNoProductsMatch() {
            given(productRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(Page.empty());

            Page<ProductResponse> result = productService.search("nonexistent", null, null, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        void search_shouldThrowInvalidSearchCriteriaException_whenMinPriceGreaterThanMaxPrice() {
            assertThatThrownBy(() -> productService.search(null, new BigDecimal("100.00"), new BigDecimal("50.00"), pageable)).isInstanceOf(InvalidSearchCriteriaException.class).hasMessageContaining("minPrice must not be greater than maxPrice");

            then(productRepository).should(never()).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        void search_shouldNotThrow_whenMinPriceEqualsMaxPrice() {
            BigDecimal samePrice = new BigDecimal("25.00");
            Page<Product> productPage = new PageImpl<>(List.of(product));
            given(productRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(productPage);
            given(productMapper.toResponse(product)).willReturn(productResponse);

            Page<ProductResponse> result = productService.search(null, samePrice, samePrice, pageable);

            assertThat(result).isNotNull();
        }

        @Test
        void search_shouldApplyOnlyKeywordFilter_whenPricesAreNull() {
            Page<Product> productPage = new PageImpl<>(List.of(product));
            given(productRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(productPage);
            given(productMapper.toResponse(product)).willReturn(productResponse);

            Page<ProductResponse> result = productService.search("Widget", null, null, pageable);

            assertThat(result.getContent()).hasSize(1);
        }
    }

    // ─── bulkUpdatePrices ────────────────────────────────────────────────────

    @Nested
    class BulkUpdatePrices {

        private Product productA;
        private Product productB;

        @BeforeEach
        void setUpProducts() {
            productA = new Product();
            productA.setId(10L);
            productA.setSku("WDG-001");
            productA.setName("Widget");
            productA.setPrice(new BigDecimal("15.00"));

            productB = new Product();
            productB.setId(11L);
            productB.setSku("GDG-001");
            productB.setName("Gadget");
            productB.setPrice(new BigDecimal("99.99"));
        }

        @Test
        void bulkUpdatePrices_shouldUpdateAllMatchingProducts() {
            BulkPriceUpdateRequest request = new BulkPriceUpdateRequest(List.of(new PriceUpdateEntry("WDG-001", new BigDecimal("19.99")), new PriceUpdateEntry("GDG-001", new BigDecimal("129.00"))));
            given(productRepository.findAllBySkuIn(List.of("WDG-001", "GDG-001"))).willReturn(List.of(productA, productB));

            BulkPriceUpdateResponse result = productService.bulkUpdatePrices(request);

            assertThat(result.updatedCount()).isEqualTo(2);
            assertThat(result.notFoundSkus()).isEmpty();
            assertThat(result.invalidSkus()).isEmpty();
            assertThat(productA.getPrice()).isEqualByComparingTo("19.99");
            assertThat(productB.getPrice()).isEqualByComparingTo("129.00");
        }

        @Test
        void bulkUpdatePrices_shouldCollectNotFoundSkus_whenSkuDoesNotExist() {
            BulkPriceUpdateRequest request = new BulkPriceUpdateRequest(List.of(new PriceUpdateEntry("WDG-001", new BigDecimal("19.99")), new PriceUpdateEntry("UNKNOWN-SKU", new BigDecimal("49.99"))));
            given(productRepository.findAllBySkuIn(List.of("WDG-001", "UNKNOWN-SKU"))).willReturn(List.of(productA));

            BulkPriceUpdateResponse result = productService.bulkUpdatePrices(request);

            assertThat(result.updatedCount()).isEqualTo(1);
            assertThat(result.notFoundSkus()).containsExactly("UNKNOWN-SKU");
            assertThat(result.invalidSkus()).isEmpty();
        }

        @Test
        void bulkUpdatePrices_shouldCollectInvalidSkus_whenPriceIsZeroOrNegative() {
            BulkPriceUpdateRequest request = new BulkPriceUpdateRequest(List.of(new PriceUpdateEntry("WDG-001", new BigDecimal("19.99")), new PriceUpdateEntry("BAD-ZERO", BigDecimal.ZERO), new PriceUpdateEntry("BAD-NEG", new BigDecimal("-5.00"))));
            given(productRepository.findAllBySkuIn(List.of("WDG-001"))).willReturn(List.of(productA));

            BulkPriceUpdateResponse result = productService.bulkUpdatePrices(request);

            assertThat(result.updatedCount()).isEqualTo(1);
            assertThat(result.invalidSkus()).containsExactlyInAnyOrder("BAD-ZERO", "BAD-NEG");
            assertThat(result.notFoundSkus()).isEmpty();
        }

        @Test
        void bulkUpdatePrices_shouldReturnZeroUpdated_whenAllSkusAreInvalid() {
            BulkPriceUpdateRequest request = new BulkPriceUpdateRequest(List.of(new PriceUpdateEntry("BAD-1", new BigDecimal("-1.00")), new PriceUpdateEntry("BAD-2", BigDecimal.ZERO)));

            BulkPriceUpdateResponse result = productService.bulkUpdatePrices(request);

            assertThat(result.updatedCount()).isZero();
            assertThat(result.invalidSkus()).containsExactlyInAnyOrder("BAD-1", "BAD-2");
            assertThat(result.notFoundSkus()).isEmpty();
            then(productRepository).should(never()).findAllBySkuIn(any());
        }

        @Test
        void bulkUpdatePrices_shouldReturnZeroUpdated_whenAllSkusNotFound() {
            BulkPriceUpdateRequest request = new BulkPriceUpdateRequest(List.of(new PriceUpdateEntry("MISSING-1", new BigDecimal("10.00")), new PriceUpdateEntry("MISSING-2", new BigDecimal("20.00"))));
            given(productRepository.findAllBySkuIn(List.of("MISSING-1", "MISSING-2"))).willReturn(List.of());

            BulkPriceUpdateResponse result = productService.bulkUpdatePrices(request);

            assertThat(result.updatedCount()).isZero();
            assertThat(result.notFoundSkus()).containsExactlyInAnyOrder("MISSING-1", "MISSING-2");
            assertThat(result.invalidSkus()).isEmpty();
        }

        @Test
        void bulkUpdatePrices_shouldHandleMixOfAllOutcomes() {
            BulkPriceUpdateRequest request = new BulkPriceUpdateRequest(List.of(new PriceUpdateEntry("WDG-001", new BigDecimal("19.99")), new PriceUpdateEntry("UNKNOWN-SKU", new BigDecimal("49.99")), new PriceUpdateEntry("BAD-PRICE", new BigDecimal("-5.00"))));
            given(productRepository.findAllBySkuIn(List.of("WDG-001", "UNKNOWN-SKU"))).willReturn(List.of(productA));

            BulkPriceUpdateResponse result = productService.bulkUpdatePrices(request);

            assertThat(result.updatedCount()).isEqualTo(1);
            assertThat(result.notFoundSkus()).containsExactly("UNKNOWN-SKU");
            assertThat(result.invalidSkus()).containsExactly("BAD-PRICE");
        }
    }

    // ─── getBySku ────────────────────────────────────────────────────────────

    @Test
    void getBySku_shouldReturnProductResponse_whenProductExists() {
        given(productRepository.findBySku("TEST-001")).willReturn(Optional.of(product));
        given(productMapper.toResponse(product)).willReturn(productResponse);

        ProductResponse result = productService.getBySku("TEST-001");

        assertThat(result).isNotNull();
        assertThat(result.sku()).isEqualTo("TEST-001");
    }

    @Test
    void getBySku_shouldThrowResourceNotFoundException_whenSkuDoesNotExist() {
        given(productRepository.findBySku("UNKNOWN")).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getBySku("UNKNOWN")).isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("UNKNOWN");
    }

    @Test
    void getBySku_shouldReturnArchivedProduct_whenProductIsArchived() {
        product.setArchived(true);
        ProductResponse archivedResponse = new ProductResponse(1L, "TEST-001", "Test Widget", new BigDecimal("29.99"), true, product.getCreatedAt(), product.getUpdatedAt());
        given(productRepository.findBySku("TEST-001")).willReturn(Optional.of(product));
        given(productMapper.toResponse(product)).willReturn(archivedResponse);

        ProductResponse result = productService.getBySku("TEST-001");

        assertThat(result.archived()).isTrue();
    }

    // ─── archive ─────────────────────────────────────────────────────────────

    @Test
    void archive_shouldSetArchivedTrue_whenProductExistsAndIsActive() {
        ProductResponse archivedResponse = new ProductResponse(1L, "TEST-001", "Test Widget", new BigDecimal("29.99"), true, product.getCreatedAt(), product.getUpdatedAt());
        given(productRepository.findBySku("TEST-001")).willReturn(Optional.of(product));
        given(productRepository.save(product)).willReturn(product);
        given(productMapper.toResponse(product)).willReturn(archivedResponse);

        ProductResponse result = productService.archive("TEST-001");

        assertThat(result.archived()).isTrue();
        assertThat(product.isArchived()).isTrue();
        then(productRepository).should().save(product);
    }

    @Test
    void archive_shouldThrowResourceNotFoundException_whenSkuDoesNotExist() {
        given(productRepository.findBySku("UNKNOWN")).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.archive("UNKNOWN")).isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("UNKNOWN");

        then(productRepository).should(never()).save(any());
    }

    @Test
    void archive_shouldThrowAlreadyArchivedException_whenProductIsAlreadyArchived() {
        product.setArchived(true);
        given(productRepository.findBySku("TEST-001")).willReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.archive("TEST-001")).isInstanceOf(AlreadyArchivedException.class).hasMessageContaining("TEST-001").hasMessageContaining("already archived");

        then(productRepository).should(never()).save(any());
    }
}
