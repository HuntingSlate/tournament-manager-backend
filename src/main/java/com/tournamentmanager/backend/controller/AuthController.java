package com.tournamentmanager.backend.controller;

import com.tournamentmanager.backend.dto.LoginRequest;
import com.tournamentmanager.backend.dto.RegisterRequest;
import com.tournamentmanager.backend.dto.AuthResponse;
import com.tournamentmanager.backend.exception.ResourceNotFoundException;
import com.tournamentmanager.backend.model.User;
import com.tournamentmanager.backend.repository.UserRepository;
import com.tournamentmanager.backend.service.AuthService;
import com.tournamentmanager.backend.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserService userService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        AuthResponse response = authService.register(registerRequest);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse authResponse = authService.login(loginRequest);
        return new ResponseEntity<>(authResponse, HttpStatus.OK);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount(@AuthenticationPrincipal UserDetails currentUser) {
        Long currentUserId = getUserIdFromUserDetails(currentUser);
        authService.deactivateAccount(currentUserId);
        return ResponseEntity.noContent().build();
    }

    private Long getUserIdFromUserDetails(UserDetails userDetails) {
        String userEmail = userDetails.getUsername();

        return userRepository.findByEmail(userEmail)
                .map(User::getId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
    }
}