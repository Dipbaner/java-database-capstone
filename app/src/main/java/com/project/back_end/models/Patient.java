package com.project.back_end.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(min = 3, max = 100)
    private String name;

    @NotNull
    @Email
    private String email;

    @NotNull
    @Size(min = 6)
    private String password;

    @NotNull
    @Pattern(regexp = "^[0-9]{10}$")
    private String phone;

    @NotNull
    @Size(max = 255)
    private String address;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public @NotNull @Size(min = 3, max = 100) String getName() {
        return name;
    }

    public void setName(@NotNull @Size(min = 3, max = 100) String name) {
        this.name = name;
    }

    public @NotNull @Email String getEmail() {
        return email;
    }

    public void setEmail(@NotNull @Email String email) {
        this.email = email;
    }

    public @NotNull @Size(min = 6) String getPassword() {
        return password;
    }

    public void setPassword(@NotNull @Size(min = 6) String password) {
        this.password = password;
    }

    public @NotNull @Pattern(regexp = "^[0-9]{10}$") String getPhone() {
        return phone;
    }

    public void setPhone(@NotNull @Pattern(regexp = "^[0-9]{10}$") String phone) {
        this.phone = phone;
    }

    public @NotNull @Size(max = 255) String getAddress() {
        return address;
    }

    public void setAddress(@NotNull @Size(max = 255) String address) {
        this.address = address;
    }
}
