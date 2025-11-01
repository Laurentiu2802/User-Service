package org.example.business;

import org.example.business.dto.userDTO.UserRequestDto;
import org.example.business.dto.userDTO.UserResponseDto;
import org.springframework.transaction.annotation.Transactional;

public interface UserService {
    UserResponseDto registerUser(UserRequestDto requestDto);

    @Transactional
    void deleteAccount(String userId);
}