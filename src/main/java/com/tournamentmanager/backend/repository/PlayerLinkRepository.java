package com.tournamentmanager.backend.repository;

import com.tournamentmanager.backend.model.Link;
import com.tournamentmanager.backend.model.PlayerLink;
import com.tournamentmanager.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerLinkRepository extends JpaRepository<PlayerLink, Long> {
    List<PlayerLink> findByUser(User user);
    List<PlayerLink> findByLink(Link link);
}