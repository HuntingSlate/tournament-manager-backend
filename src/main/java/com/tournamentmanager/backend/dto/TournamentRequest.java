package com.tournamentmanager.backend.dto;

import com.tournamentmanager.backend.model.Tournament;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
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
    private Integer buildingNumber;
    private Double latitude;
    private Double longitude;

    @NotNull(message = "Max teams cannot be null")
    @Min(value = 2, message = "Tournament must have at least 2 teams")
    private Integer maxTeams;

    private Tournament.TournamentStatus status;
}