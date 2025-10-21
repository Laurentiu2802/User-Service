package org.example.business;

import org.example.business.dto.userDTO.UserRequestDto;
import org.example.business.dto.userDTO.UserResponseDto;

public interface UserService {
    UserResponseDto registerUser(UserRequestDto requestDto);
}