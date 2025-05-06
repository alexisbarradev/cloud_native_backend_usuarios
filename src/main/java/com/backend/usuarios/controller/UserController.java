package com.backend.usuarios.controller;

import com.backend.usuarios.model.User;
import com.backend.usuarios.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        String rut = user.getRut();
        String azureFunctionUrl = "https://funcionduocrut.azurewebsites.net/api/validateRut?rut=" + rut;
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(azureFunctionUrl, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.badRequest().body("RUT inválido");
            }

            User newUser = userService.registerUser(user);

            // Evento: UserCreated
            sendEventToEventGrid("UserCreated", "usuarios/creado", newUser);

            return ResponseEntity.ok(newUser);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al registrar o enviar evento: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String username, @RequestParam String password) {
        Optional<User> userOpt = userService.authenticate(username, password);
        return userOpt.isPresent()
                ? ResponseEntity.ok("Login exitoso")
                : ResponseEntity.status(401).body("Credenciales inválidas");
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{rut}")
    public ResponseEntity<User> getUserByRut(@PathVariable String rut) {
        return userService.getUserByRut(rut)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{rut}")
    public ResponseEntity<User> updateUser(@PathVariable String rut, @RequestBody User updatedUser) {
        Optional<User> updated = userService.updateUser(rut, updatedUser);

        if (updated.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            // Evento: UserUpdated
            sendEventToEventGrid("UserUpdated", "usuarios/actualizado", updated.get());
        } catch (Exception e) {
            System.err.println("Error al enviar evento de actualización: " + e.getMessage());
        }

        return ResponseEntity.ok(updated.get());
    }

    @DeleteMapping("/{rut}")
    public ResponseEntity<String> deleteUser(@PathVariable String rut) {
        Optional<User> userOpt = userService.getUserByRut(rut);

        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        boolean deleted = userService.deleteUser(rut);
        if (deleted) {
            try {
                // Evento: UserDeleted
                sendEventToEventGrid("UserDeleted", "usuarios/eliminado", userOpt.get());
            } catch (Exception e) {
                System.err.println("Error al enviar evento de eliminación: " + e.getMessage());
            }

            return ResponseEntity.ok("Usuario eliminado");
        }

        return ResponseEntity.status(500).body("Error al eliminar usuario");
    }

    private void sendEventToEventGrid(String eventType, String subject, User user) {
        Map<String, Object> data = new HashMap<>();
        data.put("rut", user.getRut());
        data.put("nombre", user.getUsername());
        data.put("email", user.getEmail());

        Map<String, Object> event = new HashMap<>();
        event.put("id", UUID.randomUUID().toString());
        event.put("eventType", eventType);
        event.put("subject", subject);
        event.put("eventTime", OffsetDateTime.now().toString());
        event.put("data", data);
        event.put("dataVersion", "1.0");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("aeg-sas-key", "6gL2hV7OCMdodOdpETpqITIFCXx2eRVSzsUCZnpuL5Qad9CVP6xVJQQJ99BEACHYHv6XJ3w3AAABAZEGhXjz");

        HttpEntity<Object> requestEntity = new HttpEntity<>(List.of(event), headers);
        String eventGridUrl = "https://duoc-creacion-user.eastus2-1.eventgrid.azure.net/api/events";

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.postForEntity(eventGridUrl, requestEntity, String.class);
    }
}
