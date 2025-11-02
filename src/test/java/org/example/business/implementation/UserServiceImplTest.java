package org.example.business.implementation;

import org.example.business.dto.userDTO.UserRequestDto;
import org.example.business.dto.userDTO.UserResponseDto;
import org.example.persistance.UserRepository;
import org.example.persistance.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl Unit Tests")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private UserServiceImpl userService;

    private UserEntity testUserEntity;
    private UserRequestDto testRequestDto;

    @BeforeEach
    void setUp() {
        testUserEntity = UserEntity.builder()
                .id("user123")
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .username("johndoe")
                .createdAt(LocalDateTime.now())
                .build();

        testRequestDto = UserRequestDto.builder()
                .userId("user123")
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .username("johndoe")
                .build();
    }

    // ==================== HAPPY FLOW - REGISTER NEW USER ====================

    @Test
    @DisplayName("Happy Flow: Should create new user when user does not exist")
    void registerUser_WhenNewUser_ShouldCreateSuccessfully() {
        // Arrange
        when(userRepository.existsById("user123")).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUserEntity);

        // Act
        UserResponseDto result = userService.registerUser(testRequestDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("user123");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getUsername()).isEqualTo("johndoe");
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");

        verify(userRepository, times(1)).existsById("user123");
        verify(userRepository, times(1)).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("Happy Flow: Should update existing user when user already exists")
    void registerUser_WhenExistingUser_ShouldUpdateSuccessfully() {
        // Arrange
        UserEntity existingUser = UserEntity.builder()
                .id("user123")
                .email("old@example.com")
                .firstName("OldName")
                .lastName("OldLastName")
                .username("oldusername")
                .createdAt(LocalDateTime.now().minusDays(10))
                .build();

        when(userRepository.existsById("user123")).thenReturn(true);
        when(userRepository.findById("user123")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUserEntity);

        // Act
        UserResponseDto result = userService.registerUser(testRequestDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getUsername()).isEqualTo("johndoe");
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");

        verify(userRepository, times(1)).existsById("user123");
        verify(userRepository, times(1)).findById("user123");
        verify(userRepository, times(1)).save(any(UserEntity.class));
    }

    @ParameterizedTest
    @MethodSource("provideValidUserData")
    @DisplayName("Happy Flow: Should register users with various valid data")
    void registerUser_WithVariousValidData_ShouldSucceed(String userId, String email, String firstName, String lastName, String username) {
        // Arrange
        UserRequestDto requestDto = UserRequestDto.builder()
                .userId(userId)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .username(username)
                .build();

        UserEntity savedEntity = UserEntity.builder()
                .id(userId)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .username(username)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.existsById(userId)).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedEntity);

        // Act
        UserResponseDto result = userService.registerUser(requestDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getUsername()).isEqualTo(username);
    }

    static Stream<Arguments> provideValidUserData() {
        return Stream.of(
                Arguments.of("user1", "user1@test.com", "Alice", "Smith", "alice_smith"),
                Arguments.of("user2", "bob.jones@example.com", "Bob", "Jones", "bob123"),
                Arguments.of("uuid-550e8400", "test@domain.ro", "Ion", "Popescu", "ion_popescu"),
                Arguments.of("user4", "maria@gmail.com", "Maria", "Ionescu", "maria.ionescu"),
                Arguments.of("mechanic-123", "mechanic@service.com", "Mike", "Mechanic", "mike_mech")
        );
    }

    @Test
    @DisplayName("Happy Flow: Should capture correct user data when creating new user")
    void registerUser_NewUser_ShouldCaptureCorrectData() {
        // Arrange
        ArgumentCaptor<UserEntity> entityCaptor = ArgumentCaptor.forClass(UserEntity.class);
        when(userRepository.existsById("user123")).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUserEntity);

        // Act
        userService.registerUser(testRequestDto);

        // Assert
        verify(userRepository).save(entityCaptor.capture());
        UserEntity capturedEntity = entityCaptor.getValue();
        assertThat(capturedEntity.getId()).isEqualTo("user123");
        assertThat(capturedEntity.getEmail()).isEqualTo("test@example.com");
        assertThat(capturedEntity.getUsername()).isEqualTo("johndoe");
        assertThat(capturedEntity.getFirstName()).isEqualTo("John");
        assertThat(capturedEntity.getLastName()).isEqualTo("Doe");
    }

    // ==================== HAPPY FLOW - DELETE ACCOUNT ====================

    @Test
    @DisplayName("Happy Flow: Should delete account and publish event successfully")
    void deleteAccount_WhenUserExists_ShouldDeleteAndPublishEvent() {
        // Arrange
        String userId = "user123";
        when(userRepository.existsById(userId)).thenReturn(true);
        doNothing().when(userRepository).deleteById(userId);
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());

        // Act & Assert
        assertThatCode(() -> userService.deleteAccount(userId))
                .doesNotThrowAnyException();

        verify(userRepository, times(1)).existsById(userId);
        verify(userRepository, times(1)).deleteById(userId);
        verify(rabbitTemplate, times(1)).convertAndSend("user.exchange", "user.deleted", userId);
    }

    @Test
    @DisplayName("Happy Flow: Should verify correct RabbitMQ event data")
    void deleteAccount_ShouldPublishCorrectEventData() {
        // Arrange
        String userId = "user456";
        ArgumentCaptor<String> exchangeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        when(userRepository.existsById(userId)).thenReturn(true);
        doNothing().when(userRepository).deleteById(userId);

        // Act
        userService.deleteAccount(userId);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                exchangeCaptor.capture(),
                routingKeyCaptor.capture(),
                messageCaptor.capture()
        );

        assertThat(exchangeCaptor.getValue()).isEqualTo("user.exchange");
        assertThat(routingKeyCaptor.getValue()).isEqualTo("user.deleted");
        assertThat(messageCaptor.getValue()).isEqualTo(userId);
    }

    @ParameterizedTest
    @MethodSource("provideValidUserIds")
    @DisplayName("Happy Flow: Should delete accounts with various valid user IDs")
    void deleteAccount_WithVariousUserIds_ShouldSucceed(String userId) {
        // Arrange
        when(userRepository.existsById(userId)).thenReturn(true);
        doNothing().when(userRepository).deleteById(userId);
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());

        // Act & Assert
        assertThatCode(() -> userService.deleteAccount(userId))
                .doesNotThrowAnyException();

        verify(userRepository).deleteById(userId);
        verify(rabbitTemplate).convertAndSend("user.exchange","user.deleted", userId);
    }

    static Stream<String> provideValidUserIds() {
        return Stream.of(
                "user123",
                "mechanic-456",
                "550e8400-e29b-41d4-a716-446655440000",
                "very-long-user-id-with-many-characters-12345"
        );
    }

    // ==================== UNHAPPY FLOW - REGISTER USER ====================

    @Test
    @DisplayName("Unhappy Flow: Should handle repository exception during user creation")
    void registerUser_WhenRepositoryThrowsException_ShouldPropagateException() {
        // Arrange
        when(userRepository.existsById("user123")).thenReturn(false);
        when(userRepository.save(any(UserEntity.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        assertThatThrownBy(() -> userService.registerUser(testRequestDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database connection failed");
    }

    @Test
    @DisplayName("Unhappy Flow: Should handle exception when finding existing user fails")
    void registerUser_WhenFindByIdFails_ShouldPropagateException() {
        // Arrange
        when(userRepository.existsById("user123")).thenReturn(true);
        when(userRepository.findById("user123"))
                .thenThrow(new RuntimeException("Database query failed"));

        // Act & Assert
        assertThatThrownBy(() -> userService.registerUser(testRequestDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database query failed");
    }

    // ==================== UNHAPPY FLOW - DELETE ACCOUNT ====================

    @Test
    @DisplayName("Unhappy Flow: Should throw exception when user not found during deletion")
    void deleteAccount_WhenUserNotFound_ShouldThrowException() {
        // Arrange
        String userId = "nonexistent-user";
        when(userRepository.existsById(userId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> userService.deleteAccount(userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");

        verify(userRepository, times(1)).existsById(userId);
        verify(userRepository, never()).deleteById(anyString());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Unhappy Flow: Should throw exception when RabbitMQ publish fails")
    void deleteAccount_WhenRabbitMQFails_ShouldThrowException() {
        // Arrange
        String userId = "user123";
        when(userRepository.existsById(userId)).thenReturn(true);
        doNothing().when(userRepository).deleteById(userId);
        doThrow(new RuntimeException("RabbitMQ connection failed"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());

        // Act & Assert
        assertThatThrownBy(() -> userService.deleteAccount(userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to publish deletion event");

        verify(userRepository, times(1)).deleteById(userId);
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Unhappy Flow: Should handle repository exception during deletion")
    void deleteAccount_WhenRepositoryDeleteFails_ShouldThrowException() {
        // Arrange
        String userId = "user123";
        when(userRepository.existsById(userId)).thenReturn(true);
        doThrow(new RuntimeException("Database deletion failed"))
                .when(userRepository).deleteById(userId);

        // Act & Assert
        assertThatThrownBy(() -> userService.deleteAccount(userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database deletion failed");

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), anyString());
    }

    @ParameterizedTest
    @MethodSource("provideInvalidUserIds")
    @DisplayName("Unhappy Flow: Should fail deletion for non-existent user IDs")
    void deleteAccount_WithNonExistentUserIds_ShouldThrowException(String userId) {
        // Arrange
        when(userRepository.existsById(userId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> userService.deleteAccount(userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");

        verify(userRepository, never()).deleteById(anyString());
    }

    static Stream<String> provideInvalidUserIds() {
        return Stream.of(
                "nonexistent-user",
                "deleted-user-123",
                "invalid-id-999"
        );
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Edge Case: Should handle user registration with special characters")
    void registerUser_WithSpecialCharacters_ShouldHandleCorrectly() {
        // Arrange
        UserRequestDto requestDto = UserRequestDto.builder()
                .userId("user-123")
                .email("test+alias@example.com")
                .firstName("Jean-Paul")
                .lastName("O'Connor")
                .username("user_name.123")
                .build();

        UserEntity savedEntity = UserEntity.builder()
                .id("user-123")
                .email("test+alias@example.com")
                .firstName("Jean-Paul")
                .lastName("O'Connor")
                .username("user_name.123")
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.existsById("user-123")).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedEntity);

        // Act
        UserResponseDto result = userService.registerUser(requestDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test+alias@example.com");
        assertThat(result.getFirstName()).isEqualTo("Jean-Paul");
        assertThat(result.getLastName()).isEqualTo("O'Connor");
        assertThat(result.getUsername()).isEqualTo("user_name.123");
    }

    @Test
    @DisplayName("Edge Case: Should handle update when user data is identical")
    void registerUser_WithIdenticalData_ShouldStillUpdate() {
        // Arrange
        when(userRepository.existsById("user123")).thenReturn(true);
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUserEntity));
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUserEntity);

        // Act
        UserResponseDto result = userService.registerUser(testRequestDto);

        // Assert
        assertThat(result).isNotNull();
        verify(userRepository, times(1)).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("Edge Case: Should handle deletion immediately after creation")
    void deleteAccount_ImmediatelyAfterCreation_ShouldSucceed() {
        // Arrange
        String userId = "new-user-123";
        when(userRepository.existsById(userId)).thenReturn(true);
        doNothing().when(userRepository).deleteById(userId);
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());

        // Act & Assert
        assertThatCode(() -> userService.deleteAccount(userId))
                .doesNotThrowAnyException();
    }
}