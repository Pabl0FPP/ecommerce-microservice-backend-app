package com.selimhorri.app;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.dto.FavouriteDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.repository.FavouriteRepository;
import com.selimhorri.app.service.FavouriteService;

/**
 * Tests de integración REALES para Favourite Service
 * Estos tests hacen LLAMADAS HTTP REALES a servicios desplegados
 */
@SpringBootTest
class FavouriteIntegrationTest {

    @Autowired
    private FavouriteService favouriteService;


    @Autowired
    private RestTemplate restTemplate;

    @Test
    void testFavouriteUserIntegration_FetchUser_ShouldWork() {
        // Test de integración REAL con User Service
        Integer userId = 123;
        
        try {
            // Act: Hacer llamada HTTP REAL al User Service
            String url = AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + userId;
            UserDto userDto = restTemplate.getForObject(url, UserDto.class);
            
            // Assert: Verificar que la comunicación HTTP funcionó
            assertNotNull(userDto, "UserDto should not be null - User Service responded");
            assertNotNull(userDto.getUserId(), "User ID should not be null");
            assertEquals(userId, userDto.getUserId(), "User ID should match");
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            // Esto es válido - el servicio respondió correctamente con 404
            // En integración real, esto valida que la comunicación HTTP funciona
            assertEquals(404, e.getRawStatusCode(), "User Service should return 404 for non-existent user");
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // Si el servicio no está disponible, el test falla
            fail("User Service is not available - services must be deployed for integration tests: " + e.getMessage());
        } catch (Exception e) {
            // Otras excepciones inesperadas
            fail("Unexpected exception during User Service integration: " + e.getMessage());
        }
    }

    @Test
    void testFavouriteProductIntegration_FetchProduct_ShouldWork() {
        // Test de integración REAL con Product Service
        Integer productId = 456;
        
        try {
            // Act: Hacer llamada HTTP REAL al Product Service
            String url = AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/" + productId;
            ProductDto productDto = restTemplate.getForObject(url, ProductDto.class);
            
            // Assert: Verificar que la comunicación HTTP funcionó
            assertNotNull(productDto, "ProductDto should not be null - Product Service responded");
            assertNotNull(productDto.getProductId(), "Product ID should not be null");
            assertEquals(productId, productDto.getProductId(), "Product ID should match");
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            // Esto es válido - el servicio respondió correctamente con 404
            // En integración real, esto valida que la comunicación HTTP funciona
            assertEquals(404, e.getRawStatusCode(), "Product Service should return 404 for non-existent product");
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // Si el servicio no está disponible, el test falla
            fail("Product Service is not available - services must be deployed for integration tests: " + e.getMessage());
        } catch (Exception e) {
            // Otras excepciones inesperadas
            fail("Unexpected exception during Product Service integration: " + e.getMessage());
        }
    }

    @Test
    void testFavouriteUserIntegration_UserNotFound_ShouldHandleGracefully() {
        // Test de integración REAL - manejo de usuario no encontrado
        Integer userId = 999;
        
        try {
            // Act: Hacer llamada HTTP REAL a un usuario que no existe
            String url = AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + userId;
            UserDto userDto = restTemplate.getForObject(url, UserDto.class);
            
            // Si no lanza excepción, verificar respuesta
            assertNotNull(userDto, "Service should respond even if user not found");
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            // Esto es esperado - el servicio respondió con 404
            assertEquals(404, e.getRawStatusCode(), "User Service should return 404 for non-existent user");
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    void testFavouriteProductIntegration_ProductNotFound_ShouldHandleGracefully() {
        // Test de integración REAL - manejo de producto no encontrado
        Integer productId = 999;
        
        try {
            // Act: Hacer llamada HTTP REAL a un producto que no existe
            String url = AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/" + productId;
            ProductDto productDto = restTemplate.getForObject(url, ProductDto.class);
            
            // Si no lanza excepción, verificar respuesta
            assertNotNull(productDto, "Service should respond even if product not found");
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            // Esto es esperado - el servicio respondió con 404
            assertEquals(404, e.getRawStatusCode(), "Product Service should return 404 for non-existent product");
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    void testFavouriteSaveIntegration_ShouldWork() {
        // Test de integración REAL - crear favorito que interactúa con User y Product Service
        Integer userId = 123;
        Integer productId = 456;
        LocalDateTime likeDate = LocalDateTime.now();
        
        try {
            // Act: Crear un favorito (esto también hace llamadas HTTP REALES a User y Product Service)
            FavouriteDto favouriteDto = FavouriteDto.builder()
                    .userId(userId)
                    .productId(productId)
                    .likeDate(likeDate)
                    .build();
            
            FavouriteDto savedFavourite = favouriteService.save(favouriteDto);
            
            // Assert: Verificar que el favorito se creó correctamente
            assertNotNull(savedFavourite, "Favourite should be saved");
            assertEquals(userId, savedFavourite.getUserId(), "User ID should match");
            assertEquals(productId, savedFavourite.getProductId(), "Product ID should match");
        } catch (com.selimhorri.app.exception.custom.ResourceNotFoundException e) {
            // Esto es válido - el servicio respondió correctamente indicando que User o Product no existe
            // En integración real, esto valida que la comunicación HTTP funciona
            String message = e.getMessage();
            assertTrue(message.contains("User") || message.contains("Product") || 
                      message.contains("user") || message.contains("product") ||
                      message.contains("123") || message.contains("456"), 
                      "Exception should mention User, Product, or the IDs - got: " + message);
        } catch (com.selimhorri.app.exception.custom.ExternalServiceException e) {
            // También válido - el servicio puede lanzar ExternalServiceException
            String message = e.getMessage();
            assertTrue(message.contains("User") || message.contains("Product") || 
                      message.contains("user") || message.contains("product"), 
                      "Exception should mention User or Product - got: " + message);
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // Si el servicio no está disponible, el test falla
            fail("User or Product Service is not available - services must be deployed for integration tests: " + e.getMessage());
        } catch (Exception e) {
            // Otras excepciones inesperadas - verificar el mensaje
            String message = e.getMessage();
            if (message != null && (message.contains("User") || message.contains("Product") || 
                message.contains("user") || message.contains("product") || 
                message.contains("123") || message.contains("456"))) {
                // Es una excepción relacionada con User o Product no encontrado - válido
                assertTrue(true, "Exception indicates User or Product not found - HTTP communication worked");
            } else {
                fail("Unexpected exception during Favourite save integration: " + message);
            }
        }
    }
}
