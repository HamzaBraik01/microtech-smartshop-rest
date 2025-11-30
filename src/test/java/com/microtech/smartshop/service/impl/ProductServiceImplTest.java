package com.microtech.smartshop.service.impl;

import com.microtech.smartshop.dto.request.ProductRequestDTO;
import com.microtech.smartshop.dto.response.ProductResponseDTO;
import com.microtech.smartshop.entity.Product;
import com.microtech.smartshop.exception.ResourceNotFoundException;
import com.microtech.smartshop.mapper.ProductMapper;
import com.microtech.smartshop.repository.OrderItemRepository;
import com.microtech.smartshop.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product testProduct;
    private ProductRequestDTO productRequestDTO;
    private ProductResponseDTO productResponseDTO;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .id("product-1")
                .name("Test Product")
                .description("Test Description")
                .price(BigDecimal.valueOf(100))
                .stock(50)
                .isDeleted(false)
                .build();

        productRequestDTO = new ProductRequestDTO();
        productRequestDTO.setName("Test Product");
        productRequestDTO.setDescription("Test Description");
        productRequestDTO.setPrice(BigDecimal.valueOf(100));
        productRequestDTO.setStock(50);

        productResponseDTO = new ProductResponseDTO();
        productResponseDTO.setId("product-1");
        productResponseDTO.setName("Test Product");
        productResponseDTO.setPrice(BigDecimal.valueOf(100));
        productResponseDTO.setStock(50);
    }

    @Test
    void testCreateProduct_Success() {
        // Given
        when(productMapper.toEntity(any(ProductRequestDTO.class))).thenReturn(testProduct);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        when(productMapper.toDto(any(Product.class))).thenReturn(productResponseDTO);

        // When
        ProductResponseDTO result = productService.createProduct(productRequestDTO);

        // Then
        assertNotNull(result);
        assertEquals("Test Product", result.getName());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void testUpdateProduct_Success() {
        // Given
        when(productRepository.findById("product-1")).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        when(productMapper.toDto(any(Product.class))).thenReturn(productResponseDTO);

        // When
        ProductResponseDTO result = productService.updateProduct("product-1", productRequestDTO);

        // Then
        assertNotNull(result);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void testUpdateProduct_NotFound_ThrowsException() {
        // Given
        when(productRepository.findById("product-1")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            productService.updateProduct("product-1", productRequestDTO);
        });
    }

    @Test
    void testUpdateProduct_UpdatesStock() {
        // Given
        productRequestDTO.setStock(100); // Nouveau stock
        when(productRepository.findById("product-1")).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        when(productMapper.toDto(any(Product.class))).thenReturn(productResponseDTO);

        // When
        productService.updateProduct("product-1", productRequestDTO);

        // Then
        verify(productRepository, times(1)).save(argThat(product ->
            product.getStock() == 100
        ));
    }

    @Test
    void testDeleteProduct_WithoutOrders_HardDelete() {
        // Given - Produit sans commandes associées
        when(productRepository.findById("product-1")).thenReturn(Optional.of(testProduct));
        when(orderItemRepository.existsByProductId("product-1")).thenReturn(false);

        // When
        productService.deleteProduct("product-1");

        // Then
        verify(productRepository, times(1)).delete(testProduct);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void testDeleteProduct_WithOrders_SoftDelete() {
        // Given - Produit avec commandes associées
        when(productRepository.findById("product-1")).thenReturn(Optional.of(testProduct));
        when(orderItemRepository.existsByProductId("product-1")).thenReturn(true);

        // When
        productService.deleteProduct("product-1");

        // Then
        verify(productRepository, times(1)).save(argThat(product ->
            product.getIsDeleted() == true
        ));
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void testDeleteProduct_NotFound_ThrowsException() {
        // Given
        when(productRepository.findById("product-1")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            productService.deleteProduct("product-1");
        });
    }

    @Test
    void testGetProductById_Success() {
        // Given
        when(productRepository.findById("product-1")).thenReturn(Optional.of(testProduct));
        when(productMapper.toDto(any(Product.class))).thenReturn(productResponseDTO);

        // When
        ProductResponseDTO result = productService.getProductById("product-1");

        // Then
        assertNotNull(result);
        assertEquals("product-1", result.getId());
    }

    @Test
    void testGetProductById_NotFound_ThrowsException() {
        // Given
        when(productRepository.findById("product-1")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            productService.getProductById("product-1");
        });
    }

    @Test
    void testGetAllProducts_AsAdmin_IncludesDeletedProducts() {
        // Given
        Product deletedProduct = Product.builder()
                .id("product-2")
                .name("Deleted Product")
                .price(BigDecimal.valueOf(50))
                .stock(0)
                .isDeleted(true)
                .build();

        Page<Product> productPage = new PageImpl<>(Arrays.asList(testProduct, deletedProduct));
        Pageable pageable = PageRequest.of(0, 10);

        when(productRepository.findByNameContainingIgnoreCase("", pageable)).thenReturn(productPage);
        when(productMapper.toDto(any(Product.class))).thenReturn(productResponseDTO);

        // When
        Page<ProductResponseDTO> result = productService.getAllProducts("", pageable, true);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        verify(productRepository, times(1)).findByNameContainingIgnoreCase("", pageable);
        verify(productRepository, never()).findByNameContainingIgnoreCaseAndIsDeletedFalse(anyString(), any(Pageable.class));
    }

    @Test
    void testGetAllProducts_AsClient_ExcludesDeletedProducts() {
        // Given
        Page<Product> productPage = new PageImpl<>(Arrays.asList(testProduct));
        Pageable pageable = PageRequest.of(0, 10);

        when(productRepository.findByNameContainingIgnoreCaseAndIsDeletedFalse("", pageable)).thenReturn(productPage);
        when(productMapper.toDto(any(Product.class))).thenReturn(productResponseDTO);

        // When
        Page<ProductResponseDTO> result = productService.getAllProducts("", pageable, false);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(productRepository, times(1)).findByNameContainingIgnoreCaseAndIsDeletedFalse("", pageable);
        verify(productRepository, never()).findByNameContainingIgnoreCase(anyString(), any(Pageable.class));
    }

    @Test
    void testGetAllProducts_WithSearchTerm() {
        // Given
        Page<Product> productPage = new PageImpl<>(Arrays.asList(testProduct));
        Pageable pageable = PageRequest.of(0, 10);

        when(productRepository.findByNameContainingIgnoreCaseAndIsDeletedFalse("Test", pageable))
                .thenReturn(productPage);
        when(productMapper.toDto(any(Product.class))).thenReturn(productResponseDTO);

        // When
        Page<ProductResponseDTO> result = productService.getAllProducts("Test", pageable, false);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(productRepository, times(1))
                .findByNameContainingIgnoreCaseAndIsDeletedFalse("Test", pageable);
    }

    @Test
    void testStockValidation_SufficientStock() {
        // Given
        testProduct.setStock(100);
        when(productRepository.findById("product-1")).thenReturn(Optional.of(testProduct));
        when(productMapper.toDto(any(Product.class))).thenReturn(productResponseDTO);

        // When
        ProductResponseDTO result = productService.getProductById("product-1");

        // Then
        assertNotNull(result);
        // Le produit est disponible
        verify(productRepository, times(1)).findById("product-1");
    }

    @Test
    void testStockValidation_LowStock() {
        // Given
        testProduct.setStock(5);
        ProductResponseDTO lowStockDTO = new ProductResponseDTO();
        lowStockDTO.setId("product-1");
        lowStockDTO.setStock(5);

        when(productRepository.findById("product-1")).thenReturn(Optional.of(testProduct));
        when(productMapper.toDto(any(Product.class))).thenReturn(lowStockDTO);

        // When
        ProductResponseDTO result = productService.getProductById("product-1");

        // Then
        assertNotNull(result);
        assertTrue(result.getStock() <= 10); // Stock faible
    }

    @Test
    void testStockValidation_OutOfStock() {
        // Given
        testProduct.setStock(0);
        ProductResponseDTO outOfStockDTO = new ProductResponseDTO();
        outOfStockDTO.setId("product-1");
        outOfStockDTO.setStock(0);

        when(productRepository.findById("product-1")).thenReturn(Optional.of(testProduct));
        when(productMapper.toDto(any(Product.class))).thenReturn(outOfStockDTO);

        // When
        ProductResponseDTO result = productService.getProductById("product-1");

        // Then
        assertNotNull(result);
        assertEquals(0, result.getStock()); // Rupture de stock
    }
}

