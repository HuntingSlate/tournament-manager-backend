package com.tournamentmanager.backend.controller;

import com.tournamentmanager.backend.dto.MatchPlayerStatisticsResponse;
import com.tournamentmanager.backend.dto.PlayerStatisticsResponse;
import com.tournamentmanager.backend.service.MatchService;
import com.tournamentmanager.backend.service.StatisticsService;
import com.tournamentmanager.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final UserService userService;
    private final MatchService matchService;

    public StatisticsController(StatisticsService statisticsService,
                                UserService userService,
                                MatchService matchService) {
        this.statisticsService = statisticsService;
        this.userService = userService;
        this.matchService = matchService;
    }

    @GetMapping("/player/{gameId}")
    public ResponseEntity<PlayerStatisticsResponse> getMyStatisticsForGame(
            @PathVariable Long gameId,
            @AuthenticationPrincipal UserDetails currentUser) {
        Long playerId = userService.getUserIdByEmail(currentUser.getUsername());
        PlayerStatisticsResponse response = statisticsService.getPlayerStatistics(playerId, gameId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/player/{playerId}/game/{gameId}")
    public ResponseEntity<PlayerStatisticsResponse> getPlayerStatisticsForGame(
            @PathVariable Long playerId,
            @PathVariable Long gameId) {
        PlayerStatisticsResponse response = statisticsService.getPlayerStatistics(playerId, gameId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ranking/game/{gameId}/kills")
    public ResponseEntity<List<PlayerStatisticsResponse>> getRankingByKills(@PathVariable Long gameId) {
        List<PlayerStatisticsResponse> ranking = statisticsService.getPlayerRankingByKills(gameId);
        return ResponseEntity.ok(ranking);
    }

    @GetMapping("/match/{matchId}/player/{playerId}")
    public ResponseEntity<MatchPlayerStatisticsResponse> getMatchPlayerStatistics(
            @PathVariable Long matchId,
            @PathVariable Long playerId) {
        MatchPlayerStatisticsResponse response = statisticsService.getMatchPlayerStatistics(matchId, playerId);
        return ResponseEntity.ok(response);
    }
}