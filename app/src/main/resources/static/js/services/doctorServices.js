import { API_BASE_URL } from "../config/config.js";

const DOCTOR_API = API_BASE_URL + "/doctor";

// Blank filters travel as the literal "null", which Service.filterDoctor treats as "no filter"
const seg = (value) => encodeURIComponent(value || "null");

export async function getDoctors() {
  try {
    const response = await fetch(DOCTOR_API);
    const data = await response.json();
    return data.doctors || [];
  } catch (error) {
    console.error("Error fetching doctors:", error);
    return [];
  }
}

export async function deleteDoctor(id, token) {
  try {
    const response = await fetch(`${DOCTOR_API}/${id}/${token}`, { method: "DELETE" });
    const data = await response.json().catch(() => ({}));
    return { success: response.ok, message: data.message || (response.ok ? "Doctor deleted" : "Failed to delete doctor") };
  } catch (error) {
    console.error("Error deleting doctor:", error);
    return { success: false, message: "Something went wrong while deleting the doctor." };
  }
}

export async function saveDoctor(doctor, token) {
  try {
    const response = await fetch(`${DOCTOR_API}/${token}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(doctor),
    });
    const data = await response.json().catch(() => ({}));
    return { success: response.ok, message: data.message || (response.ok ? "Doctor added" : "Failed to add doctor. Check the fields and try again.") };
  } catch (error) {
    console.error("Error saving doctor:", error);
    return { success: false, message: "Something went wrong while saving the doctor." };
  }
}

export async function filterDoctors(name, time, specialty) {
  try {
    const response = await fetch(`${DOCTOR_API}/filter/${seg(name)}/${seg(time)}/${seg(specialty)}`);
    if (response.ok) {
      return await response.json();
    }
    console.error("Filter request failed:", response.status);
    return { doctors: [] };
  } catch (error) {
    console.error("Error filtering doctors:", error);
    alert("Something went wrong while filtering doctors.");
    return { doctors: [] };
  }
}