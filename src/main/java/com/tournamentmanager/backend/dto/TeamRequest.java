package com.tournamentmanager.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamRequest {

    @NotBlank(message = "Team name cannot be empty")
    @Size(max = 255, message = "Team name cannot exceed 255 characters")
    private String name;

    @NotNull(message = "Game ID cannot be null")
    private Long gameId;
}