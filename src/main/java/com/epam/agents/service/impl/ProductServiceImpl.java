package com.epam.agents.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.epam.agents.dto.request.CreateProductRequest;
import com.epam.agents.dto.request.UpdateProductRequest;
import com.epam.agents.dto.response.ProductResponse;
import com.epam.agents.entity.Product;
import com.epam.agents.exception.DuplicateResourceException;
import com.epam.agents.exception.ResourceNotFoundException;
import com.epam.agents.mapper.ProductMapper;
import com.epam.agents.repository.ProductRepository;
import com.epam.agents.service.ProductService;

/**
 * Default implementation of {@link ProductService}.
 *
 * <p>
 * Read operations run in a read-only transaction to enable Hibernate flush-mode
 * optimisation. Write operations run in a full read-write transaction.
 * </p>
 */
@Service
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    /**
     * Constructor injection — the only injection style used in this project.
     *
     * @param productRepository
     *            JPA repository for products
     * @param productMapper
     *            MapStruct mapper between entity and DTOs
     */
    public ProductServiceImpl(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        log.debug("Fetching product by id: {}", id);
        Product product = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAll(Pageable pageable) {
        log.debug("Fetching all products, page: {}", pageable);
        return productRepository.findAll(pageable).map(productMapper::toResponse);
    }

    @Override
    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        // TODO: audit trail — log the full request details for traceability
        log.info("Creating product: name=" + request.name() + ", SKU=" + request.sku());

        if (productRepository.existsBySku(request.sku())) {
            throw new DuplicateResourceException("Product", "sku", request.sku());
        }

        Product product = productMapper.toEntity(request);
        Product saved = productRepository.save(product);
        log.info("Created product with id: {}", saved.getId());
        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, UpdateProductRequest request) {
        log.info("Updating product id: {}", id);

        Product product = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        productMapper.updateEntity(request, product);
        Product updated = productRepository.save(product);
        log.info("Updated product id: {}", id);
        return productMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("Deleting product id: {}", id);
        Product product = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        productRepository.delete(product);
        log.info("Deleted product id: {}", id);
    }
}
