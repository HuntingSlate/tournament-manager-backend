package com.tournamentmanager.backend.service;

import com.tournamentmanager.backend.dto.TournamentRequest;
import com.tournamentmanager.backend.dto.TournamentResponse;
import com.tournamentmanager.backend.dto.TeamApplicationResponse;
import com.tournamentmanager.backend.dto.ApplicationStatusRequest;
import com.tournamentmanager.backend.model.Game;
import com.tournamentmanager.backend.model.Location;
import com.tournamentmanager.backend.model.Tournament;
import com.tournamentmanager.backend.model.User;
import com.tournamentmanager.backend.model.Team;
import com.tournamentmanager.backend.model.TeamApplication;
import com.tournamentmanager.backend.repository.GameRepository;
import com.tournamentmanager.backend.repository.LocationRepository;
import com.tournamentmanager.backend.repository.TournamentRepository;
import com.tournamentmanager.backend.repository.UserRepository;
import com.tournamentmanager.backend.repository.TeamRepository;
import com.tournamentmanager.backend.repository.TeamApplicationRepository;
import com.tournamentmanager.backend.exception.ResourceNotFoundException;
import com.tournamentmanager.backend.exception.UnauthorizedException;
import com.tournamentmanager.backend.exception.BadRequestException;
import com.tournamentmanager.backend.exception.ConflictException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;


