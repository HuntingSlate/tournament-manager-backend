package com.tournamentmanager.backend.repository;

import com.tournamentmanager.backend.model.TeamApplication;
import com.tournamentmanager.backend.model.Team;
import com.tournamentmanager.backend.model.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamApplicationRepository extends JpaRepository<TeamApplication, Long> {
    Optional<TeamApplication> findByTeamAndTournament(Team team, Tournament tournament);

    List<TeamApplication> findByTournament(Tournament tournament);
    List<TeamApplication> findByTeam(Team team);

    void deleteAllByTeam(Team team);
}