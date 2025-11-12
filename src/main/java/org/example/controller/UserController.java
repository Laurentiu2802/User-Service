package org.example.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.business.dto.userDTO.UserRequestDto;
import org.example.business.dto.userDTO.UserResponseDto;
import org.example.business.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            @RequestHeader(value = "X-User-Roles") String roles,
            @RequestBody(required = false) UserRequestDto additionalInfo) {

        log.info("Register request for user: {}", userId);

        UserRequestDto userDto = UserRequestDto.builder()
                .userId(userId)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .username(username)
                .roles(roles)
                .build();

        UserResponseDto response = userService.registerUser(userDto);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getAllUsers(
            @RequestParam(required = false) String role) {
        log.info("Fetching all users with role filter: {}", role);
        List<UserResponseDto> users = userService.getAllUsers(role);
        return ResponseEntity.ok(users);
    }
    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(
            @RequestHeader(value = "X-User-Id") String userId) {

        log.info("Delete account request from user: {}", userId);

        try {
            userService.deleteAccount(userId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Error deleting account: {}", e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}