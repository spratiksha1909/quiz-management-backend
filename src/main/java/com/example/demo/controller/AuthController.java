package com.example.demo.controller;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.example.demo.Entity.Role;
import com.example.demo.Entity.User;
import com.example.demo.Repository.UserRepository;
import com.example.demo.config.JwtProvider;
import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtProvider jwtProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            return ResponseEntity.badRequest().body("Email is already registered.");
        }

        User user = new User();
        
        
        String nameToSave = request.name() != null ? request.name() : "User";
        user.setName(nameToSave);
        
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        
        
        Role userRole = Role.STUDENT; // Default value
        if (request.role() != null && !request.role().isEmpty()) {
            try {
                userRole = Role.valueOf(request.role().toUpperCase());
            } catch (IllegalArgumentException e) {
                userRole = Role.STUDENT;
            }
        }
        user.setRole(userRole);
        user.setActive(true);

        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully as " + userRole);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            return ResponseEntity.status(401).body("Invalid email or password.");
        }

        if (!user.isActive()) {
            return ResponseEntity.status(403).body("Account is deactivated.");
        }

        String token = jwtProvider.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        return ResponseEntity.ok(new AuthResponse(
                token, 
                user.getId(), 
                user.getName(), 
                user.getEmail(), 
                user.getRole().name()
        ));
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        User user = userRepository.findByEmail(email).orElse(null);
        
        if (user == null) {
            return ResponseEntity.badRequest().body("User with this email does not exist.");
        }

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setTokenExpiry(LocalDateTime.now().plusMinutes(15)); // 15 Mins Expiry
        userRepository.save(user);

        // ईमेल पाठवण्याचा कोड (उदा. simpleMailMessage.setText("Reset Token: " + token))
        
        return ResponseEntity.ok(Map.of("message", "Reset token generated!", "token", token));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");

        User user = userRepository.findByResetToken(token).orElse(null);
        
        if (user == null || user.getTokenExpiry() == null || user.getTokenExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Invalid or expired token.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setTokenExpiry(null);
        userRepository.save(user);

        return ResponseEntity.ok("Password reset successfully!");
    }
}