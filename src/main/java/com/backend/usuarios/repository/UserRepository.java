package com.backend.usuarios.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.backend.usuarios.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);
}
