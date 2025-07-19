package com.tournamentmanager.backend.service;

import com.tournamentmanager.backend.dto.*;
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

        Team team1 = teamRepository.findById(request.getFirstTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team", "ID", request.getFirstTeamId()));
        Team team2 = teamRepository.findById(request.getSecondTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team", "ID", request.getSecondTeamId()));

        if (team1.getId().equals(team2.getId())) {
            throw new BadRequestException("Teams cannot be the same in a match.");
        }

        if (!tournament.getParticipatingTeams().contains(team1) || !tournament.getParticipatingTeams().contains(team2)) {
            throw new BadRequestException("Both teams must be part of the tournament.");
        }

        Match match = new Match();
        match.setTournament(tournament);
        match.setFirstTeam(team1);
        match.setSecondTeam(team2);
        match.setStartDatetime(request.getStartDatetime());
        match.setEndDatetime(request.getEndDatetime());
        match.setBracketLevel(request.getBracketLevel());
        match.setMatchNumberInRound(request.getMatchNumberInRound());
        match.setStatus(Match.MatchStatus.SCHEDULED);

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
    public MatchResponse updateMatch(Long id, MatchUpdateRequest request, Long currentUserId) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match", "ID", id));

        Team oldWinner = match.getWinningTeam();

        match.setStartDatetime(request.getStartDatetime());
        match.setEndDatetime(request.getEndDatetime());
        match.setFirstTeamScore(request.getFirstTeamScore());
        match.setSecondTeamScore(request.getSecondTeamScore());
        match.setStatus(request.getStatus());

        Team newWinner = null;
        if (request.getWinningTeamId() != null) {
            newWinner = teamRepository.findById(request.getWinningTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Winning team", "ID", request.getWinningTeamId()));
            if (!newWinner.equals(match.getFirstTeam()) && !newWinner.equals(match.getSecondTeam())) {
                throw new BadRequestException("Winning team must be one of the participating teams in the match.");
            }
            match.setWinningTeam(newWinner);
        } else {
            match.setWinningTeam(null);
        }

        match = matchRepository.save(match);
        final Match completedMatch = match;

        boolean winnerHasBeenSet = (oldWinner == null && newWinner != null);

        if (winnerHasBeenSet) {
            int partnerMatchNumber;
            if (match.getMatchNumberInRound() % 2 == 1) {
                partnerMatchNumber = match.getMatchNumberInRound() + 1;
            } else {
                partnerMatchNumber = match.getMatchNumberInRound() - 1;
            }

            Optional<Match> partnerMatchOpt = matchRepository.findByTournamentAndBracketLevelAndMatchNumberInRound(
                    match.getTournament(), match.getBracketLevel(), partnerMatchNumber);

            if (partnerMatchOpt.isPresent() && partnerMatchOpt.get().getWinningTeam() != null) {
                Match partnerMatch = partnerMatchOpt.get();
                Team winner1 = match.getWinningTeam();
                Team winner2 = partnerMatch.getWinningTeam();

                Team nextMatchFirstTeam = (match.getMatchNumberInRound() < partnerMatch.getMatchNumberInRound()) ? winner1 : winner2;
                Team nextMatchSecondTeam = (match.getMatchNumberInRound() < partnerMatch.getMatchNumberInRound()) ? winner2 : winner1;

                Match nextMatch = new Match();
                nextMatch.setTournament(match.getTournament());
                nextMatch.setBracketLevel(match.getBracketLevel() + 1);
                nextMatch.setMatchNumberInRound((match.getMatchNumberInRound() + 1) / 2);
                nextMatch.setFirstTeam(nextMatchFirstTeam);
                nextMatch.setSecondTeam(nextMatchSecondTeam);
                nextMatch.setStatus(Match.MatchStatus.SCHEDULED);
                nextMatch.setStartDatetime(match.getEndDatetime() != null ? match.getEndDatetime().plusDays(1) : LocalDateTime.now().plusDays(1));

                matchRepository.save(nextMatch);
            }
        }

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

        Set<User> team1Players = match.getFirstTeam().getTeamMembers().stream().map(PlayerTeam::getUser).collect(Collectors.toSet());
        Set<User> team2Players = match.getSecondTeam().getTeamMembers().stream().map(PlayerTeam::getUser).collect(Collectors.toSet());

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

        match.setFirstTeamScore(scoreTeam1);
        match.setSecondTeamScore(scoreTeam2);
        match.setEndDatetime(LocalDateTime.now());
        match.setStatus(Match.MatchStatus.COMPLETED);

        Team winnerTeam;
        if (scoreTeam1 > scoreTeam2) {
            winnerTeam = match.getFirstTeam();
        } else {
            winnerTeam = match.getSecondTeam();
        }
        match.setWinningTeam(winnerTeam);

        Match savedMatch = matchRepository.save(match);

        return mapToMatchResponse(savedMatch);
    }

    public void generateFirstRoundMatches(Tournament tournament) {
        List<Team> participatingTeams = new ArrayList<>(tournament.getParticipatingTeams());
        Collections.shuffle(participatingTeams);

        LocalDateTime firstRoundStartTime = tournament.getStartDate().atStartOfDay();

        int bracketLevel = 1;
        for (int i = 0; i < participatingTeams.size(); i += 2) {
            Team team1 = participatingTeams.get(i);
            Team team2 = participatingTeams.get(i + 1);

            Match match = new Match();
            match.setTournament(tournament);
            match.setMatchNumberInRound((i / 2) + 1);
            match.setFirstTeam(team1);
            match.setSecondTeam(team2);
            match.setBracketLevel(bracketLevel);
            match.setStatus(Match.MatchStatus.SCHEDULED);

            match.setStartDatetime(firstRoundStartTime);

            matchRepository.save(match);
        }
    }

    public MatchResponse mapToMatchResponse(Match match) {
        MatchResponse response = new MatchResponse();
        response.setId(match.getId());
        response.setStartDatetime(match.getStartDatetime());
        response.setEndDatetime(match.getEndDatetime());
        response.setBracketLevel(match.getBracketLevel());
        response.setFirstTeamScore(match.getFirstTeamScore());
        response.setMatchNumberInRound(match.getMatchNumberInRound());
        response.setSecondTeamScore(match.getSecondTeamScore());
        response.setStatus(match.getStatus());

        if (match.getTournament() != null) {
            response.setTournamentId(match.getTournament().getId());
            response.setTournamentName(match.getTournament().getName());
        }
        if (match.getFirstTeam() != null) {
            response.setFirstTeamId(match.getFirstTeam().getId());
            response.setFirstTeamName(match.getFirstTeam().getName());
        }
        if (match.getSecondTeam() != null) {
            response.setSecondTeamId(match.getSecondTeam().getId());
            response.setSecondTeamName(match.getSecondTeam().getName());
        }
        if (match.getWinningTeam() != null) {
            response.setWinningTeamId(match.getWinningTeam().getId());
            response.setWinningTeamName(match.getWinningTeam().getName());
        }

        List<MatchStatistics> allMatchStats = matchStatisticsRepository.findByMatch(match);
        Map<Long, MatchStatistics> statsByPlayerId = allMatchStats.stream()
                .collect(Collectors.toMap(stat -> stat.getPlayer().getId(), stat -> stat));

        List<MatchPlayerStatisticsResponse> firstTeamStats = new ArrayList<>();
        List<MatchPlayerStatisticsResponse> secondTeamStats = new ArrayList<>();

        if (match.getFirstTeam() != null && match.getFirstTeam().getTeamMembers() != null) {
            for (PlayerTeam playerTeam : match.getFirstTeam().getTeamMembers()) {
                User player = playerTeam.getUser();
                MatchStatistics stats = statsByPlayerId.get(player.getId());

                if (stats != null) {
                    firstTeamStats.add(new MatchPlayerStatisticsResponse(
                            stats.getId(), player.getId(), player.getNickname(),
                            stats.getKills(), stats.getDeaths(), stats.getAssists()));
                } else {
                    firstTeamStats.add(new MatchPlayerStatisticsResponse(
                            null, player.getId(), player.getNickname(),
                            null, null, null));
                }
            }
        }

        if (match.getSecondTeam() != null && match.getSecondTeam().getTeamMembers() != null) {
            for (PlayerTeam playerTeam : match.getSecondTeam().getTeamMembers()) {
                User player = playerTeam.getUser();
                MatchStatistics stats = statsByPlayerId.get(player.getId());

                if (stats != null) {
                    secondTeamStats.add(new MatchPlayerStatisticsResponse(
                            stats.getId(), player.getId(), player.getNickname(),
                            stats.getKills(), stats.getDeaths(), stats.getAssists()));
                } else {
                    secondTeamStats.add(new MatchPlayerStatisticsResponse(
                            null, player.getId(), player.getNickname(),
                            null, null, null));
                }
            }
        }

        response.setFirstTeamMatchStatistics(firstTeamStats);
        response.setSecondTeamMatchStatistics(secondTeamStats);

        return response;
    }
}