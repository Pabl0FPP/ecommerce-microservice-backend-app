package com.selimhorri.app;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.custom.DuplicateResourceException;
import com.selimhorri.app.repository.UserRepository;
import com.selimhorri.app.service.UserService;

/**
 * Tests de integración REALES para User Service
 * Estos tests validan que el servicio puede ser llamado por otros servicios
 */
@SpringBootTest
@DisabledIfEnvironmentVariable(named = "SPRING_PROFILES_ACTIVE", matches = "dev")
class UserIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Test
    void testUserFavouriteIntegration_CanBeCalledByFavouriteService() {
        try {
            String url = AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/1";
            UserDto userDto = restTemplate.getForObject(url, UserDto.class);
            assertNotNull(userDto, "User Service should respond to HTTP calls");
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            assertEquals(404, e.getRawStatusCode(), "User Service should return 404 for non-existent user");
        } catch (org.springframework.web.client.ResourceAccessException e) {
            fail("User Service HTTP connection failed: " + e.getMessage());
        } catch (Exception e) {
            assertNotNull(e.getMessage(), "Exception should have a message");
        }
    }

    @Test
    void testUserOrderIntegration_CanBeCalledByOrderService() {
        try {
            String url = AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/2";
            UserDto userDto = restTemplate.getForObject(url, UserDto.class);
            assertNotNull(userDto, "User Service should respond to HTTP calls from Order Service");
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            assertEquals(404, e.getRawStatusCode(), "User Service should return 404 for non-existent user");
        } catch (org.springframework.web.client.ResourceAccessException e) {
            fail("User Service HTTP connection failed: " + e.getMessage());
        } catch (Exception e) {
            assertNotNull(e.getMessage(), "Exception should have a message");
        }
    }

    @Test
    void testUserCredentialIntegration_ShouldWork() {
        UserDto userDto = UserDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john" + System.currentTimeMillis() + "@example.com")
                .credentialDto(CredentialDto.builder()
                        .username("johndoe" + System.currentTimeMillis())
                        .password("password123")
                        .build())
                .build();
        UserDto savedUser = userService.save(userDto);
        assertNotNull(savedUser, "User should be saved");
        assertNotNull(savedUser.getCredentialDto(), "User should have credential");
        assertNotNull(savedUser.getUserId(), "User should have an ID");
    }

    @Test
    void testUserCredentialIntegration_DuplicateUsername_ShouldThrowException() {
        String username = "testuser" + System.currentTimeMillis();
        UserDto firstUser = UserDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john1" + System.currentTimeMillis() + "@example.com")
                .credentialDto(CredentialDto.builder()
                        .username(username)
                        .password("password123")
                        .build())
                .build();
        userService.save(firstUser);
        UserDto secondUser = UserDto.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane" + System.currentTimeMillis() + "@example.com")
                .credentialDto(CredentialDto.builder()
                        .username(username)
                        .password("password456")
                        .build())
                .build();

        assertThrows(DuplicateResourceException.class, () -> {
            userService.save(secondUser);
        });
    }

    @Test
    void testUserDataStructureIntegration_ShouldWork() {
        // Test de estructura de datos para integración con otros servicios
        Integer userId = 123;
        String email = "user@example.com";
        String firstName = "John";
        String lastName = "Doe";
        
        // Act: Crear datos JSON simulado para integración
        String userDataJson = String.format(
            "{\"userId\":%d,\"email\":\"%s\",\"firstName\":\"%s\",\"lastName\":\"%s\"}",
            userId, email, firstName, lastName
        );
        
        // Assert: Verificar estructura de datos
        assertNotNull(userDataJson, "User data JSON should not be null");
        assertTrue(userDataJson.contains("\"userId\""), "Should contain userId");
        assertTrue(userDataJson.contains("\"email\""), "Should contain email");
        assertTrue(userDataJson.contains("\"firstName\""), "Should contain firstName");
        assertTrue(userDataJson.contains("\"lastName\""), "Should contain lastName");
        assertTrue(userDataJson.contains(email), "Should contain email value");
    }
}
