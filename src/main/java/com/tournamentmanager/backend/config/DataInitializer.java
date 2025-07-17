package com.tournamentmanager.backend.config;

import com.github.javafaker.Faker;
import com.tournamentmanager.backend.model.*;
import com.tournamentmanager.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Profile("dev")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final Faker faker = new Faker();

    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TournamentRepository tournamentRepository;
    private final PlayerTeamRepository playerTeamRepository;
    private final TeamApplicationRepository teamApplicationRepository;
    private final LocationRepository locationRepository;
    private final MatchRepository matchRepository;
    private final MatchStatisticsRepository matchStatisticsRepository;
    private final PlayerStatisticsRepository playerStatisticsRepository;

    private final PasswordEncoder passwordEncoder;

    public DataInitializer(GameRepository gameRepository, UserRepository userRepository, TeamRepository teamRepository,
                           TournamentRepository tournamentRepository, PlayerTeamRepository playerTeamRepository,
                           TeamApplicationRepository teamApplicationRepository, LocationRepository locationRepository,
                           MatchRepository matchRepository, MatchStatisticsRepository matchStatisticsRepository,
                           PlayerStatisticsRepository playerStatisticsRepository, PasswordEncoder passwordEncoder) {
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.tournamentRepository = tournamentRepository;
        this.playerTeamRepository = playerTeamRepository;
        this.teamApplicationRepository = teamApplicationRepository;
        this.locationRepository = locationRepository;
        this.matchRepository = matchRepository;
        this.matchStatisticsRepository = matchStatisticsRepository;
        this.playerStatisticsRepository = playerStatisticsRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            log.info("Database already seeded. Skipping data initialization.");
            return;
        }
        log.info("Starting database initialization... 🚀");

        log.info("Creating Games...");
        Game cs2 = createGame("Counter-Strike 2");
        Game lol = createGame("League of Legends");
        Game valorant = createGame("Valorant");
        Game r6 = createGame("Rainbow Six: Siege");

        log.info("Creating a large pool of Users...");
        User admin = createUser("admin", "admin@example.com", "password", Roles.ROLE_ADMIN);
        createUser("test", "test@test.com", "test1234", Roles.ROLE_USER);
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            users.add(createUser(faker.name().username(), faker.internet().emailAddress(), "password", Roles.ROLE_USER));
        }

        log.info("Creating Teams...");
        List<Team> cs2Teams = createTeamsForGame(cs2, users.subList(0, 40), 8);
        List<Team> lolTeams = createTeamsForGame(lol, users.subList(40, 80), 8);
        List<Team> valorantTeams = createTeamsForGame(valorant, users.subList(80, 100), 4);

        log.info("Creating current Tournaments for interaction...");
        Location iemLocation = createLocation("40-001", "Katowice", "al. Korfantego", 35);
        Tournament iem = createTournament("IEM Katowice 2026", "Nadchodzący Major CS2 w Katowicach", cs2, admin, iemLocation, 16, LocalDate.now().plusMonths(6));
        applyAndAcceptTeams(iem, cs2Teams.subList(0, 4));

        Tournament activeValorant = createTournament("Online Valorant Clash", "Aktywny turniej online", valorant, users.get(1), null, 4, LocalDate.now().minusDays(1));
        applyAndAcceptTeams(activeValorant, valorantTeams);
        activeValorant.setStatus(Tournament.TournamentStatus.ACTIVE);
        tournamentRepository.save(activeValorant);
        generateFirstRoundMatches(activeValorant);

        log.info("Generating rich history of completed tournaments and statistics...");
        for (int i = 1; i <= 3; i++) {
            Tournament historicalCsTournament = createTournament("ESL Pro League Season " + (20 - i), "Historyczny turniej CS2", cs2, admin, null, 8, LocalDate.now().minusMonths(6 * i));
            List<Team> participants = new ArrayList<>(cs2Teams);
            Collections.shuffle(participants);
            applyAndAcceptTeams(historicalCsTournament, participants.subList(0, 8));
            simulateFullTournament(historicalCsTournament);
        }
        for (int i = 1; i <= 2; i++) {
            Tournament historicalLolTournament = createTournament("Worlds " + (2024 - i), "Historyczne mistrzostwa LoL", lol, admin, null, 8, LocalDate.now().minusMonths(8 * i));
            List<Team> participants = new ArrayList<>(lolTeams);
            Collections.shuffle(participants);
            applyAndAcceptTeams(historicalLolTournament, participants.subList(0, 8));
            simulateFullTournament(historicalLolTournament);
        }

        log.info("Database initialization finished successfully! ✅");
    }

    private void applyAndAcceptTeams(Tournament tournament, List<Team> teams) {
        for (Team team : teams) {
            TeamApplication app = new TeamApplication(null, team, tournament, LocalDateTime.now(), TeamApplication.ApplicationStatus.PENDING);
            teamApplicationRepository.save(app);

            app.setStatus(TeamApplication.ApplicationStatus.ACCEPTED);
            teamApplicationRepository.save(app);

            tournament.getParticipatingTeams().add(team);
            team.getTournaments().add(tournament);
        }
        tournamentRepository.save(tournament);
    }

    private List<Team> createTeamsForGame(Game game, List<User> userPool, int numberOfTeams) {
        List<Team> teams = new ArrayList<>();
        int userIndex = 0;
        for (int i = 0; i < numberOfTeams; i++) {
            if (userIndex + 5 > userPool.size()) break;

            String teamName;
            do {
                teamName = faker.team().name();
            } while (teamRepository.findByName(teamName).isPresent());

            User leader = userPool.get(userIndex);
            List<User> members = userPool.subList(userIndex, userIndex + 5);

            teams.add(createTeam(teamName, game, leader, members));
            userIndex += 5;
        }
        return teams;
    }

    private void simulateFullTournament(Tournament tournament) {
        log.info("Simulating full tournament: {}", tournament.getName());
        tournament.setStatus(Tournament.TournamentStatus.ACTIVE);
        tournamentRepository.save(tournament);
        generateFirstRoundMatches(tournament);

        int roundNumber = 1;
        List<Match> matchesInRound = matchRepository.findByTournamentAndRoundNumber(tournament, roundNumber);

        while (matchesInRound.size() >= 1) {
            List<Team> winners = new ArrayList<>();
            log.info("-- Simulating Round {} with {} matches...", roundNumber, matchesInRound.size());

            for (Match match : matchesInRound) {
                if (match.getTeam1() != null && match.getTeam2() != null) {
                    recordMatchResult(match, faker.number().numberBetween(1, 16), faker.number().numberBetween(1, 16));
                    winners.add(match.getWinningTeam());
                    saveRandomMatchStatisticsForMatch(match);
                }
            }

            if (winners.size() < 2) {
                break;
            }

            roundNumber++;
            generateNextRoundMatches(tournament, winners, roundNumber);
            matchesInRound = matchRepository.findByTournamentAndRoundNumber(tournament, roundNumber);
        }

        tournament.setStatus(Tournament.TournamentStatus.COMPLETED);
        tournamentRepository.save(tournament);
        log.info("Simulation finished for tournament: {}", tournament.getName());
    }
    private void generateNextRoundMatches(Tournament tournament, List<Team> winners, int roundNumber) {
        LocalDateTime startTime = tournament.getStartDate().atStartOfDay().plusDays(roundNumber -1);
        for (int i = 0; i < winners.size(); i += 2) {
            Match match = new Match();
            match.setTournament(tournament);
            match.setTeam1(winners.get(i));
            match.setTeam2(winners.get(i + 1));
            match.setRoundNumber(roundNumber);
            match.setMatchNumberInRound((i / 2) + 1);
            match.setStatus(Match.MatchStatus.SCHEDULED);
            match.setStartDatetime(startTime);
            matchRepository.save(match);
        }
    }

    private void saveRandomMatchStatisticsForMatch(Match match) {
        List<User> players = new ArrayList<>();
        if (match.getTeam1() != null) players.addAll(getPlayersFromTeam(match.getTeam1()));
        if (match.getTeam2() != null) players.addAll(getPlayersFromTeam(match.getTeam2()));

        for (User player : players) {
            saveMatchStatistics(match, player,
                    faker.number().numberBetween(0, 30),
                    faker.number().numberBetween(5, 25),
                    faker.number().numberBetween(0, 20));
        }
    }
    private List<User> getPlayersFromTeam(Team team) {
        return playerTeamRepository.findByTeam(team).stream()
                .map(PlayerTeam::getUser)
                .collect(Collectors.toList());
    }

    private void generateFirstRoundMatches(Tournament tournament) {
        List<Team> participatingTeams = new ArrayList<>(tournament.getParticipatingTeams());
        LocalDateTime startTime = tournament.getStartDate().atStartOfDay();
        for (int i = 0; i < participatingTeams.size(); i += 2) {
            Match match = new Match();
            match.setTournament(tournament);
            match.setTeam1(participatingTeams.get(i));
            match.setTeam2(participatingTeams.get(i + 1));
            match.setRoundNumber(1);
            match.setMatchNumberInRound((i / 2) + 1);
            match.setStatus(Match.MatchStatus.SCHEDULED);
            match.setStartDatetime(startTime);
            matchRepository.save(match);
        }
    }

    private void recordMatchResult(Match match, int score1, int score2) {
        match.setScoreTeam1(score1);
        match.setScoreTeam2(score2);
        match.setEndDatetime(LocalDateTime.now());
        match.setStatus(Match.MatchStatus.COMPLETED);
        match.setWinningTeam(score1 > score2 ? match.getTeam1() : match.getTeam2());
        matchRepository.save(match);
    }

    private void saveMatchStatistics(Match match, User player, int kills, int deaths, int assists) {
        MatchStatistics stats = new MatchStatistics();
        stats.setMatch(match);
        stats.setPlayer(player);
        stats.setKills(kills);
        stats.setDeaths(deaths);
        stats.setAssists(assists);
        matchStatisticsRepository.save(stats);

        PlayerStatistics playerStats = playerStatisticsRepository.findByPlayerAndGame(player, match.getTournament().getGame())
                .orElseGet(() -> {
                    PlayerStatistics newStats = new PlayerStatistics();
                    newStats.setPlayer(player);
                    newStats.setGame(match.getTournament().getGame());
                    newStats.setKills(0);
                    newStats.setDeaths(0);
                    newStats.setAssists(0);
                    newStats.setMatchesPlayed(0);
                    return newStats;
                });

        playerStats.setKills(playerStats.getKills() + kills);
        playerStats.setDeaths(playerStats.getDeaths() + deaths);
        playerStats.setAssists(playerStats.getAssists() + assists);
        playerStats.setMatchesPlayed(playerStats.getMatchesPlayed() + 1);
        playerStatisticsRepository.save(playerStats);
    }

    private Game createGame(String name) {
        Game game = new Game();
        game.setName(name);
        return gameRepository.save(game);
    }

    private User createUser(String nickname, String email, String password, Roles role) {
        User user = new User();
        user.setNickname(nickname);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setFullName(faker.name().fullName());
        user.setStatus(User.AccountStatus.ACTIVE);
        return userRepository.save(user);
    }

    private Team createTeam(String name, Game game, User leader, List<User> members) {
        Team team = new Team();
        team.setName(name);
        team.setGame(game);
        team.setLeader(leader);
        teamRepository.save(team);

        for (User member : members) {
            PlayerTeam playerTeam = new PlayerTeam();
            playerTeam.setTeam(team);
            playerTeam.setUser(member);
            playerTeam.setStartDate(LocalDate.now());
            playerTeamRepository.save(playerTeam);
        }
        return team;
    }

    private Location createLocation(String postalCode, String city, String street, int buildingNumber) {
        Location location = new Location();
        location.setPostalCode(postalCode);
        location.setCity(city);
        location.setStreet(street);
        location.setBuildingNumber(buildingNumber);
        return locationRepository.save(location);
    }

    private Tournament createTournament(String name, String description, Game game, User organizer, Location location, int maxTeams, LocalDate startDate) {
        Tournament tournament = new Tournament();
        tournament.setName(name);
        tournament.setDescription(description);
        tournament.setGame(game);
        tournament.setOrganizer(organizer);
        tournament.setLocation(location);
        tournament.setMaxTeams(maxTeams);
        tournament.setStartDate(startDate);
        tournament.setEndDate(startDate.plusDays(7));
        return tournamentRepository.save(tournament);
    }
}