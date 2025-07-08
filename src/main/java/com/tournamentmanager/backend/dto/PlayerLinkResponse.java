package com.tournamentmanager.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerLinkResponse {
    private Long id;
    private String url;
    private String type;
    private String platformUsername;
    private Long userId;
}