package com.stk.auth_service.mapper;
import com.stk.auth_service.dto.UserResponse;
import com.stk.auth_service.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
	UserResponse toResponse(User user);
}
