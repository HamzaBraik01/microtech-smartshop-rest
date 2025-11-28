package com.microtech.smartshop.mapper;

import com.microtech.smartshop.dto.response.OrderItemResponseDTO;
import com.microtech.smartshop.dto.response.OrderResponseDTO;
import com.microtech.smartshop.entity.Order;
import com.microtech.smartshop.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    @Mapping(target = "items", source = "items")
    OrderResponseDTO toDto(Order order);

    @Mapping(target = "productName", source = "product.name")
    OrderItemResponseDTO toItemDto(OrderItem orderItem);
}