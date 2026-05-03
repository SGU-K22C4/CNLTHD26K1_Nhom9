package com.fashion.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fashion.orderservice.dto.request.OrderItemRequest;
import com.fashion.orderservice.dto.request.OrderRequest;
import com.fashion.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @Test
    void should_ReturnBadRequest_When_CreateOrderRequestIsInvalid() throws Exception {
        OrderRequest request = new OrderRequest();

        mockMvc.perform(post("/api/v1/orders")
                        .header("X-User-Id", "user-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    void should_ReturnBadRequest_When_XUserIdHeaderIsMissing() throws Exception {
        OrderRequest request = createValidRequest();

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
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
        request.setPaymentMethod(com.fashion.orderservice.entity.Order.PaymentMethod.COD);
        request.setItems(List.of(item));
        return request;
    }
}
