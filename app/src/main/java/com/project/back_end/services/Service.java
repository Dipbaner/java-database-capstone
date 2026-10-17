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

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Fully-qualified: this class is itself called "Service".
@org.springframework.stereotype.Service
public class Service {

    private static final Logger logger = LoggerFactory.getLogger(Service.class);
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

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

    /**
     * Validates a JWT for the given role.
     * Valid token   -> 200 with an EMPTY body (callers check {@code getBody().isEmpty()}).
     * Invalid token -> 401 with a "message".
     */
    public ResponseEntity<Map<String, String>> validateToken(String token, String user) {
        Map<String, String> body = new HashMap<>();
        if (!tokenService.validateToken(token, user)) {
            body.put("message", "Invalid or expired token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        }
        return ResponseEntity.ok(body);
    }

    /** Admin login: username + password, returns a JWT whose subject is the username. */
    public ResponseEntity<Map<String, String>> validateAdmin(Admin receivedAdmin) {
        Map<String, String> response = new HashMap<>();
        try {
            Admin admin = adminRepository.findByUsername(receivedAdmin.getUsername());
            if (admin == null || !admin.getPassword().equals(receivedAdmin.getPassword())) {
                response.put("message", "Invalid username or password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            response.put("token", tokenService.generateToken(admin.getUsername()));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error validating admin", e);
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /** Filters doctors by any combination of name, specialty and time (AM/PM). */
    public Map<String, Object> filterDoctor(String name, String specialty, String time) {
        boolean hasName = present(name);
        boolean hasSpecialty = present(specialty);
        boolean hasTime = present(time);

        if (hasName && hasSpecialty && hasTime) {
            return doctorService.filterDoctorsByNameSpecilityandTime(name, specialty, time);
        } else if (hasName && hasTime) {
            return doctorService.filterDoctorByNameAndTime(name, time);
        } else if (hasName && hasSpecialty) {
            return doctorService.filterDoctorByNameAndSpecility(name, specialty);
        } else if (hasSpecialty && hasTime) {
            return doctorService.filterDoctorByTimeAndSpecility(specialty, time);
        } else if (hasName) {
            return doctorService.findDoctorByName(name);
        } else if (hasSpecialty) {
            return doctorService.filterDoctorBySpecility(specialty);
        } else if (hasTime) {
            return doctorService.filterDoctorsByTime(time);
        }
        Map<String, Object> all = new HashMap<>();
        all.put("doctors", doctorService.getDoctors());
        return all;
    }

    /**
     * @return 1 if the requested time is one of the doctor's free slots, 0 if not, -1 if the doctor doesn't exist.
     */
    public int validateAppointment(Appointment appointment) {
        if (appointment.getDoctor() == null || appointment.getDoctor().getId() == null) {
            return -1;
        }
        Optional<Doctor> doctor = doctorRepository.findById(appointment.getDoctor().getId());
        if (doctor.isEmpty()) {
            return -1;
        }

        List<String> freeSlots = doctorService.getDoctorAvailability(
                doctor.get().getId(), appointment.getAppointmentTime().toLocalDate());
        String requested = appointment.getAppointmentTime().toLocalTime().format(HH_MM);

        for (String slot : freeSlots) {
            if (normalise(slot.split("-")[0]).equals(requested)) {
                return 1;
            }
        }
        return 0;
    }

    /** @return true if no patient already uses this email or phone number. */
    public boolean validatePatient(Patient patient) {
        try {
            return patientRepository.findByEmailOrPhone(patient.getEmail(), patient.getPhone()) == null;
        } catch (Exception e) {
            // more than one match (email belongs to one patient, phone to another) -> definitely a duplicate
            logger.warn("Duplicate patient detected for {}", patient.getEmail());
            return false;
        }
    }

    /** Patient login by email + password; returns a JWT whose subject is the email. */
    public ResponseEntity<Map<String, String>> validatePatientLogin(Login login) {
        Map<String, String> response = new HashMap<>();
        try {
            Patient patient = patientRepository.findByEmail(login.getEmail());
            if (patient == null || !patient.getPassword().equals(login.getPassword())) {
                response.put("message", "Invalid email or password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            response.put("token", tokenService.generateToken(patient.getEmail()));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error validating patient login", e);
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /** Filters the logged-in patient's appointments by condition (past/future) and/or doctor name. */
    public ResponseEntity<Map<String, Object>> filterPatient(String condition, String name, String token) {
        Map<String, Object> error = new HashMap<>();
        try {
            String email = tokenService.extractEmail(token);
            Patient patient = patientRepository.findByEmail(email);
            if (patient == null) {
                error.put("message", "Patient not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            Long id = patient.getId();

            boolean hasCondition = present(condition);
            boolean hasName = present(name);

            if (hasCondition && hasName) {
                return patientService.filterByDoctorAndCondition(condition, name, id);
            } else if (hasCondition) {
                return patientService.filterByCondition(condition, id);
            } else if (hasName) {
                return patientService.filterByDoctor(name, id);
            }
            return patientService.getPatientAppointment(id, token);
        } catch (Exception e) {
            logger.error("Error filtering patient appointments", e);
            error.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // ---------------------------------------------------------------- helpers

    /** The frontend sends the literal string "null" for an unused filter. */
    private boolean present(String value) {
        return value != null && !value.isBlank() && !value.equalsIgnoreCase("null");
    }

    private String normalise(String time) {
        String t = time.trim();
        return LocalTime.parse(t.length() == 4 ? "0" + t : t).format(HH_MM);
    }
}
