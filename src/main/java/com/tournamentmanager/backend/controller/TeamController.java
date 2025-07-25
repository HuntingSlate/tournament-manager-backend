package com.tournamentmanager.backend.controller;

import com.tournamentmanager.backend.dto.*;
import com.tournamentmanager.backend.model.TeamApplication;
import com.tournamentmanager.backend.service.TeamService;
import com.tournamentmanager.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<TeamResponse> createTeam(@Valid @RequestBody TeamRequest request,
                                                   @AuthenticationPrincipal UserDetails currentUser) {
        Long leaderId = getUserIdFromUserDetails(currentUser);
        TeamResponse response = teamService.createTeam(request, leaderId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeamResponse> getTeamById(@PathVariable Long id) {
        TeamResponse response = teamService.getTeamById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TeamResponse> updateTeam(@PathVariable Long id,
                                                   @Valid @RequestBody TeamRequest request,
                                                   @AuthenticationPrincipal UserDetails currentUser) {

        Long currentUserId = getUserIdFromUserDetails(currentUser);
        TeamResponse response = teamService.updateTeam(id, request, currentUserId);
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
    public ResponseEntity<List<TeamResponse>> searchTeams(@RequestParam(required = false) String name,
                                                          @RequestParam(required = false) String playerName) {
        List<TeamResponse> response = teamService.searchTeams(name, playerName);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/{teamId}/links")
    public ResponseEntity<List<TeamLinkResponse>> getTeamLinks(@PathVariable Long teamId) {
        List<TeamLinkResponse> links = teamService.getTeamLinks(teamId);
        return ResponseEntity.ok(links);
    }

    @PostMapping("/{teamId}/links")
    public ResponseEntity<TeamLinkResponse> addTeamLink(@PathVariable Long teamId,
                                                        @Valid @RequestBody TeamLinkRequest request,
                                                        @AuthenticationPrincipal UserDetails currentUser) {

        Long currentUserId = getUserIdFromUserDetails(currentUser);
        TeamLinkResponse response = teamService.addTeamLink(teamId, request, currentUserId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{teamId}/links/{teamLinkId}")
    public ResponseEntity<TeamLinkResponse> updateTeamLink(@PathVariable Long teamId,
                                                           @PathVariable Long teamLinkId,
                                                           @Valid @RequestBody TeamLinkRequest request,
                                                           @AuthenticationPrincipal UserDetails currentUser) {

        Long currentUserId = getUserIdFromUserDetails(currentUser);
        TeamLinkResponse response = teamService.updateTeamLink(teamId, teamLinkId, request, currentUserId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{teamId}/links/{teamLinkId}")
    public ResponseEntity<Void> deleteTeamLink(@PathVariable Long teamId,
                                               @PathVariable Long teamLinkId,
                                               @AuthenticationPrincipal UserDetails currentUser) {

        Long currentUserId = getUserIdFromUserDetails(currentUser);
        teamService.deleteTeamLink(teamId, teamLinkId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{teamId}/applications")
    public ResponseEntity<List<TeamApplicationResponse>> getTeamApplications(
            @PathVariable Long teamId,
            @RequestParam(defaultValue = "PENDING") TeamApplication.ApplicationStatus status,
            @AuthenticationPrincipal UserDetails currentUser) {

        Long currentUserId = getUserIdFromUserDetails(currentUser);
        List<TeamApplicationResponse> applications = teamService.getTeamApplications(teamId, status, currentUserId);
        return ResponseEntity.ok(applications);
    }


    private Long getUserIdFromUserDetails(UserDetails currentUser) {
        return userService.getUserIdByEmail(currentUser.getUsername());
    }


}