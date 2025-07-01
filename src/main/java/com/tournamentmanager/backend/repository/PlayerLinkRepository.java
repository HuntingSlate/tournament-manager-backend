package com.tournamentmanager.backend.repository;

import com.tournamentmanager.backend.model.PlayerLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerLinkRepository extends JpaRepository<PlayerLink, Long> {
    List<PlayerLink> findByUser(com.tournamentmanager.backend.model.User user);
    List<PlayerLink> findByLink(com.tournamentmanager.backend.model.Link link);
}