package com.project.back_end.services;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.PatientRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class PatientService {

    private static final Logger log = LoggerFactory.getLogger(
            PatientService.class);

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;

    public PatientService(
            PatientRepository patientRepository,
            AppointmentRepository appointmentRepository,
            TokenService tokenService) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }

    // 1. CREATE

    public int createPatient(Patient patient){
        try {
            patientRepository.save(patient);
            return 1;
        }
        catch(Exception e) {
            log.error("Error creating patient", e);
            return 0;
        }
    }

    // 2. APPOINTMENTS

    @Transactional
    public ResponseEntity<Map<String, Object>> getPatientAppointment
            (Long id, String token) {
        try {
            // Only the patient who owns the token may read these appointment
            String email = tokenService.extractEmail(token);
            Patient patient = patientRepository.findByEmail(email);
            if(patient == null || !patient.getId().equals(id)) {
                return error(HttpStatus.UNAUTHORIZED, "Unauthorized access");
            }
            List<AppointmentDTO> dtos = appointmentRepository.findByPatientId(id)
                    .stream().map(this::toDto).collect(Collectors.toList());
            return ok(dtos);
        }
        catch (Exception e){
            log.error("Error fetching appointments for patient {}", id, e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
        }
    }

    @Transactional
    public ResponseEntity <Map<String, Object>>  filterByCondition(
            String condition , Long id) {
        try {
            Integer status = toStatus(condition);
            if (status == null) {
                return error(HttpStatus.BAD_REQUEST, "Condition must be 'past' or 'future'");
            }
            List<AppointmentDTO> dtos = appointmentRepository
                    .findByPatient_IdAndStatusOrderByAppointmentTimeAsc(id, status)
                    .stream().map(this::toDto).collect(Collectors.toList());
            return ok(dtos);
        } catch (Exception e) {
            log.error("Error filtering by condition", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    @Transactional
    public ResponseEntity<Map<String, Object>> filterByDoctor(String name, Long patientId) {
        try {
            List<AppointmentDTO> dtos = appointmentRepository
                    .filterByDoctorNameAndPatientId(name, patientId)
                    .stream().map(this::toDto).collect(Collectors.toList());
            return ok(dtos);
        } catch (Exception e) {
            log.error("Error filtering by doctor", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    @Transactional
    public ResponseEntity<Map<String, Object>> filterByDoctorAndCondition(String condition, String name, long patientId) {
        try {
            Integer status = toStatus(condition);
            if (status == null) {
                return error(HttpStatus.BAD_REQUEST, "Condition must be 'past' or 'future'");
            }
            List<AppointmentDTO> dtos = appointmentRepository
                    .filterByDoctorNameAndPatientIdAndStatus(name, patientId, status)
                    .stream().map(this::toDto).collect(Collectors.toList());
            return ok(dtos);
        } catch (Exception e) {
            log.error("Error filtering by doctor and condition", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    // ---------- Patient details ----------

    public ResponseEntity<Map<String, Object>> getPatientDetails(String token) {
        try {
            String email = tokenService.extractEmail(token);
            Patient patient = patientRepository.findByEmail(email);
            if (patient == null) {
                return error(HttpStatus.NOT_FOUND, "Patient not found");
            }
            Map<String, Object> body = new HashMap<>();
            body.put("patient", patient);   // password is write-only in the model, so it won't serialize
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.error("Error fetching patient details", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    // ---------- Helpers ----------

    private Integer toStatus(String condition) {
        if ("future".equalsIgnoreCase(condition)) return 0;
        if ("past".equalsIgnoreCase(condition)) return 1;
        return null;
    }

    private AppointmentDTO toDto(Appointment a) {
        return new AppointmentDTO(
                a.getId(),
                a.getDoctor().getId(), a.getDoctor().getName(),
                a.getPatient().getId(), a.getPatient().getName(),
                a.getPatient().getEmail(), a.getPatient().getPhone(), a.getPatient().getAddress(),
                a.getAppointmentTime(), a.getStatus());
    }

    private ResponseEntity<Map<String, Object>> ok(List<AppointmentDTO> dtos) {
        Map<String, Object> body = new HashMap<>();
        body.put("appointments", dtos);
        return ResponseEntity.ok(body);
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
