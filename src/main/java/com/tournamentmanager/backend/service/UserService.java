package com.tournamentmanager.backend.service;

import com.tournamentmanager.backend.dto.*;
import com.tournamentmanager.backend.model.Link;
import com.tournamentmanager.backend.model.PlayerLink;
import com.tournamentmanager.backend.model.User;
import com.tournamentmanager.backend.repository.LinkRepository;
import com.tournamentmanager.backend.repository.PlayerLinkRepository;
import com.tournamentmanager.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final LinkRepository linkRepository;
    private final PlayerLinkRepository playerLinkRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       LinkRepository linkRepository,
                       PlayerLinkRepository playerLinkRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.linkRepository = linkRepository;
        this.playerLinkRepository = playerLinkRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Long getUserIdByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("User with email " + email + " not found. This should not happen for authenticated user."));
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
    }

    public List<PlayerLinkResponse> getUserLinks(Long userId) {
        User user = getUserById(userId);

        return playerLinkRepository.findByUser(user).stream()
                .map(this::mapToPlayerLinkResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PlayerLinkResponse addLinkToUser(Long userId, PlayerLinkRequest request) {
        User user = getUserById(userId);

        Optional<Link> existingLink = linkRepository.findByNameAndUrl(request.getType(), request.getUrl());
        Link link;

        if (existingLink.isPresent()) {
            link = existingLink.get();
        } else {
            link = new Link();
            link.setName(request.getType());
            link.setUrl(request.getUrl());
        }

        if (playerLinkRepository.findByUserAndLink(user, link).isPresent()) {
            throw new RuntimeException("User already has this specific link.");
        }

        PlayerLink playerLink = new PlayerLink();
        playerLink.setUser(user);
        playerLink.setLink(link);
        playerLink.setPlatformUsername(request.getPlatformUsername());

        playerLink = playerLinkRepository.save(playerLink);
        return mapToPlayerLinkResponse(playerLink);
    }

    @Transactional
    public void deleteUserLink(Long userId, Long playerLinkId) {
        User user = getUserById(userId);
        PlayerLink playerLink = playerLinkRepository.findById(playerLinkId)
                .orElseThrow(() -> new RuntimeException("Player link not found with ID: " + playerLinkId));

        if (!playerLink.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: This link does not belong to the specified user.");
        }

        playerLinkRepository.delete(playerLink);
    }

    @Transactional
    public UserResponse updateUserProfile(Long userId, UserProfileUpdateRequest request, Long currentUserId) {
        User userToUpdate = getUserById(userId);

        if (!userToUpdate.getId().equals(currentUserId)) {
            throw new RuntimeException("Unauthorized: You can only update your own profile.");
        }

        boolean changed = false;

        if (request.getFullName() != null && !request.getFullName().equals(userToUpdate.getFullName())) {
            userToUpdate.setFullName(request.getFullName());
            changed = true;
        }

        if (request.getNickname() != null && !request.getNickname().equals(userToUpdate.getNickname())) {
            if (userRepository.findByNickname(request.getNickname()).isPresent() &&
                    !userRepository.findByNickname(request.getNickname()).get().getId().equals(userToUpdate.getId())) {
                throw new RuntimeException("Nickname '" + request.getNickname() + "' is already taken.");
            }
            userToUpdate.setNickname(request.getNickname());
            changed = true;
        }

        if (changed) {
            userRepository.save(userToUpdate);
        }

        return mapToUserResponse(userToUpdate);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request, Long currentUserId) {
        User user = getUserById(userId);

        if (!user.getId().equals(currentUserId)) {
            throw new RuntimeException("Unauthorized: You can only change password for your own account.");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Incorrect current password.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public List<UserResponse> searchUsersByNickname(String nickname) {
        List<User> users;
        if (nickname != null && !nickname.isEmpty()) {
            users = userRepository.findByNicknameContainingIgnoreCase(nickname);
        } else {
            users = userRepository.findAll(); // Zwróć wszystkich, jeśli puste kryterium
        }
        return users.stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    private PlayerLinkResponse mapToPlayerLinkResponse(PlayerLink playerLink) {
        return new PlayerLinkResponse(
                playerLink.getId(),
                playerLink.getLink().getUrl(),
                playerLink.getLink().getName(),
                playerLink.getPlatformUsername(),
                playerLink.getUser().getId()
        );
    }

    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setNickname(user.getNickname());
        response.setFullName(user.getFullName());
        response.setLinks(user.getPlayerLinks().stream()
                .map(this::mapToPlayerLinkResponse)
                .collect(Collectors.toList()));
        return response;
    }
}