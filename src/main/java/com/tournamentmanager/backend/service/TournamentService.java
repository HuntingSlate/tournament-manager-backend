package com.tournamentmanager.backend.service;

import com.tournamentmanager.backend.dto.TournamentRequest;
import com.tournamentmanager.backend.dto.TournamentResponse;
import com.tournamentmanager.backend.model.Game;
import com.tournamentmanager.backend.model.Location;
import com.tournamentmanager.backend.model.Tournament;
import com.tournamentmanager.backend.model.User;
import com.tournamentmanager.backend.repository.GameRepository;
import com.tournamentmanager.backend.repository.LocationRepository;
import com.tournamentmanager.backend.repository.TournamentRepository;
import com.tournamentmanager.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final GameRepository gameRepository;
    private final LocationRepository locationRepository;
    public final UserRepository userRepository;

    public TournamentService(TournamentRepository tournamentRepository,
                             GameRepository gameRepository,
                             LocationRepository locationRepository,
                             UserRepository userRepository) {
        this.tournamentRepository = tournamentRepository;
        this.gameRepository = gameRepository;
        this.locationRepository = locationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TournamentResponse createTournament(TournamentRequest request, Long organizerId) {
        Game game = gameRepository.findById(request.getGameId())
                .orElseThrow(() -> new RuntimeException("Game not found with ID: " + request.getGameId()));

        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new RuntimeException("Organizer not found with ID: " + organizerId));

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

        tournament = tournamentRepository.save(tournament);

        return mapToTournamentResponse(tournament, isLanTournament);
    }

    public TournamentResponse getTournamentById(Long id) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tournament not found with ID: " + id));

        boolean isLanTournament = tournament.getLocation() != null;
        return mapToTournamentResponse(tournament, isLanTournament);
    }

    @Transactional
    public TournamentResponse updateTournament(Long id, TournamentRequest request, Long currentUserId) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tournament not found with ID: " + id));

        if (!tournament.getOrganizer().getId().equals(currentUserId)) {
            throw new RuntimeException("Unauthorized: Only the organizer can update this tournament.");
        }

        tournament.setName(request.getName());
        tournament.setDescription(request.getDescription());
        tournament.setStartDate(request.getStartDate());
        tournament.setEndDate(request.getEndDate());

        if (!tournament.getGame().getId().equals(request.getGameId())) {
            Game newGame = gameRepository.findById(request.getGameId())
                    .orElseThrow(() -> new RuntimeException("Game not found with ID: " + request.getGameId()));
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

        tournament = tournamentRepository.save(tournament);
        return mapToTournamentResponse(tournament, isLanTournament);
    }

    @Transactional
    public void deleteTournament(Long id, Long currentUserId) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tournament not found with ID: " + id));

        if (!tournament.getOrganizer().getId().equals(currentUserId)) {
            throw new RuntimeException("Unauthorized: Only the organizer can delete this tournament.");
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

        if (tournament.getLocation() != null && isLanTournament) {
            response.setPostalCode(tournament.getLocation().getPostalCode());
            response.setCity(tournament.getLocation().getCity());
            response.setStreet(tournament.getLocation().getStreet());
            response.setNumber(tournament.getLocation().getNumber());
        }
        return response;
    }
}