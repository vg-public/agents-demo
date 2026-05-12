package com.example.catalog.service;

import com.example.catalog.dto.CreateProductRequest;
import com.example.catalog.dto.ProductResponse;
import com.example.catalog.dto.PagedResponse;
import com.example.catalog.entity.Product;
import com.example.catalog.exception.ResourceNotFoundException;
import com.example.catalog.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = new Product("Wireless Mouse", new BigDecimal("29.99"), "Electronics");
        sampleProduct.setId(1L);
        sampleProduct.setDescription("Ergonomic wireless mouse");

        // Trigger lifecycle hooks manually for tests
        try {
            var m = Product.class.getDeclaredMethod("onCreate");
            m.setAccessible(true);
            m.invoke(sampleProduct);
        } catch (Exception ignored) {}
    }

    // ---------- getProduct ----------
    @Nested
    class GetProduct {

        @Test
        void returnsProduct_whenFound() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));

            ProductResponse response = productService.getProduct(1L);

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.name()).isEqualTo("Wireless Mouse");
            assertThat(response.price()).isEqualByComparingTo(new BigDecimal("29.99"));
            verify(productRepository).findById(1L);
        }

        @Test
        void throwsNotFound_whenMissing() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product")
                .hasMessageContaining("99");
        }
    }

    // ---------- getProducts ----------
    @Nested
    class GetProducts {

        @Test
        void returnsPagedProducts() {
            Page<Product> page = new PageImpl<>(
                List.of(sampleProduct),
                PageRequest.of(0, 20, Sort.by("createdAt").descending()),
                1
            );
            when(productRepository.findAll(any(Pageable.class))).thenReturn(page);

            PagedResponse<ProductResponse> result = productService.getProducts(null, 1, 20);

            assertThat(result.items()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.page()).isEqualTo(1);
        }

        @Test
        void filtersByCategory_whenProvided() {
            Page<Product> page = new PageImpl<>(List.of(sampleProduct));
            when(productRepository.findByCategory(eq("Electronics"), any(Pageable.class)))
                .thenReturn(page);

            PagedResponse<ProductResponse> result = productService.getProducts("Electronics", 1, 20);

            assertThat(result.items()).hasSize(1);
            verify(productRepository).findByCategory(eq("Electronics"), any(Pageable.class));
            verify(productRepository, never()).findAll(any(Pageable.class));
        }
    }

    // ---------- createProduct ----------
    @Nested
    class CreateProduct {

        @Test
        void savesAndReturnsNewProduct() {
            CreateProductRequest request = new CreateProductRequest(
                "Keyboard", "Mechanical keyboard", new BigDecimal("79.99"), "Electronics"
            );

            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
                Product p = invocation.getArgument(0);
                p.setId(2L);
                try {
                    var m = Product.class.getDeclaredMethod("onCreate");
                    m.setAccessible(true);
                    m.invoke(p);
                } catch (Exception ignored) {}
                return p;
            });

            ProductResponse response = productService.createProduct(request);

            assertThat(response.id()).isEqualTo(2L);
            assertThat(response.name()).isEqualTo("Keyboard");
            verify(productRepository).save(any(Product.class));
        }
    }

    // ---------- deleteProduct ----------
    @Nested
    class DeleteProduct {

        @Test
        void deletesExistingProduct() {
            when(productRepository.existsById(1L)).thenReturn(true);

            productService.deleteProduct(1L);

            verify(productRepository).deleteById(1L);
        }

        @Test
        void throwsNotFound_whenDeletingMissing() {
            when(productRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
