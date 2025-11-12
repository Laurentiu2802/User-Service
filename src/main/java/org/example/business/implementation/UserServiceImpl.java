package org.example.business.implementation;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.business.dto.userDTO.UserRequestDto;
import org.example.business.dto.userDTO.UserResponseDto;
import org.example.business.UserService;
import org.example.persistance.UserRepository;
import org.example.persistance.entity.UserEntity;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public UserResponseDto registerUser(UserRequestDto requestDto) {
        log.info("Registering/updating user: {}", requestDto.getUserId());

        // Check if user exists
        UserEntity user;
        if (userRepository.existsById(requestDto.getUserId())) {
            // User exists - update
            log.info("User exists, updating");
            user = userRepository.findById(requestDto.getUserId()).get();
            user.setEmail(requestDto.getEmail());
            user.setFirstName(requestDto.getFirstName());
            user.setLastName(requestDto.getLastName());
            user.setUsername(requestDto.getUsername());
            user.setRoles(requestDto.getRoles());
        } else {
            // New user - create
            log.info("Creating new user");
            user = UserEntity.builder()
                    .id(requestDto.getUserId())
                    .email(requestDto.getEmail())
                    .firstName(requestDto.getFirstName())
                    .lastName(requestDto.getLastName())
                    .username(requestDto.getUsername())
                    .roles(requestDto.getRoles())
                    .build();
        }

        user = userRepository.save(user);
        log.info("User saved successfully: {}", user.getId());

        return UserResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .createdAt(user.getCreatedAt())
                .roles(user.getRoles())
                .build();
    }
    @Override
    @Transactional
    public void deleteAccount(String userId) {
        log.info("Deleting account for user: {}", userId);

        if (!userRepository.existsById(userId)) {
            log.error("User not found: {}", userId);
            throw new RuntimeException("User not found");
        }

        userRepository.deleteById(userId);
        log.info("User deleted from local database: {}", userId);

        try {
            rabbitTemplate.convertAndSend("user.exchange", "user.deleted", userId);
            log.info("Published user.deleted event for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to publish user.deleted event: {}", e.getMessage());
            throw new RuntimeException("Failed to publish deletion event");
        }
    }

    @Override
    public List<UserResponseDto> getAllUsers(String roleFilter) {
        log.info("Getting all users with role filter: {}", roleFilter);
        List<UserEntity> users = userRepository.findAll();

        return users.stream()
                .filter(user -> {
                    if (roleFilter == null || roleFilter.isEmpty()) {
                        return true;
                    }
                    return user.getRoles() != null && user.getRoles().contains(roleFilter);
                })
                .map(user -> UserResponseDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .username(user.getUsername())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .roles(user.getRoles())
                        .createdAt(user.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}