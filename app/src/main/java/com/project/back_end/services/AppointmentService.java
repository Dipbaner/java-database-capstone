package com.project.back_end.services;

import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@org.springframework.stereotype.Service
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository appointmentRepository;
    private final Service service;
    private final TokenService tokenService;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              Service service,
                              TokenService tokenService,
                              PatientRepository patientRepository,
                              DoctorRepository doctorRepository) {
        this.appointmentRepository = appointmentRepository;
        this.service = service;
        this.tokenService = tokenService;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    @Transactional
    public int bookAppointment(Appointment appointment) {
        try {
            appointmentRepository.save(appointment);
            return 1;
        } catch (Exception e) {
            log.error("Error booking appointment", e);
            return 0;
        }
    }

    @Transactional
    public ResponseEntity<Map<String, String>> updateAppointment(Appointment appointment) {
        Map<String, String> response = new HashMap<>();
        try {
            Optional<Appointment> existing = appointmentRepository.findById(appointment.getId());
            if (existing.isEmpty()) {
                response.put("message", "Appointment not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Appointment current = existing.get();

            // Only the owning patient may update
            if (!current.getPatient().getId().equals(appointment.getPatient().getId())) {
                response.put("message", "Patient ID mismatch");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // Re-validate only if the time actually changes (the current slot is "booked" by itself)
            boolean timeChanged = !current.getAppointmentTime().equals(appointment.getAppointmentTime())
                    || !current.getDoctor().getId().equals(appointment.getDoctor().getId());
            if (timeChanged) {
                int valid = service.validateAppointment(appointment);
                if (valid == -1) {
                    response.put("message", "Doctor not found");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                }
                if (valid == 0) {
                    response.put("message", "Selected time slot is not available");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }
            }

            current.setDoctor(appointment.getDoctor());
            current.setAppointmentTime(appointment.getAppointmentTime());
            appointmentRepository.save(current);

            response.put("message", "Appointment updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating appointment", e);
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Transactional
    public ResponseEntity<Map<String, String>> cancelAppointment(long id, String token) {
        Map<String, String> response = new HashMap<>();
        try {
            Optional<Appointment> appointment = appointmentRepository.findById(id);
            if (appointment.isEmpty()) {
                response.put("message", "Appointment not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            String email = tokenService.extractEmail(token);
            Patient patient = patientRepository.findByEmail(email);
            if (patient == null || !appointment.get().getPatient().getId().equals(patient.getId())) {
                response.put("message", "You can only cancel your own appointments");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            appointmentRepository.delete(appointment.get());
            response.put("message", "Appointment cancelled successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error cancelling appointment {}", id, e);
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Transactional
    public Map<String, Object> getAppointment(String patientName, LocalDate date, String token) {
        Map<String, Object> result = new HashMap<>();
        try {
            String email = tokenService.extractEmail(token);
            Doctor doctor = doctorRepository.findByEmail(email);
            if (doctor == null) {
                result.put("message", "Doctor not found");
                return result;
            }

            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();

            Arrays appointments;
            if (patientName == null || patientName.isBlank() || "null".equalsIgnoreCase(patientName)) {
                appointments = appointmentRepository
                        .findByDoctorIdAndAppointmentTimeBetween(doctor.getId(), start, end);
            } else {
                appointments = appointmentRepository
                        .filterByPatientNameAndDoctorIdAndTime(patientName, doctor.getId(), start, end);
            }
            result.put("appointments", appointments);
        } catch (Exception e) {
            log.error("Error fetching appointments", e);
            result.put("message", "Internal server error");
        }
        return result;
    }

    @Transactional
    public ResponseEntity<Map<String, String>> changeStatus(long appointmentId, int status) {
        Map<String, String> response = new HashMap<>();
        try {
            if (!appointmentRepository.existsById(appointmentId)) {
                response.put("message", "Appointment not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            appointmentRepository.updateStatus(status, appointmentId);
            response.put("message", "Status updated");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error changing status", e);
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}