# User Stories – Smart Clinic Patient Appointment Portal

Each story follows the format: **As a [role], I want [feature], so that [benefit].**
Each story includes acceptance criteria and a priority (High, Medium or Low).

---

## Admin User Stories

### Admin Story 1: Admin login
**Title:** Admin login
_As an admin, I want to log in with my username and password, so that I can securely access the admin dashboard._

**Acceptance Criteria:**
1. Admin can log in with a valid username and password.
2. Invalid credentials show a clear error message.
3. A successful login opens the admin dashboard.

**Priority:** High
**Story Points:** 2
**Notes:**
- Passwords should be stored hashed.
- Protected admin pages require a valid session or token.

### Admin Story 2: Admin logout
**Title:** Admin logout
_As an admin, I want to log out of the portal, so that unauthorized people cannot access the system after I leave._

**Acceptance Criteria:**
1. Clicking logout ends the session or invalidates the token.
2. After logout, protected pages redirect to the login page.

**Priority:** High
**Story Points:** 1

### Admin Story 3: Add doctors
**Title:** Add doctor profiles
_As an admin, I want to add doctors to the portal, so that patients can find and book them._

**Acceptance Criteria:**
1. Admin can enter name, specialty, email, phone and password for a new doctor.
2. Email must be unique, phone must be 10 digits, and password must be at least 6 characters.
3. The new doctor appears in the doctor list immediately.

**Priority:** High
**Story Points:** 3

### Admin Story 4: Delete doctors
**Title:** Delete doctor profile
_As an admin, I want to delete a doctor's profile from the portal, so that doctors who have left the clinic no longer appear to patients._

**Acceptance Criteria:**
1. Admin can delete a doctor by id.
2. The doctor no longer appears in search results or booking options.
3. Past appointment history is preserved.

**Priority:** Medium
**Story Points:** 2
**Notes:**
- Consider deactivating instead of hard-deleting so history stays intact.

### Admin Story 5: View usage statistics
**Title:** Track monthly usage using the MySQL CLI
_As an admin, I want to run a stored procedure in the MySQL CLI to get the number of appointments per month, so that I can track usage statistics._

**Acceptance Criteria:**
1. A stored procedure returns the appointment count grouped by month.
2. It can be run from the MySQL CLI or Workbench with a single `CALL` statement.
3. Results are ordered by month.

**Priority:** Medium
**Story Points:** 3

### Admin Story 6: Find the busiest doctor
**Title:** Doctor with the most patients
_As an admin, I want to see which doctor has the most patients in a given month or year, so that I can plan staffing and workload._

**Acceptance Criteria:**
1. Stored procedures `GetDoctorWithMostPatientsByMonth` and `GetDoctorWithMostPatientsByYear` return the top doctor and patient count.
2. The result includes the doctor's name and specialty.

**Priority:** Low
**Story Points:** 3

---

## Patient User Stories

### Patient Story 1: View doctors without logging in
**Title:** Browse doctors
_As a patient, I want to view a list of doctors without logging in, so that I can explore options before registering._

**Acceptance Criteria:**
1. The doctor list is visible on the public page without authentication.
2. Each doctor shows name, specialty and available time slots.
3. Booking requires login.

**Priority:** Medium
**Story Points:** 2

### Patient Story 2: Sign up
**Title:** Patient registration
_As a patient, I want to sign up with my email and password, so that I can book appointments._

**Acceptance Criteria:**
1. Patient can register with name, email, password, phone and address.
2. Email must be unique and phone must be 10 digits.
3. Duplicate email shows a clear error message.

**Priority:** High
**Story Points:** 3

### Patient Story 3: Log in
**Title:** Patient login
_As a patient, I want to log in to the portal, so that I can manage my bookings._

**Acceptance Criteria:**
1. Valid credentials open the patient dashboard.
2. Invalid credentials show an error message.

**Priority:** High
**Story Points:** 2

### Patient Story 4: Log out
**Title:** Patient logout
_As a patient, I want to log out of the portal, so that my account stays secure._

**Acceptance Criteria:**
1. Logout ends the session or invalidates the token.
2. Protected pages redirect to the login page afterwards.

**Priority:** High
**Story Points:** 1

### Patient Story 5: Book an appointment
**Title:** Book an hour-long appointment
_As a patient, I want to book an hour-long appointment with a doctor, so that I can consult them._

