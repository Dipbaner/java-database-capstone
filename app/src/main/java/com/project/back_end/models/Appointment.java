package com.project.back_end.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @NotNull
    private Doctor doctor;

    @ManyToOne
    @NotNull
    private Patient patient;

    @Future
    private LocalDateTime appointmentTime;

    // 0 - appointment scheduled
    // 1 - appointment completed
    @NotNull
    private int status;

    @Transient
    private LocalDateTime getEndTime () {
        return appointmentTime.plusHours(1);
    }

    private LocalDate getAppointmentDate () {
        return appointmentTime.toLocalDate();
    }

    private LocalTime getAppointmentTimeOnly () {
        return appointmentTime.toLocalTime();
    }

    public Appointment(Long id, Doctor doctor, Patient patient, LocalDateTime appointmentTime, int status) {
        this.id = id;
        this.doctor = doctor;
        this.patient = patient;
        this.appointmentTime = appointmentTime;
        this.status = status;
    }

    public Appointment() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public @NotNull Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(@NotNull Doctor doctor) {
        this.doctor = doctor;
    }

    public @NotNull Patient getPatient() {
        return patient;
    }

    public void setPatient(@NotNull Patient patient) {
        this.patient = patient;
    }

    public @Future LocalDateTime getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(@Future LocalDateTime appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public @NotNull int getStatus() {
        return status;
    }

    public void setStatus(@NotNull int status) {
        this.status = status;
    }
}

