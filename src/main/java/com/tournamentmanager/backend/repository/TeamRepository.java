package com.tournamentmanager.backend.repository;

import com.tournamentmanager.backend.model.Game;
import com.tournamentmanager.backend.model.Team;
import com.tournamentmanager.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByName(String name);
    Optional<Team> findByNameAndGame(String name, Game game);
    boolean existsByLeader(User leader);
    List<Team> findByNameContainingIgnoreCase(String name);
    @Query("SELECT DISTINCT t FROM Team t JOIN t.teamMembers pt JOIN pt.user u WHERE LOWER(u.nickname) LIKE LOWER(CONCAT('%', :playerName, '%'))")
    List<Team> findByPlayerNicknameContainingIgnoreCase(@Param("playerName") String playerName);
    @Query("SELECT t FROM Team t LEFT JOIN FETCH t.teamLinks tl LEFT JOIN FETCH tl.link WHERE t.id = :id")
    Optional<Team> findByIdWithTeamLinks(@Param("id") Long id);
}