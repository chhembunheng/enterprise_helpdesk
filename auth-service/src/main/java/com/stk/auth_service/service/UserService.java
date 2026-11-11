package com.stk.auth_service.service;

import com.stk.auth_service.dto.UserResponse;
import java.util.List;

public interface UserService {
    List<UserResponse> getAllUsers();
    UserResponse getUserById(String id);
}
