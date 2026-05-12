---
description: "Use when: adding Spring caching to a service — configures cache provider, adds @Cacheable/@CacheEvict annotations, and handles cache invalidation patterns."
agent: "agent"
tools: [read, edit, search]
argument-hint: "What to cache — e.g., 'Cache product lookups by ID and by SKU' or 'Set up Redis caching for order service'"
---

# Add Caching

Add **Spring Cache** to a service layer for improved read performance.

## Instructions

1. Read the service to identify cacheable methods (reads by ID, lookups by unique key)
2. Determine cache invalidation points (create, update, delete)

## Cache Configuration — `config/CacheConfig.java`

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofMinutes(10))
            .recordStats());
        return cacheManager;
    }
}
```

## Service Annotations

```java
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final String CACHE_NAME = "products";

    // Cache read by ID
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_NAME, key = "#id")
    public ProductResponse getById(Long id) {
        return productRepository.findById(id)
            .map(productMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    // Cache read by unique key
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_NAME, key = "'sku:' + #sku")
    public ProductResponse getBySku(String sku) {
        return productRepository.findBySku(sku)
            .map(productMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "sku", sku));
    }

    // Evict on create (clear list caches)
    @Override
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public ProductResponse create(CreateProductRequest request) { ... }

    // Evict and re-cache on update
    @Override
    @Transactional
    @CachePut(value = CACHE_NAME, key = "#id")
    public ProductResponse update(Long id, UpdateProductRequest request) { ... }

    // Evict on delete
    @Override
    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "#id")
    public void delete(Long id) { ... }
}
```

## Cache Strategies

| Pattern | When to Use |
|---------|-------------|
| `@Cacheable` | Read methods that return same result for same input |
| `@CacheEvict` | Write methods that invalidate cached data |
| `@CachePut` | Update methods where you want to refresh the cache entry |
| `@CacheEvict(allEntries=true)` | When a write affects list queries |

## Providers

| Provider | Use Case | Dependency |
|----------|----------|------------|
| Caffeine | Single instance, in-memory | `com.github.ben-manes.caffeine:caffeine` |
| Redis | Multi-instance, distributed | `spring-boot-starter-data-redis` |
| EhCache | Feature-rich, in-process | `org.ehcache:ehcache` |

## Rules

- Only cache **read-heavy, write-light** data
- Always invalidate cache on writes — stale data is worse than no cache
- Use meaningful cache names and key patterns
- Don't cache paginated list results (low hit rate)
- Don't cache user-specific or session-specific data in shared caches
- Set TTL (time-to-live) — never cache forever
- Add `spring.cache.type=none` to test profiles to disable caching in tests
