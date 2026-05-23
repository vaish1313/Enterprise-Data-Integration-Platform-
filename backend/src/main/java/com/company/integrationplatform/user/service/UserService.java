package com.company.integrationplatform.user.service;

import com.company.integrationplatform.common.PageResponse;
import com.company.integrationplatform.user.dto.UserCreateRequest;
import com.company.integrationplatform.user.dto.UserDto;
import com.company.integrationplatform.user.dto.UserUpdateRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    UserDto createUser(UserCreateRequest request);

    UserDto getUserById(UUID id);

    PageResponse<UserDto> getAllUsers(Pageable pageable);

    UserDto updateUser(UUID id, UserUpdateRequest request);

    void deleteUser(UUID id);

    UserDto getCurrentUser(String username);
}
