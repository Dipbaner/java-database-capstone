package com.project.back_end.services;

import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class TokenService {

    private static final long TOKEN_VALIDITY_MS = 7L * 24 * 60 * 60 * 1000; // 7 days

    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    public TokenService(AdminRepository adminRepository,
                        DoctorRepository doctorRepository,
                        PatientRepository patientRepository) {
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    /** Builds the HMAC-SHA signing key from the configured secret. */
    public SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /** Generates a JWT (valid for 7 days) whose subject is the given identifier (email / username). */
    public String generateToken(String identifier) {
        Date now = new Date();
        return Jwts.builder()
                .subject(identifier)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + TOKEN_VALIDITY_MS))
                .signWith(getSigningKey())
                .compact();
    }

    /** Verifies the token signature and returns its subject. */
    public String extractEmail(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Checks that the token is genuine, not expired, and belongs to an existing user of the given role
     * ("admin", "doctor" or "patient").
     */
    public boolean validateToken(String token, String user) {
        try {
            String subject = extractEmail(token);
            if (user == null) {
                return false;
            }
            switch (user.toLowerCase()) {
                case "admin":
                    return adminRepository.findByUsername(subject) != null;
                case "doctor":
                    return doctorRepository.findByEmail(subject) != null;
                case "patient":
                    return patientRepository.findByEmail(subject) != null;
                default:
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
