package com.tournamentmanager.backend.repository;

import com.tournamentmanager.backend.model.Link;
import com.tournamentmanager.backend.model.Team;
import com.tournamentmanager.backend.model.TeamLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamLinkRepository extends JpaRepository<TeamLink, Long> {
    List<TeamLink> findByTeam(Team team);
    List<TeamLink> findByLink(Link link);
}