package com.project.back_end.services;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// Fully-qualified: a class named "Service" already exists in this package.
@org.springframework.stereotype.Service
public class AppointmentService {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentService.class);

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

    /** @return 1 if the appointment was saved, 0 otherwise. */
    @Transactional
    public int bookAppointment(Appointment appointment) {
        try {
            Optional<Doctor> doctor = doctorRepository.findById(appointment.getDoctor().getId());
            Optional<Patient> patient = patientRepository.findById(appointment.getPatient().getId());
            if (doctor.isEmpty() || patient.isEmpty()) {
                return 0;
            }
            appointment.setDoctor(doctor.get());
            appointment.setPatient(patient.get());
            appointmentRepository.save(appointment);
            return 1;
        } catch (Exception e) {
            logger.error("Error booking appointment", e);
            return 0;
        }
    }

    /** Updates the time (and doctor) of an existing appointment owned by the given patient. */
    @Transactional
    public ResponseEntity<Map<String, String>> updateAppointment(Appointment appointment) {
        Map<String, String> response = new HashMap<>();
        try {
            Optional<Appointment> existingOpt = appointment.getId() == null
                    ? Optional.empty()
                    : appointmentRepository.findById(appointment.getId());
            if (existingOpt.isEmpty()) {
                response.put("message", "Appointment not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            Appointment existing = existingOpt.get();

            if (appointment.getPatient() == null
                    || !existing.getPatient().getId().equals(appointment.getPatient().getId())) {
                response.put("message", "You can only update your own appointments");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // Keeping the same doctor and the same slot is always allowed; anything else must be free.
            boolean sameSlot = existing.getDoctor().getId().equals(appointment.getDoctor().getId())
                    && existing.getAppointmentTime().equals(appointment.getAppointmentTime());
            if (!sameSlot) {
                int validity = service.validateAppointment(appointment);
                if (validity == -1) {
                    response.put("message", "Invalid doctor id");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }
                if (validity == 0) {
                    response.put("message", "Appointment time is not available");
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
                }
            }

            Doctor doctor = doctorRepository.findById(appointment.getDoctor().getId()).orElse(existing.getDoctor());
            existing.setDoctor(doctor);
            existing.setAppointmentTime(appointment.getAppointmentTime());
            appointmentRepository.save(existing);

            response.put("message", "Appointment updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error updating appointment", e);
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /** Cancels (deletes) an appointment, but only for the patient who owns it. */
    @Transactional
    public ResponseEntity<Map<String, String>> cancelAppointment(long id, String token) {
        Map<String, String> response = new HashMap<>();
        try {
            Optional<Appointment> appointmentOpt = appointmentRepository.findById(id);
            if (appointmentOpt.isEmpty()) {
                response.put("message", "Appointment not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            String email = tokenService.extractEmail(token);
            Patient patient = patientRepository.findByEmail(email);
            if (patient == null || !patient.getId().equals(appointmentOpt.get().getPatient().getId())) {
                response.put("message", "You can only cancel your own appointments");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            appointmentRepository.deleteById(id);
            response.put("message", "Appointment cancelled successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error cancelling appointment {}", id, e);
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Appointments of the doctor identified by the token on the given date,
     * optionally narrowed by (partial) patient name.
     */
    @Transactional
    public Map<String, Object> getAppointment(String pname, LocalDate date, String token) {
        Map<String, Object> result = new HashMap<>();
        try {
            String email = tokenService.extractEmail(token);
            Doctor doctor = doctorRepository.findByEmail(email);
            if (doctor == null) {
                result.put("appointments", List.of());
                return result;
            }

            var start = date.atStartOfDay();
            var end = date.atTime(LocalTime.MAX);

            List<Appointment> appointments;
            if (pname == null || pname.isBlank() || pname.equalsIgnoreCase("null")) {
                appointments = appointmentRepository
                        .findByDoctorIdAndAppointmentTimeBetween(doctor.getId(), start, end);
            } else {
                appointments = appointmentRepository
                        .findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
                                doctor.getId(), pname, start, end);
            }

            List<AppointmentDTO> dtos = appointments.stream()
                    .map(a -> new AppointmentDTO(
                            a.getId(),
                            a.getDoctor().getId(),
                            a.getDoctor().getName(),
                            a.getPatient().getId(),
                            a.getPatient().getName(),
                            a.getPatient().getEmail(),
                            a.getPatient().getPhone(),
                            a.getPatient().getAddress(),
                            a.getAppointmentTime(),
                            a.getStatus()))
                    .collect(Collectors.toList());
            result.put("appointments", dtos);
        } catch (Exception e) {
            logger.error("Error fetching appointments", e);
            result.put("appointments", List.of());
        }
        return result;
    }

    /** Marks an appointment as completed (status = 1). */
    @Transactional
    public void changeStatus(long appointmentId) {
        appointmentRepository.updateStatus(1, appointmentId);
    }
}
