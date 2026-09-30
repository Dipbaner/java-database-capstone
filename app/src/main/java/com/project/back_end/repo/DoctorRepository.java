package com.project.back_end.repo;

import com.project.back_end.models.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    public Doctor findByEmail(String email);

    public List<Doctor> findByNameLike(String name);

    public List<Doctor> findByNameContainingIgnoreCaseAndSpecialityIgnoreCase
            (String name,
             String speciality);

    public List<Doctor> findBySpecialityIgnoreCase(String speciality);
}