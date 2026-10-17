// adminDashboard.js
// Admin dashboard: lists doctors, filters them, and adds new doctors through a modal form.
import { openModal } from "./components/modals.js";
import { getDoctors, filterDoctors, saveDoctor } from "./services/doctorServices.js";
import { createDoctorCard } from "./components/doctorCard.js";

// "Add Doctor" button (rendered by header.js) opens the modal
document.addEventListener("click", (event) => {
  if (event.target && event.target.id === "addDocBtn") {
    openModal("addDoctor");
  }
});

// Load all doctors once the DOM is ready
document.addEventListener("DOMContentLoaded", () => {
  loadDoctorCards();
});

async function loadDoctorCards() {
  try {
    const doctors = await getDoctors();
    renderDoctorCards(doctors);
  } catch (error) {
    console.error("Failed to load doctors:", error);
  }
}

// Search + filter listeners
document.getElementById("searchBar")?.addEventListener("input", filterDoctorsOnChange);
document.getElementById("filterTime")?.addEventListener("change", filterDoctorsOnChange);
document.getElementById("filterSpecialty")?.addEventListener("change", filterDoctorsOnChange);

async function filterDoctorsOnChange() {
  const searchBar = document.getElementById("searchBar").value.trim();
  const filterTime = document.getElementById("filterTime").value;
  const filterSpecialty = document.getElementById("filterSpecialty").value;

  const name = searchBar.length > 0 ? searchBar : null;
  const time = filterTime.length > 0 ? filterTime : null;
  const specialty = filterSpecialty.length > 0 ? filterSpecialty : null;

  try {
    const response = await filterDoctors(name, time, specialty);
    const doctors = response.doctors || [];

    if (doctors.length > 0) {
      renderDoctorCards(doctors);
    } else {
      document.getElementById("content").innerHTML =
        "<p>No doctors found with the given filters.</p>";
    }
  } catch (error) {
    console.error("Failed to filter doctors:", error);
    alert("❌ An error occurred while filtering doctors.");
  }
}

export function renderDoctorCards(doctors) {
  const contentDiv = document.getElementById("content");
  contentDiv.innerHTML = "";

  doctors.forEach((doctor) => {
    contentDiv.appendChild(createDoctorCard(doctor));
  });
}

// Called by the "Save" button of the Add Doctor modal (wired in modals.js)
window.adminAddDoctor = async function () {
  const name = document.getElementById("doctorName").value;
  const specialty = document.getElementById("specialization").value;
  const email = document.getElementById("doctorEmail").value;
  const password = document.getElementById("doctorPassword").value;
  const phone = document.getElementById("doctorPhone").value;
  const availableTimes = Array.from(
    document.querySelectorAll('input[name="availability"]:checked')
  ).map((checkbox) => checkbox.value);

  const token = localStorage.getItem("token");
  if (!token) {
    alert("Session expired. Please log in again.");
    return;
  }

  const doctor = { name, specialty, email, password, phone, availableTimes };
  const { success, message } = await saveDoctor(doctor, token);

  if (success) {
    alert(message);
    document.getElementById("modal").style.display = "none";
    window.location.reload();
  } else {
    alert("❌ " + message);
  }
};
