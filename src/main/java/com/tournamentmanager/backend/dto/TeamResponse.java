package com.tournamentmanager.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamResponse {

    private Long id;
    private String name;
    private String gameName;
    private Long leaderId;
    private String leaderNickname;

    private List<TeamMemberResponse> teamMembers;
    private List<LinkResponse> links;
}