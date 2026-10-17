// doctorServices.js
import { API_BASE_URL } from "../config/config.js";

const DOCTOR_API = API_BASE_URL + "/doctor";

/** Fetch every doctor. Returns the 'doctors' array (or [] on error). */
export async function getDoctors() {
  try {
    const response = await fetch(DOCTOR_API);
    const data = await response.json();
    return data.doctors || [];
  } catch (error) {
    console.error("Error :: getDoctors ::", error);
    return [];
  }
}

/** Delete a doctor by id (admin token required). */
export async function deleteDoctor(id, token) {
  try {
    const response = await fetch(`${DOCTOR_API}/${id}/${token}`, {
      method: "DELETE"
    });
    const data = await response.json();
    return {
      success: response.ok,
      message: data.message || (response.ok ? "Doctor deleted" : "Failed to delete doctor")
    };
  } catch (error) {
    console.error("Error :: deleteDoctor ::", error);
    return { success: false, message: "Network error. Please try again later." };
  }
}

/** Create a doctor (admin token required). */
export async function saveDoctor(doctor, token) {
  try {
    const response = await fetch(`${DOCTOR_API}/${token}`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(doctor)
    });
    const data = await response.json();
    return {
      success: response.ok,
      message: data.message || (response.ok ? "Doctor saved" : "Failed to save doctor")
    };
  } catch (error) {
    console.error("Error :: saveDoctor ::", error);
    return { success: false, message: "Network error. Please try again later." };
  }
}

/**
 * Filter doctors by name, time (AM/PM) and specialty.
 * Unused filters are sent as the literal string "null", which the backend treats as "no filter".
 */
export async function filterDoctors(name, time, specialty) {
  try {
    const n = name || "null";
    const t = time || "null";
    const s = specialty || "null";
    const response = await fetch(
      `${DOCTOR_API}/filter/${encodeURIComponent(n)}/${encodeURIComponent(t)}/${encodeURIComponent(s)}`
    );

    if (response.ok) {
      return await response.json();
    }
    console.error("Failed to filter doctors:", response.statusText);
    return { doctors: [] };
  } catch (error) {
    console.error("Error :: filterDoctors ::", error);
    alert("Something went wrong while filtering doctors!");
    return { doctors: [] };
  }
}
