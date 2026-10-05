package com.project.back_end.services;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DoctorService {

    private static final Logger log = LoggerFactory.getLogger(DoctorService.class);
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;

    public DoctorService(DoctorRepository doctorRepository,
                         AppointmentRepository appointmentRepository,
                         TokenService tokenService) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }

    // ---------- Availability ----------

    @Transactional
    public List<String> getDoctorAvailability(Long doctorId, LocalDate date) {
        Doctor doctor = doctorRepository.findById(doctorId).orElse(null);
        if (doctor == null) return Collections.emptyList();

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<Appointment> booked =
                appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(doctorId, start, end);

        Set<String> bookedStarts = booked.stream()
                .map(a -> a.getAppointmentTime().toLocalTime().format(HHMM))
                .collect(Collectors.toSet());

        List<String> slots = slotsOf(doctor);
        return slots.stream()
                .filter(slot -> !bookedStarts.contains(slotStart(slot)))
                .collect(Collectors.toList());
    }

    // ---------- CRUD ----------

    public int saveDoctor(Doctor doctor) {
        try {
            if (doctorRepository.findByEmail(doctor.getEmail()) != null) return -1;
            doctorRepository.save(doctor);
            return 1;
        } catch (Exception e) {
            log.error("Error saving doctor", e);
            return 0;
        }
    }

    public int updateDoctor(Doctor doctor) {
        try {
            if (doctor.getId() == null || !doctorRepository.existsById(doctor.getId())) return -1;
            doctorRepository.save(doctor);
            return 1;
        } catch (Exception e) {
            log.error("Error updating doctor", e);
            return 0;
        }
    }

    @Transactional
    public List<Doctor> getDoctors() {
        List<Doctor> doctors = doctorRepository.findAll();
        initSlots(doctors);
        return doctors;
    }

    @Transactional
    public int deleteDoctor(long id) {
        try {
            if (!doctorRepository.existsById(id)) return -1;
            appointmentRepository.deleteAllByDoctorId(id);
            doctorRepository.deleteById(id);
            return 1;
        } catch (Exception e) {
            log.error("Error deleting doctor {}", id, e);
            return 0;
        }
    }

    // ---------- Login ----------

    public ResponseEntity<Map<String, String>> validateDoctor(Login login) {
        Map<String, String> response = new HashMap<>();
        try {
            Doctor doctor = doctorRepository.findByEmail(login.getEmail());
            if (doctor == null || !doctor.getPassword().equals(login.getPassword())) {
                response.put("message", "Invalid email or password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            response.put("message", "Login successful");
            response.put("token", tokenService.generateToken(doctor.getEmail()));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Doctor login error", e);
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ---------- Search and filters ----------

    @Transactional
    public Map<String, Object> findDoctorByName(String name) {
        List<Doctor> doctors = doctorRepository.findByNameLike(name);
        initSlots(doctors);
        return Map.of("doctors", doctors);
    }

    @Transactional
    public Map<String, Object> filterDoctorsByNameSpecilityandTime(String name, String specialty, String amOrPm) {
        List<Doctor> doctors = doctorRepository
                .findByNameContainingIgnoreCaseAndSpecialityIgnoreCase(name, specialty);
        return Map.of("doctors", filterDoctorByTime(doctors, amOrPm));
    }

    @Transactional
    public Map<String, Object> filterDoctorByNameAndTime(String name, String amOrPm) {
        List<Doctor> doctors = doctorRepository.findByNameLike(name);
        return Map.of("doctors", filterDoctorByTime(doctors, amOrPm));
    }

    @Transactional
    public Map<String, Object> filterDoctorByNameAndSpecility(String name, String specialty) {
        List<Doctor> doctors = doctorRepository
                .findByNameContainingIgnoreCaseAndSpecialityIgnoreCase(name, specialty);
        initSlots(doctors);
        return Map.of("doctors", doctors);
    }

    @Transactional
    public Map<String, Object> filterDoctorByTimeAndSpecility(String specialty, String amOrPm) {
        List<Doctor> doctors = doctorRepository.findBySpecialityIgnoreCase(specialty);
        return Map.of("doctors", filterDoctorByTime(doctors, amOrPm));
    }

    @Transactional
    public Map<String, Object> filterDoctorBySpecility(String specialty) {
        List<Doctor> doctors = doctorRepository.findBySpecialityIgnoreCase(specialty);
        initSlots(doctors);
        return Map.of("doctors", doctors);
    }

    @Transactional
    public Map<String, Object> filterDoctorsByTime(String amOrPm) {
        List<Doctor> doctors = doctorRepository.findAll();
        return Map.of("doctors", filterDoctorByTime(doctors, amOrPm));
    }

    /** Keeps doctors with at least one slot in the requested AM/PM period. */
    @Transactional
    public List<Doctor> filterDoctorByTime(List<Doctor> doctors, String amOrPm) {
        boolean wantAm = "AM".equalsIgnoreCase(amOrPm);
        List<Doctor> result = new ArrayList<>();
        for (Doctor d : doctors) {
            List<String> slots = slotsOf(d);   // also forces lazy loading
            boolean match = slots.stream().anyMatch(slot -> isAm(slot) == wantAm);
            if (match) result.add(d);
        }
        return result;
    }

    // ---------- Helpers ----------

    /** Null-safe slot list for a doctor. */
    private List<String> slotsOf(Doctor doctor) {
        List<String> slots = doctor.getAvailableTimes();
        return slots == null ? Collections.emptyList() : slots;
    }

    /** Forces lazy collections to load while the transaction is open. */
    private void initSlots(List<Doctor> doctors) {
        for (Doctor d : doctors) {
            slotsOf(d).size();
        }
    }

    private String slotStart(String slot) {          // "09:00-10:00" -> "09:00"
        return slot.split("-")[0].trim();
    }

    private boolean isAm(String slot) {              // hour before 12 = AM
        try {
            return Integer.parseInt(slotStart(slot).split(":")[0]) < 12;
        } catch (NumberFormatException e) {
            log.warn("Unrecognised slot format: {}", slot);
            return false;
        }
    }
}