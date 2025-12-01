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
        when(productMapper.toEntity(any(ProductRequestDTO.class))).thenReturn(testProduct);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        when(productMapper.toDto(any(Product.class))).thenReturn(productResponseDTO);

        ProductResponseDTO result = productService.createProduct(productRequestDTO);

        assertNotNull(result);
        assertEquals("Test Product", result.getName());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void testUpdateProduct_Success() {
        when(productRepository.findById("product-1")).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        when(productMapper.toDto(any(Product.class))).thenReturn(productResponseDTO);

        ProductResponseDTO result = productService.updateProduct("product-1", productRequestDTO);

        assertNotNull(result);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void testUpdateProduct_NotFound_ThrowsException() {
        when(productRepository.findById("product-1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            productService.updateProduct("product-1", productRequestDTO);
        });
    }

    @Test
    void testUpdateProduct_UpdatesStock() {
        productRequestDTO.setStock(100); // Nouveau stock
        when(productRepository.findById("product-1")).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        when(productMapper.toDto(any(Product.class))).thenReturn(productResponseDTO);

        productService.updateProduct("product-1", productRequestDTO);

        verify(productRepository, times(1)).save(argThat(product ->
            product.getStock() == 100
        ));
    }

    @Test
    void testDeleteProduct_WithoutOrders_HardDelete() {
        when(productRepository.findById("product-1")).thenReturn(Optional.of(testProduct));
        when(orderItemRepository.existsByProductId("product-1")).thenReturn(false);

        productService.deleteProduct("product-1");

        verify(productRepository, times(1)).delete(testProduct);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void testDeleteProduct_WithOrders_SoftDelete() {
        when(productRepository.findById("product-1")).thenReturn(Optional.of(testProduct));
        when(orderItemRepository.existsByProductId("product-1")).thenReturn(true);

        productService.deleteProduct("product-1");

        verify(productRepository, times(1)).save(argThat(product ->
            product.getIsDeleted() == true
        ));
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void testDeleteProduct_NotFound_ThrowsException() {
        when(productRepository.findById("product-1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            productService.deleteProduct("product-1");
        });
    }

    @Test
    void testGetProductById_Success() {
        when(productRepository.findById("product-1")).thenReturn(Optional.of(testProduct));
        when(productMapper.toDto(any(Product.class))).thenReturn(productResponseDTO);

        ProductResponseDTO result = productService.getProductById("product-1");

        assertNotNull(result);
        assertEquals("product-1", result.getId());
    }

    @Test
    void testGetProductById_NotFound_ThrowsException() {
        when(productRepository.findById("product-1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            productService.getProductById("product-1");
        });
    }

    @Test
    void testGetAllProducts_AsAdmin_IncludesDeletedProducts() {
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

        Page<ProductResponseDTO> result = productService.getAllProducts("", pageable, true);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        verify(productRepository, times(1)).findByNameContainingIgnoreCase("", pageable);
        verify(productRepository, never()).findByNameContainingIgnoreCaseAndIsDeletedFalse(anyString(), any(Pageable.class));
    }

    @Test
    void testGetAllProducts_AsClient_ExcludesDeletedProducts() {
        Page<Product> productPage = new PageImpl<>(Arrays.asList(testProduct));
        Pageable pageable = PageRequest.of(0, 10);

        when(productRepository.findByNameContainingIgnoreCaseAndIsDeletedFalse("", pageable)).thenReturn(productPage);
        when(productMapper.toDto(any(Product.class))).thenReturn(productResponseDTO);

        Page<ProductResponseDTO> result = productService.getAllProducts("", pageable, false);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(productRepository, times(1)).findByNameContainingIgnoreCaseAndIsDeletedFalse("", pageable);
        verify(productRepository, never()).findByNameContainingIgnoreCase(anyString(), any(Pageable.class));
    }

    @Test
    void testGetAllProducts_WithSearchTerm() {
        Page<Product> productPage = new PageImpl<>(Arrays.asList(testProduct));
        Pageable pageable = PageRequest.of(0, 10);

        when(productRepository.findByNameContainingIgnoreCaseAndIsDeletedFalse("Test", pageable))
                .thenReturn(productPage);
        when(productMapper.toDto(any(Product.class))).thenReturn(productResponseDTO);

        Page<ProductResponseDTO> result = productService.getAllProducts("Test", pageable, false);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(productRepository, times(1))
                .findByNameContainingIgnoreCaseAndIsDeletedFalse("Test", pageable);
    }

    @Test
    void testStockValidation_SufficientStock() {
        testProduct.setStock(100);
        when(productRepository.findById("product-1")).thenReturn(Optional.of(testProduct));
        when(productMapper.toDto(any(Product.class))).thenReturn(productResponseDTO);

        ProductResponseDTO result = productService.getProductById("product-1");

        assertNotNull(result);
        verify(productRepository, times(1)).findById("product-1");
    }

    @Test
    void testStockValidation_LowStock() {
        testProduct.setStock(5);
        ProductResponseDTO lowStockDTO = new ProductResponseDTO();
        lowStockDTO.setId("product-1");
        lowStockDTO.setStock(5);

        when(productRepository.findById("product-1")).thenReturn(Optional.of(testProduct));
        when(productMapper.toDto(any(Product.class))).thenReturn(lowStockDTO);

        ProductResponseDTO result = productService.getProductById("product-1");

        assertNotNull(result);
        assertTrue(result.getStock() <= 10); // Stock faible
    }

    @Test
    void testStockValidation_OutOfStock() {
        testProduct.setStock(0);
        ProductResponseDTO outOfStockDTO = new ProductResponseDTO();
        outOfStockDTO.setId("product-1");
        outOfStockDTO.setStock(0);

        when(productRepository.findById("product-1")).thenReturn(Optional.of(testProduct));
        when(productMapper.toDto(any(Product.class))).thenReturn(outOfStockDTO);

        ProductResponseDTO result = productService.getProductById("product-1");

        assertNotNull(result);
        assertEquals(0, result.getStock());
    }
}

