package com.project.back_end.services;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Admin;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.format.DateTimeFormatter;
import java.util.*;

@org.springframework.stereotype.Service
public class Service {

    private static final Logger log = LoggerFactory.getLogger(Service.class);
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private final TokenService tokenService;
    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DoctorService doctorService;
    private final PatientService patientService;

    public Service(TokenService tokenService,
                   AdminRepository adminRepository,
                   DoctorRepository doctorRepository,
                   PatientRepository patientRepository,
                   DoctorService doctorService,
                   PatientService patientService) {
        this.tokenService = tokenService;
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.doctorService = doctorService;
        this.patientService = patientService;
    }

    /** Valid token -> 200 with an empty map; invalid -> 401 with a message. */
    public ResponseEntity<Map<String, String>> validateToken(String token, String user) {
        Map<String, String> body = new HashMap<>();
        if (!tokenService.validateToken(token, user)) {
            body.put("message", "Invalid or expired token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        }
        return ResponseEntity.ok(body);
    }

    public ResponseEntity<Map<String, String>> validateAdmin(Admin receivedAdmin) {
        Map<String, String> body = new HashMap<>();
        try {
            Admin admin = adminRepository.findByUsername(receivedAdmin.getUsername());
            if (admin == null || !admin.getPassword().equals(receivedAdmin.getPassword())) {
                body.put("message", "Invalid username or password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
            }
            body.put("token", tokenService.generateToken(admin.getUsername()));
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.error("Admin login error", e);
            body.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    public Map<String, Object> filterDoctor(String name, String specialty, String time) {
        boolean hasName = notBlank(name), hasSpec = notBlank(specialty), hasTime = notBlank(time);

        if (hasName && hasSpec && hasTime) return doctorService.filterDoctorsByNameSpecilityandTime(name, specialty, time);
        if (hasName && hasTime)            return doctorService.filterDoctorByNameAndTime(name, time);
        if (hasName && hasSpec)            return doctorService.filterDoctorByNameAndSpecility(name, specialty);
        if (hasSpec && hasTime)            return doctorService.filterDoctorByTimeAndSpecility(specialty, time);
        if (hasName)                       return doctorService.findDoctorByName(name);
        if (hasSpec)                       return doctorService.filterDoctorBySpecility(specialty);
        if (hasTime)                       return doctorService.filterDoctorsByTime(time);
        return Map.of("doctors", doctorService.getDoctors());
    }

    /** 1 = slot available, 0 = slot unavailable, -1 = doctor not found */
    public int validateAppointment(Appointment appointment) {
        Optional<Doctor> doctor = doctorRepository.findById(appointment.getDoctor().getId());
        if (doctor.isEmpty()) return -1;

        List<String> slots = doctorService.getDoctorAvailability(
                doctor.get().getId(), appointment.getAppointmentTime().toLocalDate());
        String requested = appointment.getAppointmentTime().toLocalTime().format(HHMM);

        return slots.stream().anyMatch(s -> s.split("-")[0].trim().equals(requested)) ? 1 : 0;
    }

    /** true = no existing patient with this email/phone, safe to register */
    public boolean validatePatient(Patient patient) {
        return patientRepository.findByEmailOrPhone(patient.getEmail(), patient.getPhone()) == null;
    }

    public ResponseEntity<Map<String, String>> validatePatientLogin(Login login) {
        Map<String, String> body = new HashMap<>();
        try {
            Patient patient = patientRepository.findByEmail(login.getEmail());
            if (patient == null || !patient.getPassword().equals(login.getPassword())) {
                body.put("message", "Invalid email or password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
            }
            body.put("token", tokenService.generateToken(patient.getEmail()));
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.error("Patient login error", e);
            body.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    public ResponseEntity<Map<String, Object>> filterPatient(String condition, String name, String token) {
        try {
            Patient patient = patientRepository.findByEmail(tokenService.extractEmail(token));
            if (patient == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Patient not found"));
            }
            Long id = patient.getId();
            boolean hasCond = notBlank(condition), hasName = notBlank(name);

            if (hasCond && hasName) return patientService.filterByDoctorAndCondition(condition, name, id);
            if (hasCond)            return patientService.filterByCondition(condition, id);
            if (hasName)            return patientService.filterByDoctor(name, id);
            return patientService.getPatientAppointment(id, token);
        } catch (Exception e) {
            log.error("Error filtering patient appointments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Internal server error"));
        }
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank() && !"null".equalsIgnoreCase(s);
    }
}