package com.epam.agents.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.epam.agents.entity.Product;
import com.epam.agents.repository.specification.ProductSpecifications;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void withFilters_shouldMatchNameCaseInsensitivelyAndIncludePriceBounds() {
        productRepository.saveAll(List.of(product("MOUSE-001", "Wireless Mouse", "10.00"), product("MOUSE-002", "Gaming Mouse", "20.00"), product("MOUSE-003", "Travel Mouse", "30.01"), product("KEYBOARD-001", "Wireless Keyboard", "15.00")));

        Page<Product> results = productRepository.findAll(ProductSpecifications.withFilters("MOUSE", new BigDecimal("10.00"), new BigDecimal("30.00")), Pageable.unpaged());

        assertThat(results.getContent()).extracting(Product::getSku).containsExactlyInAnyOrder("MOUSE-001", "MOUSE-002");
    }

    @Test
    void withFilters_shouldSearchSkuAndApplySinglePriceBound() {
        productRepository.saveAll(List.of(product("USB-RECEIVER", "Adapter", "19.99"), product("USB-CABLE", "Cable", "25.00"), product("CASE-001", "Storage Case", "9.99")));

        Page<Product> searchResults = productRepository.findAll(ProductSpecifications.withFilters("receiver", null, null), Pageable.unpaged());
        Page<Product> priceResults = productRepository.findAll(ProductSpecifications.withFilters(null, new BigDecimal("20.00"), null), Pageable.unpaged());

        assertThat(searchResults.getContent()).extracting(Product::getSku).containsExactly("USB-RECEIVER");
        assertThat(priceResults.getContent()).extracting(Product::getSku).containsExactly("USB-CABLE");
    }

    @Test
    void withFilters_shouldTreatSearchWildcardsAsLiteralCharacters() {
        productRepository.saveAll(List.of(product("SALE-001", "50% Off", "12.00"), product("SALE-002", "50X Off", "12.00")));

        Page<Product> results = productRepository.findAll(ProductSpecifications.withFilters("%", null, null), Pageable.unpaged());

        assertThat(results.getContent()).extracting(Product::getSku).containsExactly("SALE-001");
    }

    private Product product(String sku, String name, String price) {
        Product product = new Product();
        product.setSku(sku);
        product.setName(name);
        product.setPrice(new BigDecimal(price));
        return product;
    }
}
