package org.example.business.implementation;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.business.dto.userDTO.UserRequestDto;
import org.example.business.dto.userDTO.UserResponseDto;
import org.example.business.UserService;
import org.example.persistance.UserRepository;
import org.example.persistance.entity.UserEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

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
        } else {
            // New user - create
            log.info("Creating new user");
            user = UserEntity.builder()
                    .id(requestDto.getUserId())
                    .email(requestDto.getEmail())
                    .firstName(requestDto.getFirstName())
                    .lastName(requestDto.getLastName())
                    .username(requestDto.getUsername())
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
                .build();
    }
}