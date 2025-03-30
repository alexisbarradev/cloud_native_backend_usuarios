package com.backend.usuarios.service;

import com.backend.usuarios.model.User;
import com.backend.usuarios.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Registrar usuario
    public User registerUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    // Login
    public Optional<User> authenticate(String username, String rawPassword) {
        return userRepository.findByUsername(username)
                .filter(user -> passwordEncoder.matches(rawPassword, user.getPassword()));
    }

    // Listar todos los usuarios
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Buscar usuario por RUT
    public Optional<User> getUserByRut(String rut) {
        return userRepository.findById(rut); // Porque rut es la clave primaria (@Id)
    }

    // Actualizar usuario
    public Optional<User> updateUser(String rut, User updatedUser) {
        return userRepository.findById(rut).map(existingUser -> {
            existingUser.setUsername(updatedUser.getUsername());
            existingUser.setRole(updatedUser.getRole());
            existingUser.setEmail(updatedUser.getEmail());

            if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
                existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
            }
            return userRepository.save(existingUser);
        });
    }

    // Eliminar usuario
    public boolean deleteUser(String rut) {
        return userRepository.findById(rut).map(user -> {
            userRepository.delete(user);
            return true;
        }).orElse(false);
    }
}
