package com.epam.agents.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import com.epam.agents.entity.Product;

/**
 * {@code @DataJpaTest} slice tests for {@link ProductSpecification} predicates.
 *
 * <p>
 * Verifies that each specification predicate correctly filters the in-memory
 * H2 database used during testing.
 * </p>
 */
@DataJpaTest
class ProductSpecificationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProductRepository productRepository;

    private Product widgetCheap;
    private Product gadgetExpensive;

    @BeforeEach
    void setUp() {
        widgetCheap = new Product();
        widgetCheap.setName("Blue Widget");
        widgetCheap.setSku("WDG-001");
        widgetCheap.setPrice(new BigDecimal("15.00"));
        entityManager.persistAndFlush(widgetCheap);

        gadgetExpensive = new Product();
        gadgetExpensive.setName("Smart Gadget");
        gadgetExpensive.setSku("GDG-001");
        gadgetExpensive.setPrice(new BigDecimal("99.99"));
        entityManager.persistAndFlush(gadgetExpensive);
    }

    @Nested
    class NameContains {

        @Test
        void nameContains_shouldReturnMatchingProducts_whenKeywordMatchesPartially() {
            Specification<Product> spec = ProductSpecification.nameContains("widget");

            List<Product> results = productRepository.findAll(spec);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getSku()).isEqualTo("WDG-001");
        }

        @Test
        void nameContains_shouldBeCaseInsensitive() {
            Specification<Product> spec = ProductSpecification.nameContains("BLUE");

            List<Product> results = productRepository.findAll(spec);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getName()).isEqualTo("Blue Widget");
        }

        @Test
        void nameContains_shouldReturnAllProducts_whenKeywordIsNull() {
            Specification<Product> spec = ProductSpecification.nameContains(null);

            List<Product> results = productRepository.findAll(spec);

            assertThat(results).hasSize(2);
        }

        @Test
        void nameContains_shouldReturnAllProducts_whenKeywordIsBlank() {
            Specification<Product> spec = ProductSpecification.nameContains("   ");

            List<Product> results = productRepository.findAll(spec);

            assertThat(results).hasSize(2);
        }

        @Test
        void nameContains_shouldReturnEmpty_whenKeywordMatchesNothing() {
            Specification<Product> spec = ProductSpecification.nameContains("nonexistent");

            List<Product> results = productRepository.findAll(spec);

            assertThat(results).isEmpty();
        }
    }

    @Nested
    class PriceGreaterThanOrEqual {

        @Test
        void priceGte_shouldReturnProductsAboveMinPrice() {
            Specification<Product> spec = ProductSpecification.priceGreaterThanOrEqual(new BigDecimal("50.00"));

            List<Product> results = productRepository.findAll(spec);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getSku()).isEqualTo("GDG-001");
        }

        @Test
        void priceGte_shouldReturnAllProducts_whenMinPriceIsNull() {
            Specification<Product> spec = ProductSpecification.priceGreaterThanOrEqual(null);

            List<Product> results = productRepository.findAll(spec);

            assertThat(results).hasSize(2);
        }

        @Test
        void priceGte_shouldIncludeProductWithExactMinPrice() {
            Specification<Product> spec = ProductSpecification.priceGreaterThanOrEqual(new BigDecimal("15.00"));

            List<Product> results = productRepository.findAll(spec);

            assertThat(results).hasSize(2);
        }
    }

    @Nested
    class PriceLessThanOrEqual {

        @Test
        void priceLte_shouldReturnProductsBelowMaxPrice() {
            Specification<Product> spec = ProductSpecification.priceLessThanOrEqual(new BigDecimal("50.00"));

            List<Product> results = productRepository.findAll(spec);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getSku()).isEqualTo("WDG-001");
        }

        @Test
        void priceLte_shouldReturnAllProducts_whenMaxPriceIsNull() {
            Specification<Product> spec = ProductSpecification.priceLessThanOrEqual(null);

            List<Product> results = productRepository.findAll(spec);

            assertThat(results).hasSize(2);
        }

        @Test
        void priceLte_shouldIncludeProductWithExactMaxPrice() {
            Specification<Product> spec = ProductSpecification.priceLessThanOrEqual(new BigDecimal("99.99"));

            List<Product> results = productRepository.findAll(spec);

            assertThat(results).hasSize(2);
        }
    }

    @Nested
    class CombinedSpecifications {

        @Test
        void combinedSpecs_shouldFilterByKeywordAndPriceRange() {
            Specification<Product> spec = Specification.where(ProductSpecification.nameContains("widget")).and(ProductSpecification.priceGreaterThanOrEqual(new BigDecimal("10.00"))).and(ProductSpecification.priceLessThanOrEqual(new BigDecimal("20.00")));

            Page<Product> page = productRepository.findAll(spec, PageRequest.of(0, 10));

            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getSku()).isEqualTo("WDG-001");
        }

        @Test
        void combinedSpecs_shouldReturnEmpty_whenPriceRangeExcludesAllProducts() {
            Specification<Product> spec = Specification.where(ProductSpecification.priceGreaterThanOrEqual(new BigDecimal("200.00"))).and(ProductSpecification.priceLessThanOrEqual(new BigDecimal("300.00")));

            List<Product> results = productRepository.findAll(spec);

            assertThat(results).isEmpty();
        }

        @Test
        void combinedSpecs_shouldReturnAll_whenAllFiltersAreNull() {
            Specification<Product> spec = Specification.where(ProductSpecification.nameContains(null)).and(ProductSpecification.priceGreaterThanOrEqual(null)).and(ProductSpecification.priceLessThanOrEqual(null));

            List<Product> results = productRepository.findAll(spec);

            assertThat(results).hasSize(2);
        }
    }

    @Nested
    class IsActive {

        @Test
        void isActive_shouldReturnOnlyNonArchivedProducts() {
            widgetCheap.setArchived(true);
            entityManager.persistAndFlush(widgetCheap);

            Specification<Product> spec = ProductSpecification.isActive();

            List<Product> results = productRepository.findAll(spec);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getSku()).isEqualTo("GDG-001");
        }

        @Test
        void isActive_shouldReturnAllProducts_whenNoneAreArchived() {
            Specification<Product> spec = ProductSpecification.isActive();

            List<Product> results = productRepository.findAll(spec);

            assertThat(results).hasSize(2);
        }

        @Test
        void isActive_shouldReturnEmpty_whenAllProductsAreArchived() {
            widgetCheap.setArchived(true);
            gadgetExpensive.setArchived(true);
            entityManager.persistAndFlush(widgetCheap);
            entityManager.persistAndFlush(gadgetExpensive);

            Specification<Product> spec = ProductSpecification.isActive();

            List<Product> results = productRepository.findAll(spec);

            assertThat(results).isEmpty();
        }

        @Test
        void isActive_combinedWithNameFilter_shouldOnlyReturnActiveMatchingProducts() {
            widgetCheap.setArchived(true);
            entityManager.persistAndFlush(widgetCheap);

            Specification<Product> spec = Specification.where(ProductSpecification.isActive()).and(ProductSpecification.nameContains("widget"));

            List<Product> results = productRepository.findAll(spec);

            assertThat(results).isEmpty();
        }
    }
}
