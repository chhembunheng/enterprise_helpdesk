package com.stk.auth_service.repository;

import com.stk.auth_service.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
	Optional<User> findByUsername(String username);
	Optional<User> findByEmail(String email);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByEmailIgnoreCase(String email);
    Optional<User> findByUsernameIgnoreCase(String username);
}
