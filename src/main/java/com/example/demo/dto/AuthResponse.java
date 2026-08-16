package com.example.demo.dto;

public record AuthResponse(String token, Long id, String name, String email, String role) {}

