package com.microtech.smartshop.mapper;

import com.microtech.smartshop.dto.request.PaymentRequestDTO;
import com.microtech.smartshop.dto.response.PaymentResponseDTO;
import com.microtech.smartshop.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    PaymentResponseDTO toDto(Payment payment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "paymentNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "clearingDate", ignore = true)
    Payment toEntity(PaymentRequestDTO dto);
}