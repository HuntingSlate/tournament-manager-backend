package com.tournamentmanager.backend.repository;

import com.tournamentmanager.backend.model.Match;
import com.tournamentmanager.backend.model.Tournament;
import com.tournamentmanager.backend.model.Match.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByTournament(Tournament tournament);

    Optional<Match> findByPrevMatch1OrPrevMatch2(Match prevMatch1, Match prevMatch2);

    List<Match> findByTournamentAndRoundNumber(Tournament tournament, int roundNumber);

    @Query("SELECT DISTINCT m FROM Match m " +
            "LEFT JOIN m.tournament t " +
            "LEFT JOIN t.game g " +
            "LEFT JOIN m.team1 t1 " +
            "LEFT JOIN m.team2 t2 " +
            "LEFT JOIN t1.teamMembers pt1 " +
            "LEFT JOIN pt1.user u1 " +
            "LEFT JOIN t2.teamMembers pt2 " +
            "LEFT JOIN pt2.user u2 " +
            "WHERE (:tournamentId IS NULL OR t.id = :tournamentId) " +
            "AND (:tournamentName IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :tournamentName, '%'))) " +
            "AND (:gameId IS NULL OR g.id = :gameId) " +
            "AND (:gameName IS NULL OR LOWER(g.name) LIKE LOWER(CONCAT('%', :gameName, '%'))) " +
            "AND (:teamId IS NULL OR t1.id = :teamId OR t2.id = :teamId) " +
            "AND (:teamName IS NULL OR LOWER(t1.name) LIKE LOWER(CONCAT('%', :teamName, '%')) OR LOWER(t2.name) LIKE LOWER(CONCAT('%', :teamName, '%'))) " +
            "AND (:playerId IS NULL OR u1.id = :playerId OR u2.id = :playerId) " +
            "AND (:playerName IS NULL OR LOWER(u1.nickname) LIKE LOWER(CONCAT('%', :playerName, '%')) OR LOWER(u2.nickname) LIKE LOWER(CONCAT('%', :playerName, '%')))")
    List<Match> searchMatches(
            @Param("tournamentId") Long tournamentId,
            @Param("tournamentName") String tournamentName,
            @Param("gameId") Long gameId,
            @Param("gameName") String gameName,
            @Param("teamId") Long teamId,
            @Param("teamName") String teamName,
            @Param("playerId") Long playerId,
            @Param("playerName") String playerName
    );
}