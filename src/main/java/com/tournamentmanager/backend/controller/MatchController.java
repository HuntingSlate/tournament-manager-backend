package com.tournamentmanager.backend.controller;

import com.tournamentmanager.backend.dto.MatchRequest;
import com.tournamentmanager.backend.dto.MatchResponse;
import com.tournamentmanager.backend.dto.MatchStatisticsRequest;
import com.tournamentmanager.backend.model.Match;
import com.tournamentmanager.backend.security.CustomUserDetails;
import com.tournamentmanager.backend.service.MatchService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @tournamentService.isOrganizer(#request.getTournamentId(), authentication.principal.id)")
    @PostMapping("/matches")
    public ResponseEntity<MatchResponse> createMatch(@Valid @RequestBody MatchRequest request) {
        Match createdMatch = matchService.createMatch(request);
        MatchResponse response = matchService.mapToMatchResponse(createdMatch);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/matches/{id}")
    public ResponseEntity<MatchResponse> getMatchById(@PathVariable Long id) {
        MatchResponse response = matchService.getMatchById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tournaments/{tournamentId}/matches")
    public ResponseEntity<List<MatchResponse>> getMatchesByTournament(@PathVariable Long tournamentId) {
        List<MatchResponse> responses = matchService.getMatchesByTournament(tournamentId);
        return ResponseEntity.ok(responses);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @tournamentService.isOrganizer(#id, authentication.principal.id)")
    @PutMapping("/matches/{id}")
    public ResponseEntity<MatchResponse> updateMatch(@PathVariable Long id, @Valid @RequestBody MatchRequest request) {
        MatchResponse response = matchService.updateMatch(id, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @tournamentService.isOrganizer(#id, authentication.principal.id)")
    @PatchMapping("/matches/{id}/record-result")
    public ResponseEntity<MatchResponse> recordMatchResult(@PathVariable Long id,
                                                           @RequestParam @NotNull Integer scoreTeam1,
                                                           @RequestParam @NotNull Integer scoreTeam2) {
        MatchResponse response = matchService.recordMatchResult(id, scoreTeam1, scoreTeam2);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @tournamentService.isOrganizer(#id, authentication.principal.id)")
    @PostMapping("/matches/{id}/statistics")
    public ResponseEntity<MatchResponse> saveMatchStatistics(@PathVariable Long id,
                                                             @Valid @RequestBody List<MatchStatisticsRequest> statisticsRequests) {
        MatchResponse response = matchService.saveMatchStatistics(id, statisticsRequests);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @tournamentService.isOrganizer(#id, authentication.principal.id)")
    @DeleteMapping("/matches/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMatch(@PathVariable Long id) {
        matchService.deleteMatch(id);
    }

    @GetMapping("/matches/search")
    public ResponseEntity<List<MatchResponse>> searchMatches(
            @RequestParam(required = false) Long tournamentId,
            @RequestParam(required = false) Long gameId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Long playerId,
            @RequestParam(required = false) String tournamentName,
            @RequestParam(required = false) String gameName,
            @RequestParam(required = false) String teamName,
            @RequestParam(required = false) String playerName) {
        List<MatchResponse> matches = matchService.searchMatches(tournamentName,
                gameName, teamName, playerName, tournamentId, gameId, teamId, playerId);
        return ResponseEntity.ok(matches);
    }
}