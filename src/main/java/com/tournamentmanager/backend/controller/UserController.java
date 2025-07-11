package com.tournamentmanager.backend.controller;

import com.tournamentmanager.backend.dto.*;
import com.tournamentmanager.backend.model.User;
import com.tournamentmanager.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(@AuthenticationPrincipal UserDetails currentUser) {
        Long userId = userService.getUserIdByEmail(currentUser.getUsername());
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(mapToUserResponse(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserProfile(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(mapToUserResponse(user));
    }

    @GetMapping("/me/links")
    public ResponseEntity<List<PlayerLinkResponse>> getMyLinks(@AuthenticationPrincipal UserDetails currentUser) {
        Long userId = userService.getUserIdByEmail(currentUser.getUsername());
        List<PlayerLinkResponse> links = userService.getUserLinks(userId);
        return ResponseEntity.ok(links);
    }

    @PostMapping("/me/links")
    public ResponseEntity<PlayerLinkResponse> addLinkToMyProfile(@Valid @RequestBody PlayerLinkRequest request,
                                                                 @AuthenticationPrincipal UserDetails currentUser) {
        Long userId = userService.getUserIdByEmail(currentUser.getUsername());
        PlayerLinkResponse response = userService.addLinkToUser(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/me/links/{playerLinkId}")
    public ResponseEntity<PlayerLinkResponse> updateLink(@PathVariable Long playerLinkId,
                                                         @Valid @RequestBody PlayerLinkRequest request,
                                                         @AuthenticationPrincipal UserDetails currentUser) {
        Long userId = userService.getUserIdByEmail(currentUser.getUsername());
        PlayerLinkResponse response = userService.updateUserLink(userId, playerLinkId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me/links/{playerLinkId}")
    public ResponseEntity<Void> deleteLinkFromMyProfile(@PathVariable Long playerLinkId,
                                                        @AuthenticationPrincipal UserDetails currentUser) {
        Long userId = userService.getUserIdByEmail(currentUser.getUsername());
        userService.deleteUserLink(userId, playerLinkId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/profile")
    public ResponseEntity<UserResponse> updateMyProfile(@Valid @RequestBody UserProfileUpdateRequest request,
                                                        @AuthenticationPrincipal UserDetails currentUser) {
        Long userId = userService.getUserIdByEmail(currentUser.getUsername());
        UserResponse response = userService.updateUserProfile(userId, request, userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changeMyPassword(@Valid @RequestBody ChangePasswordRequest request,
                                                 @AuthenticationPrincipal UserDetails currentUser) {
        Long userId = userService.getUserIdByEmail(currentUser.getUsername());
        userService.changePassword(userId, request, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserResponse>> searchUsers(
            @RequestParam(required = false) String nickname) {
        List<UserResponse> response = userService.searchUsersByNickname(nickname);
        return ResponseEntity.ok(response);
    }

    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setNickname(user.getNickname());
        response.setFullName(user.getFullName());
        response.setLinks(userService.getUserLinks(user.getId()));
        return response;
    }
}