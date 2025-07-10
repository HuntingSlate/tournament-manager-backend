package com.tournamentmanager.backend.controller;

import com.tournamentmanager.backend.dto.GameResponse;
import com.tournamentmanager.backend.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping
    public ResponseEntity<List<GameResponse>> getAllGames() {
        List<GameResponse> games = gameService.getAllGames();
        return ResponseEntity.ok(games);
    }
}