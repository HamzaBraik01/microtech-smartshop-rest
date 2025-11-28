package com.microtech.smartshop.mapper;
import com.microtech.smartshop.dto.request.ProductRequestDTO;
import com.microtech.smartshop.dto.response.ProductResponseDTO;
import com.microtech.smartshop.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    ProductResponseDTO toDto(Product product);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isDeleted", constant = "false")
    Product toEntity(ProductRequestDTO dto);
}
