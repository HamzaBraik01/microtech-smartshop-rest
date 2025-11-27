package com.microtech.smartshop.mapper;

import com.microtech.smartshop.entity.Client;
import com.microtech.smartshop.dto.request.ClientRequestDTO;
import com.microtech.smartshop.dto.response.ClientResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    ClientResponseDTO toDto(Client client);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fidelityLevel", ignore = true)
    @Mapping(target = "totalOrders", ignore = true)
    @Mapping(target = "totalSpent", ignore = true)
    @Mapping(target = "user", ignore = true)
    Client toEntity(ClientRequestDTO dto);
}