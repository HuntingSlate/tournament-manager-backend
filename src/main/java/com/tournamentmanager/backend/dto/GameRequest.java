package com.tournamentmanager.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameRequest {

    @NotBlank(message = "Game name cannot be empty")
    @Size(max = 50, message = "Game name cannot exceed 50 characters")
    private String name;

}