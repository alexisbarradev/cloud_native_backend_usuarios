package com.backend.usuarios.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "roles") // Apunta a la tabla ya existente
@Immutable // ✅ Evita que Hibernate intente modificar esta tabla
public class Role {

    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    // Constructor vacío requerido por JPA
    public Role() {}

    public Role(String name) {
        this.name = name;
    }

    // Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

