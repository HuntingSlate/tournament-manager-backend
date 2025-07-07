package com.tournamentmanager.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TournamentRequest {

    @NotBlank(message = "Tournament name cannot be empty")
    @Size(max = 255, message = "Tournament name cannot exceed 255 characters")
    private String name;

    @Size(max = 500, message = "Tournament description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Start date cannot be null")
    private LocalDate startDate;

    private LocalDate endDate;

    @NotNull(message = "Game ID cannot be null")
    private Long gameId;


    private String postalCode;
    private String city;
    private String street;
    private Integer number;
}