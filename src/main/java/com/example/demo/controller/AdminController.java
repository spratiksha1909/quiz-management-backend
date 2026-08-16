package com.example.demo.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.Entity.User;
import com.example.demo.Repository.QuizAttemptRepository;
import com.example.demo.Repository.QuizRepository;
import com.example.demo.Repository.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminController {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private QuizRepository quizRepository;
    @Autowired
    private QuizAttemptRepository quizAttemptRepository;

    @GetMapping("/stats")
    public ResponseEntity<?> getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalStudents = userRepository.findAll().stream()
                .filter(u -> u.getRole().name().equals("STUDENT")).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("totalStudents", totalStudents);
        stats.put("activeQuizzes", 5); // Dummy or Repository count

        return ResponseEntity.ok(stats);
    }

    // 2. Fetch All Users List
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    // 3. Toggle User Active Status (Activate / Deactivate)
    @PutMapping("/users/{id}/toggle-status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long id) {
        return userRepository.findById(id).map(user -> {
            user.setActive(!user.isActive());
            userRepository.save(user);
            return ResponseEntity.ok("User status updated to: " + user.isActive());
        }).orElse(ResponseEntity.notFound().build());
    }
 // 📊 Admin Analytics API Endpoint
    @GetMapping("/analytics")
    public ResponseEntity<?> getAdminAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        
        long totalStudents = userRepository.count();
        long totalQuizzes = quizRepository.count();
        long totalAttempts = quizAttemptRepository.count();
        
        analytics.put("totalStudents", totalStudents);
        analytics.put("totalQuizzes", totalQuizzes);
        analytics.put("totalAttempts", totalAttempts);

        return ResponseEntity.ok(analytics);
    }
}