package com.epam.agents.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.epam.agents.dto.request.CreateProductRequest;
import com.epam.agents.dto.request.UpdateProductRequest;
import com.epam.agents.dto.response.ProductResponse;
import com.epam.agents.entity.Product;

/**
 * MapStruct mapper that converts between {@link Product} entities and DTOs.
 *
 * <p>
 * Spring-managed via {@code componentModel = "spring"}. Unmapped target
 * properties generate warnings at compile time (see {@link ReportingPolicy#IGNORE}).
 * </p>
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {

    /**
     * Converts a {@link Product} entity to a {@link ProductResponse} DTO.
     *
     * @param product
     *            the source entity
     * @return the response DTO
     */
    ProductResponse toResponse(Product product);

    /**
     * Converts a {@link CreateProductRequest} to a new {@link Product} entity.
     * Audit and identity fields are excluded from mapping.
     *
     * @param request
     *            the creation request
     * @return a new, unsaved {@link Product}
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Product toEntity(CreateProductRequest request);

    /**
     * Applies non-null fields from an {@link UpdateProductRequest} onto an existing entity.
     * Null fields are silently skipped (partial-update semantics).
     * The SKU and audit timestamps are never overwritten.
     *
     * @param request
     *            the update request
     * @param product
     *            the existing entity to mutate (annotated with {@link MappingTarget})
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sku", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdateProductRequest request, @MappingTarget Product product);
}
