// doctorDashboard.js
import { getAllAppointments } from "./services/appointmentRecordService.js";
import { createPatientRow } from "./components/patientRows.js";

const tableBody = document.getElementById("patientTableBody");
const token = localStorage.getItem("token");

function todayString() {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${now.getFullYear()}-${month}-${day}`;
}

let selectedDate = todayString();
let patientName = null;

// Search by patient name
document.getElementById("searchBar")?.addEventListener("input", (e) => {
  const value = e.target.value.trim();
  patientName = value.length > 0 ? value : "null"; // backend expects the string "null" for "no filter"
  loadAppointments();
});

// "Today" button
document.getElementById("todayButton")?.addEventListener("click", () => {
  selectedDate = todayString();
  const datePicker = document.getElementById("datePicker");
  if (datePicker) datePicker.value = selectedDate;
  loadAppointments();
});

// Date picker
document.getElementById("datePicker")?.addEventListener("change", (e) => {
  selectedDate = e.target.value;
  loadAppointments();
});

async function loadAppointments() {
  try {
    const data = await getAllAppointments(selectedDate, patientName || "null", token);
    const appointments = data.appointments || [];

    tableBody.innerHTML = "";

    if (appointments.length === 0) {
      tableBody.innerHTML =
        `<tr><td class="noPatientRecord" colspan="5">No Appointments found for today.</td></tr>`;
      return;
    }

    appointments.forEach((appointment) => {
      const patient = {
        id: appointment.patientId,
        name: appointment.patientName,
        phone: appointment.patientPhone,
        email: appointment.patientEmail
      };
      const row = createPatientRow(patient, appointment.id, appointment.doctorId);
      tableBody.appendChild(row);
    });
  } catch (error) {
    console.error("Error loading appointments:", error);
    tableBody.innerHTML =
      `<tr><td class="noPatientRecord" colspan="5">Error loading appointments. Try again later.</td></tr>`;
  }
}

document.addEventListener("DOMContentLoaded", () => {
  if (typeof renderContent === "function") renderContent();
  const datePicker = document.getElementById("datePicker");
  if (datePicker) datePicker.value = selectedDate;
  loadAppointments();
});
