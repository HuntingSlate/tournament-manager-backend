package com.tournamentmanager.backend.service;

import com.tournamentmanager.backend.dto.MatchRequest;
import com.tournamentmanager.backend.dto.MatchResponse;
import com.tournamentmanager.backend.dto.MatchStatisticsRequest;
import com.tournamentmanager.backend.dto.MatchPlayerStatisticsResponse;
import com.tournamentmanager.backend.exception.BadRequestException;
import com.tournamentmanager.backend.exception.ResourceNotFoundException;
import com.tournamentmanager.backend.exception.UnauthorizedException;
import com.tournamentmanager.backend.model.*;
import com.tournamentmanager.backend.repository.MatchRepository;
import com.tournamentmanager.backend.repository.TournamentRepository;
import com.tournamentmanager.backend.repository.TeamRepository;
import com.tournamentmanager.backend.repository.UserRepository;
import com.tournamentmanager.backend.repository.MatchStatisticsRepository;
import com.tournamentmanager.backend.repository.PlayerStatisticsRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MatchService {

    private final MatchRepository matchRepository;
    private final TournamentRepository tournamentRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final MatchStatisticsRepository matchStatisticsRepository;
    private final PlayerStatisticsRepository playerStatisticsRepository;
    private final UserService userService;

    public MatchService(MatchRepository matchRepository, TournamentRepository tournamentRepository,
                        TeamRepository teamRepository, UserRepository userRepository,
                        MatchStatisticsRepository matchStatisticsRepository,
                        PlayerStatisticsRepository playerStatisticsRepository, UserService userService) {
        this.matchRepository = matchRepository;
        this.tournamentRepository = tournamentRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.matchStatisticsRepository = matchStatisticsRepository;
        this.playerStatisticsRepository = playerStatisticsRepository;
        this.userService = userService;
    }

    @Transactional
    public MatchResponse createMatch(MatchRequest request, Long currentUserId) {
        Tournament tournament = tournamentRepository.findById(request.getTournamentId())
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", "ID", request.getTournamentId()));

        if (!tournament.getOrganizer().getId().equals(currentUserId)) {
            throw new RuntimeException("Unauthorized: Only the tournament organizer can create matches.");
        }

        Team team1 = teamRepository.findById(request.getTeam1Id())
                .orElseThrow(() -> new ResourceNotFoundException("Team", "ID", request.getTeam1Id()));
        Team team2 = teamRepository.findById(request.getTeam2Id())
                .orElseThrow(() -> new ResourceNotFoundException("Team", "ID", request.getTeam2Id()));

        if (team1.getId().equals(team2.getId())) {
            throw new BadRequestException("Teams cannot be the same in a match.");
        }

        if (!tournament.getParticipatingTeams().contains(team1) || !tournament.getParticipatingTeams().contains(team2)) {
            throw new BadRequestException("Both teams must be part of the tournament.");
        }

        Match match = new Match();
        match.setTournament(tournament);
        match.setTeam1(team1);
        match.setTeam2(team2);
        match.setStartDatetime(request.getStartDatetime());
        match.setEndDatetime(request.getEndDatetime());

        if (request.getPrevMatch1Id() != null) {
            Match prevMatch1 = matchRepository.findById(request.getPrevMatch1Id())
                    .orElseThrow(() -> new ResourceNotFoundException("Previous match", "ID", request.getPrevMatch1Id()));
            match.setPrevMatch1(prevMatch1);
        }
        if (request.getPrevMatch2Id() != null) {
            Match prevMatch2 = matchRepository.findById(request.getPrevMatch2Id())
                    .orElseThrow(() -> new ResourceNotFoundException("Previous match", "ID", request.getPrevMatch2Id()));
            match.setPrevMatch2(prevMatch2);
        }

        match = matchRepository.save(match);
        return mapToMatchResponse(match);
    }

    public MatchResponse getMatchById(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match", "ID", id));
        return mapToMatchResponse(match);
    }

    @Transactional
    public MatchResponse updateMatch(Long id, MatchRequest request, Long currentUserId) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match", "ID", id));

        if (!match.getTournament().getOrganizer().getId().equals(currentUserId)) {
            throw new UnauthorizedException("Only the tournament organizer can update this match.");
        }

        match.setStartDatetime(request.getStartDatetime());
        match.setEndDatetime(request.getEndDatetime());

        if (request.getWinningTeamId() != null) {
            Team winningTeam = teamRepository.findById(request.getWinningTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Winning team", "ID", request.getWinningTeamId()));
            if (!winningTeam.equals(match.getTeam1()) && !winningTeam.equals(match.getTeam2())) {
                throw new BadRequestException("Winning team must be one of the participating teams in the match.");
            }
            match.setWinningTeam(winningTeam);
        } else {
            match.setWinningTeam(null);
        }

        match = matchRepository.save(match);
        return mapToMatchResponse(match);
    }

    @Transactional
    public void deleteMatch(Long id, Long currentUserId) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match", "ID", id));

        if (!match.getTournament().getOrganizer().getId().equals(currentUserId)) {
            throw new UnauthorizedException("Only the tournament organizer can delete this match.");
        }

        matchRepository.delete(match);
    }

    @Transactional
    public MatchResponse saveMatchStatistics(Long matchId, List<MatchStatisticsRequest> statisticsRequests, Long currentUserId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match", "ID", matchId));

        if (!match.getTournament().getOrganizer().getId().equals(currentUserId)) {
            throw new UnauthorizedException("Only the tournament organizer can save match statistics.");
        }

        for (MatchStatisticsRequest req : statisticsRequests) {
            User player = userService.getUserById(req.getPlayerId());

            Set<PlayerTeam> team1Members = match.getTeam1().getTeamMembers();
            Set<PlayerTeam> team2Members = match.getTeam2().getTeamMembers();

            boolean isPlayerInMatchTeams = team1Members.stream().anyMatch(pt -> pt.getUser().equals(player)) ||
                    team2Members.stream().anyMatch(pt -> pt.getUser().equals(player));

            if (!isPlayerInMatchTeams) {
                throw new BadRequestException("Player " + player.getNickname() + " is not a member of teams participating in this match.");
            }

            MatchStatistics matchStats = matchStatisticsRepository.findByMatchAndPlayer(match, player)
                    .orElse(new MatchStatistics());

            matchStats.setMatch(match);
            matchStats.setPlayer(player);
            matchStats.setKills(req.getKills());
            matchStats.setDeaths(req.getDeaths());
            matchStats.setAssists(req.getAssists());
            matchStatisticsRepository.save(matchStats);

            updatePlayerOverallStatistics(player, match.getTournament().getGame(), req.getKills(), req.getDeaths(), req.getAssists());
        }

        return mapToMatchResponse(matchRepository.findById(matchId).get());
    }

    @Transactional
    public void updatePlayerOverallStatistics(User player, Game game, Integer kills, Integer deaths, Integer assists) {
        PlayerStatistics playerStats = playerStatisticsRepository.findByPlayerAndGame(player, game)
                .orElse(new PlayerStatistics());

        if (playerStats.getId() == null) {
            playerStats.setPlayer(player);
            playerStats.setGame(game);
            playerStats.setKills(kills);
            playerStats.setDeaths(deaths);
            playerStats.setAssists(assists);
            playerStats.setMatchesPlayed(1);
        } else {
            playerStats.setKills(playerStats.getKills() + kills);
            playerStats.setDeaths(playerStats.getDeaths() + deaths);
            playerStats.setAssists(playerStats.getAssists() + assists);
            playerStats.setMatchesPlayed(playerStats.getMatchesPlayed() + 1);
        }
        playerStatisticsRepository.save(playerStats);
    }


    private MatchResponse mapToMatchResponse(Match match) {
        MatchResponse response = new MatchResponse();
        response.setId(match.getId());
        response.setStartDatetime(match.getStartDatetime());
        response.setEndDatetime(match.getEndDatetime());

        if (match.getTournament() != null) {
            response.setTournamentId(match.getTournament().getId());
            response.setTournamentName(match.getTournament().getName());
        }
        if (match.getTeam1() != null) {
            response.setTeam1Id(match.getTeam1().getId());
            response.setTeam1Name(match.getTeam1().getName());
        }
        if (match.getTeam2() != null) {
            response.setTeam2Id(match.getTeam2().getId());
            response.setTeam2Name(match.getTeam2().getName());
        }
        if (match.getPrevMatch1() != null) {
            response.setPrevMatch1Id(match.getPrevMatch1().getId());
        }
        if (match.getPrevMatch2() != null) {
            response.setPrevMatch2Id(match.getPrevMatch2().getId());
        }
        if (match.getWinningTeam() != null) {
            response.setWinningTeamId(match.getWinningTeam().getId());
            response.setWinningTeamName(match.getWinningTeam().getName());
        }

        List<MatchStatistics> matchStats = matchStatisticsRepository.findByMatch(match);
        if (matchStats != null) {
            response.setMatchStatistics(matchStats.stream()
                    .map(ms -> new MatchPlayerStatisticsResponse(
                            ms.getId(),
                            ms.getPlayer().getId(),
                            ms.getPlayer().getNickname(),
                            ms.getKills(),
                            ms.getDeaths(),
                            ms.getAssists()))
                    .collect(Collectors.toList()));
        }

        return response;
    }
}