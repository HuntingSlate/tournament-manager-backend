package com.tournamentmanager.backend.controller;

import com.tournamentmanager.backend.dto.TournamentRequest;
import com.tournamentmanager.backend.dto.TournamentResponse;
import com.tournamentmanager.backend.dto.ApplicationStatusRequest;
import com.tournamentmanager.backend.dto.TeamApplicationResponse;
import com.tournamentmanager.backend.dto.MatchResponse;
import com.tournamentmanager.backend.model.Tournament;
import com.tournamentmanager.backend.model.Tournament.TournamentStatus;
import com.tournamentmanager.backend.model.User;
import com.tournamentmanager.backend.service.MatchService;
import com.tournamentmanager.backend.service.TournamentService;
import com.tournamentmanager.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tournaments")
public class TournamentController {

    private final TournamentService tournamentService;
    private final UserService userService;
    private final MatchService matchService;

    public TournamentController(TournamentService tournamentService, UserService userService, MatchService matchService) {
        this.tournamentService = tournamentService;
        this.userService = userService;
        this.matchService = matchService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<TournamentResponse> createTournament(@Valid @RequestBody TournamentRequest tournamentRequest,
                                                               @AuthenticationPrincipal UserDetails currentUser) {
        Long organizerId = getUserIdFromUserDetails(currentUser);
        TournamentResponse response = tournamentService.createTournament(tournamentRequest, organizerId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TournamentResponse> getTournamentById(@PathVariable Long id) {
        TournamentResponse response = tournamentService.getTournamentById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @tournamentService.isOrganizer(#id, authentication.principal.id)")
    public ResponseEntity<TournamentResponse> updateTournament(@PathVariable Long id,
                                                               @Valid @RequestBody TournamentRequest tournamentRequest) {
        TournamentResponse response = tournamentService.updateTournament(id, tournamentRequest);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @tournamentService.isOrganizer(#id, authentication.principal.id)")
    public ResponseEntity<Void> deleteTournament(@PathVariable Long id) {
        tournamentService.deleteTournament(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<TournamentResponse>> searchTournaments(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String organizerNickname) {
        List<TournamentResponse> response = tournamentService.searchTournaments(name, location, startDate, endDate, organizerNickname);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{tournamentId}/applications")
    @PreAuthorize("hasRole('ADMIN') or @tournamentService.isOrganizer(#tournamentId, authentication.principal.id)")
    public ResponseEntity<List<TeamApplicationResponse>> getTournamentApplications(
            @PathVariable Long tournamentId) {
        List<TeamApplicationResponse> response = tournamentService.getTournamentApplications(tournamentId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{tournamentId}/applications/{applicationId}/status")
    @PreAuthorize("hasRole('ADMIN') or @tournamentService.isOrganizer(#tournamentId, authentication.principal.id)")
    public ResponseEntity<TeamApplicationResponse> updateApplicationStatus(
            @PathVariable Long tournamentId,
            @PathVariable Long applicationId,
            @Valid @RequestBody ApplicationStatusRequest request) {
        TeamApplicationResponse response = tournamentService.updateApplicationStatus(
                tournamentId, applicationId, request.getAccepted());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or @tournamentService.isOrganizer(#id, authentication.principal.id)")
    public ResponseEntity<TournamentResponse> changeTournamentStatus(@PathVariable Long id,
                                                                     @RequestParam TournamentStatus newStatus) {
        TournamentResponse updatedTournament = tournamentService.changeTournamentStatus(id, newStatus);
        return ResponseEntity.ok(updatedTournament);
    }

    @PatchMapping("/{tournamentId}/applications/{applicationId}/withdraw")
    @PreAuthorize("@tournamentService.isTeamApplicationLeader(#applicationId, authentication.principal.id)")
    public ResponseEntity<TeamApplicationResponse> withdrawTeamApplication(@PathVariable Long tournamentId,
                                                                           @PathVariable Long applicationId) {
        TeamApplicationResponse updatedApplication = tournamentService.withdrawTeamApplication(tournamentId, applicationId);
        return ResponseEntity.ok(updatedApplication);
    }

    @DeleteMapping("/{tournamentId}/teams/{teamId}/remove")
    @PreAuthorize("hasRole('ADMIN') or @tournamentService.isOrganizer(#tournamentId, authentication.principal.id)")
    public ResponseEntity<TournamentResponse> removeTeamFromTournament(@PathVariable Long tournamentId,
                                                                       @PathVariable Long teamId) {
        TournamentResponse updatedTournament = tournamentService.removeTeamFromTournament(tournamentId, teamId);
        return ResponseEntity.ok(updatedTournament);
    }

    @PatchMapping("/{id}/start")
    @PreAuthorize("hasRole('ADMIN') or @tournamentService.isOrganizer(#id, authentication.principal.id)")
    public ResponseEntity<TournamentResponse> startTournament(@PathVariable Long id) {
        Tournament tournament = tournamentService.startTournament(id);
        matchService.generateFirstRoundMatches(tournament);

        boolean isLanTournament = tournament.getLocation() != null;
        TournamentResponse response =  tournamentService.mapToTournamentResponse(tournament, isLanTournament);
        return ResponseEntity.ok(response);
    }


    private Long getUserIdFromUserDetails(UserDetails currentUser) {
        return userService.getUserIdByEmail(currentUser.getUsername());
    }
}