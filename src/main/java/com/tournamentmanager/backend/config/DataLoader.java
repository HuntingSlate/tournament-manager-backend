package com.tournamentmanager.backend.config;

import com.tournamentmanager.backend.dto.TournamentRequest;
import com.tournamentmanager.backend.dto.TournamentResponse;
import com.tournamentmanager.backend.exception.ResourceNotFoundException;
import com.tournamentmanager.backend.model.Game;
import com.tournamentmanager.backend.model.PlayerTeam;
import com.tournamentmanager.backend.model.Roles;
import com.tournamentmanager.backend.model.Team;
import com.tournamentmanager.backend.model.Tournament;
import com.tournamentmanager.backend.model.User;
import com.tournamentmanager.backend.dto.GameRequest;
import com.tournamentmanager.backend.repository.*;
import com.tournamentmanager.backend.service.GameService;
import com.tournamentmanager.backend.service.TeamService;
import com.tournamentmanager.backend.service.TournamentService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Configuration
public class DataLoader {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    @Bean
    public CommandLineRunner initDatabase(UserRepository userRepository,
                                          PasswordEncoder passwordEncoder,
                                          GameService gameService,
                                          TeamService teamService,
                                          TournamentService tournamentService,
                                          TeamRepository teamRepository,
                                          PlayerTeamRepository playerTeamRepository,
                                          TournamentRepository tournamentRepository,
                                          TeamApplicationRepository teamApplicationRepository) {
        return args -> {
            Optional<User> adminUserOptional = userRepository.findByEmail("admin@example.com");
            User adminUser;
            if (adminUserOptional.isEmpty()) {
                log.info("Creating default ADMIN user...");
                adminUser = new User();
                adminUser.setEmail("admin@example.com");
                adminUser.setNickname("Admin");
                adminUser.setPassword(passwordEncoder.encode("AdminPass123!"));
                adminUser.setFullName("Administrator Account");
                adminUser.setRole(Roles.ROLE_ADMIN);
                userRepository.save(adminUser);
                log.info("Default ADMIN user created: " + adminUser.getEmail());
            } else {
                adminUser = adminUserOptional.get();
                log.info("ADMIN user already exists.");
            }

            List<String> defaultGameNames = Arrays.asList(
                    "Dota 2", "League of Legends", "Counter-Strike 2", "Valorant",
                    "Rainbow Six: Siege", "Overwatch 2", "Apex Legends", "Fortnite",
                    "Rocket League", "StarCraft II"
            );

            log.info("Checking and creating default games...");
            for (String gameName : defaultGameNames) {
                if (gameService.findGameByName(gameName) == null) {
                    GameRequest gameRequest = new GameRequest();
                    gameRequest.setName(gameName);
                    gameService.createGame(gameRequest);
                    log.info("Created default game: " + gameName);
                } else {
                    log.info("Game already exists: " + gameName);
                }
            }
            Game defaultGame = gameService.findGameByName("League of Legends");
            if (defaultGame == null) {
                log.error("Critical: Default game 'League of Legends' not found after seeding. Cannot create teams/tournament.");
                return;
            }

            log.info("Checking and creating 12 default users...");
            List<User> seededUsers = new ArrayList<>();
            for (int i = 1; i <= 12; i++) {
                String email = "user" + i + "@example.com";
                String nickname = "User" + i;
                Optional<User> userOptional = userRepository.findByEmail(email);
                User user;
                if (userOptional.isEmpty()) {
                    user = new User();
                    user.setEmail(email);
                    user.setNickname(nickname);
                    user.setPassword(passwordEncoder.encode("UserPass" + i + "!"));
                    user.setFullName("Test User " + i);
                    user.setRole(Roles.ROLE_USER);
                    userRepository.save(user);
                    log.info("Created default user: " + email);
                } else {
                    user = userOptional.get();
                    log.info("User already exists: " + email);
                }
                seededUsers.add(user);
            }

            log.info("Checking and creating 4 default teams...");
            List<Team> seededTeams = new ArrayList<>();
            int userIndex = 0;
            for (int i = 1; i <= 4; i++) {
                String teamName = "Team" + i;
                Optional<Team> teamOptional = teamRepository.findByName(teamName);
                Team team;
                if (teamOptional.isEmpty()) {
                    log.info("Creating team: " + teamName);
                    team = new Team();
                    team.setName(teamName);
                    team.setGame(defaultGame);

                    User leader = seededUsers.get(userIndex % seededUsers.size());
                    team.setLeader(leader);
                    teamRepository.save(team);

                    for (int j = 0; j < 3; j++) {
                        User member = seededUsers.get((userIndex + j) % seededUsers.size());
                        if (playerTeamRepository.findByTeamAndUser(team, member).isEmpty()) {
                            PlayerTeam playerTeam = new PlayerTeam();
                            playerTeam.setTeam(team);
                            playerTeam.setUser(member);
                            playerTeam.setStartDate(LocalDate.now());
                            playerTeamRepository.save(playerTeam);
                            log.info("Added user " + member.getNickname() + " to " + teamName);
                        }
                    }
                    userIndex += 3;
                    seededTeams.add(team);

                } else {
                    team = teamOptional.get();
                    log.info("Team already exists: " + teamName);
                    seededTeams.add(team);
                }
            }

            String tournamentName = "Seeded Test Tournament";
            Optional<Tournament> tournamentOptional = tournamentRepository.findByName(tournamentName);
            Tournament tournament;
            if (tournamentOptional.isEmpty()) {
                log.info("Creating default tournament: " + tournamentName);
                TournamentRequest tournamentRequest = new TournamentRequest();
                tournamentRequest.setName(tournamentName);
                tournamentRequest.setDescription("A test tournament automatically created by DataLoader.");
                tournamentRequest.setStartDate(LocalDate.now().plusDays(7));
                tournamentRequest.setEndDate(LocalDate.now().plusDays(14));
                tournamentRequest.setGameId(defaultGame.getId());
                tournamentRequest.setMaxTeams(4);
                tournamentRequest.setPostalCode("00-001");
                tournamentRequest.setCity("Warsaw");
                tournamentRequest.setStreet("Testowa");
                tournamentRequest.setBuildingNumber(1);
                tournamentRequest.setLatitude(52.2297);
                tournamentRequest.setLongitude(21.0122);

                TournamentResponse createdTournament;
                createdTournament = tournamentService.createTournament(tournamentRequest, adminUser.getId());
                Long createdTournamentId = createdTournament.getId();
                tournament = tournamentRepository.findById(createdTournamentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Tournament", "ID", createdTournamentId));

                for (Team team : seededTeams) {
                    if (team.getLeader() != null) {
                        teamService.applyToTournament(team.getId(), tournament.getId(), team.getLeader().getId());
                        log.info("Team " + team.getName() + " applied to tournament " + tournament.getName());

                        teamApplicationRepository.findByTeamAndTournament(team, tournament)
                                .ifPresent(app -> {
                                    tournamentService.updateApplicationStatus(tournament.getId(), app.getId(), true);
                                    log.info("Application for team " + team.getName() + " accepted by Admin.");
                                });
                    }
                }
            } else {
                tournament = tournamentOptional.get();
                log.info("Default tournament '" + tournament.getName() + "' already exists.");
            }
        };
    }
}