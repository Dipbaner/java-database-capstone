function setRole(role) {
  localStorage.setItem("userRole", role);
}

function getRole() {
  return localStorage.getItem("userRole");
}

function selectRole(role) {
  setRole(role);
  const token = localStorage.getItem("token");

  if (role === "admin" && token) {
    window.location.href = `/adminDashboard/${token}`;
  } else if (role === "doctor" && token) {
    window.location.href = `/doctorDashboard/${token}`;
  } else if (role === "patient") {
    window.location.href = "/pages/patientDashboard.html";
  } else if (role === "loggedPatient") {
    window.location.href = "/pages/loggedPatientDashboard.html";
  }
}

// Called by dashboard pages on load: no role in storage means not logged in
function renderContent() {
  if (!getRole()) {
    window.location.href = "/";
  }
}