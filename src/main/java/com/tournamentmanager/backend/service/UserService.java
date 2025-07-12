package com.tournamentmanager.backend.service;

import com.tournamentmanager.backend.dto.ChangePasswordRequest;
import com.tournamentmanager.backend.dto.PlayerLinkRequest;
import com.tournamentmanager.backend.dto.PlayerLinkResponse;
import com.tournamentmanager.backend.dto.UserProfileUpdateRequest;
import com.tournamentmanager.backend.dto.UserResponse;
import com.tournamentmanager.backend.exception.BadRequestException;
import com.tournamentmanager.backend.exception.ConflictException;
import com.tournamentmanager.backend.exception.ResourceNotFoundException;
import com.tournamentmanager.backend.exception.UnauthorizedException;
import com.tournamentmanager.backend.model.Link;
import com.tournamentmanager.backend.model.PlayerLink;
import com.tournamentmanager.backend.model.User;
import com.tournamentmanager.backend.repository.LinkRepository;
import com.tournamentmanager.backend.repository.PlayerLinkRepository;
import com.tournamentmanager.backend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "ID", userId));
    }

    @Transactional
    public List<PlayerLinkResponse> getUserLinks(Long userId) {
        User user = getUserById(userId);

        List<PlayerLink> playerLinks = playerLinkRepository.findByUser(user);

        return playerLinks.stream()
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
            link = linkRepository.save(link);
        }

        if (playerLinkRepository.findByUserAndLink(user, link).isPresent()) {
            throw new ConflictException("User already has this specific link.");
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
        PlayerLink playerLink = playerLinkRepository.findById(playerLinkId)
                .orElseThrow(() -> new ResourceNotFoundException("Player link", "ID", playerLinkId));

        if (!playerLink.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("This link does not belong to the specified user.");
        }

        playerLinkRepository.delete(playerLink);
    }

    @Transactional
    public PlayerLinkResponse updateUserLink(Long userId, Long playerLinkId, PlayerLinkRequest request) {
        User user = getUserById(userId);

        PlayerLink playerLinkToUpdate = playerLinkRepository.findById(playerLinkId)
                .orElseThrow(() -> new ResourceNotFoundException("Player link", "ID", playerLinkId));

        if (!playerLinkToUpdate.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("This link does not belong to the specified user.");
        }

        Optional<Link> existingLink = linkRepository.findByNameAndUrl(request.getType(), request.getUrl());
        Link link;

        if (existingLink.isPresent()) {
            link = existingLink.get();
        } else {
            link = new Link();
            link.setName(request.getType());
            link.setUrl(request.getUrl());
        }
        Optional<PlayerLink> duplicatePlayerLink = playerLinkRepository.findByUserAndLink(user, link);
        if (duplicatePlayerLink.isPresent() && !duplicatePlayerLink.get().getId().equals(playerLinkId)) {
            throw new ConflictException("User already has another link with the same type and URL.");
        }


        playerLinkToUpdate.setLink(link);
        playerLinkToUpdate.setPlatformUsername(request.getPlatformUsername());

        PlayerLink updatedPlayerLink = playerLinkRepository.save(playerLinkToUpdate);
        return mapToPlayerLinkResponse(updatedPlayerLink);
    }

    @Transactional
    public UserResponse updateUserProfile(Long userId, UserProfileUpdateRequest request, Long currentUserId) {
        User userToUpdate = getUserById(userId);

        if (!userToUpdate.getId().equals(currentUserId)) {
            throw new UnauthorizedException("You can only update your own profile.");
        }

        boolean changed = false;

        if (request.getFullName() != null && !request.getFullName().equals(userToUpdate.getFullName())) {
            userToUpdate.setFullName(request.getFullName());
            changed = true;
        }

        if (request.getNickname() != null && !request.getNickname().equals(userToUpdate.getNickname())) {
            Optional<User> existingNicknameUser = userRepository.findByNickname(request.getNickname());
            if (existingNicknameUser.isPresent() && !existingNicknameUser.get().getId().equals(userToUpdate.getId())) {
                throw new ConflictException("Nickname '" + request.getNickname() + "' is already taken.");
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
            throw new UnauthorizedException("You can only change password for your own account.");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Incorrect current password.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public List<UserResponse> searchUsersByNickname(String nickname) {
        List<User> users;
        if (nickname != null && !nickname.isEmpty()) {
            users = userRepository.findByNicknameContainingIgnoreCase(nickname);
        } else {
            users = userRepository.findAll();
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

    public UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setNickname(user.getNickname());
        response.setFullName(user.getFullName());
        response.setLinks(getUserLinks(user.getId()));
        return response;
    }
}