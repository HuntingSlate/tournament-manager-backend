package com.tournamentmanager.backend.config;

import com.tournamentmanager.backend.model.User;
import com.tournamentmanager.backend.model.Roles;
import com.tournamentmanager.backend.model.Game;
import com.tournamentmanager.backend.dto.GameRequest;
import com.tournamentmanager.backend.repository.UserRepository;
import com.tournamentmanager.backend.service.GameService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Arrays;

@Configuration
public class DataLoader {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    @Bean
    public CommandLineRunner initDatabase(UserRepository userRepository,
                                          PasswordEncoder passwordEncoder,
                                          GameService gameService) {
        return args -> {
            if (userRepository.findByEmail("admin@example.com").isEmpty()) {
                log.info("Creating default ADMIN user...");

                User adminUser = new User();
                adminUser.setEmail("admin@example.com");
                adminUser.setNickname("Admin");
                adminUser.setPassword(passwordEncoder.encode("AdminPass123!"));
                adminUser.setFullName("Administrator Account");
                adminUser.setRole(Roles.ROLE_ADMIN);

                userRepository.save(adminUser);
                log.info("Default ADMIN user created: " + adminUser.getEmail());
            } else {
                log.info("ADMIN user already exists.");
            }

            List<String> defaultGameNames = Arrays.asList(
                    "Dota 2", "League of Legends", "Counter-Strike 2", "Valorant",
                    "Rainbow Six: Siege", "Overwatch 2", "Apex Legends", "Fortnite",
                    "Rocket League", "StarCraft II"
            );

            log.info("Checking and creating default games...");
            for (String gameName : defaultGameNames) {
                Game existingGame = gameService.findGameByName(gameName);

                if (existingGame == null) {
                    GameRequest gameRequest = new GameRequest();
                    gameRequest.setName(gameName);
                    gameService.createGame(gameRequest);
                    log.info("Created default game: " + gameName);
                } else {
                    log.info("Game already exists: " + gameName);
                }
            }
        };
    }
}