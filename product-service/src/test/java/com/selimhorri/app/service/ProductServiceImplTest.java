package com.selimhorri.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.selimhorri.app.domain.Category;
import com.selimhorri.app.domain.Product;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.exception.custom.InvalidInputException;
import com.selimhorri.app.exception.custom.ResourceNotFoundException;
import com.selimhorri.app.repository.CategoryRepository;
import com.selimhorri.app.repository.ProductRepository;
import com.selimhorri.app.service.impl.ProductServiceImpl;

public class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void findAll_returnsMappedList() {
    Product p = Product.builder().productId(1).productTitle("asus")
        .category(Category.builder().categoryId(3).categoryTitle("Computer").build())
        .build();
        when(this.productRepository.findAllWithoutDeleted()).thenReturn(List.of(p));

        var list = this.productService.findAll();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("asus", list.get(0).getProductTitle());
    }

    @Test
    void findById_notFound_throws() {
        when(this.productRepository.findByIdWithoutDeleted(99)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> this.productService.findById(99));
    }

    @Test
    void save_missingTitle_throwsInvalidInput() {
        ProductDto dto = ProductDto.builder()
                .imageUrl("i").sku("s").priceUnit(1.0).quantity(1)
                .categoryDto(CategoryDto.builder().categoryId(3).build()).build();

        assertThrows(InvalidInputException.class, () -> this.productService.save(dto));
    }

    @Test
    void save_success_callsRepository() {
        ProductDto dto = ProductDto.builder().productTitle("x").imageUrl("i").sku("s").priceUnit(1.0).quantity(1)
                .categoryDto(CategoryDto.builder().categoryId(3).build()).build();

        when(this.categoryRepository.findById(3)).thenReturn(Optional.of(Category.builder().categoryId(3).build()));
        when(this.productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setProductId(10);
            return p;
        });

        var res = this.productService.save(dto);
        assertNotNull(res);
        assertEquals(10, res.getProductId());
    }

    @Test
    void deleteById_setsDeletedCategory() {
        Product p = Product.builder().productId(5).build();
        when(this.productRepository.findByIdWithoutDeleted(5)).thenReturn(Optional.of(p));
        when(this.categoryRepository.findByCategoryTitle("Deleted")).thenReturn(Optional.of(Category.builder().categoryId(1).categoryTitle("Deleted").build()));
        when(this.productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> this.productService.deleteById(5));
        verify(this.productRepository).save(any(Product.class));
    }

}
