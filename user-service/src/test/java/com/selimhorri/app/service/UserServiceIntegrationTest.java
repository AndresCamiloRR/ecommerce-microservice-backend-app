package com.selimhorri.app.service;

import com.selimhorri.app.domain.Credential;
import com.selimhorri.app.domain.User;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.UserObjectNotFoundException;
import com.selimhorri.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    private UserDto userDto;
    private User user;

    private static final String DEFAULT_FIRST_NAME = "John";
    private static final String DEFAULT_LAST_NAME = "Doe";
    private static final String DEFAULT_EMAIL = "john.doe@example.com";
    private static final String DEFAULT_PHONE = "1234567890";
    private static final String DEFAULT_IMAGE_URL = "http://example.com/image.jpg";
    private static final String DEFAULT_USERNAME = "johndoe";
    private static final String DEFAULT_PASSWORD = "password123";

    private static final String UPDATED_FIRST_NAME = "Jane";
    private static final String UPDATED_LAST_NAME = "Doe";
    private static final String UPDATED_EMAIL = "jane.doe@example.com";


    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        CredentialDto credentialDto = CredentialDto.builder()
                .username(DEFAULT_USERNAME)
                .password(DEFAULT_PASSWORD)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();

        userDto = UserDto.builder()
                .firstName(DEFAULT_FIRST_NAME)
                .lastName(DEFAULT_LAST_NAME)
                .email(DEFAULT_EMAIL)
                .phone(DEFAULT_PHONE)
                .imageUrl(DEFAULT_IMAGE_URL)
                .credentialDto(credentialDto)
                .build();

        // Entidad User para comparaciones directas con el repositorio
        Credential credential = Credential.builder()
            .username(DEFAULT_USERNAME)
            .password(DEFAULT_PASSWORD)
            .isEnabled(true)
            .isAccountNonExpired(true)
            .isAccountNonLocked(true)
            .isCredentialsNonExpired(true)
            .build();

        user = User.builder()
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
    void saveUser_shouldPersistUser() {
        UserDto savedUserDto = userService.save(userDto);

        assertThat(savedUserDto).isNotNull();
        assertThat(savedUserDto.getUserId()).isNotNull();
        assertThat(savedUserDto.getFirstName()).isEqualTo(DEFAULT_FIRST_NAME);
        assertThat(savedUserDto.getLastName()).isEqualTo(DEFAULT_LAST_NAME);
        assertThat(savedUserDto.getEmail()).isEqualTo(DEFAULT_EMAIL);
        assertThat(savedUserDto.getPhone()).isEqualTo(DEFAULT_PHONE);
        assertThat(savedUserDto.getImageUrl()).isEqualTo(DEFAULT_IMAGE_URL);
        assertThat(savedUserDto.getCredentialDto().getUsername()).isEqualTo(DEFAULT_USERNAME);

        Optional<User> repoUser = userRepository.findById(savedUserDto.getUserId());
        assertThat(repoUser).isPresent();
        assertThat(repoUser.get().getFirstName()).isEqualTo(DEFAULT_FIRST_NAME);
        assertThat(repoUser.get().getLastName()).isEqualTo(DEFAULT_LAST_NAME);
        assertThat(repoUser.get().getEmail()).isEqualTo(DEFAULT_EMAIL);
        assertThat(repoUser.get().getCredential().getUsername()).isEqualTo(DEFAULT_USERNAME);
    }

    @Test
    void updateUser_shouldModifyExistingUser() {
        User savedEntity = userRepository.saveAndFlush(user); // Guardar entidad base

        UserDto dtoToUpdate = UserDto.builder()
                .userId(savedEntity.getUserId())
                .firstName(UPDATED_FIRST_NAME)
                .lastName(UPDATED_LAST_NAME)
                .email(UPDATED_EMAIL)
                .phone(savedEntity.getPhone()) // Keep other fields same or update as needed
                .imageUrl(savedEntity.getImageUrl())
                .credentialDto(CredentialDto.builder() // Assume credential details might also be updatable or need to be passed
                    .credentialId(savedEntity.getCredential().getCredentialId())
                    .username(savedEntity.getCredential().getUsername()) // Usually username is not updated, but depends on logic
                    .password(savedEntity.getCredential().getPassword()) // Password update logic might be different
                    .isEnabled(savedEntity.getCredential().getIsEnabled())
                    .isAccountNonExpired(savedEntity.getCredential().getIsAccountNonExpired())
                    .isAccountNonLocked(savedEntity.getCredential().getIsAccountNonLocked())
                    .isCredentialsNonExpired(savedEntity.getCredential().getIsCredentialsNonExpired())
                    .build())
                .build();

        UserDto updatedDto = userService.update(dtoToUpdate);

        assertThat(updatedDto).isNotNull();
        assertThat(updatedDto.getUserId()).isEqualTo(savedEntity.getUserId());
        assertThat(updatedDto.getFirstName()).isEqualTo(UPDATED_FIRST_NAME);
        assertThat(updatedDto.getLastName()).isEqualTo(UPDATED_LAST_NAME);
        assertThat(updatedDto.getEmail()).isEqualTo(UPDATED_EMAIL);

        Optional<User> repoUser = userRepository.findById(savedEntity.getUserId());
        assertThat(repoUser).isPresent();
        assertThat(repoUser.get().getFirstName()).isEqualTo(UPDATED_FIRST_NAME);
        assertThat(repoUser.get().getLastName()).isEqualTo(UPDATED_LAST_NAME);
        assertThat(repoUser.get().getEmail()).isEqualTo(UPDATED_EMAIL);
    }

    @Test
    void deleteUserById_shouldRemoveUserFromDatabase() {
        User savedEntity = userRepository.saveAndFlush(user);
        Integer userIdToDelete = savedEntity.getUserId();

        assertThat(userRepository.findById(userIdToDelete)).isPresent();

        userService.deleteById(userIdToDelete);

        assertThat(userRepository.findById(userIdToDelete)).isNotPresent();
    }

    @Test
    void findUserById_whenExists_shouldReturnUser() {
        User savedEntity = userRepository.saveAndFlush(user);
        Integer userIdToFind = savedEntity.getUserId();

        UserDto foundDto = userService.findById(userIdToFind);

        assertThat(foundDto).isNotNull();
        assertThat(foundDto.getUserId()).isEqualTo(userIdToFind);
        assertThat(foundDto.getFirstName()).isEqualTo(DEFAULT_FIRST_NAME);
    }

    @Test
    void findUserById_whenNotExists_shouldThrowUserObjectNotFoundException() {
        Integer nonExistentId = -99;
        Exception exception = assertThrows(UserObjectNotFoundException.class, () -> {
            userService.findById(nonExistentId);
        });
        assertThat(exception.getMessage()).contains("User with id: " + nonExistentId + " not found");
    }

    @Test
    void findByUsername_whenExists_shouldReturnUser() {
        userRepository.saveAndFlush(user); // Ensure user with DEFAULT_USERNAME is in DB

        UserDto foundDto = userService.findByUsername(DEFAULT_USERNAME);

        assertThat(foundDto).isNotNull();
        assertThat(foundDto.getCredentialDto().getUsername()).isEqualTo(DEFAULT_USERNAME);
        assertThat(foundDto.getFirstName()).isEqualTo(DEFAULT_FIRST_NAME);
    }

    @Test
    void findByUsername_whenNotExists_shouldThrowUserObjectNotFoundException() {
        String nonExistentUsername = "nonexistentuser";
        Exception exception = assertThrows(UserObjectNotFoundException.class, () -> {
            userService.findByUsername(nonExistentUsername);
        });
        assertThat(exception.getMessage()).contains("User with username: " + nonExistentUsername + " not found");
    }

    @Test
    void findAll_shouldReturnAllUsers() {
        userRepository.saveAndFlush(user); // User 1

        // Create and save another user
        Credential credential2 = Credential.builder().username("janedoe").password("pw").isEnabled(true).isAccountNonExpired(true).isAccountNonLocked(true).isCredentialsNonExpired(true).build();
        User user2 = User.builder().firstName("Jane").lastName("Doe").email("jane@example.com").phone("0987654321").imageUrl("http://example.com/jane.jpg").credential(credential2).build();
        credential2.setUser(user2);
        userRepository.saveAndFlush(user2); // User 2

        List<UserDto> users = userService.findAll();

        assertThat(users).isNotNull();
        assertThat(users.size()).isEqualTo(2);
        // Add more specific assertions if needed, e.g., check for presence of specific users
        assertThat(users.stream().anyMatch(u -> u.getFirstName().equals(DEFAULT_FIRST_NAME))).isTrue();
        assertThat(users.stream().anyMatch(u -> u.getFirstName().equals("Jane"))).isTrue();
    }

}
