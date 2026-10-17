// doctorCard.js
import { showBookingOverlay } from "../loggedPatient.js";
import { deleteDoctor } from "../services/doctorServices.js";
import { getPatientData } from "../services/patientServices.js";

/** Builds the DOM element for a single doctor card; actions depend on the current role. */
export function createDoctorCard(doctor) {
  const card = document.createElement("div");
  card.classList.add("doctor-card");

  const role = localStorage.getItem("userRole");

  // --- doctor info -------------------------------------------------------
  const infoDiv = document.createElement("div");
  infoDiv.classList.add("doctor-info");

  const name = document.createElement("h3");
  name.textContent = doctor.name;

  const specialization = document.createElement("p");
  specialization.textContent = "Specialty: " + doctor.specialty;

  const email = document.createElement("p");
  email.textContent = "Email: " + doctor.email;

  const availability = document.createElement("p");
  availability.textContent =
    "Available: " + (doctor.availableTimes ? doctor.availableTimes.join(", ") : "N/A");

  infoDiv.appendChild(name);
  infoDiv.appendChild(specialization);
  infoDiv.appendChild(email);
  infoDiv.appendChild(availability);

  // --- action buttons ----------------------------------------------------
  const actionsDiv = document.createElement("div");
  actionsDiv.classList.add("card-actions");

  if (role === "admin") {
    const removeBtn = document.createElement("button");
    removeBtn.textContent = "Delete";
    removeBtn.addEventListener("click", async () => {
      if (!confirm(`Delete Dr. ${doctor.name}? This also removes their appointments.`)) return;

      const token = localStorage.getItem("token");
      const { success, message } = await deleteDoctor(doctor.id, token);
      alert(message);
      if (success) {
        card.remove();
      }
    });
    actionsDiv.appendChild(removeBtn);

  } else if (role === "patient") {
    // visitor who has not logged in yet
    const bookNow = document.createElement("button");
    bookNow.textContent = "Book Now";
    bookNow.addEventListener("click", () => {
      alert("Please log in or sign up as a patient before booking an appointment.");
    });
    actionsDiv.appendChild(bookNow);

  } else if (role === "loggedPatient") {
    const bookNow = document.createElement("button");
    bookNow.textContent = "Book Now";
    bookNow.addEventListener("click", async (e) => {
      const token = localStorage.getItem("token");
      if (!token) {
        alert("Session expired. Please log in again.");
        window.location.href = "/";
        return;
      }

      const patientData = await getPatientData(token);
      if (!patientData) {
        alert("Could not load your details. Please log in again.");
        return;
      }
      showBookingOverlay(e, doctor, patientData);
    });
    actionsDiv.appendChild(bookNow);
  }

  card.appendChild(infoDiv);
  card.appendChild(actionsDiv);
  return card;
}
