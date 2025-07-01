package com.tournamentmanager.backend.service;

import com.tournamentmanager.backend.dto.LoginRequest;
import com.tournamentmanager.backend.dto.RegisterRequest;
import com.tournamentmanager.backend.dto.AuthResponse;
import com.tournamentmanager.backend.model.User;
import com.tournamentmanager.backend.repository.UserRepository;
import com.tournamentmanager.backend.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public AuthResponse register(RegisterRequest registerRequest) {
        if (userRepository.findByEmail(registerRequest.getEmail()) != null) {
            throw new RuntimeException("Email is already taken!");
        }
        if (userRepository.findByNickname(registerRequest.getNickname()) != null) {
            throw new RuntimeException("Nickname is already taken!");
        }

        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setNickname(registerRequest.getNickname());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

        userRepository.save(user);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        registerRequest.getEmail(),
                        registerRequest.getPassword()
                )
        );
        String token = jwtTokenProvider.generateToken(authentication);

        return new AuthResponse(token, user.getId(), user.getEmail(), user.getNickname());
    }

    public AuthResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtTokenProvider.generateToken(authentication);
        User user = userRepository.findByEmail(loginRequest.getEmail());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getNickname());
    }
}