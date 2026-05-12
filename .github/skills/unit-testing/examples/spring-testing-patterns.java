package com.example.testing.patterns;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// =====================================================================
// PATTERN 1: Pure unit test with Mockito
// =====================================================================
@ExtendWith(MockitoExtension.class)
class ServiceLayerTest {

    interface OrderRepository {
        Optional<Order> findById(Long id);
        Order save(Order order);
    }

    record Order(Long id, String status, BigDecimal total) {
        Order withStatus(String newStatus) {
            return new Order(id, newStatus, total);
        }
    }

    static class OrderService {
        private final OrderRepository repo;
        OrderService(OrderRepository repo) { this.repo = repo; }

        Order cancelOrder(Long orderId) {
            Order order = repo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
            if ("SHIPPED".equals(order.status())) {
                throw new IllegalStateException("Cannot cancel shipped order");
            }
            Order cancelled = order.withStatus("CANCELLED");
            return repo.save(cancelled);
        }
    }

    @Mock OrderRepository orderRepository;
    @InjectMocks OrderService orderService;

    @Test
    void cancelOrder_success() {
        Order order = new Order(1L, "PENDING", new BigDecimal("99.99"));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.cancelOrder(1L);

        assertThat(result.status()).isEqualTo("CANCELLED");
        verify(orderRepository).save(argThat(o -> "CANCELLED".equals(o.status())));
    }

    @Test
    void cancelOrder_throwsWhenShipped() {
        Order shipped = new Order(2L, "SHIPPED", new BigDecimal("49.99"));
        when(orderRepository.findById(2L)).thenReturn(Optional.of(shipped));

        assertThatThrownBy(() -> orderService.cancelOrder(2L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("shipped");
    }

    @Test
    void cancelOrder_throwsWhenNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder(99L))
            .isInstanceOf(IllegalArgumentException.class);
    }
}

// =====================================================================
// PATTERN 2: Parameterized tests
// =====================================================================
class ParameterizedPatternTest {

    static int add(int a, int b) { return a + b; }

    @ParameterizedTest(name = "{0} + {1} = {2}")
    @CsvSource({"1, 2, 3", "0, 0, 0", "-1, 1, 0", "100, 200, 300"})
    void addReturnsCorrectSum(int a, int b, int expected) {
        assertThat(add(a, b)).isEqualTo(expected);
    }

    static String normalize(String input) {
        if (input == null || input.isBlank()) return "";
        return input.trim().toLowerCase();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t"})
    void normalizeReturnsEmptyForBlankInputs(String input) {
        assertThat(normalize(input)).isEmpty();
    }
}

// =====================================================================
// PATTERN 3: Controller integration test with MockMvc
// =====================================================================
// Uncomment and adjust when you have actual controller + service classes.
//
// @WebMvcTest(ProductController.class)
// class ProductControllerTest {
//
//     @Autowired private MockMvc mockMvc;
//     @MockBean  private ProductService productService;
//
//     @Test
//     void getProduct_returns200() throws Exception {
//         when(productService.getProduct(1L))
//             .thenReturn(new ProductResponse(1L, "Mouse", ...));
//
//         mockMvc.perform(get("/api/v1/products/1"))
//             .andExpect(status().isOk())
//             .andExpect(jsonPath("$.name").value("Mouse"));
//     }
//
//     @Test
//     void createProduct_returns201() throws Exception {
//         String body = """
//             { "name": "Keyboard", "price": 79.99, "category": "Electronics" }
//             """;
//
//         mockMvc.perform(post("/api/v1/products")
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .content(body))
//             .andExpect(status().isCreated());
//     }
//
//     @Test
//     void createProduct_returns400_whenNameMissing() throws Exception {
//         String body = """
//             { "price": 79.99, "category": "Electronics" }
//             """;
//
//         mockMvc.perform(post("/api/v1/products")
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .content(body))
//             .andExpect(status().isBadRequest())
//             .andExpect(jsonPath("$.errors.name").exists());
//     }
// }
