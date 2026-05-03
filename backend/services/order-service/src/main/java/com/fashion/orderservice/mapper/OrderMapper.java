package com.fashion.orderservice.mapper;

import com.fashion.orderservice.dto.response.OrderItemResponse;
import com.fashion.orderservice.dto.response.OrderResponse;
import com.fashion.orderservice.entity.Order;
import com.fashion.orderservice.entity.OrderItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponse toResponse(Order order);

    OrderItemResponse toResponse(OrderItem item);

    default String map(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
