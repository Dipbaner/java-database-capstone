# Schema Design – Smart Clinic Management System

This document describes the data structure for the Smart Clinic Management System. Structured, validated and interrelated data (users, appointments, schedules) lives in **MySQL**. Flexible, free-form data (prescriptions, feedback, logs) lives in **MongoDB**.

---

## MySQL Database Design

### Table: admin
- id: INT, Primary Key, Auto Increment
- username: VARCHAR(50), Not Null, Unique
- password: VARCHAR(255), Not Null

> Admins manage the system (add or remove doctors, view reports). Passwords should be stored hashed, not as plain text.

### Table: doctor
- id: INT, Primary Key, Auto Increment
- name: VARCHAR(100), Not Null
- specialty: VARCHAR(50), Not Null
- email: VARCHAR(100), Not Null, Unique
- password: VARCHAR(255), Not Null (minimum 6 characters, validated in code)
- phone: VARCHAR(10), Not Null (exactly 10 digits, validated in code)

> Email is Unique because it is the login identifier. Phone is stored as VARCHAR so leading zeros are never lost.

### Table: doctor_available_times
- id: INT, Primary Key, Auto Increment
- doctor_id: INT, Foreign Key → doctor(id), Not Null
- available_times: VARCHAR(20), Not Null (for example `09:00-10:00`)
- Unique constraint on (doctor_id, available_times)

> Each doctor has their own time slots, one row per slot. This is a one-to-many relationship (one doctor, many slots). In JPA it maps to an `@ElementCollection` on the Doctor entity.

### Table: patient
- id: INT, Primary Key, Auto Increment
- name: VARCHAR(100), Not Null
- email: VARCHAR(100), Not Null, Unique
- password: VARCHAR(255), Not Null
- phone: VARCHAR(10), Not Null (exactly 10 digits, validated in code)
- address: VARCHAR(255), Not Null

### Table: appointment
- id: INT, Primary Key, Auto Increment
- doctor_id: INT, Foreign Key → doctor(id), Not Null
- patient_id: INT, Foreign Key → patient(id), Not Null
- appointment_time: DATETIME, Not Null
- status: INT, Not Null, Default 0 (0 = Scheduled, 1 = Completed, 2 = Cancelled)
- Unique constraint on (doctor_id, appointment_time)

> The unique constraint stops a doctor from being double-booked at the same time. Overlap beyond identical start times (for example a 30-minute visit that crosses into the next slot) is checked in the service layer.

### Table: clinic_locations (optional)
- id: INT, Primary Key, Auto Increment
- name: VARCHAR(100), Not Null
- address: VARCHAR(255), Not Null
- phone: VARCHAR(10)

### Table: payments (optional)
- id: INT, Primary Key, Auto Increment
- appointment_id: INT, Foreign Key → appointment(id), Not Null
- amount: DECIMAL(10,2), Not Null
- payment_method: VARCHAR(30), Not Null
- payment_status: INT, Not Null, Default 0 (0 = Pending, 1 = Paid, 2 = Refunded)
- paid_at: DATETIME

### Design decisions

- **Deleting a patient:** appointments are not silently deleted. Medical and billing history should be retained, so the foreign key uses `ON DELETE RESTRICT`. If deletion is required, the patient record is anonymised or marked inactive instead.
- **Deleting a doctor:** same approach. Past appointments are kept, and the doctor is deactivated rather than removed.
- **Overlapping appointments:** not allowed for the same doctor. The unique key on (doctor_id, appointment_time) plus a service-layer check enforce this.
- **Appointment history:** retained indefinitely. Status changes (Scheduled, Completed, Cancelled) are used instead of deleting rows.
- **Doctor availability:** each doctor has their own slots in `doctor_available_times`. A slot is offered to patients only if no non-cancelled appointment exists for it.
- **Validation:** email format, 10-digit phone and password length are validated in the Java code (Bean Validation annotations), not in the database.
- **Prescriptions:** stored in MongoDB and linked to an appointment by `appointmentId`, so they depend on a MySQL appointment.

---

## MongoDB Collection Design

Prescriptions are a good fit for MongoDB. Their shape varies (one medicine or many, optional notes, attachments, pharmacy details), and the structure may change over time without requiring table migrations.

### Collection: prescriptions

```json
{
  "_id": "ObjectId('64abc123456')",
  "appointmentId": 3,
  "patientId": 3,
  "patientName": "Arjun Verma",
  "doctorId": 1,
  "doctorName": "Dr. Aarav Sharma",
  "issuedAt": "2026-10-17T10:30:00Z",
  "medications": [
    {
      "name": "Paracetamol",
      "dosage": "500mg",
      "frequency": "Every 6 hours",
      "durationDays": 5
    },
    {
      "name": "Cetirizine",
      "dosage": "10mg",
      "frequency": "Once at night",
      "durationDays": 3
    }
  ],
  "doctorNotes": "Rest, drink plenty of fluids. Return if fever persists beyond 3 days.",
  "refillCount": 2,
  "tags": ["fever", "allergy"],
  "pharmacy": {
    "name": "Apollo Pharmacy",
    "location": "MG Road, Indore"
  },
  "attachments": [
    {
      "fileName": "blood-test-report.pdf",
      "url": "https://files.example.com/reports/blood-test-report.pdf"
    }
  ],
  "metadata": {
    "createdBy": "doctor",
    "version": 1
  }
}
```

### Collection: messages (optional)

```json
{
  "_id": "ObjectId('64def789012')",
  "appointmentId": 3,
  "senderId": 3,
  "senderRole": "patient",
  "receiverId": 1,
  "receiverRole": "doctor",
  "message": "Should I continue the medicine after the fever goes away?",
  "sentAt": "2026-10-18T08:15:00Z",
  "readAt": null
}
```

### Design decisions

- **IDs instead of full objects:** documents store `patientId`, `doctorId` and `appointmentId` as references to MySQL rows. Names are copied in (`patientName`, `doctorName`) as a snapshot so a prescription still reads correctly if a profile changes later. Full patient objects are not embedded, to avoid stale or duplicated personal data.
- **Embedded data:** medications, pharmacy and attachments are embedded because they belong to one prescription and are always read together with it.
- **Schema evolution:** new fields (for example `allergies` or `insuranceInfo`) can be added to new documents without altering old ones, and `metadata.version` lets the application handle older formats.
- **Chat messages:** each message is its own document linked to an appointment, which makes it easy to fetch a conversation sorted by `sentAt`.
