package com.company.integrationplatform.user.service;

import com.company.integrationplatform.audit.AuditService;
import com.company.integrationplatform.common.Constants;
import com.company.integrationplatform.common.PageResponse;
import com.company.integrationplatform.exception.ResourceNotFoundException;
import com.company.integrationplatform.exception.ValidationException;
import com.company.integrationplatform.user.dto.UserCreateRequest;
import com.company.integrationplatform.user.dto.UserDto;
import com.company.integrationplatform.user.dto.UserUpdateRequest;
import com.company.integrationplatform.user.entity.User;
import com.company.integrationplatform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Override
    @Transactional
    public UserDto createUser(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ValidationException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(request.getRole())
                .enabled(true)
                .build();

        User saved = userRepository.save(user);
        auditService.log(Constants.ACTION_CREATE_USER, saved.getUsername(), "SUCCESS",
                "Created user: " + saved.getUsername());
        log.info("User created: {}", saved.getUsername());
        return UserDto.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(UUID id) {
        return UserDto.from(findUserById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserDto> getAllUsers(Pageable pageable) {
        Page<UserDto> page = userRepository.findAll(pageable).map(UserDto::from);
        return PageResponse.of(page);
    }

    @Override
    @Transactional
    public UserDto updateUser(UUID id, UserUpdateRequest request) {
        User user = findUserById(id);

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new ValidationException("Email already in use: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName()  != null) user.setLastName(request.getLastName());
        if (request.getRole()      != null) user.setRole(request.getRole());
        if (request.getEnabled()   != null) user.setEnabled(request.getEnabled());

        User saved = userRepository.save(user);
        auditService.log(Constants.ACTION_UPDATE_USER, saved.getUsername(), "SUCCESS",
                "Updated user: " + saved.getUsername());
        return UserDto.from(saved);
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        User user = findUserById(id);
        userRepository.delete(user);
        auditService.log(Constants.ACTION_DELETE_USER, user.getUsername(), "SUCCESS",
                "Deleted user: " + user.getUsername());
        log.info("User deleted: {}", user.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
        return UserDto.from(user);
    }

    private User findUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
