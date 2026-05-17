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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.epam.agents.dto.request.CreateProductRequest;
import com.epam.agents.dto.request.UpdateProductRequest;
import com.epam.agents.dto.response.ProductResponse;
import com.epam.agents.entity.Product;
import com.epam.agents.exception.DuplicateResourceException;
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

        productResponse = new ProductResponse(1L, "TEST-001", "Test Widget", new BigDecimal("29.99"), product.getCreatedAt(), product.getUpdatedAt());
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
        given(productRepository.findAll(pageable)).willReturn(productPage);
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
        given(productRepository.findAll(pageable)).willReturn(Page.empty(pageable));

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

        then(productRepository).should(never()).delete(any());
    }
}