@Service
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final GameRepository gameRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamApplicationRepository teamApplicationRepository;

    public TournamentService(TournamentRepository tournamentRepository,
                             GameRepository gameRepository,
                             LocationRepository locationRepository,
                             UserRepository userRepository,
                             TeamRepository teamRepository,
                             TeamApplicationRepository teamApplicationRepository) {
        this.tournamentRepository = tournamentRepository;
        this.gameRepository = gameRepository;
        this.locationRepository = locationRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.teamApplicationRepository = teamApplicationRepository;
    }

    @Transactional
    public TournamentResponse createTournament(TournamentRequest request, Long organizerId) {
        Game game = gameRepository.findById(request.getGameId())
                .orElseThrow(() -> new ResourceNotFoundException("Game", "ID", request.getGameId()));

        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("Organizer", "ID", organizerId));

        Location location = null;
        boolean isLanTournament = false;
        if (request.getPostalCode() != null && !request.getPostalCode().isEmpty() &&
                request.getCity() != null && !request.getCity().isEmpty()) {
            isLanTournament = true;
            location = new Location();
            location.setPostalCode(request.getPostalCode());
            location.setCity(request.getCity());
            location.setStreet(request.getStreet());
            location.setNumber(request.getNumber());
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
        tournament.setCurrentTeams(0);
        tournament.setStatus(Tournament.TournamentStatus.PENDING);

        tournament = tournamentRepository.save(tournament);

        return mapToTournamentResponse(tournament, isLanTournament);
    }

    public TournamentResponse getTournamentById(Long id) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", "ID", id));

        boolean isLanTournament = tournament.getLocation() != null;
        return mapToTournamentResponse(tournament, isLanTournament);
    }

    @Transactional
    public TournamentResponse updateTournament(Long id, TournamentRequest request, Long currentUserId) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", "ID", id));

        if (!tournament.getOrganizer().getId().equals(currentUserId)) {
            throw new UnauthorizedException("Only the organizer can update this tournament.");
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
        boolean isLanTournament = false;
        if (request.getPostalCode() != null && !request.getPostalCode().isEmpty() &&
                request.getCity() != null && !request.getCity().isEmpty()) {
            isLanTournament = true;
            if (tournament.getLocation() != null) {
                newLocation = tournament.getLocation();
                newLocation.setPostalCode(request.getPostalCode());
                newLocation.setCity(request.getCity());
                newLocation.setStreet(request.getStreet());
                newLocation.setNumber(request.getNumber());
                locationRepository.save(newLocation);
            } else {
                newLocation = new Location();
                newLocation.setPostalCode(request.getPostalCode());
                newLocation.setCity(request.getCity());
                newLocation.setStreet(request.getStreet());
                newLocation.setNumber(request.getNumber());
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
        if (request.getStatus() != null) {
            tournament.setStatus(request.getStatus());
        }

        tournament = tournamentRepository.save(tournament);
        return mapToTournamentResponse(tournament, isLanTournament);
    }

    @Transactional
    public void deleteTournament(Long id, Long currentUserId) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", "ID", id));

        if (!tournament.getOrganizer().getId().equals(currentUserId)) {
            throw new UnauthorizedException("Only the organizer can delete this tournament.");
        }

        if (tournament.getLocation() != null) {
            locationRepository.delete(tournament.getLocation());
        }

        tournamentRepository.delete(tournament);
    }

    public List<TournamentResponse> searchTournaments(String name, String location, LocalDate startDate, LocalDate endDate, String organizerNickname) {
        List<Tournament> tournaments;

        if (name != null && !name.isEmpty()) {
            tournaments = tournamentRepository.findByNameContainingIgnoreCase(name);
        } else if (location != null && !location.isEmpty()) {
            tournaments = tournamentRepository.findByLocationCityContainingIgnoreCase(location);
        } else if (organizerNickname != null && !organizerNickname.isEmpty()) {
            tournaments = tournamentRepository.findByOrganizerNicknameContainingIgnoreCase(organizerNickname);
        } else if (startDate != null && endDate != null) {
            tournaments = tournamentRepository.findByStartDateBetween(startDate, endDate);
        } else {
            tournaments = tournamentRepository.findAll();
        }

        return tournaments.stream()
                .map(t -> mapToTournamentResponse(t, t.getLocation() != null))
                .collect(Collectors.toList());
    }

    private TournamentResponse mapToTournamentResponse(Tournament tournament, boolean isLanTournament) {
        TournamentResponse response = new TournamentResponse();
        response.setId(tournament.getId());
        response.setName(tournament.getName());
        response.setDescription(tournament.getDescription());
        response.setStartDate(tournament.getStartDate());
        response.setEndDate(tournament.getEndDate());

        if (tournament.getGame() != null) {
            response.setGameName(tournament.getGame().getName());
        }
        if (tournament.getOrganizer() != null) {
            response.setOrganizerId(tournament.getOrganizer().getId());
            response.setOrganizerNickname(tournament.getOrganizer().getNickname());
        }

        response.setLanTournament(isLanTournament);

        if (tournament.getLocation() != null) {
            response.setPostalCode(tournament.getLocation().getPostalCode());
            response.setCity(tournament.getLocation().getCity());
            response.setStreet(tournament.getLocation().getStreet());
            response.setNumber(tournament.getLocation().getNumber());
        }

        response.setMaxTeams(tournament.getMaxTeams());
        response.setCurrentTeams(tournament.getCurrentTeams());
        response.setStatus(tournament.getStatus());

        return response;
    }

    public List<TeamApplicationResponse> getTournamentApplications(Long tournamentId, Long organizerId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", "ID", tournamentId));

        if (!tournament.getOrganizer().getId().equals(organizerId)) {
            throw new UnauthorizedException("User is not authorized to view applications for this tournament.");
        }

        return teamApplicationRepository.findByTournament(tournament).stream()
                .map(this::mapToTeamApplicationResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TeamApplicationResponse updateApplicationStatus(Long tournamentId, Long applicationId, Boolean accepted, Long organizerId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", "ID", tournamentId));

        if (!tournament.getOrganizer().getId().equals(organizerId)) {
            throw new UnauthorizedException("User is not authorized to manage applications for this tournament.");
        }

        TeamApplication application = teamApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "ID", applicationId));

        if (!application.getTournament().getId().equals(tournamentId)) {
            throw new BadRequestException("Application does not belong to this tournament.");
        }

        if (accepted) {
            if (tournament.getParticipatingTeams() == null) {
                tournament.setParticipatingTeams(new HashSet<>());
            }
            if (tournament.getParticipatingTeams().size() >= tournament.getMaxTeams()) {
                throw new ConflictException("Tournament has reached its maximum number of teams.");
            }
            if (application.getStatus() != TeamApplication.ApplicationStatus.PENDING) {
                throw new ConflictException("Application is not in PENDING status or already accepted/rejected.");
            }

            application.setStatus(TeamApplication.ApplicationStatus.ACCEPTED);
            tournament.getParticipatingTeams().add(application.getTeam());
            tournament.setCurrentTeams(tournament.getCurrentTeams() + 1);
            tournamentRepository.save(tournament);

            Team team = application.getTeam();
            if (team.getTournaments() == null) {
                team.setTournaments(new HashSet<>());
            }
            team.getTournaments().add(tournament);
            teamRepository.save(team);

        } else {
            if (application.getStatus() != TeamApplication.ApplicationStatus.PENDING &&
                    application.getStatus() != TeamApplication.ApplicationStatus.ACCEPTED) {
                throw new BadRequestException("Application cannot be rejected from its current status.");
            }

            if (application.getStatus() == TeamApplication.ApplicationStatus.ACCEPTED) {
                if (tournament.getParticipatingTeams() != null) {
                    tournament.getParticipatingTeams().remove(application.getTeam());
                }
                if (tournament.getCurrentTeams() > 0) {
                    tournament.setCurrentTeams(tournament.getCurrentTeams() - 1);
                }
                tournamentRepository.save(tournament);

                Team team = application.getTeam();
                if (team.getTournaments() != null) {
                    team.getTournaments().remove(tournament);
                    teamRepository.save(team);
                }
            }
            application.setStatus(TeamApplication.ApplicationStatus.REJECTED);
        }

        TeamApplication updatedApplication = teamApplicationRepository.save(application);
        return mapToTeamApplicationResponse(updatedApplication);
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
}