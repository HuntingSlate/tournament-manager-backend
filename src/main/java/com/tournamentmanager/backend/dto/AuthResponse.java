package com.tournamentmanager.backend.dto;

import com.tournamentmanager.backend.model.Roles;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Long userId;
    private String email;
    private String nickname;
    private Roles role;
}