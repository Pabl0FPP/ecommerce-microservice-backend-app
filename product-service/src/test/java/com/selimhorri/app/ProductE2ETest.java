package com.selimhorri.app;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.service.CategoryService;
import com.selimhorri.app.service.ProductService;

/**
 * Tests E2E (End-to-End) para Product Service
 * Estos tests validan flujos completos de usuario que involucran gestión de productos
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductE2ETest {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    private CategoryDto getAnyExistingCategoryOrCreate(String fallbackTitle) {
        List<CategoryDto> categories = categoryService.findAll();
        if (categories != null && !categories.isEmpty()) {
            return categories.get(0);
        }
        // Crear una categoría válida si no hay ninguna disponible
        CategoryDto created = categoryService.save(
                CategoryDto.builder()
                        .categoryTitle(fallbackTitle)
                        .imageUrl("https://example.com/category.jpg")
                        .build());
        return created;
    }


    /**
     * Test E2E: Flujo completo de gestión de productos
     * Crear producto → Actualizar producto → Eliminar producto
     */
    @Test
    void testE2E_CompleteProductManagementFlow() {
        // Arrange: Preparar datos de prueba
        String timestamp = String.valueOf(System.currentTimeMillis());

        // Paso 1: Obtener o crear una categoría válida (con categoryId)
        CategoryDto categoryDto = getAnyExistingCategoryOrCreate("E2E Test Category");

        // Paso 2: Crear producto
        ProductDto productDto = ProductDto.builder()
                .productTitle("E2E Test Product " + timestamp)
                .imageUrl("https://example.com/product.jpg")
                .sku("SKU-" + timestamp)
                .priceUnit(99.99)
                .quantity(10)
                .categoryDto(categoryDto)
                .build();

        ProductDto createdProduct = productService.save(productDto);
        assertNotNull(createdProduct, "Product should be created");
        assertNotNull(createdProduct.getProductId(), "Product should have an ID");
        assertEquals(productDto.getProductTitle(), createdProduct.getProductTitle(), 
                "Product title should match");
        assertEquals(productDto.getPriceUnit(), createdProduct.getPriceUnit(), 
                "Product price should match");
        assertEquals(productDto.getQuantity(), createdProduct.getQuantity(), 
                "Product quantity should match");

        // Paso 3: Verificar que el producto se puede recuperar
        ProductDto retrievedProduct = productService.findById(createdProduct.getProductId());
        assertNotNull(retrievedProduct, "Product should be retrievable");
        assertEquals(createdProduct.getProductId(), retrievedProduct.getProductId(), 
                "Product IDs should match");
        assertEquals(createdProduct.getProductTitle(), retrievedProduct.getProductTitle(), 
                "Product titles should match");

        // Paso 4: Actualizar producto
        ProductDto updateDto = ProductDto.builder()
                .productId(createdProduct.getProductId())
                .productTitle("Updated E2E Test Product " + timestamp)
                .imageUrl("https://example.com/updated-product.jpg")
                .sku(createdProduct.getSku())
                .priceUnit(149.99)
                .quantity(20)
                .categoryDto(categoryDto)
                .build();

        ProductDto updatedProduct = productService.update(updateDto);
        assertNotNull(updatedProduct, "Product should be updated");
        assertEquals(createdProduct.getProductId(), updatedProduct.getProductId(), 
                "Product ID should remain the same");
        assertEquals(updateDto.getProductTitle(), updatedProduct.getProductTitle(), 
                "Product title should be updated");
        assertEquals(updateDto.getPriceUnit(), updatedProduct.getPriceUnit(), 
                "Product price should be updated");
        assertEquals(updateDto.getQuantity(), updatedProduct.getQuantity(), 
                "Product quantity should be updated");

        // Paso 5: Verificar que la actualización se reflejó
        ProductDto reRetrievedProduct = productService.findById(createdProduct.getProductId());
        assertNotNull(reRetrievedProduct, "Product should still be retrievable");
        assertEquals(updateDto.getProductTitle(), reRetrievedProduct.getProductTitle(), 
                "Product title should be updated in database");
        assertEquals(updateDto.getPriceUnit(), reRetrievedProduct.getPriceUnit(), 
                "Product price should be updated in database");

        // Paso 6: Eliminar producto
        productService.deleteById(createdProduct.getProductId());

        // Verificar que el producto fue eliminado
        try {
            productService.findById(createdProduct.getProductId());
            fail("Product should have been deleted");
        } catch (com.selimhorri.app.exception.custom.ResourceNotFoundException e) {
            // Esperado - el producto fue eliminado
            assertTrue(e.getMessage().contains("Product") || 
                      e.getMessage().contains("product") || 
                      e.getMessage().contains("not found"),
                      "Exception should indicate product not found");
        }

        // Assert: Verificar flujo completo
        assertNotNull(createdProduct.getProductId(), "Product should have been created");
        assertNotNull(updatedProduct.getProductId(), "Product should have been updated");
    }

    /**
     * Test E2E: Flujo de búsqueda y listado de productos
     * Crear múltiples productos → Listar todos → Buscar por ID
     */
    @Test
    void testE2E_ProductSearchAndListingFlow() {
        // Arrange: Preparar datos
        String timestamp = String.valueOf(System.currentTimeMillis());

        // Paso 1: Crear múltiples productos
        ProductDto[] createdProducts = new ProductDto[3];
        CategoryDto categoryDto = getAnyExistingCategoryOrCreate("E2E Test Category");

        for (int i = 0; i < 3; i++) {
            ProductDto productDto = ProductDto.builder()
                    .productTitle("E2E Test Product " + i + " " + timestamp)
                    .imageUrl("https://example.com/product" + i + ".jpg")
                    .sku("SKU-" + i + "-" + timestamp)
                    .priceUnit((i + 1) * 50.0)
                    .quantity((i + 1) * 10)
                    .categoryDto(categoryDto)
                    .build();

            createdProducts[i] = productService.save(productDto);
            assertNotNull(createdProducts[i], "Product " + (i + 1) + " should be created");
            assertNotNull(createdProducts[i].getProductId(), 
                    "Product " + (i + 1) + " should have an ID");
        }

        // Paso 2: Listar todos los productos
        List<ProductDto> allProducts = productService.findAll();
        assertNotNull(allProducts, "Products list should not be null");
        assertTrue(allProducts.size() >= 3, "Should have at least 3 products");

        // Verificar que los productos creados están en la lista
        for (int i = 0; i < 3; i++) {
            final int index = i;
            boolean found = allProducts.stream()
                    .anyMatch(p -> p.getProductId().equals(createdProducts[index].getProductId()));
            assertTrue(found, "Product " + (i + 1) + " should be in the list");
        }

        // Paso 3: Buscar cada producto por ID
        for (int i = 0; i < 3; i++) {
            ProductDto foundProduct = productService.findById(createdProducts[i].getProductId());
            assertNotNull(foundProduct, "Product " + (i + 1) + " should be found by ID");
            assertEquals(createdProducts[i].getProductId(), foundProduct.getProductId(), 
                    "Product IDs should match");
            assertEquals(createdProducts[i].getProductTitle(), foundProduct.getProductTitle(), 
                    "Product titles should match");
        }

        // Assert: Verificar flujo completo
        assertEquals(3, createdProducts.length, "Should have created 3 products");
        assertTrue(allProducts.size() >= 3, "Should have at least 3 products in list");
    }

    /**
     * Test E2E: Flujo de actualización parcial de producto
     * Crear producto → Actualizar solo precio → Actualizar solo cantidad
     */
    @Test
    void testE2E_PartialProductUpdateFlow() {
        // Arrange: Preparar datos
        String timestamp = String.valueOf(System.currentTimeMillis());

        // Paso 1: Obtener o crear categoría válida
        CategoryDto categoryDto = getAnyExistingCategoryOrCreate("E2E Test Category");

        ProductDto productDto = ProductDto.builder()
                .productTitle("E2E Test Product " + timestamp)
                .imageUrl("https://example.com/product.jpg")
                .sku("SKU-" + timestamp)
                .priceUnit(99.99)
                .quantity(10)
                .categoryDto(categoryDto)
                .build();

        ProductDto createdProduct = productService.save(productDto);
        assertNotNull(createdProduct, "Product should be created");
        Double originalPrice = createdProduct.getPriceUnit();
        Integer originalQuantity = createdProduct.getQuantity();

        // Paso 2: Actualizar solo el precio
        ProductDto priceUpdateDto = ProductDto.builder()
                .productId(createdProduct.getProductId())
                .productTitle(createdProduct.getProductTitle())
                .imageUrl(createdProduct.getImageUrl())
                .sku(createdProduct.getSku())
                .priceUnit(149.99)
                .quantity(originalQuantity)
                .categoryDto(categoryDto)
                .build();

        ProductDto priceUpdatedProduct = productService.update(priceUpdateDto);
        assertNotNull(priceUpdatedProduct, "Product should be updated");
        assertEquals(createdProduct.getProductId(), priceUpdatedProduct.getProductId(), 
                "Product ID should remain the same");
        assertEquals(149.99, priceUpdatedProduct.getPriceUnit(), 
                "Product price should be updated");
        assertEquals(originalQuantity, priceUpdatedProduct.getQuantity(), 
                "Product quantity should remain the same");

        // Paso 3: Actualizar solo la cantidad
        ProductDto quantityUpdateDto = ProductDto.builder()
                .productId(createdProduct.getProductId())
                .productTitle(createdProduct.getProductTitle())
                .imageUrl(createdProduct.getImageUrl())
                .sku(createdProduct.getSku())
                .priceUnit(149.99)
                .quantity(25)
                .categoryDto(categoryDto)
                .build();

        ProductDto quantityUpdatedProduct = productService.update(quantityUpdateDto);
        assertNotNull(quantityUpdatedProduct, "Product should be updated");
        assertEquals(createdProduct.getProductId(), quantityUpdatedProduct.getProductId(), 
                "Product ID should remain the same");
        assertEquals(149.99, quantityUpdatedProduct.getPriceUnit(), 
                "Product price should remain the same");
        assertEquals(25, quantityUpdatedProduct.getQuantity(), 
                "Product quantity should be updated");

        // Paso 4: Verificar que todas las actualizaciones se reflejaron
        ProductDto finalProduct = productService.findById(createdProduct.getProductId());
        assertNotNull(finalProduct, "Product should still be retrievable");
        assertEquals(149.99, finalProduct.getPriceUnit(), 
                "Product price should be updated in database");
        assertEquals(25, finalProduct.getQuantity(), 
                "Product quantity should be updated in database");

        // Assert: Verificar flujo completo
        assertNotEquals(originalPrice, finalProduct.getPriceUnit(), 
                "Price should have been updated");
        assertNotEquals(originalQuantity, finalProduct.getQuantity(), 
                "Quantity should have been updated");
    }

    /**
     * Test E2E: Flujo de creación de producto con categoría
     * Crear categoría → Crear producto con categoría → Verificar categoría
     */
    @Test
    void testE2E_ProductWithCategoryFlow() {
        // Arrange: Preparar datos
        String timestamp = String.valueOf(System.currentTimeMillis());

        // Paso 1: Obtener o crear categoría válida
        CategoryDto categoryDto = getAnyExistingCategoryOrCreate("E2E Test Category " + timestamp);

        // Paso 2: Crear producto con categoría
        ProductDto productDto = ProductDto.builder()
                .productTitle("E2E Test Product with Category " + timestamp)
                .imageUrl("https://example.com/product.jpg")
                .sku("SKU-CAT-" + timestamp)
                .priceUnit(199.99)
                .quantity(15)
                .categoryDto(categoryDto)
                .build();

        ProductDto createdProduct = productService.save(productDto);
        assertNotNull(createdProduct, "Product should be created");
        assertNotNull(createdProduct.getCategoryDto(), "Product should have a category");
        assertEquals(categoryDto.getCategoryTitle(), 
                createdProduct.getCategoryDto().getCategoryTitle(), 
                "Category title should match");

        // Paso 3: Verificar que el producto mantiene la categoría al recuperarlo
        ProductDto retrievedProduct = productService.findById(createdProduct.getProductId());
        assertNotNull(retrievedProduct, "Product should be retrievable");
        assertNotNull(retrievedProduct.getCategoryDto(), "Product should still have a category");
        assertEquals(categoryDto.getCategoryTitle(), 
                retrievedProduct.getCategoryDto().getCategoryTitle(), 
                "Category title should match after retrieval");

        // Paso 4: Actualizar producto con nueva categoría (existente o creada)
        CategoryDto newCategoryDto = getAnyExistingCategoryOrCreate("E2E Test New Category " + timestamp);

        ProductDto updateDto = ProductDto.builder()
                .productId(createdProduct.getProductId())
                .productTitle(createdProduct.getProductTitle())
                .imageUrl(createdProduct.getImageUrl())
                .sku(createdProduct.getSku())
                .priceUnit(createdProduct.getPriceUnit())
                .quantity(createdProduct.getQuantity())
                .categoryDto(newCategoryDto)
                .build();

        ProductDto updatedProduct = productService.update(updateDto);
        assertNotNull(updatedProduct, "Product should be updated");
        assertNotNull(updatedProduct.getCategoryDto(), "Product should have a category");
        assertEquals(newCategoryDto.getCategoryTitle(), 
                updatedProduct.getCategoryDto().getCategoryTitle(), 
                "Category title should be updated");

        // Assert: Verificar flujo completo
        assertNotNull(createdProduct.getCategoryDto(), "Product should have category");
        assertNotNull(updatedProduct.getCategoryDto(), "Product should have updated category");
    }

    /**
     * Test E2E: Soft delete - producto no debe aparecer en listados tras eliminar
     */
    @Test
    void testE2E_SoftDeleteProduct_ShouldNotAppearInListings() {
        String ts = String.valueOf(System.currentTimeMillis());
        CategoryDto category = getAnyExistingCategoryOrCreate("E2E SoftDelete Cat " + ts);

        ProductDto p = productService.save(ProductDto.builder()
                .productTitle("Delete Me " + ts)
                .imageUrl("https://example.com/p.jpg")
                .sku("SKU-DEL-" + ts)
                .priceUnit(10.0)
                .quantity(1)
                .categoryDto(category)
                .build());

        assertNotNull(p.getProductId());

        // Eliminar (soft delete mueve a categoría 'Deleted')
        productService.deleteById(p.getProductId());

        // findById debería lanzar not found (excluye Deleted)
        assertThrows(com.selimhorri.app.exception.custom.ResourceNotFoundException.class,
                () -> productService.findById(p.getProductId()));

        // findAll no debe contenerlo
        List<ProductDto> all = productService.findAll();
        boolean present = all.stream().anyMatch(x -> x.getProductId().equals(p.getProductId()));
        assertFalse(present, "Deleted product should not appear in listings");
    }
}

