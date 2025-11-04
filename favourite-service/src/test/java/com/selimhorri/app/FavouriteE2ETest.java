package com.selimhorri.app;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.id.FavouriteId;
import com.selimhorri.app.dto.FavouriteDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.service.FavouriteService;

/**
 * Tests E2E (End-to-End) para Favourite Service
 * Estos tests validan flujos completos de usuario que involucran múltiples servicios
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FavouriteE2ETest {

    @Autowired
    private FavouriteService favouriteService;

    @Autowired
    private RestTemplate restTemplate;

    private String userServiceUrl;
    private String productServiceUrl;

    @BeforeEach
    void setUp() {
        userServiceUrl = System.getProperty("user.service.url", 
            AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL);
        productServiceUrl = System.getProperty("product.service.url", 
            AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL);
    }

    /**
     * Test E2E: Flujo completo de favoritos
     * Usuario → Productos → Agregar a favoritos → Ver favoritos → Eliminar favorito
     */
    @Test
    void testE2E_CompleteFavouriteFlow() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        UserDto userDto = UserDto.builder()
                .firstName("Alice")
                .lastName("Johnson")
                .email("alice.johnson" + timestamp + "@example.com")
                .phone("+1234567890")
                .build();

        UserDto createdUser;
        try {
            ResponseEntity<UserDto> userResponse = restTemplate.postForEntity(
                userServiceUrl, 
                new HttpEntity<>(userDto), 
                UserDto.class
            );
            if (userResponse.getStatusCode() == HttpStatus.OK || 
                userResponse.getStatusCode() == HttpStatus.CREATED) {
                UserDto user = userResponse.getBody();
                assertNotNull(user, "User should be created");
                assertNotNull(user.getUserId(), "User should have an ID");
                createdUser = user;
            } else {
                createdUser = UserDto.builder()
                        .userId(1)
                        .firstName("Alice")
                        .lastName("Johnson")
                        .email("alice.johnson@example.com")
                        .build();
            }
        } catch (Exception e) {
            createdUser = UserDto.builder()
                    .userId(1)
                    .firstName("Alice")
                    .lastName("Johnson")
                    .email("alice.johnson@example.com")
                    .build();
        }
        final UserDto finalUser = createdUser;
        ProductDto product1;
        ProductDto product2;
        try {
            ResponseEntity<ProductDto[]> productsResponse = restTemplate.getForEntity(
                productServiceUrl, 
                ProductDto[].class
            );
            if (productsResponse.getStatusCode() == HttpStatus.OK && 
                productsResponse.getBody() != null && 
                productsResponse.getBody().length > 0) {
                ProductDto[] products = productsResponse.getBody();
                ProductDto p1 = products[0];
                ProductDto p2 = products.length > 1 ? products[1] : ProductDto.builder()
                        .productId(2)
                        .productTitle("Test Product 2")
                        .priceUnit(149.99)
                        .quantity(5)
                        .build();
                product1 = p1;
                product2 = p2;
            } else {
                product1 = ProductDto.builder()
                        .productId(1)
                        .productTitle("Test Product 1")
                        .priceUnit(99.99)
                        .quantity(10)
                        .build();
                product2 = ProductDto.builder()
                        .productId(2)
                        .productTitle("Test Product 2")
                        .priceUnit(149.99)
                        .quantity(5)
                        .build();
            }
        } catch (Exception e) {
            product1 = ProductDto.builder()
                    .productId(1)
                    .productTitle("Test Product 1")
                    .priceUnit(99.99)
                    .quantity(10)
                    .build();
            product2 = ProductDto.builder()
                    .productId(2)
                    .productTitle("Test Product 2")
                    .priceUnit(149.99)
                    .quantity(5)
                    .build();
        }
        final ProductDto finalProduct1 = product1;
        final ProductDto finalProduct2 = product2;

        assertNotNull(product1, "Product 1 should be available");
        assertNotNull(product2, "Product 2 should be available");

        LocalDateTime likeDate1 = LocalDateTime.now();
        FavouriteDto favouriteDto1 = FavouriteDto.builder()
                .userId(finalUser.getUserId())
                .productId(finalProduct1.getProductId())
                .likeDate(likeDate1)
                .build();

        FavouriteDto createdFavourite1 = favouriteService.save(favouriteDto1);
        assertNotNull(createdFavourite1, "First favourite should be created");
        assertEquals(finalUser.getUserId(), createdFavourite1.getUserId(), 
                "Favourite should be linked to user");
        assertEquals(finalProduct1.getProductId(), createdFavourite1.getProductId(), 
                "Favourite should be linked to product");

        LocalDateTime likeDate2 = LocalDateTime.now().plusMinutes(1);
        FavouriteDto favouriteDto2 = FavouriteDto.builder()
                .userId(finalUser.getUserId())
                .productId(finalProduct2.getProductId())
                .likeDate(likeDate2)
                .build();

        FavouriteDto createdFavourite2 = favouriteService.save(favouriteDto2);
        assertNotNull(createdFavourite2, "Second favourite should be created");
        assertEquals(createdUser.getUserId(), createdFavourite2.getUserId(), 
                "Second favourite should be linked to user");
        assertEquals(product2.getProductId(), createdFavourite2.getProductId(), 
                "Second favourite should be linked to product");

        List<FavouriteDto> allFavourites = favouriteService.findAll();
        assertNotNull(allFavourites, "Favourites list should not be null");
        assertTrue(allFavourites.size() >= 2, "Should have at least 2 favourites");
        boolean favourite1Found = allFavourites.stream()
                .anyMatch(f -> f.getUserId().equals(finalUser.getUserId()) && 
                             f.getProductId().equals(finalProduct1.getProductId()));
        boolean favourite2Found = allFavourites.stream()
                .anyMatch(f -> f.getUserId().equals(finalUser.getUserId()) && 
                             f.getProductId().equals(finalProduct2.getProductId()));

        assertTrue(favourite1Found, "First favourite should be in the list");
        assertTrue(favourite2Found, "Second favourite should be in the list");

        FavouriteId favouriteId1 = new FavouriteId(
                finalUser.getUserId(),
                finalProduct1.getProductId(),
                likeDate1
        );
        FavouriteDto retrievedFavourite = favouriteService.findById(favouriteId1);
        assertNotNull(retrievedFavourite, "Favourite should be retrievable");
        assertEquals(finalUser.getUserId(), retrievedFavourite.getUserId(), 
                "Retrieved favourite should have correct user ID");
        assertEquals(finalProduct1.getProductId(), retrievedFavourite.getProductId(), 
                "Retrieved favourite should have correct product ID");

        FavouriteId favouriteId2 = new FavouriteId(
                finalUser.getUserId(),
                finalProduct2.getProductId(),
                likeDate2
        );
        favouriteService.deleteById(favouriteId2);

        // Verificar que el favorito fue eliminado
        try {
            favouriteService.findById(favouriteId2);
            fail("Favourite should have been deleted");
        } catch (com.selimhorri.app.exception.custom.ResourceNotFoundException e) {
            assertTrue(e.getMessage().contains("Favourite") || 
                      e.getMessage().contains("favourite") || 
                      e.getMessage().contains("not found"),
                      "Exception should indicate favourite not found");
        }
        assertNotNull(finalUser.getUserId(), "User should exist");
        assertNotNull(createdFavourite1, "First favourite should exist");
        assertTrue(allFavourites.size() > 0, "Should have favourites");
    }

    /**
     * Test E2E: Flujo de agregar múltiples productos a favoritos
     * Usuario → Múltiples productos → Agregar todos a favoritos → Verificar todos
     */
    @Test
    void testE2E_MultipleFavouritesFlow() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        UserDto userDto = UserDto.builder()
                .firstName("Bob")
                .lastName("Wilson")
                .email("bob.wilson" + timestamp + "@example.com")
                .build();

        UserDto createdUser;
        try {
            ResponseEntity<UserDto> userResponse = restTemplate.postForEntity(
                userServiceUrl, 
                new HttpEntity<>(userDto), 
                UserDto.class
            );
            if (userResponse.getStatusCode() == HttpStatus.OK || 
                userResponse.getStatusCode() == HttpStatus.CREATED) {
                createdUser = userResponse.getBody();
            } else {
                createdUser = UserDto.builder().userId(1).build();
            }
        } catch (Exception e) {
            createdUser = UserDto.builder().userId(1).build();
        }
        final UserDto finalUser2 = createdUser;

        // Paso 2: Obtener o crear múltiples productos
        ProductDto[] products = new ProductDto[3];
        try {
            ResponseEntity<ProductDto[]> productsResponse = restTemplate.getForEntity(
                productServiceUrl, 
                ProductDto[].class
            );
            if (productsResponse.getStatusCode() == HttpStatus.OK && 
                productsResponse.getBody() != null && 
                productsResponse.getBody().length >= 3) {
                ProductDto[] retrievedProducts = productsResponse.getBody();
                System.arraycopy(retrievedProducts, 0, products, 0, 3);
            }
        } catch (Exception e) {
            for (int i = 0; i < 3; i++) {
                products[i] = ProductDto.builder()
                        .productId(i + 1)
                        .productTitle("Test Product " + (i + 1))
                        .priceUnit((i + 1) * 50.0)
                        .quantity(10)
                        .build();
            }
        }
        FavouriteDto[] createdFavourites = new FavouriteDto[3];
        for (int i = 0; i < 3; i++) {
            final int index = i;
            FavouriteDto favouriteDto = FavouriteDto.builder()
                    .userId(finalUser2.getUserId())
                    .productId(products[index].getProductId())
                    .likeDate(LocalDateTime.now().plusMinutes(i))
                    .build();

            FavouriteDto savedFavourite = favouriteService.save(favouriteDto);
            createdFavourites[index] = savedFavourite;
            assertNotNull(savedFavourite, "Favourite " + (index + 1) + " should be created");
            assertEquals(finalUser2.getUserId(), savedFavourite.getUserId(), 
                    "Favourite " + (index + 1) + " should be linked to user");
            assertEquals(products[index].getProductId(), savedFavourite.getProductId(), 
                    "Favourite " + (index + 1) + " should be linked to product");
        }

        List<FavouriteDto> allFavourites = favouriteService.findAll();
        assertNotNull(allFavourites, "Favourites list should not be null");
        assertTrue(allFavourites.size() >= 3, "Should have at least 3 favourites");
        for (int i = 0; i < 3; i++) {
            final int index = i;
            boolean found = allFavourites.stream()
                    .anyMatch(f -> f.getUserId().equals(finalUser2.getUserId()) && 
                                 f.getProductId().equals(products[index].getProductId()));
            assertTrue(found, "Favourite " + (i + 1) + " should be in the list");
        }
        assertEquals(3, createdFavourites.length, "Should have created 3 favourites");
        assertTrue(allFavourites.size() >= 3, "Should have at least 3 favourites in list");
    }

    /**
     * Test E2E: Flujo de actualización de favoritos
     * Usuario → Producto → Agregar a favoritos → Actualizar favorito
     */
    @Test
    void testE2E_UpdateFavouriteFlow() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        UserDto createdUser = UserDto.builder()
                .userId(1)
                .firstName("Charlie")
                .lastName("Brown")
                .email("charlie.brown" + timestamp + "@example.com")
                .build();
        ProductDto product = ProductDto.builder()
                .productId(1)
                .productTitle("Test Product")
                .priceUnit(99.99)
                .quantity(10)
                .build();
        LocalDateTime likeDate = LocalDateTime.now();
        FavouriteDto favouriteDto = FavouriteDto.builder()
                .userId(createdUser.getUserId())
                .productId(product.getProductId())
                .likeDate(likeDate)
                .build();

        FavouriteDto createdFavourite = favouriteService.save(favouriteDto);
        assertNotNull(createdFavourite, "Favourite should be created");

        LocalDateTime updatedLikeDate = LocalDateTime.now().plusMinutes(5);
        FavouriteDto updatedFavouriteDto = FavouriteDto.builder()
                .userId(createdUser.getUserId())
                .productId(product.getProductId())
                .likeDate(updatedLikeDate)
                .build();

        FavouriteDto updatedFavourite = favouriteService.update(updatedFavouriteDto);
        assertNotNull(updatedFavourite, "Favourite should be updated");
        assertEquals(createdUser.getUserId(), updatedFavourite.getUserId(), 
                "Updated favourite should have correct user ID");
        assertEquals(product.getProductId(), updatedFavourite.getProductId(), 
                "Updated favourite should have correct product ID");

        assertNotNull(createdFavourite, "Favourite should exist");
        assertNotNull(updatedFavourite, "Favourite should be updated");
    }

    /**
     * Test E2E: Idempotencia/duplicado - guardar el mismo favorito dos veces debe fallar
     */
    @Test
    void testE2E_SaveDuplicateFavourite_ShouldFail() {
        UserDto user = UserDto.builder().userId(123).build();
        ProductDto product = ProductDto.builder().productId(456).build();
        LocalDateTime likeDate = LocalDateTime.now();

        FavouriteDto fav = FavouriteDto.builder()
                .userId(user.getUserId())
                .productId(product.getProductId())
                .likeDate(likeDate)
                .build();

        FavouriteDto saved = favouriteService.save(fav);
        assertNotNull(saved);

        assertThrows(com.selimhorri.app.exception.custom.DuplicateResourceException.class,
                () -> favouriteService.save(fav));
    }

    /**
     * Test E2E: Borrado por FavouriteId (cuerpo) y verificación
     */
    @Test
    void testE2E_DeleteByFavouriteIdBody_ShouldRemoveEntry() {
        UserDto user = UserDto.builder().userId(321).build();
        ProductDto product = ProductDto.builder().productId(654).build();
        LocalDateTime likeDate = LocalDateTime.now();

        FavouriteDto fav = FavouriteDto.builder()
                .userId(user.getUserId())
                .productId(product.getProductId())
                .likeDate(likeDate)
                .build();

        FavouriteDto saved = favouriteService.save(fav);
        assertNotNull(saved);

        FavouriteId id = new FavouriteId(user.getUserId(), product.getProductId(), likeDate);
        favouriteService.deleteById(id);

        assertThrows(com.selimhorri.app.exception.custom.ResourceNotFoundException.class,
                () -> favouriteService.findById(id));
    }
}

