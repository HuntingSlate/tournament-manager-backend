package com.tournamentmanager.backend.controller;

import com.tournamentmanager.backend.dto.MatchRequest;
import com.tournamentmanager.backend.dto.MatchResponse;
import com.tournamentmanager.backend.dto.MatchStatisticsRequest;
import com.tournamentmanager.backend.model.Match;
import com.tournamentmanager.backend.security.CustomUserDetails;
import com.tournamentmanager.backend.service.MatchService;
import com.tournamentmanager.backend.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MatchController {

    private final MatchService matchService;
    private final UserService userService;

    public MatchController(MatchService matchService, UserService userService) {
        this.matchService = matchService;
        this.userService = userService;
    }


    @PostMapping("/matches")
    public ResponseEntity<MatchResponse> createMatch(@Valid @RequestBody MatchRequest request,
                                                     @AuthenticationPrincipal UserDetails currentUser) {

        Long currentUserId = getUserIdFromUserDetails(currentUser);
        Match createdMatch = matchService.createMatch(request, currentUserId);
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

    @PutMapping("/matches/{id}")
    public ResponseEntity<MatchResponse> updateMatch(@PathVariable Long id, @Valid @RequestBody MatchRequest request,
                                                     @AuthenticationPrincipal UserDetails currentUser) {

        Long currentUserId = getUserIdFromUserDetails(currentUser);
        MatchResponse response = matchService.updateMatch(id, request, currentUserId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/matches/{id}/record-result")
    public ResponseEntity<MatchResponse> recordMatchResult(@PathVariable Long id,
                                                           @RequestParam @NotNull Integer scoreTeam1,
                                                           @RequestParam @NotNull Integer scoreTeam2,
                                                           @AuthenticationPrincipal UserDetails currentUser) {

        Long currentUserId = getUserIdFromUserDetails(currentUser);
        MatchResponse response = matchService.recordMatchResult(id, scoreTeam1, scoreTeam2, currentUserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/matches/{id}/statistics")
    public ResponseEntity<MatchResponse> saveMatchStatistics(@PathVariable Long id,
                                                             @Valid @RequestBody List<MatchStatisticsRequest> statisticsRequests,
                                                             @AuthenticationPrincipal UserDetails currentUser) {

        Long currentUserId = getUserIdFromUserDetails(currentUser);
        MatchResponse response = matchService.saveMatchStatistics(id, statisticsRequests, currentUserId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/matches/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMatch(@PathVariable Long id,
                            @AuthenticationPrincipal UserDetails currentUser) {

        Long currentUserId = getUserIdFromUserDetails(currentUser);
        matchService.deleteMatch(id, currentUserId);
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

    private Long getUserIdFromUserDetails(UserDetails currentUser) {
        return userService.getUserIdByEmail(currentUser.getUsername());
    }
}