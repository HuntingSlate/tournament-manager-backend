package com.tournamentmanager.backend.controller;

import com.tournamentmanager.backend.dto.ApplicationStatusRequest;
import com.tournamentmanager.backend.dto.TeamApplicationResponse;
import com.tournamentmanager.backend.dto.TournamentRequest;
import com.tournamentmanager.backend.dto.TournamentResponse;
import com.tournamentmanager.backend.model.TournamentStatus;
import com.tournamentmanager.backend.security.CustomUserDetails;
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

    public TournamentController(TournamentService tournamentService, UserService userService) {
        this.tournamentService = tournamentService;
        this.userService = userService;
    }

    @PostMapping
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
    public ResponseEntity<TournamentResponse> updateTournament(@PathVariable Long id,
                                                               @Valid @RequestBody TournamentRequest tournamentRequest,
                                                               @AuthenticationPrincipal UserDetails currentUser) {
        Long currentUserId = getUserIdFromUserDetails(currentUser);
        TournamentResponse response = tournamentService.updateTournament(id, tournamentRequest, currentUserId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTournament(@PathVariable Long id,
                                                 @AuthenticationPrincipal UserDetails currentUser) {
        Long currentUserId = getUserIdFromUserDetails(currentUser);
        tournamentService.deleteTournament(id, currentUserId);
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
    public ResponseEntity<List<TeamApplicationResponse>> getTournamentApplications(
            @PathVariable Long tournamentId,
            @AuthenticationPrincipal UserDetails currentUser) {
        Long organizerId = userService.getUserIdByEmail(currentUser.getUsername());
        List<TeamApplicationResponse> response = tournamentService.getTournamentApplications(tournamentId, organizerId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{tournamentId}/applications/{applicationId}/status")
    public ResponseEntity<TeamApplicationResponse> updateApplicationStatus(
            @PathVariable Long tournamentId,
            @PathVariable Long applicationId,
            @Valid @RequestBody ApplicationStatusRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        Long organizerId = userService.getUserIdByEmail(currentUser.getUsername());
        TeamApplicationResponse response = tournamentService.updateApplicationStatus(
                tournamentId, applicationId, request.getAccepted(), organizerId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TournamentResponse> changeTournamentStatus(@PathVariable Long id,
                                                                     @RequestParam TournamentStatus newStatus) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = ((CustomUserDetails) authentication.getPrincipal()).getId();

        TournamentResponse updatedTournament = tournamentService.changeTournamentStatus(id, newStatus, currentUserId);
        return ResponseEntity.ok(updatedTournament);
    }

    @PatchMapping("/{tournamentId}/applications/{applicationId}/withdraw")
    public ResponseEntity<TeamApplicationResponse> withdrawTeamApplication(@PathVariable Long tournamentId,
                                                                           @PathVariable Long applicationId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = ((CustomUserDetails) authentication.getPrincipal()).getId();

        TeamApplicationResponse updatedApplication = tournamentService.withdrawTeamApplication(tournamentId, applicationId, currentUserId);
        return ResponseEntity.ok(updatedApplication);
    }

    @PreAuthorize("hasRole('ADMIN') or @tournamentService.isOrganizer(#tournamentId, authentication.principal.id)")
    @DeleteMapping("/{tournamentId}/teams/{teamId}/remove")
    public ResponseEntity<TournamentResponse> removeTeamFromTournament(@PathVariable Long tournamentId,
                                                                       @PathVariable Long teamId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = ((CustomUserDetails) authentication.getPrincipal()).getId();

        TournamentResponse updatedTournament = tournamentService.removeTeamFromTournament(tournamentId, teamId, currentUserId);
        return ResponseEntity.ok(updatedTournament);
    }

    private Long getUserIdFromUserDetails(UserDetails currentUser) {
        return userService.getUserIdByEmail(currentUser.getUsername());
    }
}