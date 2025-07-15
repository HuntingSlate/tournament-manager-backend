package com.tournamentmanager.backend.repository;

import com.tournamentmanager.backend.model.TeamLink;
import com.tournamentmanager.backend.model.Team;
import com.tournamentmanager.backend.model.Link;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamLinkRepository extends JpaRepository<TeamLink, Long> {
    List<TeamLink> findByTeam(Team team);
    Optional<TeamLink> findByTeamAndLink(Team team, Link link);
}