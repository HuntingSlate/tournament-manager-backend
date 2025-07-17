package com.tournamentmanager.backend.repository;

import com.tournamentmanager.backend.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    Game findByName(String name);
    List<Game> findByNameContainingIgnoreCase(String name);
}