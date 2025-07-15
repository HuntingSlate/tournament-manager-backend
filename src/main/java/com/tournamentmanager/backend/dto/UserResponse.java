package com.tournamentmanager.backend.dto;

import com.tournamentmanager.backend.model.Roles;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String nickname;
    private String fullName;
    private List<PlayerLinkResponse> links;
    private Roles role;
}