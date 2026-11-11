package com.stk.auth_service.service.impl;

import com.stk.auth_service.repository.UserRepository;
import com.stk.auth_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.stk.auth_service.dto.UserResponse;
import com.stk.auth_service.mapper.UserMapper;
import java.util.List;
import com.stk.auth_service.model.User;

@Service
public class UserServiceImpl implements UserService {
	private final UserRepository userRepository;
	private final UserMapper userMapper;

	@Autowired
	public UserServiceImpl(UserRepository userRepository, UserMapper userMapper) {
		this.userRepository = userRepository;
		this.userMapper = userMapper;
	}

	@Override
	public List<UserResponse> getAllUsers() {
		List <User> users = userRepository.findAll();
		return users.stream()
				.map(userMapper::toResponse)
				.toList();
	}

	@Override
	public UserResponse getUserById(String id) {
		try {
			User user = userRepository.findById(java.util.UUID.fromString(id))
					.orElseThrow(() -> new RuntimeException("User not found with id: " + id));
			return userMapper.toResponse(user);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("Invalid UUID format: " + id);
		}
	}



}
