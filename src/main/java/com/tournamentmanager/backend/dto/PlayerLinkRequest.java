package com.tournamentmanager.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerLinkRequest {

    @NotBlank(message = "Link URL cannot be empty")
    @URL(message = "Invalid URL format")
    private String url;

    @NotBlank(message = "Link type cannot be empty")
    private String type;

    private String platformUsername;
}