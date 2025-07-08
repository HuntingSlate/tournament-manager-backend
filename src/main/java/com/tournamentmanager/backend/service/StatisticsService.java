package com.tournamentmanager.backend.service;

import com.tournamentmanager.backend.dto.MatchPlayerStatisticsResponse;
import com.tournamentmanager.backend.dto.PlayerStatisticsResponse;
import com.tournamentmanager.backend.exception.ResourceNotFoundException;
import com.tournamentmanager.backend.model.*;
import com.tournamentmanager.backend.repository.*;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    private final PlayerStatisticsRepository playerStatisticsRepository;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final MatchRepository matchRepository;
    private final MatchStatisticsRepository matchStatisticsRepository;

    public StatisticsService(PlayerStatisticsRepository playerStatisticsRepository,
                             UserRepository userRepository,
                             GameRepository gameRepository,
                             MatchRepository matchRepository,
                             MatchStatisticsRepository matchStatisticsRepository) {
        this.playerStatisticsRepository = playerStatisticsRepository;
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
        this.matchRepository = matchRepository;
        this.matchStatisticsRepository = matchStatisticsRepository;
    }

    public PlayerStatisticsResponse getPlayerStatistics(Long playerId, Long gameId) {
        User player = userRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player", "ID", playerId));
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Game", "ID", gameId));

        PlayerStatistics stats = playerStatisticsRepository.findByPlayerAndGame(player, game)
                .orElse(null);
        return mapToPlayerStatisticsResponse(stats, player, game);
    }

    public List<PlayerStatisticsResponse> getPlayerRankingByKills(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Game", "ID", gameId));

        List<PlayerStatistics> statsList = playerStatisticsRepository.findByGame(game);

        return statsList.stream()
                .sorted(Comparator.comparing(PlayerStatistics::getKills, Comparator.reverseOrder()))
                .map(stats -> mapToPlayerStatisticsResponse(stats, stats.getPlayer(), stats.getGame()))
                .collect(Collectors.toList());
    }

    private PlayerStatisticsResponse mapToPlayerStatisticsResponse(PlayerStatistics stats, User player, Game game) {
        if (stats == null) {
            return new PlayerStatisticsResponse(
                    null, player.getId(), player.getNickname(), game.getId(), game.getName(),
                    0, 0, 0, 0.0, 0.0, 0.0, 0
            );
        }

        return new PlayerStatisticsResponse(
                stats.getId(),
                stats.getPlayer().getId(),
                stats.getPlayer().getNickname(),
                stats.getGame().getId(),
                stats.getGame().getName(),
                stats.getKills(),
                stats.getDeaths(),
                stats.getAssists(),
                stats.getAverageKills(),
                stats.getAverageDeaths(),
                stats.getAverageAssists(),
                stats.getMatchesPlayed()
        );
    }

    public MatchPlayerStatisticsResponse getMatchPlayerStatistics(Long matchId, Long playerId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match", "ID", matchId));
        User player = userRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player", "ID", playerId));

        MatchStatistics stats = matchStatisticsRepository.findByMatchAndPlayer(match, player)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Match statistics", "player and match", player.getNickname() + " in match " + match.getId()));

        return mapToMatchPlayerStatisticsResponse(stats);
    }


    private MatchPlayerStatisticsResponse mapToMatchPlayerStatisticsResponse(MatchStatistics stats) {
        return new MatchPlayerStatisticsResponse(
                stats.getId(),
                stats.getPlayer().getId(),
                stats.getPlayer().getNickname(),
                stats.getKills(),
                stats.getDeaths(),
                stats.getAssists()
        );
    }
}