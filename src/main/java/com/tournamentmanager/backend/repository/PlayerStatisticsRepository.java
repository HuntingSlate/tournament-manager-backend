package com.tournamentmanager.backend.repository;

import com.tournamentmanager.backend.model.PlayerStatistics;
import com.tournamentmanager.backend.model.Game;
import com.tournamentmanager.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerStatisticsRepository extends JpaRepository<PlayerStatistics, Long> {
    Optional<PlayerStatistics> findByPlayerAndGame(User player, Game game);

    List<PlayerStatistics> findByGame(Game game);
}