package com.tournamentmanager.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamLinkRequest {
    private Long id;
    @NotBlank(message = "Link type cannot be empty")
    private String type;
    @NotBlank(message = "Link URL cannot be empty")
    @URL(message = "Invalid URL format")
    @Size(max = 2048, message = "Link URL cannot exceed 2048 characters")
    private String url;
    @Size(max = 255, message = "Platform username cannot exceed 255 characters")
    private String platformUsername;
}