---
description: "Use when: creating a MapStruct mapper or updating an existing one — generates entity-to-DTO mapping interfaces with custom mappings, partial updates, and nested conversions."
agent: "agent"
tools: [read, edit, search]
argument-hint: "Entity name — e.g., 'Create mapper for Product entity' or 'Add partial update mapping to OrderMapper'"
---

# Generate MapStruct Mapper

Generate or update a **MapStruct mapper** for entity ↔ DTO conversions in a Spring Boot application.

## Instructions

1. Read the entity, request DTOs, and response DTOs
2. Identify fields that need custom mapping (different names, type conversions, nested objects)

## Standard Mapper — `mapper/<Resource>Mapper.java`

```java
@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProductMapper {

    /**
     * Maps a create request DTO to a new entity.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Product toEntity(CreateProductRequest request);

    /**
     * Maps an entity to a response DTO.
     */
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    ProductResponse toResponse(Product entity);

    /**
     * Maps a page of entities to a page of response DTOs.
     */
    default Page<ProductResponse> toResponsePage(Page<Product> page) {
        return page.map(this::toResponse);
    }

    /**
     * Partially updates an entity from an update request.
     * Only non-null fields in the request overwrite entity fields.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    void updateEntity(UpdateProductRequest request, @MappingTarget Product entity);
}
```

## Common Custom Mappings

### Enum ↔ String
```java
@Mapping(target = "status", expression = "java(entity.getStatus().name())")
ProductResponse toResponse(Product entity);
```

### Nested object → flat fields
```java
@Mapping(source = "address.city", target = "city")
@Mapping(source = "address.zipCode", target = "zipCode")
CustomerResponse toResponse(Customer entity);
```

### Multiple sources → one target
```java
@Mapping(source = "product.name", target = "productName")
@Mapping(source = "quantity", target = "quantity")
@Mapping(target = "lineTotal", expression = "java(item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))")
OrderItemResponse toResponse(OrderItem item);
```

### Collection mapping
```java
List<ProductResponse> toResponseList(List<Product> entities);
Set<TagResponse> toTagResponses(Set<Tag> tags);
```

### After-mapping hook
```java
@AfterMapping
default void setFullName(@MappingTarget CustomerResponse.CustomerResponseBuilder response, Customer entity) {
    response.fullName(entity.getFirstName() + " " + entity.getLastName());
}
```

## Rules

- Always use `componentModel = "spring"` — enables `@Autowired` injection
- Use `@BeanMapping(nullValuePropertyMappingStrategy = IGNORE)` for partial updates
- Ignore audit fields (`id`, `createdAt`, `updatedAt`, `createdBy`, `updatedBy`) in `toEntity`
- For nested objects: map IDs and summary fields to response, not full nested DTOs
- Don't map from entity to request DTO — only entity → response and request → entity
- Test mappers with `@SpringBootTest` or by instantiating `Mappers.getMapper()`
