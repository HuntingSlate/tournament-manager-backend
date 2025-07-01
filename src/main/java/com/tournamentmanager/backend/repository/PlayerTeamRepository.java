package com.tournamentmanager.backend.repository;

import com.tournamentmanager.backend.model.PlayerTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerTeamRepository extends JpaRepository<PlayerTeam, Long> {
    List<PlayerTeam> findByTeam(com.tournamentmanager.backend.model.Team team);
    List<PlayerTeam> findByUser(com.tournamentmanager.backend.model.User user);
}