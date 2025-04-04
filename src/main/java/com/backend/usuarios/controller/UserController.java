package com.backend.usuarios.controller;

import com.backend.usuarios.model.User;
import com.backend.usuarios.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // Registrar nuevo usuario
    //@PostMapping("/register")
    //public ResponseEntity<User> registerUser(@RequestBody User user) {
    //    User newUser = userService.registerUser(user);
    //    return ResponseEntity.ok(newUser);
    //}

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        String rut = user.getRut(); // asegúrate de que el modelo User tenga este campo

        // URL de tu función en Azure
        String azureFunctionUrl = "https://funcionduocrut.azurewebsites.net/api/validateRut?rut=" + rut;

        // Usar RestTemplate para validar RUT
        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(azureFunctionUrl, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                // RUT válido → continúa con registro
                User newUser = userService.registerUser(user);
                return ResponseEntity.ok(newUser);
            } else {
                // RUT inválido
                return ResponseEntity.badRequest().body("RUT inválido");
            }

        } catch (Exception e) {
            // Fallo al contactar Azure Function
            return ResponseEntity.status(500).body("Error al validar el RUT: " + e.getMessage());
        }
    }

    // Login
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String username, @RequestParam String password) {
        Optional<User> userOpt = userService.authenticate(username, password);
        if (userOpt.isPresent()) {
            return ResponseEntity.ok("Login exitoso");
        } else {
            return ResponseEntity.status(401).body("Credenciales inválidas");
        }
    }

    // Listar todos los usuarios
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // Buscar usuario por RUT
    @GetMapping("/{rut}")
    public ResponseEntity<User> getUserByRut(@PathVariable String rut) {
        return userService.getUserByRut(rut)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Actualizar usuario por RUT
    @PutMapping("/{rut}")
    public ResponseEntity<User> updateUser(@PathVariable String rut, @RequestBody User updatedUser) {
        return userService.updateUser(rut, updatedUser)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Eliminar usuario por RUT
    @DeleteMapping("/{rut}")
    public ResponseEntity<String> deleteUser(@PathVariable String rut) {
        boolean deleted = userService.deleteUser(rut);
        if (deleted) {
            return ResponseEntity.ok("Usuario eliminado");
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
