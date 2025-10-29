package com.selimhorri.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.selimhorri.app.domain.User;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.custom.DuplicateResourceException;
import com.selimhorri.app.exception.custom.ResourceNotFoundException;
import com.selimhorri.app.helper.UserMappingHelper;
import com.selimhorri.app.repository.UserRepository;
import com.selimhorri.app.service.impl.UserServiceImpl;

public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void save_whenEmailExists_throwsDuplicate() {
        UserDto dto = UserDto.builder().email("test@example.com").build();
        when(this.userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> userService.save(dto));
    }

    @Test
    void save_whenUsernameExists_throwsDuplicate() {
        UserDto dto = UserDto.builder()
                .credentialDto(com.selimhorri.app.dto.CredentialDto.builder().username("u1").build())
                .build();

        when(this.userRepository.findByCredentialUsername("u1")).thenReturn(Optional.of(new User()));

        assertThrows(DuplicateResourceException.class, () -> userService.save(dto));
    }

    @Test
    void findById_notFound_throwsResourceNotFound() {
        when(this.userRepository.findByIdWithCredential(99)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.findById(99));
    }

    @Test
    void save_success_returnsDto() {
        UserDto dto = UserDto.builder().email("ok@example.com").build();
        User entity = UserMappingHelper.map(dto);
        when(this.userRepository.existsByEmailIgnoreCase("ok@example.com")).thenReturn(false);
        when(this.userRepository.save(any(User.class))).thenReturn(entity);

        UserDto result = userService.save(dto);
        assertNotNull(result);
        assertEquals(dto.getEmail(), result.getEmail());
    }

    @Test
    void update_nonExisting_throwsResourceNotFound() {
        UserDto dto = UserDto.builder().userId(123).firstName("A").build();
        when(this.userRepository.existsById(123)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> userService.update(dto));
    }

    @Test
    void findAll_returnsMappedList() {
        // build entity with credential
        com.selimhorri.app.domain.Credential cred = com.selimhorri.app.domain.Credential.builder()
                .credentialId(1)
                .username("user1")
                .password("pwd")
                .roleBasedAuthority(com.selimhorri.app.domain.RoleBasedAuthority.ROLE_USER)
                .isEnabled(true)
                .build();

        User user = User.builder()
                .userId(10)
                .firstName("Fn")
                .lastName("Ln")
                .email("f@e.com")
                .phone("123")
                .build();
        // set bi-directional
        cred.setUser(user);
        user.setCredential(cred);

        when(this.userRepository.findAllWithCredentials()).thenReturn(java.util.List.of(user));

        java.util.List<UserDto> list = userService.findAll();
        assertNotNull(list);
        assertEquals(1, list.size());
        UserDto dto = list.get(0);
        assertEquals(user.getUserId(), dto.getUserId());
        assertNotNull(dto.getCredentialDto());
        assertEquals("user1", dto.getCredentialDto().getUsername());
    }

    @Test
    void findByUsername_returnsMapped() {
        User user = User.builder().userId(20).email("u@e.com").build();
        com.selimhorri.app.domain.Credential cred = com.selimhorri.app.domain.Credential.builder()
                .credentialId(2)
                .username("findme")
                .user(user)
                .build();
        user.setCredential(cred);

        when(this.userRepository.findByCredentialUsername("findme")).thenReturn(Optional.of(user));

        UserDto dto = userService.findByUsername("findme");
        assertNotNull(dto);
        assertEquals(20, dto.getUserId());
        assertNotNull(dto.getCredentialDto());
        assertEquals("findme", dto.getCredentialDto().getUsername());
    }

    @Test
    void update_existing_updatesFields() {
        // existing user
        User existing = User.builder().userId(1).firstName("Old").lastName("Last").email("o@e.com").build();
        com.selimhorri.app.domain.Credential existingCred = com.selimhorri.app.domain.Credential.builder()
                .credentialId(5)
                .username("olduser")
                .password("oldpwd")
                .isEnabled(true)
                .user(existing)
                .build();
        existing.setCredential(existingCred);

        when(this.userRepository.findById(1)).thenReturn(Optional.of(existing));
        when(this.userRepository.existsById(1)).thenReturn(true);
        when(this.userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        com.selimhorri.app.dto.CredentialDto credDto = com.selimhorri.app.dto.CredentialDto.builder()
                .username("newuser").password("newpwd").isEnabled(false).build();

        UserDto updateDto = UserDto.builder().userId(1).firstName("New").credentialDto(credDto).build();

        UserDto result = userService.update(updateDto);
        assertNotNull(result);
        assertEquals("New", result.getFirstName());
        assertNotNull(result.getCredentialDto());
        assertEquals("newuser", result.getCredentialDto().getUsername());
        assertEquals("newpwd", result.getCredentialDto().getPassword());
    }

    @Test
    void deleteById_existing_deletes() {
        when(this.userRepository.existsById(77)).thenReturn(true);
        doNothing().when(this.userRepository).deleteById(77);

        assertDoesNotThrow(() -> userService.deleteById(77));
        verify(this.userRepository).deleteById(77);
    }
}
