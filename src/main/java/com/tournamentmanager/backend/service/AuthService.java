package com.tournamentmanager.backend.service;

import com.tournamentmanager.backend.dto.LoginRequest;
import com.tournamentmanager.backend.dto.RegisterRequest;
import com.tournamentmanager.backend.dto.AuthResponse;
import com.tournamentmanager.backend.exception.BadRequestException;
import com.tournamentmanager.backend.exception.ConflictException;
import com.tournamentmanager.backend.exception.ResourceNotFoundException;
import com.tournamentmanager.backend.exception.UnauthorizedException;
import com.tournamentmanager.backend.model.PlayerTeam;
import com.tournamentmanager.backend.model.Tournament;
import com.tournamentmanager.backend.model.User;
import com.tournamentmanager.backend.model.Roles;
import com.tournamentmanager.backend.repository.PlayerTeamRepository;
import com.tournamentmanager.backend.repository.TeamRepository;
import com.tournamentmanager.backend.repository.UserRepository;
import com.tournamentmanager.backend.security.JwtTokenProvider;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final TeamRepository teamRepository;
    private final PlayerTeamRepository playerTeamRepository;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider, TeamRepository teamRepository,
                       PlayerTeamRepository playerTeamRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.teamRepository = teamRepository;
        this.playerTeamRepository = playerTeamRepository;
    }

    public AuthResponse register(RegisterRequest registerRequest) {
        if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            throw new ConflictException("Email is already taken!");
        }
        if (userRepository.findByNickname(registerRequest.getNickname()).isPresent()) {
            throw new ConflictException("Nickname is already taken!");
        }

        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setNickname(registerRequest.getNickname());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRole(Roles.ROLE_USER);
        user.setStatus(User.AccountStatus.ACTIVE);

        userRepository.save(user);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        registerRequest.getEmail(),
                        registerRequest.getPassword()
                )
        );
        String token = jwtTokenProvider.generateToken(authentication);

        return new AuthResponse(token, user.getId(), user.getEmail(), user.getNickname(), user.getRole());
    }

    public AuthResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtTokenProvider.generateToken(authentication);
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", loginRequest.getEmail()));

        if (user.getStatus() == User.AccountStatus.INACTIVE) {
            throw new UnauthorizedException("User account is inactive.");
        }

        return new AuthResponse(token, user.getId(), user.getEmail(), user.getNickname(), user.getRole());
    }

    @Transactional
    public void deactivateAccount(Long userId) {
        User userToDeactivate = getUserById(userId);

        if (teamRepository.existsByLeader(userToDeactivate)) {
            throw new BadRequestException("Cannot deactivate account. You are a leader of at least one team. Please transfer leadership first.");
        }

        List<PlayerTeam> memberships = playerTeamRepository.findByUser(userToDeactivate);

        if (!memberships.isEmpty()) {
            boolean isInActiveTournament = memberships.stream()
                    .map(PlayerTeam::getTeam)
                    .flatMap(team -> team.getTournaments().stream())
                    .anyMatch(tournament -> tournament.getStatus() == Tournament.TournamentStatus.ACTIVE);

            if (isInActiveTournament) {
                throw new BadRequestException("Cannot deactivate account while being a member of a team in an active tournament.");
            }

            playerTeamRepository.deleteAll(memberships);
        }

        userToDeactivate.setStatus(User.AccountStatus.INACTIVE);
        userRepository.save(userToDeactivate);
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "ID", userId));
    }
}