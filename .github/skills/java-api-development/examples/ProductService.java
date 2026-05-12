package com.example.catalog.service;

import com.example.catalog.dto.CreateProductRequest;
import com.example.catalog.dto.ProductResponse;
import com.example.catalog.dto.PagedResponse;
import com.example.catalog.entity.Product;
import com.example.catalog.exception.ResourceNotFoundException;
import com.example.catalog.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // ---------- Read ----------
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getProducts(String category, int page, int pageSize) {
        PageRequest pageRequest = PageRequest.of(page - 1, pageSize, Sort.by("createdAt").descending());

        Page<Product> productPage = (category != null && !category.isBlank())
            ? productRepository.findByCategory(category, pageRequest)
            : productRepository.findAll(pageRequest);

        var items = productPage.getContent().stream()
            .map(this::toResponse)
            .toList();

        return new PagedResponse<>(
            items,
            productPage.getNumber() + 1,
            productPage.getSize(),
            productPage.getTotalElements(),
            productPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        return toResponse(product);
    }

    // ---------- Write ----------
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        Product product = new Product(request.name(), request.price(), request.category());
        product.setDescription(request.description());

        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, CreateProductRequest request) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setCategory(request.category());

        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product", id);
        }
        productRepository.deleteById(id);
    }

    // ---------- Mapper ----------
    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getCategory(),
            product.isInStock(),
            product.getCreatedAt().toString(),
            product.getUpdatedAt().toString()
        );
    }
}
