package org.example.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.business.dto.userDTO.UserRequestDto;
import org.example.business.dto.userDTO.UserResponseDto;
import org.example.business.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/users")
@AllArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> registerUser(
            @RequestHeader(value = "X-User-Id") String userId,
            @RequestHeader(value = "X-User-Email") String email,
            @RequestHeader(value = "X-User-FirstName") String firstName,
            @RequestHeader(value = "X-User-LastName") String lastName,
            @RequestHeader(value = "X-User-Username") String username,
            @RequestBody(required = false) UserRequestDto additionalInfo) {

        log.info("Register request for user: {}", userId);

        UserRequestDto userDto = UserRequestDto.builder()
                .userId(userId)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .username(username)
                .build();

        UserResponseDto response = userService.registerUser(userDto);
        return ResponseEntity.ok(response);
    }
}