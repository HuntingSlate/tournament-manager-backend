package com.tournamentmanager.backend.repository;

import com.tournamentmanager.backend.model.MatchStatistics;
import com.tournamentmanager.backend.model.Match;
import com.tournamentmanager.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchStatisticsRepository extends JpaRepository<MatchStatistics, Long> {
    Optional<MatchStatistics> findByMatchAndPlayer(Match match, User player);

    List<MatchStatistics> findByMatch(Match match);
    List<MatchStatistics> findByPlayer(User player);
}