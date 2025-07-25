package com.tournamentmanager.backend.repository;

import com.tournamentmanager.backend.model.PlayerTeam;
import com.tournamentmanager.backend.model.Team;
import com.tournamentmanager.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerTeamRepository extends JpaRepository<PlayerTeam, Long> {
    List<PlayerTeam> findByTeam(Team team);
    List<PlayerTeam> findByUser(User user);
    long countByTeam(Team team);
    Optional<PlayerTeam> findByTeamAndUser(Team team, User user);
}