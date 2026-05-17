package com.epam.agents.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.epam.agents.dto.request.CreateProductRequest;
import com.epam.agents.dto.request.UpdateProductRequest;
import com.epam.agents.dto.response.ProductResponse;
import com.epam.agents.exception.DuplicateResourceException;
import com.epam.agents.exception.ResourceNotFoundException;
import com.epam.agents.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * MockMvc slice tests for {@link ProductController}.
 *
 * <p>
 * Uses {@code @WebMvcTest} so only the web layer (controller + advice) is loaded.
 * {@link ProductService} is replaced with a Mockito mock via {@code @MockBean}.
 * </p>
 */
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private ProductResponse productResponse;

    @BeforeEach
    void setUp() {
        productResponse = new ProductResponse(1L, "TEST-001", "Test Widget", new BigDecimal("29.99"), LocalDateTime.of(2025, 1, 1, 10, 0), LocalDateTime.of(2025, 1, 1, 10, 0));
    }

    // ─── GET /api/v1/products/{id} ──────────────────────────────────────────

    @Test
    void getById_shouldReturn200_whenProductExists() throws Exception {
        given(productService.getById(1L)).willReturn(productResponse);

        mockMvc.perform(get("/api/v1/products/1")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.sku").value("TEST-001")).andExpect(jsonPath("$.name").value("Test Widget")).andExpect(jsonPath("$.price").value(29.99));
    }

    @Test
    void getById_shouldReturn404_whenProductNotFound() throws Exception {
        given(productService.getById(99L)).willThrow(new ResourceNotFoundException("Product", "id", 99L));

        mockMvc.perform(get("/api/v1/products/99")).andExpect(status().isNotFound()).andExpect(jsonPath("$.title").value("Resource Not Found")).andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    // ─── GET /api/v1/products ───────────────────────────────────────────────

    @Test
    void getAll_shouldReturn200_withPagedProducts() throws Exception {
        given(productService.getAll(any(Pageable.class))).willReturn(new PageImpl<>(List.of(productResponse)));

        mockMvc.perform(get("/api/v1/products")).andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray()).andExpect(jsonPath("$.content[0].sku").value("TEST-001")).andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.first").value(true));
    }

    @Test
    void getAll_shouldReturn200_withCustomPaginationParams() throws Exception {
        given(productService.getAll(any(Pageable.class))).willReturn(new PageImpl<>(List.of(productResponse)));

        mockMvc.perform(get("/api/v1/products").param("page", "0").param("size", "5").param("sortBy", "name").param("sortDir", "desc")).andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
    }

    // ─── POST /api/v1/products ──────────────────────────────────────────────

    @Test
    void create_shouldReturn201_withLocationHeader_whenRequestIsValid() throws Exception {
        CreateProductRequest request = new CreateProductRequest("TEST-001", "Test Widget", new BigDecimal("29.99"));
        given(productService.create(any(CreateProductRequest.class))).willReturn(productResponse);

        mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated()).andExpect(header().exists("Location")).andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.sku").value("TEST-001"));
    }

    @Test
    void create_shouldReturn400_whenSkuIsBlank() throws Exception {
        CreateProductRequest invalidRequest = new CreateProductRequest("", "Test Widget", new BigDecimal("29.99"));

        mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(invalidRequest))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.title").value("Validation Failed")).andExpect(jsonPath("$.fieldErrors").isNotEmpty());
    }

    @Test
    void create_shouldReturn400_whenPriceIsNull() throws Exception {
        CreateProductRequest invalidRequest = new CreateProductRequest("TEST-001", "Test Widget", null);

        mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(invalidRequest))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.title").value("Validation Failed")).andExpect(jsonPath("$.fieldErrors.price").exists());
    }

    @Test
    void create_shouldReturn409_whenSkuAlreadyExists() throws Exception {
        CreateProductRequest request = new CreateProductRequest("TEST-001", "Test Widget", new BigDecimal("29.99"));
        given(productService.create(any(CreateProductRequest.class))).willThrow(new DuplicateResourceException("Product", "sku", "TEST-001"));

        mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isConflict()).andExpect(jsonPath("$.title").value("Duplicate Resource")).andExpect(jsonPath("$.errorCode").value("DUPLICATE_RESOURCE"));
    }

    // ─── PUT /api/v1/products/{id} ──────────────────────────────────────────

    @Test
    void update_shouldReturn200_whenProductExistsAndRequestIsValid() throws Exception {
        UpdateProductRequest request = new UpdateProductRequest("Updated Widget", new BigDecimal("39.99"));
        given(productService.update(eq(1L), any(UpdateProductRequest.class))).willReturn(productResponse);

        mockMvc.perform(put("/api/v1/products/1").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void update_shouldReturn404_whenProductNotFound() throws Exception {
        UpdateProductRequest request = new UpdateProductRequest("Updated Widget", new BigDecimal("39.99"));
        given(productService.update(eq(99L), any(UpdateProductRequest.class))).willThrow(new ResourceNotFoundException("Product", "id", 99L));

        mockMvc.perform(put("/api/v1/products/99").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isNotFound());
    }

    // ─── DELETE /api/v1/products/{id} ───────────────────────────────────────

    @Test
    void delete_shouldReturn204_whenProductExists() throws Exception {
        willDoNothing().given(productService).delete(1L);

        mockMvc.perform(delete("/api/v1/products/1")).andExpect(status().isNoContent());
    }

    @Test
    void delete_shouldReturn404_whenProductNotFound() throws Exception {
        willThrow(new ResourceNotFoundException("Product", "id", 99L)).given(productService).delete(99L);

        mockMvc.perform(delete("/api/v1/products/99")).andExpect(status().isNotFound()).andExpect(jsonPath("$.title").value("Resource Not Found"));
    }
}
