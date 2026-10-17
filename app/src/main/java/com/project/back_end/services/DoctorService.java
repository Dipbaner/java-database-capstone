package com.project.back_end.services;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DoctorService {

    private static final Logger logger = LoggerFactory.getLogger(DoctorService.class);
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

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

    /** Returns the doctor's time slots for a date that are not yet booked. */
    @Transactional
    public List<String> getDoctorAvailability(Long doctorId, LocalDate date) {
        Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);
        if (doctorOpt.isEmpty() || doctorOpt.get().getAvailableTimes() == null) {
            return new ArrayList<>();
        }

        List<Appointment> booked = appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(
                doctorId, date.atStartOfDay(), date.atTime(LocalTime.MAX));

        Set<String> bookedStarts = new HashSet<>();
        for (Appointment appointment : booked) {
            bookedStarts.add(appointment.getAppointmentTime().toLocalTime().format(HH_MM));
        }

        return doctorOpt.get().getAvailableTimes().stream()
                .filter(slot -> !bookedStarts.contains(slotStart(slot)))
                .collect(Collectors.toList());
    }

    /** @return 1 on success, -1 if a doctor with the same email exists, 0 on internal error. */
    @Transactional
    public int saveDoctor(Doctor doctor) {
        try {
            if (doctorRepository.findByEmail(doctor.getEmail()) != null) {
                return -1;
            }
            doctorRepository.save(doctor);
            return 1;
        } catch (Exception e) {
            logger.error("Error saving doctor", e);
            return 0;
        }
    }

    /** @return 1 on success, -1 if the doctor does not exist, 0 on internal error. */
    @Transactional
    public int updateDoctor(Doctor doctor) {
        try {
            if (doctor.getId() == null || doctorRepository.findById(doctor.getId()).isEmpty()) {
                return -1;
            }
            doctorRepository.save(doctor);
            return 1;
        } catch (Exception e) {
            logger.error("Error updating doctor", e);
            return 0;
        }
    }

    @Transactional
    public List<Doctor> getDoctors() {
        List<Doctor> doctors = doctorRepository.findAll();
        doctors.forEach(d -> {
            if (d.getAvailableTimes() != null) {
                d.getAvailableTimes().size(); // make sure the collection is initialised
            }
        });
        return doctors;
    }

    /** Deletes a doctor and all of their appointments. @return 1 ok, -1 not found, 0 error. */
    @Transactional
    public int deleteDoctor(long id) {
        try {
            if (doctorRepository.findById(id).isEmpty()) {
                return -1;
            }
            appointmentRepository.deleteAllByDoctorId(id);
            doctorRepository.deleteById(id);
            return 1;
        } catch (Exception e) {
            logger.error("Error deleting doctor {}", id, e);
            return 0;
        }
    }

    /** Validates doctor credentials and returns a JWT on success. */
    public ResponseEntity<Map<String, String>> validateDoctor(Login login) {
        Map<String, String> response = new HashMap<>();
        try {
            Doctor doctor = doctorRepository.findByEmail(login.getEmail());
            if (doctor == null) {
                response.put("message", "Invalid email or password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            if (!doctor.getPassword().equals(login.getPassword())) {
                response.put("message", "Invalid email or password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            response.put("message", "Login successful");
            response.put("token", tokenService.generateToken(doctor.getEmail()));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error validating doctor login", e);
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Transactional
    public Map<String, Object> findDoctorByName(String name) {
        return wrap(withTimes(doctorRepository.findByNameLike(name)));
    }

    @Transactional
    public Map<String, Object> filterDoctorsByNameSpecilityandTime(String name, String specialty, String amOrPm) {
        List<Doctor> doctors = doctorRepository.findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(name, specialty);
        return wrap(filterDoctorByTime(doctors, amOrPm));
    }

    public List<Doctor> filterDoctorByTime(List<Doctor> doctors, String amOrPm) {
        if (amOrPm == null) {
            return doctors;
        }
        String period = amOrPm.trim().toUpperCase();
        if (!period.equals("AM") && !period.equals("PM")) {
            return doctors;
        }
        return doctors.stream()
                .filter(d -> d.getAvailableTimes() != null && d.getAvailableTimes().stream()
                        .anyMatch(slot -> matchesPeriod(slot, period)))
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> filterDoctorByNameAndTime(String name, String amOrPm) {
        return wrap(filterDoctorByTime(withTimes(doctorRepository.findByNameLike(name)), amOrPm));
    }

    @Transactional
    public Map<String, Object> filterDoctorByNameAndSpecility(String name, String specialty) {
        return wrap(withTimes(
                doctorRepository.findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(name, specialty)));
    }

    @Transactional
    public Map<String, Object> filterDoctorByTimeAndSpecility(String specialty, String amOrPm) {
        return wrap(filterDoctorByTime(withTimes(doctorRepository.findBySpecialtyIgnoreCase(specialty)), amOrPm));
    }

    @Transactional
    public Map<String, Object> filterDoctorBySpecility(String specialty) {
        return wrap(withTimes(doctorRepository.findBySpecialtyIgnoreCase(specialty)));
    }

    @Transactional
    public Map<String, Object> filterDoctorsByTime(String amOrPm) {
        return wrap(filterDoctorByTime(withTimes(doctorRepository.findAll()), amOrPm));
    }

    // ---------------------------------------------------------------- helpers

    private Map<String, Object> wrap(List<Doctor> doctors) {
        Map<String, Object> result = new HashMap<>();
        result.put("doctors", doctors);
        return result;
    }

    private List<Doctor> withTimes(List<Doctor> doctors) {
        doctors.forEach(d -> {
            if (d.getAvailableTimes() != null) {
                d.getAvailableTimes().size();
            }
        });
        return doctors;
    }

    /** "09:00-10:00" -> "09:00" (zero-padded so it matches LocalTime formatting). */
    private String slotStart(String slot) {
        String start = slot.split("-")[0].trim();
        if (start.length() == 4 && start.charAt(1) == ':') {
            start = "0" + start;
        }
        return start;
    }

    private boolean matchesPeriod(String slot, String period) {
        try {
            int hour = Integer.parseInt(slotStart(slot).split(":")[0]);
            return period.equals("AM") ? hour < 12 : hour >= 12;
        } catch (Exception e) {
            return false;
        }
    }
}
