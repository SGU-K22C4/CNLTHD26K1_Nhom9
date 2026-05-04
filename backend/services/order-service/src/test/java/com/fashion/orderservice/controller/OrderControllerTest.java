package com.fashion.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fashion.orderservice.dto.request.OrderItemRequest;
import com.fashion.orderservice.dto.request.OrderRequest;
import com.fashion.orderservice.dto.response.OrderResponse;
import com.fashion.orderservice.entity.Order;
import com.fashion.orderservice.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/orders - tao don hang thanh cong")
    void createOrder_Success() throws Exception {
        OrderRequest request = createValidRequest();
        OrderResponse response = OrderResponse.builder().id(1L).orderNumber("ORD-123").build();

        when(orderService.createOrder(anyString(), any(OrderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/orders")
                        .header("X-User-Id", "user123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("ORD-123"));
    }

    @Test
    @DisplayName("GET /api/v1/orders/{id} - tra ve 404 khi khong tim thay don hang")
    void getOrder_NotFound() throws Exception {
        when(orderService.getOrderForUser(anyLong(), anyString())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/orders/1")
                        .header("X-User-Id", "user123"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/v1/orders/{id}/cancel - huy don hang thanh cong")
    void cancelOrder_Success() throws Exception {
        OrderResponse response = OrderResponse.builder().id(1L).status("CANCELLED").build();
        when(orderService.cancelOrder(anyLong(), anyString())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/orders/1/cancel")
                        .header("X-User-Id", "user123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    private OrderRequest createValidRequest() {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId("PROD-1");
        item.setProductName("Test Product");
        item.setColor("Black");
        item.setSize("M");
        item.setQuantity(1);
        item.setUnitPrice(BigDecimal.valueOf(199000));

        OrderRequest request = new OrderRequest();
        request.setRecipientName("John Doe");
        request.setRecipientPhone("0123456789");
        request.setShippingAddress("123 Test Street");
        request.setPaymentMethod(Order.PaymentMethod.COD);
        request.setItems(List.of(item));
        return request;
    }
}
