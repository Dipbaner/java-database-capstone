package com.project.back_end.controllers;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Doctor;
import com.project.back_end.services.DoctorService;
import com.project.back_end.services.Service;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doctor")
public class DoctorController {

    private final DoctorService doctorService;
    private final Service service;

    public DoctorController(DoctorService doctorService, Service service) {
        this.doctorService = doctorService;
        this.service = service;
    }

    // GET {api.path}doctor/availability/{user}/{doctorId}/{date}/{token}
    @GetMapping("/availability/{user}/{doctorId}/{date}/{token}")
    public ResponseEntity<Map<String, Object>> getDoctorAvailability(
            @PathVariable String user,
            @PathVariable Long doctorId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable String token) {

        ResponseEntity<Map<String, String>> validation = service.validateToken(token, user);
        if (!validation.getBody().isEmpty()) {
            return ResponseEntity.status(validation.getStatusCode())
                    .body(new HashMap<String, Object>(validation.getBody()));
        }

        List<String> availability = doctorService.getDoctorAvailability(doctorId, date);
        Map<String, Object> body = new HashMap<>();
        body.put("availability", availability);
        return ResponseEntity.ok(body);
    }

    // GET {api.path}doctor
    @GetMapping
    public ResponseEntity<Map<String, Object>> getDoctor() {
        Map<String, Object> body = new HashMap<>();
        body.put("doctors", doctorService.getDoctors());
        return ResponseEntity.ok(body);
    }

    // POST {api.path}doctor/{token}
    @PostMapping("/{token}")
    public ResponseEntity<Map<String, String>> saveDoctor(
            @RequestBody @Valid Doctor doctor,
            @PathVariable String token) {

        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "admin");
        if (!validation.getBody().isEmpty()) {
            return validation;
        }

        Map<String, String> body = new HashMap<>();
        int result = doctorService.saveDoctor(doctor);
        if (result == 1) {
            body.put("message", "Doctor added to db");
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        }
        if (result == -1) {
            body.put("message", "Doctor already exists");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
        }
        body.put("message", "Some internal error occurred while adding doctor");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    // POST {api.path}doctor/login
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> doctorLogin(@RequestBody @Valid Login login) {
        return doctorService.validateDoctor(login);
    }

    // PUT {api.path}doctor/{token}
    @PutMapping("/{token}")
    public ResponseEntity<Map<String, String>> updateDoctor(
            @RequestBody @Valid Doctor doctor,
            @PathVariable String token) {

        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "admin");
        if (!validation.getBody().isEmpty()) {
            return validation;
        }

        Map<String, String> body = new HashMap<>();
        int result = doctorService.updateDoctor(doctor);
        if (result == 1) {
            body.put("message", "Doctor updated");
            return ResponseEntity.ok(body);
        }
        if (result == -1) {
            body.put("message", "Doctor not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }
        body.put("message", "Some internal error occurred while updating doctor");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    // DELETE {api.path}doctor/{id}/{token}
    @DeleteMapping("/{id}/{token}")
    public ResponseEntity<Map<String, String>> deleteDoctor(
            @PathVariable long id,
            @PathVariable String token) {

        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "admin");
        if (!validation.getBody().isEmpty()) {
            return validation;
        }

        Map<String, String> body = new HashMap<>();
        int result = doctorService.deleteDoctor(id);
        if (result == 1) {
            body.put("message", "Doctor deleted successfully");
            return ResponseEntity.ok(body);
        }
        if (result == -1) {
            body.put("message", "Doctor not found with id");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }
        body.put("message", "Some internal error occurred while deleting doctor");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    // GET {api.path}doctor/filter/{name}/{time}/{speciality}
    @GetMapping("/filter/{name}/{time}/{speciality}")
    public ResponseEntity<Map<String, Object>> filter(
            @PathVariable String name,
            @PathVariable String time,
            @PathVariable String speciality) {

        // Service.filterDoctor takes (name, specialty, time)
        return ResponseEntity.ok(service.filterDoctor(name, speciality, time));
    }
}