package com.project.back_end.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

@Entity
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(min = 1, max = 100)
    private String name;

    @NotNull
    @Size(min = 3, max = 50)
    private String speciality;

    @NotNull
    @Email
    private String email;

    @NotNull
    @Size(min = 6)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @NotNull
    @Pattern(regexp = "^[0-9]{10}$")
    private String phone;

    @ElementCollection
    private List<String> availableTimes;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public @NotNull @Size(min = 1, max = 100) String getName() {
        return name;
    }

    public void setName(@NotNull @Size(min = 1, max = 100) String name) {
        this.name = name;
    }

    public @NotNull @Size(min = 3, max = 50) String getSpeciality() {
        return speciality;
    }

    public void setSpeciality(@NotNull @Size(min = 3, max = 50) String speciality) {
        this.speciality = speciality;
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

    public List<String> getAvailableTimes() {
        return availableTimes;
    }

    public void setAvailableTimes(List<String> availableTimes) {
        this.availableTimes = availableTimes;
    }
}


