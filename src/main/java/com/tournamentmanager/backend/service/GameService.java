package com.tournamentmanager.backend.service;

import com.tournamentmanager.backend.dto.GameRequest;
import com.tournamentmanager.backend.dto.GameResponse;
import com.tournamentmanager.backend.model.Game;
import com.tournamentmanager.backend.repository.GameRepository;
import jakarta.persistence.EntityNotFoundException;
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

    public Game createGame(GameRequest gameRequest) {
        Game game = new Game();
        game.setName(gameRequest.getName());
        return gameRepository.save(game);
    }

    public Game getGameById(Long id) {
        return gameRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Game not found with ID: " + id));
    }

    public Game updateGame(Long id, GameRequest gameRequest) {
        return gameRepository.findById(id)
                .map(existingGame -> {
                    existingGame.setName(gameRequest.getName());
                    return gameRepository.save(existingGame);
                })
                .orElseThrow(() -> new EntityNotFoundException("Game not found with ID: " + id));
    }

    public void deleteGame(Long id) {
        if (!gameRepository.existsById(id)) {
            throw new EntityNotFoundException("Game not found with ID: " + id);
        }
        gameRepository.deleteById(id);
    }


    private GameResponse mapToGameResponse(Game game) {
        return new GameResponse(game.getId(), game.getName());
    }
}