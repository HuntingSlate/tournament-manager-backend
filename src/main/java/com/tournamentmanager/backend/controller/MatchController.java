package com.tournamentmanager.backend.controller;

import com.tournamentmanager.backend.dto.MatchRequest;
import com.tournamentmanager.backend.dto.MatchResponse;
import com.tournamentmanager.backend.dto.MatchStatisticsRequest;
import com.tournamentmanager.backend.service.MatchService;
import com.tournamentmanager.backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;
    private final UserService userService;

    public MatchController(MatchService matchService, UserService userService) {
        this.matchService = matchService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<MatchResponse> createMatch(@Valid @RequestBody MatchRequest matchRequest,
                                                     @AuthenticationPrincipal UserDetails currentUser) {
        Long currentUserId = userService.getUserIdByEmail(currentUser.getUsername());
        MatchResponse response = matchService.createMatch(matchRequest, currentUserId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchResponse> getMatchById(@PathVariable Long id) {
        MatchResponse response = matchService.getMatchById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MatchResponse> updateMatch(@PathVariable Long id,
                                                     @Valid @RequestBody MatchRequest matchRequest,
                                                     @AuthenticationPrincipal UserDetails currentUser) {
        Long currentUserId = userService.getUserIdByEmail(currentUser.getUsername());
        MatchResponse response = matchService.updateMatch(id, matchRequest, currentUserId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMatch(@PathVariable Long id,
                                            @AuthenticationPrincipal UserDetails currentUser) {
        Long currentUserId = userService.getUserIdByEmail(currentUser.getUsername());
        matchService.deleteMatch(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{matchId}/statistics")
    public ResponseEntity<MatchResponse> saveMatchStatistics(@PathVariable Long matchId,
                                                             @Valid @RequestBody List<MatchStatisticsRequest> statisticsRequests,
                                                             @AuthenticationPrincipal UserDetails currentUser) {
        Long currentUserId = userService.getUserIdByEmail(currentUser.getUsername());
        MatchResponse response = matchService.saveMatchStatistics(matchId, statisticsRequests, currentUserId);
        return ResponseEntity.ok(response);
    }
}