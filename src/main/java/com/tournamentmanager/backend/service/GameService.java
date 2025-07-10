package com.tournamentmanager.backend.service;

import com.tournamentmanager.backend.dto.GameResponse;
import com.tournamentmanager.backend.model.Game;
import com.tournamentmanager.backend.repository.GameRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GameService {

    private final GameRepository gameRepository;

    public GameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    public List<GameResponse> getAllGames() {
        return gameRepository.findAll().stream()
                .map(this::mapToGameResponse)
                .collect(Collectors.toList());
    }

    private GameResponse mapToGameResponse(Game game) {
        return new GameResponse(game.getId(), game.getName());
    }
}