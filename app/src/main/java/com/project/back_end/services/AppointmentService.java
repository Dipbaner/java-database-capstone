package com.project.back_end.services;

import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AppointmentService {

    private AppointmentRepository appointmentRepository;
    private TokenService tokenService;
    private PatientRepository patientRepository;
    private DoctorRepository doctorRepository;
    private final Service service;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            TokenService tokenService,
            PatientRepository patientRepository,
            DoctorRepository doctorRepository, Service service) {
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.service = service;
    }

    @Transactional
    public int bookAppointment(Appointment appointment){
        try{
            appointmentRepository.save(appointment);
            return 1;
        }
        catch (Exception e){
            return 0;
        }
    }

    @Transactional
    public ResponseEntity<Map<String, String>> updateAppointment(
            Appointment appointment
    ) {
        Map<String, String> response = new HashMap<>();

        // Appointment must exist

        Optional<Appointment> existingOpt =
                appointmentRepository.findById(appointment.getId());

        if(existingOpt.isEmpty()){
            response.put("message", "Appointment not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(response);
        }

        Appointment existing = existingOpt.get();

        // PatientId must match the owner of the appointment
        if(appointment.getPatient() == null ||
        appointment.getPatient().getName() == null ||
               !existing.getPatient().getId().equals(
                       appointment.getPatient().getId())) {
            response.put("message", "You are not allowed to update this " +
                    "appointment");
            return ResponseEntity.status(
                    HttpStatus.FORBIDDEN
            ).body(response);
        }

        // Doctor must exist and be available at request time
        int validation = service.validateAppointment(appointment);

        if(validation == -1) {
            response.put("message", "Doctor not found");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(response);
        }

        if(validation == 0) {
            response.put("message", "Selected time slot is not available");
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(response);
        }

        try {
            existing.setDoctor(appointment.getDoctor());
            existing.setAppointmentTime(appointment.getAppointmentTime());
            appointmentRepository.save(existing);
            response.put("message", "Appointment updated successfully");
            return ResponseEntity.ok(response);
        }
        catch (Exception e) {
            response.put("message", "Error updating appointment");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    // 6. Cancel (delete) an appointment; only the patient who owns it may do so.
    @Transactional
    public ResponseEntity<Map<String, String>> cancelAppointment(
            long id, String token ){
        Map<String, String> response = new HashMap<>();

        Optional<Appointment> appointmentOpt = appointmentRepository
                .findById(id);

        if(appointmentOpt.isEmpty()){
            response.put("message", "Appointment not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(response);
        }

        String email = tokenService.extractEmail(token);
        Patient patient = patientRepository.findByEmail(email);
        Appointment appointment = appointmentOpt.get();

        if(patient == null ||
            !appointment.getPatient().getId().equals(patient.getId())){
            response.put("message", "You are not allowed to cancel " +
                    "this appointment");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(response);
        }

        try {
            appointmentRepository.delete(appointment);
            response.put("message", "Appointment cancelled successfully");
            return ResponseEntity.ok(response);
        }
        catch (Exception e) {
            response.put("message", "Error cancelling appointment");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    // Get a doctor's appointment for a day, optionally filtered by patient name
    @Transactional()
    public Map<String, Object> getAppointment(
            String pname, LocalDate date, String token){
        Map<String, Object> result =  new HashMap<>();

        String email = tokenService.extractEmail(token);
        Doctor doctor = doctorRepository.findByEmail(email);

        if(doctor == null) {
            result.put("appointments", List.of());
            return result;
        }

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        List<Appointment> appointments;

        if(pname == null || pname.isBlank() || pname.equalsIgnoreCase("null")){
            appointments = appointmentRepository
                    .fi
        }
    }
}
