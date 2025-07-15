package com.tournamentmanager.backend.service;

import com.tournamentmanager.backend.Specification.TournamentSpecification;
import com.tournamentmanager.backend.dto.*;
import com.tournamentmanager.backend.exception.BadRequestException;
import com.tournamentmanager.backend.exception.ConflictException;
import com.tournamentmanager.backend.exception.ResourceNotFoundException;
import com.tournamentmanager.backend.exception.UnauthorizedException;
import com.tournamentmanager.backend.model.Game;
import com.tournamentmanager.backend.model.Location;
import jakarta.persistence.criteria.Predicate;
import com.tournamentmanager.backend.model.Tournament;
import com.tournamentmanager.backend.model.Tournament.TournamentStatus;
import com.tournamentmanager.backend.model.User;
import com.tournamentmanager.backend.model.Team;
import com.tournamentmanager.backend.model.TeamApplication;
import com.tournamentmanager.backend.model.Roles;
import com.tournamentmanager.backend.repository.GameRepository;
import com.tournamentmanager.backend.repository.LocationRepository;
import com.tournamentmanager.backend.repository.TournamentRepository;
import com.tournamentmanager.backend.repository.UserRepository;
import com.tournamentmanager.backend.repository.TeamRepository;
import com.tournamentmanager.backend.repository.TeamApplicationRepository;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collections;


