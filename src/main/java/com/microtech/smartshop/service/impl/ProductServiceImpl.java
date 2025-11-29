package com.microtech.smartshop.service.impl;

import com.microtech.smartshop.dto.request.ProductRequestDTO;
import com.microtech.smartshop.dto.response.ProductResponseDTO;
import com.microtech.smartshop.entity.Product;
import com.microtech.smartshop.exception.ResourceNotFoundException;
import com.microtech.smartshop.mapper.ProductMapper;
import com.microtech.smartshop.repository.OrderItemRepository;
import com.microtech.smartshop.repository.ProductRepository;
import com.microtech.smartshop.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductMapper productMapper;

    @Override
    public ProductResponseDTO createProduct(ProductRequestDTO dto) {
        Product product = productMapper.toEntity(dto);
        return productMapper.toDto(productRepository.save(product));
    }

    @Override
    public ProductResponseDTO updateProduct(String id, ProductRequestDTO dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable"));

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());

        return productMapper.toDto(productRepository.save(product));
    }

    @Override
    @Transactional
    public void deleteProduct(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable"));

        boolean hasOrders = orderItemRepository.existsByProductId(id);

        if (hasOrders) {
            product.setIsDeleted(true);
            productRepository.save(product);
        } else {
            productRepository.delete(product);
        }
    }

    @Override
    public ProductResponseDTO getProductById(String id) {
        return productRepository.findById(id)
                .map(productMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable"));
    }

    @Override
    public Page<ProductResponseDTO> getAllProducts(String search, Pageable pageable, boolean isAdmin) {
        String searchTerm = (search == null) ? "" : search;

        Page<Product> page;
        if (isAdmin) {
            page = productRepository.findByNameContainingIgnoreCase(searchTerm, pageable);
        } else {
            page = productRepository.findByNameContainingIgnoreCaseAndIsDeletedFalse(searchTerm, pageable);
        }

        return page.map(productMapper::toDto);
    }
}