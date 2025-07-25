package com.tournamentmanager.backend.service;

import com.tournamentmanager.backend.dto.GameRequest;
import com.tournamentmanager.backend.dto.GameResponse;
import com.tournamentmanager.backend.exception.ConflictException;
import com.tournamentmanager.backend.exception.ResourceNotFoundException;
import com.tournamentmanager.backend.model.Game;
import com.tournamentmanager.backend.repository.GameRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GameService {

    private final GameRepository gameRepository;

    public GameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    public List<GameResponse> getAllGames(String name) {
        List<Game> games;

        if (name != null && !name.isEmpty()) {
            games = gameRepository.findByNameContainingIgnoreCase(name);
        } else {
            games = gameRepository.findAll();
        }

        return games.stream()
                .map(this::mapToGameResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public Game createGame(GameRequest gameRequest) {
        if (gameRepository.findByName(gameRequest.getName()) != null) {
            throw new ConflictException("Game with name '" + gameRequest.getName() + "' already exists.");
        }
        Game game = new Game();
        game.setName(gameRequest.getName());
        return gameRepository.save(game);
    }

    public Game findGameById(Long id) {
        return gameRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Game not found with ID: " + id));
    }

    public GameResponse getGameDtoById(Long id) {
        Game game = findGameById(id);
        return mapToGameResponse(game);
    }

    @Transactional
    public Game updateGame(Long id, GameRequest gameRequest) {
        Game existingGame = gameRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Game", "ID", id));

        if (!existingGame.getName().equals(gameRequest.getName())) {
            if (gameRepository.findByName(gameRequest.getName()) != null) {
                throw new ConflictException("Game with name '" + gameRequest.getName() + "' already exists.");
            }
        }

        existingGame.setName(gameRequest.getName());
        return gameRepository.save(existingGame);
    }



    @Transactional
    public void deleteGame(Long id) {
        if (!gameRepository.existsById(id)) {
            throw new ResourceNotFoundException("Game", "ID", id);
        }
        gameRepository.deleteById(id);
    }

    public Game findGameByName(String name) {
        return gameRepository.findByName(name);
    }


    private GameResponse mapToGameResponse(Game game) {
        return new GameResponse(game.getId(), game.getName());
    }
}