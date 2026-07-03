package com.gateway.api_gateway.controller;

import com.gateway.api_gateway.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;

    @PostMapping("/token")
    public ResponseEntity<Map<String, String>> getToken(@RequestBody LoginRequest request) {
        if ("admin".equals(request.username()) && "password".equals(request.password())) {
            String token = jwtUtil.generateToken(request.username(), "ADMIN");
            return ResponseEntity.ok(Map.of("token", token));
        }
        if ("user".equals(request.username()) && "password".equals(request.password())) {
            String token = jwtUtil.generateToken(request.username(), "USER");
            return ResponseEntity.ok(Map.of("token", token));
        }
        return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
    }
}
