package com.tournamentmanager.backend.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateRequest {

    @Size(max = 50, message = "Full name cannot exceed 50 characters")
    private String fullName;

    @Size(min = 3, max = 20, message = "Nickname must be between 3 and 20 characters")
    private String nickname;
}