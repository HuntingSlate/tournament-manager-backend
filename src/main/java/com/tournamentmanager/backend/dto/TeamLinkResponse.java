package com.tournamentmanager.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamLinkResponse {
    private Long id;
    private String type;
    private String url;
    private String platformUsername;
    private Long teamId;
}