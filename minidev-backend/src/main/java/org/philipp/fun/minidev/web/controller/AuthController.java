package org.philipp.fun.minidev.web.controller;

import org.philipp.fun.minidev.web.objects.AuthResponse;
import org.philipp.fun.minidev.web.objects.LoginRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {


    //TODO Real Login with DB and JWT
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        if ("admin".equals(request.username()) && "password".equals(request.password())) {
            return ResponseEntity.ok(new AuthResponse(
                    UUID.randomUUID().toString(),
                    "Admin",
                    "ADMIN",
                    "fake-jwt-token-admin"
            ));
        } else if ("user".equals(request.username()) && "password".equals(request.password())) {
            return ResponseEntity.ok(new AuthResponse(
                    UUID.randomUUID().toString(),
                    "Standard User",
                    "USER",
                    "fake-jwt-token-user"
            ));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
