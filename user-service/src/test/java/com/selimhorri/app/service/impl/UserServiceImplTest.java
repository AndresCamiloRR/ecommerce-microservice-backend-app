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

        // Entidad User para comparaciones directas con el repositorio
        Credential credential = Credential.builder()
            // Assuming credential might also have an ID, though not strictly necessary for this fix
            // .credentialId(1) 
            .username(DEFAULT_USERNAME)
            .password(DEFAULT_PASSWORD)
            .isEnabled(true)
            .isAccountNonExpired(true)
            .isAccountNonLocked(true)
            .isCredentialsNonExpired(true)
            .build();

        user = User.builder()
                .userId(1) // This is the crucial fix for the findById test
                .firstName(DEFAULT_FIRST_NAME)
                .lastName(DEFAULT_LAST_NAME)
                .email(DEFAULT_EMAIL)
                .phone(DEFAULT_PHONE)
                .imageUrl(DEFAULT_IMAGE_URL)
                .credential(credential)
                .build();
        // Link credential to user for bidirectional relationship if necessary in your mapping
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
        // 1. Prepare the Credential and User entities that we expect to work with.
        Credential credentialEntity = Credential.builder()
            .credentialId(1) // Assuming an ID for mapping if necessary
            .username("testsaveuser")
            .password("password")
            .isEnabled(true)
            .isAccountNonExpired(true)
            .isAccountNonLocked(true)
            .isCredentialsNonExpired(true)
            .build();

        User userEntity = User.builder()
            .userId(1) // Assuming an ID
            .firstName("TestSave")
            .lastName("UserSave")
            .email("testsave@example.com")
            .credential(credentialEntity) // Associate credential with user
            .build();
        
        // Establish the bidirectional relationship if your JPA/mapping setup relies on it.
        // This is crucial if the mapping logic navigates from credential back to user.
        credentialEntity.setUser(userEntity);

        // 2. Create the UserDto that will be the input to the service's save method.
        // UserMappingHelper.map(userEntity) will convert the User entity (with its Credential) to UserDto (with CredentialDto).
        // This step simulates what would happen if you had a User entity and wanted to create a DTO from it.
        UserDto userDtoInput = UserMappingHelper.map(userEntity);
        // Ensure the credential DTO part is also correctly mapped for the input if needed,
        // or that UserMappingHelper.map(userEntity) correctly populates it.
        // For this test, userDtoInput is the argument to userService.save().

        // 3. Mock the repository's save method.
        // When userRepository.save() is called with any User object,
        // it should return our fully initialized userEntity (which includes the credential).
        // The UserServiceImpl will then call UserMappingHelper.map() on this returned userEntity.
        // This is where the NPE was likely occurring if the returned User had a null credential.
        when(this.userRepository.save(any(User.class))).thenReturn(userEntity);

        // 4. Call the service method.
        UserDto savedUserDto = this.userService.save(userDtoInput);

        // 5. Assertions.
        assertNotNull(savedUserDto, "Saved UserDto should not be null");
        assertEquals(userEntity.getUserId(), savedUserDto.getUserId(), "User ID mismatch");
        assertEquals(userEntity.getFirstName(), savedUserDto.getFirstName(), "First name mismatch");
        assertEquals(userEntity.getEmail(), savedUserDto.getEmail(), "Email mismatch");
        
        assertNotNull(savedUserDto.getCredentialDto(), "CredentialDto in saved UserDto should not be null");
        assertEquals(credentialEntity.getUsername(), savedUserDto.getCredentialDto().getUsername(), "Username in CredentialDto mismatch");
        // Add more assertions for other credential fields if necessary

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
