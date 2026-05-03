package com.fashion.orderservice.controller;

import com.fashion.orderservice.dto.request.OrderRequest;
import com.fashion.orderservice.dto.response.OrderResponse;
import com.fashion.orderservice.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false) // Tạm tắt Security để tập trung test logic Controller
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/orders - Tạo đơn hàng thành công")
    void createOrder_Success() throws Exception {
        OrderRequest request = new OrderRequest();
        // Giả sử request hợp lệ
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
    @DisplayName("GET /api/v1/orders/{id} - Trả về 404 khi không tìm thấy đơn hàng")
    void getOrder_NotFound() throws Exception {
        when(orderService.getOrderForUser(anyLong(), anyString())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/orders/1")
                .header("X-User-Id", "user123"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/v1/orders/{id}/cancel - Hủy đơn hàng thành công")
    void cancelOrder_Success() throws Exception {
        OrderResponse response = OrderResponse.builder().id(1L).status("CANCELLED").build();
        when(orderService.cancelOrder(anyLong(), anyString())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/orders/1/cancel")
                .header("X-User-Id", "user123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}