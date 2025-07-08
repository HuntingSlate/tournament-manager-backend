package com.tournamentmanager.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberResponse {
    private Long userId;
    private String userNickname;
    private LocalDate startDate;
    private LocalDate endDate;
}