@Service
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final GameRepository gameRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamApplicationRepository teamApplicationRepository;
    private final MatchService matchService;

    public TournamentService(TournamentRepository tournamentRepository,
                             GameRepository gameRepository,
                             LocationRepository locationRepository,
                             UserRepository userRepository,
                             TeamRepository teamRepository,
                             TeamApplicationRepository teamApplicationRepository,
                             MatchService matchService) {
        this.tournamentRepository = tournamentRepository;
        this.gameRepository = gameRepository;
        this.locationRepository = locationRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.teamApplicationRepository = teamApplicationRepository;
        this.matchService = matchService;
    }

    @Transactional
    public TournamentResponse createTournament(TournamentRequest request, Long organizerId) {
        Game game = gameRepository.findById(request.getGameId())
                .orElseThrow(() -> new ResourceNotFoundException("Game", "ID", request.getGameId()));

        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("Organizer", "ID", organizerId));

        Location location = null;
        if (request.getPostalCode() != null && !request.getPostalCode().isEmpty() &&
                request.getCity() != null && !request.getCity().isEmpty() &&
                request.getLatitude() != null && request.getLongitude() != null) {
            location = new Location();
            location.setPostalCode(request.getPostalCode());
            location.setCity(request.getCity());
            location.setStreet(request.getStreet());
            location.setBuildingNumber(request.getBuildingNumber());
            location.setLatitude(request.getLatitude());
            location.setLongitude(request.getLongitude());
            location = locationRepository.save(location);
        }

        Tournament tournament = new Tournament();
        tournament.setName(request.getName());
        tournament.setDescription(request.getDescription());
        tournament.setStartDate(request.getStartDate());
        tournament.setEndDate(request.getEndDate());
        tournament.setGame(game);
        tournament.setOrganizer(organizer);
        tournament.setLocation(location);
        tournament.setMaxTeams(request.getMaxTeams());
        tournament.setStatus(TournamentStatus.PENDING);

        tournament = tournamentRepository.save(tournament);

        return mapToTournamentResponse(tournament);
    }

    public TournamentResponse getTournamentById(Long id) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", "ID", id));

        return mapToTournamentResponse(tournament);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN') or @tournamentService.isOrganizer(#id, #currentUserId)")
    public TournamentResponse updateTournament(Long id, TournamentRequest request, Long currentUserId) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", "ID", id));

        if (tournament.getStatus() == TournamentStatus.ACTIVE || tournament.getStatus() == TournamentStatus.COMPLETED) {
            throw new BadRequestException("Cannot update an active or completed tournament.");
        }

        tournament.setName(request.getName());
        tournament.setDescription(request.getDescription());
        tournament.setStartDate(request.getStartDate());
        tournament.setEndDate(request.getEndDate());

        if (!tournament.getGame().getId().equals(request.getGameId())) {
            Game newGame = gameRepository.findById(request.getGameId())
                    .orElseThrow(() -> new ResourceNotFoundException("Game", "ID", request.getGameId()));
            tournament.setGame(newGame);
        }

        Location newLocation = null;
        if (request.getPostalCode() != null && !request.getPostalCode().isEmpty() &&
                request.getCity() != null && !request.getCity().isEmpty() &&
                request.getLatitude() != null && request.getLongitude() != null) {
            if (tournament.getLocation() != null) {
                newLocation = tournament.getLocation();
                newLocation.setPostalCode(request.getPostalCode());
                newLocation.setCity(request.getCity());
                newLocation.setStreet(request.getStreet());
                newLocation.setBuildingNumber(request.getBuildingNumber());
                newLocation.setLatitude(request.getLatitude());
                newLocation.setLongitude(request.getLongitude());
                locationRepository.save(newLocation);
            } else {
                newLocation = new Location();
                newLocation.setPostalCode(request.getPostalCode());
                newLocation.setCity(request.getCity());
                newLocation.setStreet(request.getStreet());
                newLocation.setBuildingNumber(request.getBuildingNumber());
                newLocation.setLatitude(request.getLatitude());
                newLocation.setLongitude(request.getLongitude());
                newLocation = locationRepository.save(newLocation);
            }
            tournament.setLocation(newLocation);
        } else {
            if (tournament.getLocation() != null) {
                locationRepository.delete(tournament.getLocation());
                tournament.setLocation(null);
            }
        }
        tournament.setMaxTeams(request.getMaxTeams());

        if (request.getMaxTeams() != null && request.getMaxTeams() < tournament.getParticipatingTeams().size()) {
            throw new BadRequestException("Max teams cannot be less than the current number of participating teams.");
        }

        tournament = tournamentRepository.save(tournament);
        return mapToTournamentResponse(tournament);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN') or @tournamentService.isOrganizer(#id, #currentUserId)")
    public void deleteTournament(Long id,  Long currentUserId) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", "ID", id));

        if (tournament.getStatus() == TournamentStatus.ACTIVE || tournament.getStatus() == TournamentStatus.COMPLETED) {
            throw new BadRequestException("Cannot delete an active or completed tournament.");
        }

        if (tournament.getLocation() != null) {
            locationRepository.delete(tournament.getLocation());
        }

        teamApplicationRepository.deleteAll(teamApplicationRepository.findByTournament(tournament));

        if (tournament.getParticipatingTeams() != null) {
            for (Team team : tournament.getParticipatingTeams()) {
                team.getTournaments().remove(tournament);
                teamRepository.save(team);
            }
        }

        tournamentRepository.delete(tournament);
    }

    public List<TournamentResponse> searchTournaments(
            String name, String location, LocalDate startDate, LocalDate endDate,
            String organizerNickname, String gameName, Tournament.TournamentStatus status) {

        Specification<Tournament> spec = TournamentSpecification.findByCriteria(
                name, location, startDate, endDate, organizerNickname, gameName, status);

        List<Tournament> tournaments = tournamentRepository.findAll(spec);

        return tournaments.stream()
                .map(this::mapToTournamentResponse)
                .collect(Collectors.toList());
    }

    public TournamentResponse mapToTournamentResponse(Tournament tournament) {
        TournamentResponse response = new TournamentResponse();
        response.setId(tournament.getId());
        response.setName(tournament.getName());
        response.setDescription(tournament.getDescription());
        response.setStartDate(tournament.getStartDate());
        response.setEndDate(tournament.getEndDate());
        response.setGameName(tournament.getGame().getName());
        response.setGameId(tournament.getGame().getId());
        response.setOrganizerId(tournament.getOrganizer().getId());
        response.setOrganizerNickname(tournament.getOrganizer().getNickname());
        response.setMaxTeams(tournament.getMaxTeams());
        response.setStatus(tournament.getStatus());

        response.setCurrentTeams(tournament.getParticipatingTeams().size());

        boolean isLan = tournament.getLocation() != null;
        response.setLanTournament(isLan);
        if (isLan) {
            Location location = tournament.getLocation();
            response.setPostalCode(location.getPostalCode());
            response.setCity(location.getCity());
            response.setStreet(location.getStreet());
            response.setBuildingNumber(location.getBuildingNumber());
            response.setLatitude(location.getLatitude());
            response.setLongitude(location.getLongitude());
        }

        response.setParticipatingTeams(
                tournament.getParticipatingTeams().stream()
                        .map(this::mapToTournamentTeamResponse)
                        .collect(Collectors.toList())
        );

        return response;
    }

    private TournamentTeamResponse mapToTournamentTeamResponse(Team team) {
        return new TournamentTeamResponse(team.getId(), team.getName());
    }

    @PreAuthorize("hasRole('ADMIN') or @tournamentService.isOrganizer(#tournamentId, #currentUserId)")
    public List<TeamApplicationResponse> getTournamentApplications(Long tournamentId, Long currentUserId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", "ID", tournamentId));


        return teamApplicationRepository.findByTournament(tournament).stream()
                .map(this::mapToTeamApplicationResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN') or @tournamentService.isOrganizer(#tournamentId, #organizerId)")
    public Void updateApplicationStatus(Long tournamentId, Long applicationId, Boolean accepted, Long organizerId) {
        Tournament tournament = tournamentRepository.findByIdWithParticipatingTeams(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", "ID", tournamentId));

        User currentUser = userRepository.findById(organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "ID", organizerId));
        if (!tournament.getOrganizer().getId().equals(organizerId) && !currentUser.getRole().equals(Roles.ROLE_ADMIN)) {
            throw new UnauthorizedException("Only the tournament organizer or an Admin can update application status.");
        }

        TeamApplication application = teamApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "ID", applicationId));

        if (!application.getTournament().getId().equals(tournamentId)) {
            throw new BadRequestException("Application does not belong to this tournament.");
        }

        if (tournament.getStatus() == TournamentStatus.ACTIVE || tournament.getStatus() == TournamentStatus.COMPLETED) {
            throw new BadRequestException("Cannot update application status for an active or completed tournament.");
        }

        if (accepted) {
            if (tournament.getParticipatingTeams().size() >= tournament.getMaxTeams()) {
                throw new ConflictException("Tournament has reached its maximum number of teams.");
            }
            if (application.getStatus() != TeamApplication.ApplicationStatus.PENDING) {
                throw new ConflictException("Application is not in PENDING status.");
            }
            if (tournament.getParticipatingTeams().contains(application.getTeam())) {
                throw new ConflictException("Team is already participating in this tournament.");
            }

            application.setStatus(TeamApplication.ApplicationStatus.ACCEPTED);
            tournament.getParticipatingTeams().add(application.getTeam());
            tournamentRepository.save(tournament);

            Team team = application.getTeam();
            if (team.getTournaments() == null) {
                team.setTournaments(new HashSet<>());
            }
            team.getTournaments().add(tournament);

        } else {
            if (application.getStatus() != TeamApplication.ApplicationStatus.PENDING &&
                    application.getStatus() != TeamApplication.ApplicationStatus.ACCEPTED) {
                throw new BadRequestException("Application cannot be rejected from its current status (must be PENDING or ACCEPTED).");
            }

            if (application.getStatus() == TeamApplication.ApplicationStatus.ACCEPTED) {
                if (tournament.getParticipatingTeams() != null) {
                    tournament.getParticipatingTeams().remove(application.getTeam());
                }
                tournamentRepository.save(tournament);

                Team team = application.getTeam();
                if (team.getTournaments() != null) {
                    team.getTournaments().remove(tournament);
                }
            }
            application.setStatus(TeamApplication.ApplicationStatus.REJECTED);
        }

        TeamApplication updatedApplication = teamApplicationRepository.save(application);
        return null;
    }

    private TeamApplicationResponse mapToTeamApplicationResponse(TeamApplication application) {
        return new TeamApplicationResponse(
                application.getId(),
                application.getTeam().getId(),
                application.getTeam().getName(),
                application.getTournament().getId(),
                application.getTournament().getName(),
                application.getApplicationDate(),
                application.getStatus()
        );
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN') or @tournamentService.isOrganizer(#tournamentId, #currentUserId)")
    public TournamentResponse changeTournamentStatus(Long tournamentId, TournamentStatus newStatus, Long currentUserId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", "ID", tournamentId));

        if (tournament.getStatus() == TournamentStatus.COMPLETED || tournament.getStatus() == TournamentStatus.CANCELLED) {
            if (newStatus != TournamentStatus.CANCELLED) {
                throw new BadRequestException("Cannot change status from COMPLETED or CANCELLED.");
            }
        }
        if (tournament.getStatus() == TournamentStatus.PENDING && newStatus == TournamentStatus.ACTIVE) {
            if (tournament.getParticipatingTeams().size() < 2) {
                throw new BadRequestException("At least 2 teams are required to start the tournament.");
            }
             int numTeams = tournament.getParticipatingTeams().size();
             if ((numTeams & (numTeams - 1)) != 0) {
                 throw new BadRequestException("Number of participating teams must be a power of two to start the bracket.");
             }
        }

        tournament.setStatus(newStatus);
        Tournament updatedTournament = tournamentRepository.save(tournament);

        return mapToTournamentResponse(updatedTournament);
    }

    @Transactional
    @PreAuthorize("@tournamentService.isTeamApplicationLeader(#applicationId, #currentUserId)")
    public TeamApplicationResponse withdrawTeamApplication(Long tournamentId, Long applicationId, Long currentUserId) {
        TeamApplication application = teamApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "ID", applicationId));

        if (!application.getTournament().getId().equals(tournamentId)) {
            throw new BadRequestException("Application does not belong to this tournament.");
        }

        if (application.getStatus() != TeamApplication.ApplicationStatus.PENDING) {
            throw new BadRequestException("Only PENDING applications can be withdrawn.");
        }

        application.setStatus(TeamApplication.ApplicationStatus.REJECTED);
        TeamApplication updatedApplication = teamApplicationRepository.save(application);

        return mapToTeamApplicationResponse(updatedApplication);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN') or @tournamentService.isOrganizer(#tournamentId, #currentUserId)")
    public TournamentResponse removeTeamFromTournament(Long tournamentId, Long teamId, Long currentUserId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", "ID", tournamentId));

        Team teamToRemove = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "ID", teamId));

        if (!tournament.getParticipatingTeams().contains(teamToRemove)) {
            throw new BadRequestException("Team is not a participant in this tournament.");
        }

        if (tournament.getStatus() == TournamentStatus.ACTIVE || tournament.getStatus() == TournamentStatus.COMPLETED) {
            throw new BadRequestException("Cannot remove teams from an active or completed tournament.");
        }

        tournament.getParticipatingTeams().remove(teamToRemove);
        Tournament updatedTournament = tournamentRepository.save(tournament);

        teamToRemove.getTournaments().remove(tournament);
        teamRepository.save(teamToRemove);

        teamApplicationRepository.findByTeamAndTournament(teamToRemove, tournament)
                .ifPresent(app -> {
                    app.setStatus(TeamApplication.ApplicationStatus.REJECTED);
                    teamApplicationRepository.save(app);
                });

        return mapToTournamentResponse(updatedTournament);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN') or @tournamentService.isOrganizer(#tournamentId, #currentUserId)")
    public Tournament startTournament(Long tournamentId, Long currentUserId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", "ID", tournamentId));

        if (tournament.getStatus() != TournamentStatus.PENDING) {
            throw new BadRequestException("Only PENDING tournaments can be started.");
        }

        List<Team> participatingTeams = new ArrayList<>(tournament.getParticipatingTeams());
        if (participatingTeams.size() < 2) {
            throw new BadRequestException("At least 2 teams are required to start the tournament.");
        }

        int numTeams = participatingTeams.size();
        if ((numTeams & (numTeams - 1)) != 0) {
            throw new BadRequestException("Number of participating teams must be a power of two to start the bracket (e.g., 2, 4, 8, 16...). Current teams: " + numTeams);
        }

        tournament.setStatus(TournamentStatus.ACTIVE);
        tournamentRepository.save(tournament);

        return tournament;
    }

    public boolean isOrganizer(Long tournamentId, Long userId) {
        return tournamentRepository.findById(tournamentId)
                .map(tournament -> tournament.getOrganizer() != null && tournament.getOrganizer().getId().equals(userId))
                .orElse(false);
    }

    public boolean isTeamApplicationLeader(Long applicationId, Long userId) {
        return teamApplicationRepository.findById(applicationId)
                .map(application -> application.getTeam() != null && application.getTeam().getLeader() != null && application.getTeam().getLeader().getId().equals(userId))
                .orElse(false);
    }
}