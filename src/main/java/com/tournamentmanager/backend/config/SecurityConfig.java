package com.tournamentmanager.backend.config;

import com.tournamentmanager.backend.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        // Publiczne endpointy
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tournaments/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/teams/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/matches/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/statistics/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/search").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll()

                        // User:
                        .requestMatchers(HttpMethod.PUT, "/api/users/me/profile").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/users/me/password").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/users/me/links").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/users/me/links/{playerLinkId}").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users/me/**").authenticated()

                        // Tournaments:
                        .requestMatchers(HttpMethod.POST, "/api/tournaments").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/tournaments/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/tournaments/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/tournaments/{tournamentId}/applications").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/tournaments/{tournamentId}/applications/{applicationId}/status").authenticated()


                        // Teams:
                        .requestMatchers(HttpMethod.POST, "/api/teams").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/teams/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/teams/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/teams/{teamId}/members/{memberId}").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/teams/{teamId}/members/{memberId}").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/teams/**/apply/**").authenticated()

                        // Matches:
                        .requestMatchers(HttpMethod.POST, "/api/matches").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/matches/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/matches/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/matches/**/statistics").authenticated()

                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}