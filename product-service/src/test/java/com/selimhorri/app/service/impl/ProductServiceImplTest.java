package com.selimhorri.app.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.selimhorri.app.domain.Category;
import com.selimhorri.app.domain.Product;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.exception.wrapper.ProductNotFoundException;
import com.selimhorri.app.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Category category;
    private CategoryDto categoryDto;
    private Product product1;
    private Product product2;
    private Product updatedProduct1;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .categoryId(1)
                .categoryTitle("Electronics")
                .imageUrl("url")
                .build();
        categoryDto = CategoryDto.builder()
                .categoryId(1)
                .categoryTitle("Electronics")
                .imageUrl("url")
                .build();
        product1 = Product.builder()
                .productId(10)
                .productTitle("Phone")
                .imageUrl("phone.png")
                .sku("SKU123")
                .priceUnit(199.99)
                .quantity(5)
                .category(category)
                .build();
        product2 = Product.builder()
                .productId(20)
                .productTitle("Laptop")
                .imageUrl("laptop.png")
                .sku("SKU456")
                .priceUnit(999.99)
                .quantity(3)
                .category(category)
                .build();
        updatedProduct1 = Product.builder()
                .productId(10)
                .productTitle("Phone Updated")
                .imageUrl("phone2.png")
                .sku("SKU123")
                .priceUnit(249.99)
                .quantity(10)
                .category(category)
                .build();
    }

    @Test
    void testFindAll_ReturnsMappedList() {
        when(productRepository.findAll()).thenReturn(Arrays.asList(product1, product2));

        List<ProductDto> dtos = productService.findAll();

        assertEquals(2, dtos.size());
        assertEquals(product1.getProductId(), dtos.get(0).getProductId());
        assertEquals(product2.getSku(), dtos.get(1).getSku());
        verify(productRepository, times(1)).findAll();
    }

    @Test
    void testFindById_ReturnsDto() {
        when(productRepository.findById(10)).thenReturn(Optional.of(product1));

        ProductDto dto = productService.findById(10);

        assertNotNull(dto);
        assertEquals("Phone", dto.getProductTitle());
        assertEquals(category.getCategoryTitle(), dto.getCategoryDto().getCategoryTitle());
        verify(productRepository, times(1)).findById(10);
    }

    @Test
    void testFindById_NotFound_ThrowsException() {
        when(productRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.findById(99));
        verify(productRepository, times(1)).findById(99);
    }

    @Test
    void testSave_CallsRepositoryAndReturnsDto() {
        when(productRepository.save(any(Product.class))).thenReturn(product1);
        ProductDto input = ProductDto.builder()
                .productTitle("Phone")
                .imageUrl("phone.png")
                .sku("SKU123")
                .priceUnit(199.99)
                .quantity(5)
                .categoryDto(categoryDto)
                .build();

        ProductDto result = productService.save(input);

        assertEquals(product1.getProductId(), result.getProductId());
        assertEquals(product1.getSku(), result.getSku());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void testUpdate_CallsRepositoryAndReturnsUpdatedDto() {
        when(productRepository.findById(10)).thenReturn(Optional.of(product1));
        when(productRepository.save(any(Product.class))).thenReturn(updatedProduct1);
        ProductDto updateDto = ProductDto.builder()
                .productId(10)
                .productTitle("Phone Updated")
                .imageUrl("phone2.png")
                .sku("SKU123")
                .priceUnit(249.99)
                .quantity(10)
                .categoryDto(null)
                .build();

        ProductDto result = productService.update(10, updateDto);

        assertEquals(10, result.getProductId());
        assertEquals(249.99, result.getPriceUnit());
        verify(productRepository, times(1)).findById(10);
        verify(productRepository, times(1)).save(any(Product.class));
    }
}
