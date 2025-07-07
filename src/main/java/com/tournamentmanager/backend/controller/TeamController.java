package com.tournamentmanager.backend.controller;

import com.tournamentmanager.backend.dto.TeamRequest;
import com.tournamentmanager.backend.dto.TeamResponse;
import com.tournamentmanager.backend.service.TeamService;
import com.tournamentmanager.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;
    private final UserService userService;

    public TeamController(TeamService teamService, UserService userService) {
        this.teamService = teamService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(@Valid @RequestBody TeamRequest teamRequest,
                                                   @AuthenticationPrincipal UserDetails currentUser) {
        Long leaderId = getUserIdFromUserDetails(currentUser);
        TeamResponse response = teamService.createTeam(teamRequest, leaderId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeamResponse> getTeamById(@PathVariable Long id) {
        TeamResponse response = teamService.getTeamById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TeamResponse> updateTeam(@PathVariable Long id,
                                                   @Valid @RequestBody TeamRequest teamRequest,
                                                   @AuthenticationPrincipal UserDetails currentUser) {
        Long currentUserId = getUserIdFromUserDetails(currentUser);
        TeamResponse response = teamService.updateTeam(id, teamRequest, currentUserId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeam(@PathVariable Long id,
                                           @AuthenticationPrincipal UserDetails currentUser) {
        Long currentUserId = getUserIdFromUserDetails(currentUser);
        teamService.deleteTeam(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{teamId}/members/{memberId}")
    public ResponseEntity<TeamResponse> addTeamMember(@PathVariable Long teamId,
                                                      @PathVariable Long memberId,
                                                      @AuthenticationPrincipal UserDetails currentUser) {
        Long currentUserId = getUserIdFromUserDetails(currentUser);
        TeamResponse response = teamService.addTeamMember(teamId, memberId, currentUserId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{teamId}/members/{memberId}")
    public ResponseEntity<TeamResponse> removeTeamMember(@PathVariable Long teamId,
                                                         @PathVariable Long memberId,
                                                         @AuthenticationPrincipal UserDetails currentUser) {
        Long currentUserId = getUserIdFromUserDetails(currentUser);
        TeamResponse response = teamService.removeTeamMember(teamId, memberId, currentUserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{teamId}/apply/{tournamentId}")
    public ResponseEntity<TeamResponse> applyToTournament(@PathVariable Long teamId,
                                                          @PathVariable Long tournamentId,
                                                          @AuthenticationPrincipal UserDetails currentUser) {
        Long currentUserId = getUserIdFromUserDetails(currentUser);
        TeamResponse response = teamService.applyToTournament(teamId, tournamentId, currentUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<List<TeamResponse>> searchTeams(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String playerName) {
        List<TeamResponse> response = teamService.searchTeams(name, playerName);
        return ResponseEntity.ok(response);
    }

    private Long getUserIdFromUserDetails(UserDetails currentUser) {
        return userService.getUserIdByEmail(currentUser.getUsername());
    }
}