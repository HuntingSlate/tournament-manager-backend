package com.tournamentmanager.backend.service;

import com.tournamentmanager.backend.dto.*;
import com.tournamentmanager.backend.model.*;
import com.tournamentmanager.backend.repository.*;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final LinkRepository linkRepository;
    private final TeamLinkRepository teamLinkRepository;

    public TeamService(TeamRepository teamRepository, GameRepository gameRepository,
                       UserRepository userRepository, PlayerTeamRepository playerTeamRepository,
                       TournamentRepository tournamentRepository,
                       TeamApplicationRepository teamApplicationRepository,
                       LinkRepository linkRepository, TeamLinkRepository teamLinkRepository) {
        this.teamRepository = teamRepository;
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
        this.playerTeamRepository = playerTeamRepository;
        this.tournamentRepository = tournamentRepository;
        this.teamApplicationRepository = teamApplicationRepository;
        this.linkRepository = linkRepository;
        this.teamLinkRepository = teamLinkRepository;
    }

    @Transactional
    public TeamResponse createTeam(TeamRequest request, Long leaderId) {
        Game game = gameRepository.findById(request.getGameId())
                .orElseThrow(() -> new ResourceNotFoundException("Game", "ID", request.getGameId()));

        User leader = userRepository.findById(leaderId)
                .orElseThrow(() -> new ResourceNotFoundException("Leader", "ID", leaderId));

        if (teamRepository.findByName(request.getName()).isPresent()) {
            throw new ConflictException("Team with name '" + request.getName() + "' already exists.");
        }

        Team team = new Team();
        team.setName(request.getName());
        team.setGame(game);
        team.setLeader(leader);
        PlayerTeam leaderMembership = new PlayerTeam();
        leaderMembership.setUser(leader);
        leaderMembership.setTeam(team);
        leaderMembership.setStartDate(LocalDate.now());
        playerTeamRepository.save(leaderMembership);

        if (request.getTeamLinks() != null && !request.getTeamLinks().isEmpty()) {
            for (TeamLinkRequest linkRequest : request.getTeamLinks()) {
                Link link = findOrCreateLink(linkRequest.getType(), linkRequest.getUrl());

                if (teamLinkRepository.findByTeamAndLink(team, link).isPresent()) {
                    throw new ConflictException("Team already has a link with type '" + linkRequest.getType() + "' and URL '" + linkRequest.getUrl() + "'.");
                }

                TeamLink teamLink = new TeamLink();
                teamLink.setTeam(team);
                teamLink.setLink(link);
                teamLink.setPlatformUsername(linkRequest.getPlatformUsername());
                team.getTeamLinks().add(teamLink);
            }
        }

        Team savedTeam = teamRepository.save(team);
        return mapToTeamResponse(savedTeam);
    }

    public TeamResponse getTeamById(Long id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "ID", id));
        return mapToTeamResponse(team);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN') or @teamService.isTeamLeader(#id, #currentUserId)")
    public TeamResponse updateTeam(Long id, TeamRequest request) {
        Team existingTeam = teamRepository.findByIdWithTeamLinks(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "ID", id));

        if (!existingTeam.getName().equals(request.getName())) {
            if (teamRepository.findByName(request.getName()).isPresent() && !teamRepository.findByName(request.getName()).get().getId().equals(id)) {
                throw new ConflictException("Team with name '" + request.getName() + "' already exists.");
            }
            existingTeam.setName(request.getName());
        }

        if (!existingTeam.getGame().getId().equals(request.getGameId())) {
            Game newGame = gameRepository.findById(request.getGameId())
                    .orElseThrow(() -> new ResourceNotFoundException("Game", "ID", request.getGameId()));
            existingTeam.setGame(newGame);
        }

        updateTeamLinks(existingTeam, request.getTeamLinks());

        Team updatedTeam = teamRepository.save(existingTeam);
        return mapToTeamResponse(updatedTeam);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN') or @teamService.isTeamLeader(#id, #currentUserId)")
    public void deleteTeam(Long id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "ID", id));

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

        boolean isTeamLeader = team.getLeader().getId().equals(currentUserId);
        boolean isRemovingSelf = memberToRemove.getId().equals(currentUserId);

        if (!isTeamLeader && !isRemovingSelf) {
            throw new UnauthorizedException("Only the team leader or the member themselves can remove a member from this team.");
        }

        PlayerTeam playerTeam = playerTeamRepository.findByTeamAndUser(team, memberToRemove)
                .orElseThrow(() -> new BadRequestException("User is not a member of this team."));

        if (team.getLeader().getId().equals(memberToRemove.getId()) && !isTeamLeader) {
            throw new BadRequestException("Only the team leader can remove themselves as leader. Change leader first.");
        }

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

    @Transactional(readOnly = true)
    public List<TeamLinkResponse> getTeamLinks(Long teamId) {
        Team team = teamRepository.findByIdWithTeamLinks(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "ID", teamId));

        return team.getTeamLinks().stream()
                .map(this::mapToTeamLinkResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN') or @teamService.isTeamLeader(#teamId, #currentUserId)")
    public TeamLinkResponse addTeamLink(Long teamId, TeamLinkRequest request, Long currentUserId) {
        Team team = teamRepository.findByIdWithTeamLinks(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "ID", teamId));

        if (request.getId() != null) {
            throw new BadRequestException("ID must be null for adding new links. Use PUT for updates.");
        }

        Link link = findOrCreateLink(request.getType(), request.getUrl());

        TeamLink teamLink = new TeamLink();
        teamLink.setTeam(team);
        teamLink.setLink(link);
        teamLink.setPlatformUsername(request.getPlatformUsername());

        TeamLink savedTeamLink = teamLinkRepository.save(teamLink);


        return mapToTeamLinkResponse(savedTeamLink);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN') or @teamService.isTeamLeader(#teamId, #currentUserId)")
    public TeamLinkResponse updateTeamLink(Long teamId, Long teamLinkId, TeamLinkRequest request, Long currentUserId) {
        Team team = teamRepository.findByIdWithTeamLinks(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "ID", teamId));

        TeamLink teamLinkToUpdate = teamLinkRepository.findById(teamLinkId)
                .orElseThrow(() -> new ResourceNotFoundException("Team link", "ID", teamLinkId));

        if (!teamLinkToUpdate.getTeam().getId().equals(teamId)) {
            throw new BadRequestException("Team link does not belong to the specified team.");
        }

        if (request.getId() != null && !request.getId().equals(teamLinkId)) {
            throw new BadRequestException("Link ID in path and request body do not match.");
        }

        Link link = findOrCreateLink(request.getType(), request.getUrl());

        Optional<TeamLink> duplicateTeamLink = teamLinkRepository.findByTeamAndLink(team, link);
        if (duplicateTeamLink.isPresent() && !duplicateTeamLink.get().getId().equals(teamLinkId)) {
            throw new ConflictException("Team already has another link with the same type and URL.");
        }

        teamLinkToUpdate.setLink(link);
        teamLinkToUpdate.setPlatformUsername(request.getPlatformUsername());

        TeamLink updatedTeamLink = teamLinkRepository.save(teamLinkToUpdate);
        return mapToTeamLinkResponse(updatedTeamLink);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN') or @teamService.isTeamLeader(#teamId, #currentUserId)")
    public void deleteTeamLink(Long teamId, Long teamLinkId, Long currentUserId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "ID", teamId));

        TeamLink teamLinkToDelete = teamLinkRepository.findById(teamLinkId)
                .orElseThrow(() -> new ResourceNotFoundException("Team link", "ID", teamLinkId));

        if (!teamLinkToDelete.getTeam().getId().equals(teamId)) {
            throw new BadRequestException("Team link does not belong to the specified team.");
        }

        teamLinkRepository.delete(teamLinkToDelete);
    }

    private Link findOrCreateLink(String type, String url) {
        return linkRepository.findByNameAndUrl(type, url)
                .orElseGet(() -> {
                    Link newLink = new Link();
                    newLink.setName(type);
                    newLink.setUrl(url);
                    return linkRepository.save(newLink);
                });
    }

    private void updateTeamLinks(Team team, List<TeamLinkRequest> newLinkRequests) {
        Set<TeamLink> existingTeamLinks = team.getTeamLinks();
        Set<TeamLink> linksToRemove = new HashSet<>();

        Set<TeamLinkRequest> linksToProcess = new HashSet<>(newLinkRequests);

        for (TeamLink existingLink : existingTeamLinks) {
            boolean found = false;
            for (TeamLinkRequest newLinkRequest : newLinkRequests) {
                if (existingLink.getId().equals(newLinkRequest.getId())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                linksToRemove.add(existingLink);
            }
        }
        existingTeamLinks.removeAll(linksToRemove);
        teamLinkRepository.deleteAll(linksToRemove);

        for (TeamLinkRequest linkRequest : newLinkRequests) {
            if (linkRequest.getId() != null) {
                TeamLink existingTeamLink = existingTeamLinks.stream()
                        .filter(tl -> tl.getId().equals(linkRequest.getId()))
                        .findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("TeamLink", "ID", linkRequest.getId()));

                Link link = findOrCreateLink(linkRequest.getType(), linkRequest.getUrl());

                Optional<TeamLink> duplicateTeamLink = teamLinkRepository.findByTeamAndLink(team, link);
                if (duplicateTeamLink.isPresent() && !duplicateTeamLink.get().getId().equals(existingTeamLink.getId())) {
                    throw new ConflictException("Team already has another link with type '" + linkRequest.getType() + "' and URL '" + linkRequest.getUrl() + "'.");
                }

                existingTeamLink.setLink(link);
                existingTeamLink.setPlatformUsername(linkRequest.getPlatformUsername());
                teamLinkRepository.save(existingTeamLink);

            } else {
                Link link = findOrCreateLink(linkRequest.getType(), linkRequest.getUrl());

                if (teamLinkRepository.findByTeamAndLink(team, link).isPresent()) {
                    throw new ConflictException("Team already has a link with type '" + linkRequest.getType() + "' and URL '" + linkRequest.getUrl() + "'.");
                }

                TeamLink newTeamLink = new TeamLink();
                newTeamLink.setTeam(team);
                newTeamLink.setLink(link);
                newTeamLink.setPlatformUsername(linkRequest.getPlatformUsername());
                team.getTeamLinks().add(newTeamLink);
            }
        }
    }

    public TeamResponse mapToTeamResponse(Team team) {
        TeamResponse response = new TeamResponse();
        response.setId(team.getId());
        response.setName(team.getName());

        if (team.getGame() != null) {
            response.setGameId(team.getGame().getId());
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
        } else {
            response.setTeamMembers(List.of());
        }

        if (team.getTeamLinks() != null) {
            response.setTeamLinks(team.getTeamLinks().stream()
                    .map(this::mapToTeamLinkResponse)
                    .collect(Collectors.toList()));
        } else {
            response.setTeamLinks(List.of());
        }

        return response;
    }

    private TeamMemberResponse mapToTeamMemberResponse(PlayerTeam playerTeam) {
        return new TeamMemberResponse(
                playerTeam.getUser().getId(),
                playerTeam.getUser().getNickname(),
                playerTeam.getStartDate(),
                playerTeam.getEndDate()
        );
    }

    private TeamLinkResponse mapToTeamLinkResponse(TeamLink teamLink) {
        return new TeamLinkResponse(
                teamLink.getId(),
                teamLink.getLink().getName(),
                teamLink.getLink().getUrl(),
                teamLink.getPlatformUsername(),
                teamLink.getTeam().getId()
        );
    }

    public Long getUserIdByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    public boolean isTeamLeader(Long teamId, Long userId) {
        return teamRepository.findById(teamId)
                .map(team -> team.getLeader() != null && team.getLeader().getId().equals(userId))
                .orElse(false);
    }
}