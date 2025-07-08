package com.tournamentmanager.backend.repository;

import com.tournamentmanager.backend.model.Game;
import com.tournamentmanager.backend.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team> findByNameContainingIgnoreCase(String name);

    Optional<Team> findByNameAndGame(String name, Game game);

    @Query("SELECT DISTINCT t FROM Team t JOIN t.teamMembers pt JOIN pt.user u WHERE LOWER(u.nickname) LIKE LOWER(CONCAT('%', :playerName, '%'))")
    List<Team> findByPlayerNicknameContainingIgnoreCase(@Param("playerName") String playerName);
}