**Acceptance Criteria:**
1. Patient can pick a doctor, a date and one of the doctor's available time slots.
2. Slots that are already booked cannot be selected.
3. A confirmation is shown after booking, and the appointment status is Scheduled.

**Priority:** High
**Story Points:** 5

### Patient Story 6: View upcoming appointments
**Title:** View my appointments
_As a patient, I want to view my upcoming appointments, so that I can prepare accordingly._

**Acceptance Criteria:**
1. Patient sees only their own appointments.
2. Each entry shows doctor name, specialty, date, time and status.
3. Appointments are sorted by date.

**Priority:** High
**Story Points:** 3

### Patient Story 7: Cancel an appointment
**Title:** Cancel appointment
_As a patient, I want to cancel an upcoming appointment, so that the slot is freed if my plans change._

**Acceptance Criteria:**
1. Patient can cancel only their own scheduled appointments.
2. The status changes to Cancelled and the slot becomes available again.

**Priority:** Medium
**Story Points:** 2

### Patient Story 8: Search doctors by specialty and time
**Title:** Filter doctors
_As a patient, I want to filter doctors by specialty and available time, so that I can quickly find a suitable doctor._

**Acceptance Criteria:**
1. Patient can select a specialty and a time slot.
2. Only matching doctors are returned.
3. If none match, a "no doctors found" message is shown.

**Priority:** Medium
**Story Points:** 3

---

## Doctor User Stories

### Doctor Story 1: Log in
**Title:** Doctor login
_As a doctor, I want to log in to the portal, so that I can manage my appointments._

**Acceptance Criteria:**
1. Valid credentials open the doctor dashboard.
2. Invalid credentials show an error message.

**Priority:** High
**Story Points:** 2

### Doctor Story 2: Log out
**Title:** Doctor logout
_As a doctor, I want to log out of the portal, so that patient data stays protected._

**Acceptance Criteria:**
1. Logout ends the session or invalidates the token.
2. Protected pages redirect to the login page afterwards.

**Priority:** High
**Story Points:** 1

### Doctor Story 3: View my appointment calendar
**Title:** View appointment calendar
_As a doctor, I want to view my appointment calendar, so that I can stay organized._

**Acceptance Criteria:**
1. Doctor sees only their own appointments.
2. Appointments can be viewed by day.
3. Each entry shows patient name, phone, time and status.

**Priority:** High
**Story Points:** 3

### Doctor Story 4: Mark unavailability
**Title:** Mark unavailable time
_As a doctor, I want to mark my unavailability, so that patients only see the slots I can actually take._

**Acceptance Criteria:**
1. Doctor can remove or block specific time slots.
2. Blocked slots no longer appear to patients.
3. Existing booked appointments are not silently removed.

**Priority:** High
**Story Points:** 3

### Doctor Story 5: Update my profile
**Title:** Update profile information
_As a doctor, I want to update my profile with specialization and contact information, so that patients have up-to-date information._

**Acceptance Criteria:**
1. Doctor can edit specialty, email and phone.
2. Validation rules (unique email, 10-digit phone) still apply.
3. Changes are visible to patients immediately.

**Priority:** Medium
**Story Points:** 2

### Doctor Story 6: View patient details for upcoming appointments
**Title:** View patient details
_As a doctor, I want to view the patient details for upcoming appointments, so that I can be prepared._

**Acceptance Criteria:**
1. Doctor can open an upcoming appointment and see the patient's name, phone and address.
2. Doctor cannot see patients who have no appointment with them.

**Priority:** Medium
**Story Points:** 2

### Doctor Story 7: Mark appointment as completed
**Title:** Complete an appointment
_As a doctor, I want to mark an appointment as completed, so that records stay accurate._

**Acceptance Criteria:**
1. Doctor can change an appointment's status from Scheduled to Completed.
2. Completed appointments no longer appear under upcoming appointments.

**Priority:** Medium
**Story Points:** 2

### Doctor Story 8: Daily appointment report
**Title:** Daily report
_As a doctor, I want to get a daily report of my appointments using a stored procedure, so that I can review my schedule for the day._

**Acceptance Criteria:**
1. `GetDailyAppointmentReportByDoctor` returns patient name, phone, time and status for a given date and doctor.
2. Results are ordered by time.

**Priority:** Low
**Story Points:** 3
