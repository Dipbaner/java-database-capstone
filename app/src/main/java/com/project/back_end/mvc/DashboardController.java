package com.project.back_end.mvc;

import com.project.back_end.services.Service;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@Controller
public class DashboardController {

    private final Service service;

    public DashboardController(Service service) {
        this.service = service;
    }

    // GET /adminDashboard/{token}
    @GetMapping("/adminDashboard/{token}")
    public String adminDashboard(@PathVariable String token) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "admin");
        if (validation.getBody().isEmpty()) {
            return "admin/adminDashboard";   // templates/admin/adminDashboard.html
        }
        return "redirect:/";
    }

    // GET /doctorDashboard/{token}
    @GetMapping("/doctorDashboard/{token}")
    public String doctorDashboard(@PathVariable String token) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "doctor");
        if (validation.getBody().isEmpty()) {
            return "doctor/doctorDashboard"; // templates/doctor/doctorDashboard.html
        }
        return "redirect:/";
    }
}