package com.tournamentmanager.backend.config;

import com.tournamentmanager.backend.model.User;
import com.tournamentmanager.backend.model.Roles; // Import Roles enum
import com.tournamentmanager.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class DataLoader {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    @Bean
    public CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
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
        };
    }
}