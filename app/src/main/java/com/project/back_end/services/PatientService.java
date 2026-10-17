package com.project.back_end.services;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PatientService {

    private static final Logger logger = LoggerFactory.getLogger(PatientService.class);

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;

    public PatientService(PatientRepository patientRepository,
                          AppointmentRepository appointmentRepository,
                          TokenService tokenService) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }

    /** @return 1 on success, 0 on failure. */
    public int createPatient(Patient patient) {
        try {
            patientRepository.save(patient);
            return 1;
        } catch (Exception e) {
            logger.error("Error creating patient", e);
            return 0;
        }
    }

    /** All appointments of a patient (as DTOs). */
    @Transactional
    public ResponseEntity<Map<String, Object>> getPatientAppointment(Long id, String token) {
        Map<String, Object> response = new HashMap<>();
        try {
            // A logged-in patient may only read their own appointments. Doctors (whose token
            // does not resolve to a patient) are validated by the controller before this call.
            String email = tokenService.extractEmail(token);
            Patient tokenPatient = patientRepository.findByEmail(email);
            if (tokenPatient != null && !tokenPatient.getId().equals(id)) {
                response.put("message", "Unauthorized access to another patient's appointments");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            List<AppointmentDTO> appointments = appointmentRepository.findByPatientId(id).stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
            response.put("appointments", appointments);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching appointments for patient {}", id, e);
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /** Filters by "past" (status 1) or "future" (status 0). */
    @Transactional
    public ResponseEntity<Map<String, Object>> filterByCondition(String condition, Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Integer status = statusFor(condition);
            if (status == null) {
                response.put("message", "Invalid condition. Use 'past' or 'future'");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            List<AppointmentDTO> appointments = appointmentRepository
                    .findByPatient_IdAndStatusOrderByAppointmentTimeAsc(id, status).stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
            response.put("appointments", appointments);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error filtering appointments by condition", e);
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Transactional
    public ResponseEntity<Map<String, Object>> filterByDoctor(String name, Long patientId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<AppointmentDTO> appointments = appointmentRepository
                    .filterByDoctorNameAndPatientId(name, patientId).stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
            response.put("appointments", appointments);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error filtering appointments by doctor", e);
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Transactional
    public ResponseEntity<Map<String, Object>> filterByDoctorAndCondition(String condition, String name, long patientId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Integer status = statusFor(condition);
            if (status == null) {
                response.put("message", "Invalid condition. Use 'past' or 'future'");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            List<AppointmentDTO> appointments = appointmentRepository
                    .filterByDoctorNameAndPatientIdAndStatus(name, patientId, status).stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
            response.put("appointments", appointments);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error filtering appointments by doctor and condition", e);
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /** Resolves the patient from the token's email. */
    @Transactional
    public ResponseEntity<Map<String, Object>> getPatientDetails(String token) {
        Map<String, Object> response = new HashMap<>();
        try {
            String email = tokenService.extractEmail(token);
            Patient patient = patientRepository.findByEmail(email);
            if (patient == null) {
                response.put("message", "Patient not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            response.put("patient", patient);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching patient details", e);
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ---------------------------------------------------------------- helpers

    private Integer statusFor(String condition) {
        if (condition == null) {
            return null;
        }
        switch (condition.trim().toLowerCase()) {
            case "future":
                return 0;
            case "past":
                return 1;
            default:
                return null;
        }
    }

    private AppointmentDTO toDto(Appointment a) {
        return new AppointmentDTO(
                a.getId(),
                a.getDoctor().getId(),
                a.getDoctor().getName(),
                a.getPatient().getId(),
                a.getPatient().getName(),
                a.getPatient().getEmail(),
                a.getPatient().getPhone(),
                a.getPatient().getAddress(),
                a.getAppointmentTime(),
                a.getStatus());
    }
}
