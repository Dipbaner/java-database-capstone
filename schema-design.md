# Schema Design

The system uses two databases:

- **MySQL** (relational, via Spring Data JPA) for structured, related data: admins, doctors, patients and appointments.
- **MongoDB** (document, via Spring Data MongoDB) for flexible, document-style data: prescriptions.

---

## MySQL Database Design

### Table: admin

| Column   | Data Type    | Constraints                 |
|----------|--------------|-----------------------------|
| id       | BIGINT       | PRIMARY KEY, AUTO_INCREMENT |
| username | VARCHAR(255) | NOT NULL                    |
| password | VARCHAR(255) | NOT NULL                    |

**Notes**
- `password` is write-only in JSON (`@JsonProperty(access = WRITE_ONLY)`), so it is never returned in API responses.
- Passwords should be stored hashed (e.g. BCrypt), never as plain text.
- Consider adding a `UNIQUE` constraint on `username`.

---

### Table: doctor

| Column     | Data Type    | Constraints                                   |
|------------|--------------|-----------------------------------------------|
| id         | BIGINT       | PRIMARY KEY, AUTO_INCREMENT                   |
| name       | VARCHAR(100) | NOT NULL, length 1-100                        |
| speciality | VARCHAR(50)  | NOT NULL, length 3-50                         |
| email      | VARCHAR(255) | NOT NULL, must be a valid email format        |
| password   | VARCHAR(255) | NOT NULL, min length 6                        |
| phone      | VARCHAR(10)  | NOT NULL, exactly 10 digits (`^[0-9]{10}$`)   |

**Notes**
- `password` is write-only in JSON responses.
- `email` should be `UNIQUE` so two doctors cannot share the same login.

### Table: doctor_available_times

Generated automatically by `@ElementCollection` on `List<String> availableTimes`.

| Column          | Data Type    | Constraints                                  |
|-----------------|--------------|----------------------------------------------|
| doctor_id       | BIGINT       | FOREIGN KEY -> doctor(id), NOT NULL          |
| available_times | VARCHAR(255) | Time slot, e.g. `"09:00-10:00"`              |

**Notes**
- One doctor has many available time slots (one-to-many).
- Slots are stored as plain strings, one row per slot.

---

### Table: patient

| Column   | Data Type    | Constraints                                   |
|----------|--------------|-----------------------------------------------|
| id       | BIGINT       | PRIMARY KEY, AUTO_INCREMENT                   |
| name     | VARCHAR(100) | NOT NULL, length 3-100                        |
| email    | VARCHAR(255) | NOT NULL, must be a valid email format        |
| password | VARCHAR(255) | NOT NULL, min length 6                        |
| phone    | VARCHAR(10)  | NOT NULL, exactly 10 digits (`^[0-9]{10}$`)   |
| address  | VARCHAR(255) | NOT NULL, max length 255                      |

**Notes**
- `email` should be `UNIQUE`.
- Passwords should be stored hashed.

---

### Table: appointment

| Column           | Data Type | Constraints                                          |
|------------------|-----------|------------------------------------------------------|
| id               | BIGINT    | PRIMARY KEY, AUTO_INCREMENT                          |
| doctor_id        | BIGINT    | FOREIGN KEY -> doctor(id), NOT NULL                  |
| patient_id       | BIGINT    | FOREIGN KEY -> patient(id), NOT NULL                 |
| appointment_time | DATETIME  | Must be a future date/time (`@Future`)               |
| status           | INT       | NOT NULL. `0` = Scheduled, `1` = Completed           |

**Derived (non-persisted) values**
- `endTime` = `appointment_time` + 1 hour (each appointment lasts one hour).
- `appointmentDate` = date portion of `appointment_time`.
- `appointmentTimeOnly` = time portion of `appointment_time`.

**Notes**
- Many appointments belong to one doctor (`@ManyToOne`).
- Many appointments belong to one patient (`@ManyToOne`).
- Consider a unique index on `(doctor_id, appointment_time)` to prevent double-booking a doctor.

---

### Relationships Summary

| Relationship                     | Type        | Implemented By                            |
|----------------------------------|-------------|-------------------------------------------|
| Doctor -> Available times        | One-to-many | `@ElementCollection` (`doctor_available_times`) |
| Doctor -> Appointments           | One-to-many | `appointment.doctor_id` FK                |
| Patient -> Appointments          | One-to-many | `appointment.patient_id` FK               |
| Appointment -> Prescription      | One-to-one (logical) | `appointmentId` stored in MongoDB |

---

## MongoDB Collection Design

### Collection: prescriptions

Prescriptions are stored as documents because their content (medication details, free-text notes) is flexible and self-contained. Each prescription links back to MySQL through `appointmentId`.

```json
{
  "_id": "64abc123def4567890abc123",
  "patientName": "John Smith",
  "appointmentId": 51,
  "medication": "Paracetamol",
  "dosage": "500mg, twice a day for 5 days",
  "doctorNotes": "Take after meals. Return if fever persists beyond 3 days."
}
```

| Field         | Type   | Constraints                          |
|---------------|--------|--------------------------------------|
| _id           | String | Auto-generated ObjectId (`@Id`)      |
| patientName   | String | Required, length 3-100               |
| appointmentId | Long   | Required, references MySQL `appointment.id` |
| medication    | String | Required, length 3-100               |
| dosage        | String | Required                             |
| doctorNotes   | String | Optional, max 200 characters         |

**Notes**
- `appointmentId` is a logical reference only; MongoDB does not enforce foreign keys, so the service layer should confirm the appointment exists before saving.
- An index on `appointmentId` is recommended for fast lookup of prescriptions by appointment.
