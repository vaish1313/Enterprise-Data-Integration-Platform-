package com.company.integrationplatform.auth.service;

import com.company.integrationplatform.auth.dto.LoginRequest;
import com.company.integrationplatform.auth.dto.LoginResponse;
import com.company.integrationplatform.auth.dto.RefreshTokenRequest;
import com.company.integrationplatform.auth.dto.RegisterRequest;
import com.company.integrationplatform.user.dto.UserDto;

public interface AuthService {

    UserDto register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    LoginResponse refreshToken(RefreshTokenRequest request);

    void logout(String username);
}
