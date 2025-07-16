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
import java.util.List;
import java.util.Optional;

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

        log.info("Creating Users...");
        User admin = createUser("admin", "admin@example.com", "password", Roles.ROLE_ADMIN);
        createUser("test", "test@test.com", "test1234", Roles.ROLE_USER);
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            users.add(createUser(faker.name().username(), faker.internet().emailAddress(), "password", Roles.ROLE_USER));
        }

        log.info("Creating Teams...");
        Team teamFaze = createTeam("FaZe Clan", cs2, users.get(0), List.of(users.get(0), users.get(1), users.get(2), users.get(3), users.get(4)));
        Team teamNavi = createTeam("Natus Vincere", cs2, users.get(5), List.of(users.get(5), users.get(6), users.get(7), users.get(8), users.get(9)));
        Team teamG2 = createTeam("G2 Esports", lol, users.get(10), List.of(users.get(10), users.get(11), users.get(12), users.get(13), users.get(14)));
        Team teamFnatic = createTeam("Fnatic", lol, users.get(15), List.of(users.get(15), users.get(16), users.get(17), users.get(18), users.get(19)));

        log.info("Creating Tournaments...");

        Location iemLocation = createLocation("40-001", "Katowice", "al. Korfantego", 35);
        Tournament iem = createTournament("IEM Katowice 2026", "Major CS2 w Katowicach", cs2, admin, iemLocation, 4, LocalDate.now().plusDays(10));
        applyToTournament(teamFaze, iem);
        applyToTournament(teamNavi, iem);
        acceptApplicationForTeam(teamFaze, iem);
        acceptApplicationForTeam(teamNavi, iem);

        Tournament activeOnline = createTournament("Online Valorant Clash", "Tygodniowe starcie online", valorant, users.get(1), null, 2, LocalDate.now().minusDays(3));
        Team valorantTeamA = createTeam("Sentinels", valorant, users.get(2), List.of(users.get(2), users.get(3)));
        Team valorantTeamB = createTeam("LOUD", valorant, users.get(4), List.of(users.get(4), users.get(5)));
        applyToTournament(valorantTeamA, activeOnline);
        applyToTournament(valorantTeamB, activeOnline);
        acceptApplicationForTeam(valorantTeamA, activeOnline);
        acceptApplicationForTeam(valorantTeamB, activeOnline);

        activeOnline.setStatus(Tournament.TournamentStatus.ACTIVE);
        tournamentRepository.save(activeOnline);
        generateFirstRoundMatches(activeOnline);
        log.info("Started tournament '{}' and generated matches.", activeOnline.getName());

        Tournament finishedLol = createTournament("Summoner's Rift Championship", "Zakończony turniej LoL", lol, admin, null, 2, LocalDate.now().minusDays(30));
        applyToTournament(teamG2, finishedLol);
        applyToTournament(teamFnatic, finishedLol);
        acceptApplicationForTeam(teamG2, finishedLol);
        acceptApplicationForTeam(teamFnatic, finishedLol);

        finishedLol.setStatus(Tournament.TournamentStatus.ACTIVE);
        tournamentRepository.save(finishedLol);
        generateFirstRoundMatches(finishedLol);

        Match lolMatch = matchRepository.findByTournament(finishedLol).get(0);
        recordMatchResult(lolMatch, 2, 1);
        saveMatchStatistics(lolMatch, users.get(10), 10, 2, 5);

        finishedLol.setStatus(Tournament.TournamentStatus.COMPLETED);
        tournamentRepository.save(finishedLol);
        log.info("Finished tournament '{}' and recorded results.", finishedLol.getName());

        log.info("Database initialization finished successfully! ✅");
    }

    private void applyToTournament(Team team, Tournament tournament) {
        TeamApplication app = new TeamApplication();
        app.setTeam(team);
        app.setTournament(tournament);
        app.setApplicationDate(LocalDateTime.now());
        app.setStatus(TeamApplication.ApplicationStatus.PENDING);
        teamApplicationRepository.save(app);
    }

    private void acceptApplicationForTeam(Team team, Tournament tournament) {
        teamApplicationRepository.findByTeamAndTournament(team, tournament).ifPresent(app -> {
            app.setStatus(TeamApplication.ApplicationStatus.ACCEPTED);
            teamApplicationRepository.save(app);
            tournament.getParticipatingTeams().add(team);
            team.getTournaments().add(tournament);
            tournamentRepository.save(tournament);
        });
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