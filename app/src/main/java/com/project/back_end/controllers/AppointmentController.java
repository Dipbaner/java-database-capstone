package com.project.back_end.controllers;

import com.project.back_end.models.Appointment;
import com.project.back_end.services.AppointmentService;
import com.project.back_end.services.Service;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final Service service;

    public AppointmentController(AppointmentService appointmentService, Service service) {
        this.appointmentService = appointmentService;
        this.service = service;
    }

    /** A doctor's appointments for a date, optionally filtered by patient name ("null" = no filter). */
    @GetMapping("/{date}/{patientName}/{token}")
    public ResponseEntity<Map<String, Object>> getAppointments(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable String patientName,
            @PathVariable String token) {

        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "doctor");
        if (!validation.getBody().isEmpty()) {
            return ResponseEntity.status(validation.getStatusCode()).body(new HashMap<>(validation.getBody()));
        }
        return ResponseEntity.ok(appointmentService.getAppointment(patientName, date, token));
    }

    /** Books an appointment for a patient. */
    @PostMapping("/{token}")
    public ResponseEntity<Map<String, String>> bookAppointment(@RequestBody @Valid Appointment appointment,
                                                               @PathVariable String token) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "patient");
        if (!validation.getBody().isEmpty()) {
            return validation;
        }

        Map<String, String> response = new HashMap<>();
        int validity = service.validateAppointment(appointment);
        if (validity == -1) {
            response.put("message", "Invalid doctor id");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        if (validity == 0) {
            response.put("message", "Appointment slot is not available");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        if (appointmentService.bookAppointment(appointment) == 1) {
            response.put("message", "Appointment booked successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
        response.put("message", "Failed to book appointment");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /** Updates an existing appointment. */
    @PutMapping("/{token}")
    public ResponseEntity<Map<String, String>> updateAppointment(@RequestBody @Valid Appointment appointment,
                                                                 @PathVariable String token) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "patient");
        if (!validation.getBody().isEmpty()) {
            return validation;
        }
        return appointmentService.updateAppointment(appointment);
    }

    /** Cancels an appointment (only by the patient who owns it). */
    @DeleteMapping("/{id}/{token}")
    public ResponseEntity<Map<String, String>> cancelAppointment(@PathVariable long id,
                                                                 @PathVariable String token) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "patient");
        if (!validation.getBody().isEmpty()) {
            return validation;
        }
        return appointmentService.cancelAppointment(id, token);
    }
}
