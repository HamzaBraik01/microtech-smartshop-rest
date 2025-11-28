package com.microtech.smartshop.service;

import com.microtech.smartshop.dto.request.ProductRequestDTO;
import com.microtech.smartshop.dto.response.ProductResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    ProductResponseDTO createProduct(ProductRequestDTO dto);
    ProductResponseDTO updateProduct(String id, ProductRequestDTO dto);
    void deleteProduct(String id); // Soft Delete logic here
    ProductResponseDTO getProductById(String id);
    Page<ProductResponseDTO> getAllProducts(String search, Pageable pageable, boolean isAdmin);
}