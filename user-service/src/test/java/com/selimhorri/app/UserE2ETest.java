package com.selimhorri.app;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.service.UserService;

@SpringBootTest
@DisabledIfEnvironmentVariable(named = "SPRING_PROFILES_ACTIVE", matches = "dev")
class UserE2ETest {

    @Autowired
    private UserService userService;

    @Test
    void testE2E_CreateUser_WithCredential() {
        String ts = String.valueOf(System.currentTimeMillis());
        UserDto user = UserDto.builder()
                .firstName("E2E")
                .lastName("User")
                .email("e2e." + ts + "@example.com")
                .credentialDto(Ccredential(ts))
                .build();

        UserDto saved = userService.save(user);
        assertNotNull(saved.getUserId());
        assertNotNull(saved.getCredentialDto());
        assertEquals(user.getCredentialDto().getUsername(), saved.getCredentialDto().getUsername());
    }

    @Test
    void testE2E_GetUpdateDeleteUser_Flow() {
        String ts = String.valueOf(System.currentTimeMillis());
        UserDto user = UserDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john." + ts + "@example.com")
                .credentialDto(Ccredential("john" + ts))
                .build();

        UserDto saved = userService.save(user);
        assertNotNull(saved.getUserId());

        UserDto fetched = userService.findById(saved.getUserId());
        assertEquals(saved.getUserId(), fetched.getUserId());

        UserDto updated = userService.update(UserDto.builder()
                .userId(saved.getUserId())
                .firstName("Johnny")
                .lastName("Doe")
                .email("john." + ts + "@example.com")
                .credentialDto(saved.getCredentialDto())
                .build());
        assertEquals("Johnny", updated.getFirstName());

        userService.deleteById(saved.getUserId());
        assertThrows(com.selimhorri.app.exception.custom.ResourceNotFoundException.class,
                () -> userService.findById(saved.getUserId()));
    }

    @Test
    void testE2E_FindByUsername() {
        String ts = String.valueOf(System.currentTimeMillis());
        String username = "alice" + ts;
        UserDto user = UserDto.builder()
                .firstName("Alice")
                .lastName("Smith")
                .email("alice." + ts + "@example.com")
                .credentialDto(Ccredential(username))
                .build();

        UserDto saved = userService.save(user);
        UserDto byUsername = userService.findByUsername(username);
        assertEquals(saved.getUserId(), byUsername.getUserId());
    }

    @Test
    void testE2E_DuplicateUsername_ShouldFail() {
        String ts = String.valueOf(System.currentTimeMillis());
        String username = "dupuser" + ts;

        UserDto u1 = UserDto.builder()
                .firstName("A")
                .lastName("A")
                .email("dup1." + ts + "@example.com")
                .credentialDto(Ccredential(username))
                .build();
        userService.save(u1);

        UserDto u2 = UserDto.builder()
                .firstName("B")
                .lastName("B")
                .email("dup2." + ts + "@example.com")
                .credentialDto(Ccredential(username))
                .build();

        assertThrows(com.selimhorri.app.exception.custom.DuplicateResourceException.class,
                () -> userService.save(u2));
    }

    @Test
    void testE2E_FindAll_ShouldContainCreatedUser() {
        String ts = String.valueOf(System.currentTimeMillis());
        UserDto user = UserDto.builder()
                .firstName("Lister")
                .lastName("User")
                .email("lister." + ts + "@example.com")
                .credentialDto(Ccredential("lister" + ts))
                .build();

        UserDto saved = userService.save(user);
        boolean exists = userService.findAll().stream()
                .anyMatch(u -> u.getUserId().equals(saved.getUserId()));
        assertTrue(exists);
    }

    private CredentialDto Ccredential(String username) {
        return CredentialDto.builder()
                .username(username)
                .password("pwd-" + username)
                .build();
    }
}


