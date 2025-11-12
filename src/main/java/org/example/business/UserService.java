package org.example.business;

import org.example.business.dto.userDTO.UserRequestDto;
import org.example.business.dto.userDTO.UserResponseDto;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserService {
    UserResponseDto registerUser(UserRequestDto requestDto);
    List<UserResponseDto> getAllUsers(String roleFilter);
    @Transactional
    void deleteAccount(String userId);
}