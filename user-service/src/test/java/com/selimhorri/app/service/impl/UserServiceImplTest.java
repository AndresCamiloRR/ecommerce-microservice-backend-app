package com.selimhorri.app.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.selimhorri.app.domain.Credential;
import com.selimhorri.app.domain.User;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.UserObjectNotFoundException;
import com.selimhorri.app.helper.UserMappingHelper;
import com.selimhorri.app.repository.UserRepository;
import com.selimhorri.app.service.impl.UserServiceImpl;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;

    private static final String DEFAULT_FIRST_NAME = "John";
    private static final String DEFAULT_LAST_NAME = "Doe";
    private static final String DEFAULT_EMAIL = "john.doe@example.com";
    private static final String DEFAULT_PHONE = "1234567890";
    private static final String DEFAULT_IMAGE_URL = "http://example.com/image.jpg";
    private static final String DEFAULT_USERNAME = "johndoe";
    private static final String DEFAULT_PASSWORD = "password123";
    
    @BeforeEach
    void setUp() {

        Credential credential = Credential.builder()
            .username(DEFAULT_USERNAME)
            .password(DEFAULT_PASSWORD)
            .isEnabled(true)
            .isAccountNonExpired(true)
            .isAccountNonLocked(true)
            .isCredentialsNonExpired(true)
            .build();

        user = User.builder()
                .userId(1)
                .firstName(DEFAULT_FIRST_NAME)
                .lastName(DEFAULT_LAST_NAME)
                .email(DEFAULT_EMAIL)
                .phone(DEFAULT_PHONE)
                .imageUrl(DEFAULT_IMAGE_URL)
                .credential(credential)
                .build();

        if (credential != null) {
            credential.setUser(user);
        }
    }

    @Test
    void findAll_shouldReturnListOfUserDtos() {
        when(this.userRepository.findAll()).thenReturn(Collections.singletonList(
            user
            ));
        List<UserDto> userDtos = this.userService.findAll();
        assertNotNull(userDtos);
        assertEquals(1, userDtos.size());
        assertEquals(DEFAULT_FIRST_NAME, userDtos.get(0).getFirstName());
        verify(this.userRepository, times(1)).findAll();
    }

    @Test
    void findById_shouldReturnUserDto_whenUserExists() {
        when(this.userRepository.findById(1)).thenReturn(Optional.of(user));
        UserDto userDto = this.userService.findById(1);
        assertNotNull(userDto);
        assertEquals(1, userDto.getUserId());
        assertEquals(DEFAULT_FIRST_NAME, userDto.getFirstName());
        verify(this.userRepository, times(1)).findById(1);
    }

    @Test
    void findById_shouldThrowUserObjectNotFoundException_whenUserDoesNotExist() {
        when(this.userRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(UserObjectNotFoundException.class, () -> {
            this.userService.findById(1);
        });
        verify(this.userRepository, times(1)).findById(1);
    }

    @Test
    void save_shouldReturnSavedUserDto() {
        Credential credentialEntity = Credential.builder()
            .credentialId(1)
            .username("testsaveuser")
            .password("password")
            .isEnabled(true)
            .isAccountNonExpired(true)
            .isAccountNonLocked(true)
            .isCredentialsNonExpired(true)
            .build();

        User userEntity = User.builder()
            .userId(1)
            .firstName("TestSave")
            .lastName("UserSave")
            .email("testsave@example.com")
            .credential(credentialEntity)
            .build();
        
        // Relación bidireccional entre User y Credential
        credentialEntity.setUser(userEntity);

        UserDto userDtoInput = UserMappingHelper.map(userEntity);
        
        when(this.userRepository.save(any(User.class))).thenReturn(userEntity);

        UserDto savedUserDto = this.userService.save(userDtoInput);

        assertNotNull(savedUserDto, "Saved UserDto should not be null");
        assertEquals(userEntity.getUserId(), savedUserDto.getUserId(), "User ID mismatch");
        assertEquals(userEntity.getFirstName(), savedUserDto.getFirstName(), "First name mismatch");
        assertEquals(userEntity.getEmail(), savedUserDto.getEmail(), "Email mismatch");
        
        assertNotNull(savedUserDto.getCredentialDto(), "CredentialDto in saved UserDto should not be null");
        assertEquals(credentialEntity.getUsername(), savedUserDto.getCredentialDto().getUsername(), "Username in CredentialDto mismatch");

        verify(this.userRepository, times(1)).save(any(User.class));
    }

    @Test
    void deleteById_shouldCallRepositoryDeleteById() {
        Integer userId = 1;
        doNothing().when(this.userRepository).deleteById(userId);
        this.userService.deleteById(userId);
        verify(this.userRepository, times(1)).deleteById(userId);
    }

}
