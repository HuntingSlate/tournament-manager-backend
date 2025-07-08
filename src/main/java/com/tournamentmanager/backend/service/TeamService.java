package com.tournamentmanager.backend.service;

import com.tournamentmanager.backend.dto.LinkResponse;
import com.tournamentmanager.backend.dto.TeamMemberResponse;
import com.tournamentmanager.backend.dto.TeamRequest;
import com.tournamentmanager.backend.dto.TeamResponse;
import com.tournamentmanager.backend.model.*;
import com.tournamentmanager.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tournamentmanager.backend.exception.ResourceNotFoundException;
import com.tournamentmanager.backend.exception.UnauthorizedException;
import com.tournamentmanager.backend.exception.BadRequestException;
import com.tournamentmanager.backend.exception.ConflictException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.HashSet;


@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final PlayerTeamRepository playerTeamRepository;
    private final TournamentRepository tournamentRepository;
    private final TeamApplicationRepository teamApplicationRepository;

    public TeamService(TeamRepository teamRepository, GameRepository gameRepository,
                       UserRepository userRepository, PlayerTeamRepository playerTeamRepository,
                       TournamentRepository tournamentRepository,
                       TeamApplicationRepository teamApplicationRepository) {
        this.teamRepository = teamRepository;
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
        this.playerTeamRepository = playerTeamRepository;
        this.tournamentRepository = tournamentRepository;
        this.teamApplicationRepository = teamApplicationRepository;
    }

    @Transactional
    public TeamResponse createTeam(TeamRequest request, Long leaderId) {
        Game game = gameRepository.findById(request.getGameId())
                .orElseThrow(() -> new ResourceNotFoundException("Game", "ID", request.getGameId()));

        User leader = userRepository.findById(leaderId)
                .orElseThrow(() -> new ResourceNotFoundException("Leader", "ID", leaderId));

        if (teamRepository.findByNameAndGame(request.getName(), game).isPresent()) {
            throw new ConflictException("Team with this name already exists for this game.");
        }

        Team team = new Team();
        team.setName(request.getName());
        team.setGame(game);
        team.setLeader(leader);

        team = teamRepository.save(team);

        PlayerTeam leaderMembership = new PlayerTeam();
        leaderMembership.setUser(leader);
        leaderMembership.setTeam(team);
        leaderMembership.setStartDate(LocalDate.now());
        playerTeamRepository.save(leaderMembership);

        return mapToTeamResponse(team);
    }

    public TeamResponse getTeamById(Long id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "ID", id));
        return mapToTeamResponse(team);
    }

    @Transactional
    public TeamResponse updateTeam(Long id, TeamRequest request, Long currentUserId) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "ID", id));

        if (!team.getLeader().getId().equals(currentUserId)) {
            throw new UnauthorizedException("Only the team leader can update this team.");
        }

        if (!team.getName().equals(request.getName())) {
            if (teamRepository.findByNameAndGame(request.getName(), team.getGame()).isPresent()) {
                throw new ConflictException("Team with this name already exists for this game.");
            }
            team.setName(request.getName());
        }

        if (!team.getGame().getId().equals(request.getGameId())) {
            Game newGame = gameRepository.findById(request.getGameId())
                    .orElseThrow(() -> new ResourceNotFoundException("Game", "ID", request.getGameId()));
            team.setGame(newGame);
        }

        team = teamRepository.save(team);
        return mapToTeamResponse(team);
    }

    @Transactional
    public void deleteTeam(Long id, Long currentUserId) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "ID", id));

        if (!team.getLeader().getId().equals(currentUserId)) {
            throw new UnauthorizedException("Only the team leader can delete this team.");
        }

        teamRepository.delete(team);
    }

    @Transactional
    public TeamResponse addTeamMember(Long teamId, Long memberId, Long currentUserId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "ID", teamId));
        User newMember = userRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", "ID", memberId));

        if (!team.getLeader().getId().equals(currentUserId)) {
            throw new UnauthorizedException("Only the team leader can add members to this team.");
        }

        if (playerTeamRepository.findByTeamAndUser(team, newMember).isPresent()) {
            throw new ConflictException("User is already a member of this team.");
        }


        PlayerTeam playerTeam = new PlayerTeam();
        playerTeam.setTeam(team);
        playerTeam.setUser(newMember);
        playerTeam.setStartDate(LocalDate.now());
        playerTeamRepository.save(playerTeam);

        return mapToTeamResponse(team);
    }

    @Transactional
    public TeamResponse removeTeamMember(Long teamId, Long memberId, Long currentUserId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "ID", teamId));
        User memberToRemove = userRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", "ID", memberId));

        if (!team.getLeader().getId().equals(currentUserId)) {
            throw new UnauthorizedException("Only the team leader can remove members from this team.");
        }

        PlayerTeam playerTeam = playerTeamRepository.findByTeamAndUser(team, memberToRemove)
                .orElseThrow(() -> new BadRequestException("User is not a member of this team."));

        playerTeamRepository.delete(playerTeam);

        return mapToTeamResponse(team);
    }

    @Transactional
    public TeamResponse applyToTournament(Long teamId, Long tournamentId, Long currentUserId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "ID", teamId));
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", "ID", tournamentId));

        if (!team.getLeader().getId().equals(currentUserId)) {
            throw new UnauthorizedException("Only the team leader can apply this team to a tournament.");
        }

        if (!team.getGame().equals(tournament.getGame())) {
            throw new BadRequestException("Team's game does not match tournament's game.");
        }

        if (teamApplicationRepository.findByTeamAndTournament(team, tournament).isPresent()) {
            throw new ConflictException("Team has already applied to this tournament.");
        }

        TeamApplication newApplication = new TeamApplication();
        newApplication.setTeam(team);
        newApplication.setTournament(tournament);
        newApplication.setApplicationDate(LocalDateTime.now());
        newApplication.setStatus(TeamApplication.ApplicationStatus.PENDING);

        teamApplicationRepository.save(newApplication);

        return mapToTeamResponse(team);
    }

    public List<TeamResponse> searchTeams(String name, String playerName) {
        List<Team> teams;

        if (name != null && !name.isEmpty()) {
            teams = teamRepository.findByNameContainingIgnoreCase(name);
        } else if (playerName != null && !playerName.isEmpty()) {
            teams = teamRepository.findByPlayerNicknameContainingIgnoreCase(playerName);
        } else {
            teams = teamRepository.findAll();
        }

        return teams.stream()
                .map(this::mapToTeamResponse)
                .collect(Collectors.toList());
    }

    private TeamResponse mapToTeamResponse(Team team) {
        TeamResponse response = new TeamResponse();
        response.setId(team.getId());
        response.setName(team.getName());

        if (team.getGame() != null) {
            response.setGameName(team.getGame().getName());
        }
        if (team.getLeader() != null) {
            response.setLeaderId(team.getLeader().getId());
            response.setLeaderNickname(team.getLeader().getNickname());
        }

        if (team.getTeamMembers() != null) {
            response.setTeamMembers(team.getTeamMembers().stream()
                    .map(pt -> new TeamMemberResponse(pt.getUser().getId(), pt.getUser().getNickname(), pt.getStartDate(), pt.getEndDate()))
                    .collect(Collectors.toList()));
        }

        if (team.getTeamLinks() != null) {
            response.setLinks(team.getTeamLinks().stream()
                    .map(tl -> new LinkResponse(tl.getLink().getId(), tl.getLink().getName(), tl.getLink().getUrl()))
                    .collect(Collectors.toList()));
        }

        return response;
    }
}