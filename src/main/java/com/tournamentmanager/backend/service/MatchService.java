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
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MatchService {

    private final MatchRepository matchRepository;
    private final TournamentRepository tournamentRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final MatchStatisticsRepository matchStatisticsRepository;
    private final PlayerStatisticsRepository playerStatisticsRepository;

    public MatchService(MatchRepository matchRepository, TournamentRepository tournamentRepository,
                        TeamRepository teamRepository, UserRepository userRepository,
                        MatchStatisticsRepository matchStatisticsRepository,
                        PlayerStatisticsRepository playerStatisticsRepository) {
        this.matchRepository = matchRepository;
        this.tournamentRepository = tournamentRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.matchStatisticsRepository = matchStatisticsRepository;
        this.playerStatisticsRepository = playerStatisticsRepository;
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @tournamentService.isOrganizer(#request.getTournamentId(), #currentUserId)")
    public Match createMatch(MatchRequest request, Long currentUserId) {
        Tournament tournament = tournamentRepository.findById(request.getTournamentId())
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", "ID", request.getTournamentId()));

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
        match.setRoundNumber(request.getRoundNumber());
        match.setMatchNumberInRound(request.getMatchNumberInRound());
        match.setStatus(Match.MatchStatus.SCHEDULED);

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
        return match;
    }

    public MatchResponse getMatchById(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match", "ID", id));
        return mapToMatchResponse(match);
    }

    public List<MatchResponse> getMatchesByTournament(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", "ID", tournamentId));
        return matchRepository.findByTournament(tournament).stream()
                .map(this::mapToMatchResponse)
                .collect(Collectors.toList());
    }

    public List<MatchResponse> searchMatches(String tournamentName, String gameName, String teamName,
                                             String playerName, Long tournamentId, Long gameId, Long teamId,
                                             Long playerId) {
        List<Match> matches = matchRepository.searchMatches(tournamentId, tournamentName, gameId,
                gameName, teamId, teamName, playerId, playerName);
        return matches.stream()
                .map(this::mapToMatchResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @tournamentService.isOrganizer(#id, #currentUserId)")
    public MatchResponse updateMatch(Long id, MatchRequest request, Long currentUserId) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match", "ID", id));

        match.setStartDatetime(request.getStartDatetime());
        match.setEndDatetime(request.getEndDatetime());
        match.setRoundNumber(request.getRoundNumber());
        match.setMatchNumberInRound(request.getMatchNumberInRound());

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
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @tournamentService.isOrganizer(#id, #currentUserId)")
    public void deleteMatch(Long id,  Long currentUserId) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match", "ID", id));

        if (match.getTournament().getStatus() == Tournament.TournamentStatus.ACTIVE ||
                match.getTournament().getStatus() == Tournament.TournamentStatus.COMPLETED) {
            throw new BadRequestException("Cannot delete matches from an ongoing or finished tournament.");
        }

        matchRepository.delete(match);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @tournamentService.isOrganizer(#matchId, #currentUserId)")
    public MatchResponse saveMatchStatistics(Long matchId, List<MatchStatisticsRequest> statisticsRequests, Long currentUserId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match", "ID", matchId));

        if (match.getStatus() != Match.MatchStatus.COMPLETED && match.getStatus() != Match.MatchStatus.IN_PROGRESS) {
            throw new BadRequestException("Statistics can only be saved for completed or in-progress matches.");
        }

        Set<User> team1Players = match.getTeam1().getTeamMembers().stream().map(PlayerTeam::getUser).collect(Collectors.toSet());
        Set<User> team2Players = match.getTeam2().getTeamMembers().stream().map(PlayerTeam::getUser).collect(Collectors.toSet());

        for (MatchStatisticsRequest req : statisticsRequests) {
            User player = userRepository.findById(req.getPlayerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Player", "ID", req.getPlayerId()));

            boolean isPlayerInMatchTeams = team1Players.contains(player) || team2Players.contains(player);

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
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @tournamentService.isOrganizer(#matchId, #currentUserId)")
    public MatchResponse recordMatchResult(Long matchId, Integer scoreTeam1, Integer scoreTeam2, Long currentUserId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match", "ID", matchId));

        if (match.getStatus() == Match.MatchStatus.COMPLETED || match.getStatus() == Match.MatchStatus.CANCELLED) {
            throw new BadRequestException("Cannot record results for a completed or cancelled match.");
        }

        if (scoreTeam1 == null || scoreTeam2 == null || scoreTeam1 < 0 || scoreTeam2 < 0) {
            throw new BadRequestException("Scores cannot be null or negative.");
        }

        if (scoreTeam1.equals(scoreTeam2)) {
            throw new BadRequestException("Draws are not allowed. One team must win.");
        }

        match.setScoreTeam1(scoreTeam1);
        match.setScoreTeam2(scoreTeam2);
        match.setEndDatetime(LocalDateTime.now());
        match.setStatus(Match.MatchStatus.COMPLETED);

        Team winnerTeam;
        if (scoreTeam1 > scoreTeam2) {
            winnerTeam = match.getTeam1();
        } else {
            winnerTeam = match.getTeam2();
        }
        match.setWinningTeam(winnerTeam);

        Match savedMatch = matchRepository.save(match);

        Optional<Match> nextMatchOptional = matchRepository.findByPrevMatch1OrPrevMatch2(savedMatch, savedMatch);

        if (nextMatchOptional.isPresent()) {
            Match nextMatch = nextMatchOptional.get();

            if (nextMatch.getPrevMatch1() != null && nextMatch.getPrevMatch1().getId().equals(savedMatch.getId())) {
                nextMatch.setTeam1(winnerTeam);
            } else if (nextMatch.getPrevMatch2() != null && nextMatch.getPrevMatch2().getId().equals(savedMatch.getId())) {
                nextMatch.setTeam2(winnerTeam);
            }
            matchRepository.save(nextMatch);
        }

        return mapToMatchResponse(savedMatch);
    }

    public void generateFirstRoundMatches(Tournament tournament) {
        List<Team> participatingTeams = new ArrayList<>(tournament.getParticipatingTeams());
        Collections.shuffle(participatingTeams);


        int roundNumber = 1;
        for (int i = 0; i < participatingTeams.size(); i += 2) {
            Team team1 = participatingTeams.get(i);
            Team team2 = participatingTeams.get(i + 1);

            Match match = new Match();
            match.setTournament(tournament);
            match.setTeam1(team1);
            match.setTeam2(team2);
            match.setRoundNumber(roundNumber);
            match.setMatchNumberInRound((i / 2) + 1);
            match.setStatus(Match.MatchStatus.SCHEDULED);

            matchRepository.save(match);
        }
    }

    public MatchResponse mapToMatchResponse(Match match) {
        MatchResponse response = new MatchResponse();
        response.setId(match.getId());
        response.setStartDatetime(match.getStartDatetime());
        response.setEndDatetime(match.getEndDatetime());
        response.setRoundNumber(match.getRoundNumber());
        response.setMatchNumberInRound(match.getMatchNumberInRound());
        response.setScoreTeam1(match.getScoreTeam1());
        response.setScoreTeam2(match.getScoreTeam2());
        response.setStatus(match.getStatus());

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