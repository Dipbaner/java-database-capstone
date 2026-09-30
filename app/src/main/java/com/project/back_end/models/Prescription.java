package com.project.back_end.models;

import org.springframework.data.annotation.Id;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class Prescription {

    @Id
    private String id;

    @NotNull
    @Size(min = 3, max = 100)
    private String patientName;

    @NotNull
    private Long appointmentId;

    @NotNull
    @Size(min = 3, max = 100)
    private String medication;

    @NotNull
    private String dosage;

    @Size(max = 200)
    private String doctorNotes;

    public Prescription() {

    }

    public Prescription(String patientName, Long appointmentId, String medication, String dosage, String doctorNotes) {
        this.patientName = patientName;
        this.appointmentId = appointmentId;
        this.medication = medication;
        this.dosage = dosage;
        this.doctorNotes = doctorNotes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public @NotNull @Size(min = 3, max = 100) String getPatientName() {
        return patientName;
    }

    public void setPatientName(@NotNull @Size(min = 3, max = 100) String patientName) {
        this.patientName = patientName;
    }

    public @NotNull Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(@NotNull Long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public @NotNull @Size(min = 3, max = 100) String getMedication() {
        return medication;
    }

    public void setMedication(@NotNull @Size(min = 3, max = 100) String medication) {
        this.medication = medication;
    }

    public @NotNull String getDosage() {
        return dosage;
    }

    public void setDosage(@NotNull String dosage) {
        this.dosage = dosage;
    }

    public @Size(max = 200) String getDoctorNotes() {
        return doctorNotes;
    }

    public void setDoctorNotes(@Size(max = 200) String doctorNotes) {
        this.doctorNotes = doctorNotes;
    }
}
