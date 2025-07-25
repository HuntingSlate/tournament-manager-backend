package com.tournamentmanager.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationStatusRequest {
    @NotNull(message = "Status cannot be null")
    private Boolean accepted;
}