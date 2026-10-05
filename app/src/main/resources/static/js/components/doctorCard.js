import { deleteDoctor } from "../services/doctorServices.js";

function infoLine(label, value) {
  const p = document.createElement("p");
  const strong = document.createElement("strong");
  strong.textContent = label + " ";
  p.appendChild(strong);
  p.appendChild(document.createTextNode(value));
  return p;
}

export function createDoctorCard(doctor) {
  const card = document.createElement("div");
  card.classList.add("doctor-card");

  const role = localStorage.getItem("userRole");

  // Doctor info (textContent keeps any user-entered text from being run as HTML)
  const info = document.createElement("div");
  info.classList.add("doctor-info");

  const name = document.createElement("h3");
  name.textContent = doctor.name;

  const times = (doctor.availableTimes && doctor.availableTimes.length)
    ? doctor.availableTimes.join(", ")
    : "Not set";

  info.appendChild(name);
  info.appendChild(infoLine("Specialty:", doctor.speciality || ""));
  info.appendChild(infoLine("Email:", doctor.email || ""));
  info.appendChild(infoLine("Available:", times));

  const actions = document.createElement("div");
  actions.classList.add("card-actions");

  if (role === "admin") {
    const removeBtn = document.createElement("button");
    removeBtn.textContent = "Delete";
    removeBtn.addEventListener("click", async () => {
      if (!confirm(`Delete ${doctor.name}? This also removes their appointments.`)) return;

      const token = localStorage.getItem("token");
      if (!token) {
        alert("Session expired. Please log in again.");
        window.location.href = "/";
        return;
      }

      const result = await deleteDoctor(doctor.id, token);
      alert(result.message);
      if (result.success) card.remove();
    });
    actions.appendChild(removeBtn);
  } else if (role === "patient") {
    const bookNow = document.createElement("button");
    bookNow.textContent = "Book Now";
    bookNow.addEventListener("click", () => {
      alert("Please log in before booking an appointment.");
    });
    actions.appendChild(bookNow);
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
      try {
        // Loaded on demand; implemented in the patient pages
        const { getPatientData } = await import("../services/patientServices.js");
        const { showBookingOverlay } = await import("../loggedPatient.js");
        const patientData = await getPatientData(token);
        showBookingOverlay(e, doctor, patientData);
      } catch (error) {
        console.error("Booking overlay error:", error);
        alert("Booking isn't available yet.");
      }
    });
    actions.appendChild(bookNow);
  }

  card.appendChild(info);
  card.appendChild(actions);
  return card;
}