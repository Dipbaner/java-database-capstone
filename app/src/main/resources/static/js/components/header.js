// header.js
function renderHeader() {
  const headerDiv = document.getElementById("header");
  if (!headerDiv) return;

  // Root page: clear any session role and show only the logo
  if (window.location.pathname === "/" || window.location.pathname.endsWith("/index.html")) {
    localStorage.removeItem("userRole");
    headerDiv.innerHTML = `
      <header class="header">
        <div class="logo-section">
          <img src="../assets/images/logo/logo.png" alt="Hospital CRM Logo" class="logo-img">
          <span class="logo-title">Hospital CMS</span>
        </div>
      </header>`;
    return;
  }

  const role = localStorage.getItem("userRole");
  const token = localStorage.getItem("token");

  let headerContent = `
    <header class="header">
      <div class="logo-section">
        <img src="../assets/images/logo/logo.png" alt="Hospital CRM Logo" class="logo-img">
        <span class="logo-title">Hospital CMS</span>
      </div>
      <nav>`;

  // A role that needs a token but has none means the session is gone
  if ((role === "loggedPatient" || role === "admin" || role === "doctor") && !token) {
    localStorage.removeItem("userRole");
    alert("Session expired or invalid login. Please log in again.");
    window.location.href = "/";
    return;
  }

  if (role === "admin") {
    headerContent += `
      <button id="addDocBtn" class="adminBtn">Add Doctor</button>
      <a href="#" id="logoutLink">Logout</a>`;
  } else if (role === "doctor") {
    headerContent += `
      <button class="adminBtn" id="doctorHomeBtn">Home</button>
      <a href="#" id="logoutLink">Logout</a>`;
  } else if (role === "patient") {
    headerContent += `
      <button id="patientLogin" class="adminBtn">Login</button>
      <button id="patientSignup" class="adminBtn">Sign Up</button>`;
  } else if (role === "loggedPatient") {
    headerContent += `
      <button id="home" class="adminBtn" onclick="window.location.href='/pages/loggedPatientDashboard.html'">Home</button>
      <button id="patientAppointments" class="adminBtn" onclick="window.location.href='/pages/patientAppointments.html'">Appointments</button>
      <a href="#" id="logoutPatientLink">Logout</a>`;
  }

  headerContent += `
      </nav>
    </header>`;

  headerDiv.innerHTML = headerContent;
  attachHeaderButtonListeners();
}

function attachHeaderButtonListeners() {
  const logoutLink = document.getElementById("logoutLink");
  if (logoutLink) {
    logoutLink.addEventListener("click", (e) => {
      e.preventDefault();
      logout();
    });
  }

  const logoutPatientLink = document.getElementById("logoutPatientLink");
  if (logoutPatientLink) {
    logoutPatientLink.addEventListener("click", (e) => {
      e.preventDefault();
      logoutPatient();
    });
  }

  const doctorHomeBtn = document.getElementById("doctorHomeBtn");
  if (doctorHomeBtn) {
    doctorHomeBtn.addEventListener("click", () => selectRole("doctor"));
  }
}

function logout() {
  localStorage.removeItem("token");
  localStorage.removeItem("userRole");
  window.location.href = "/";
}

function logoutPatient() {
  localStorage.removeItem("token");
  localStorage.setItem("userRole", "patient");
  window.location.href = "/pages/patientDashboard.html";
}

renderHeader();
