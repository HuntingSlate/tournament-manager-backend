package com.tournamentmanager.backend.repository;

import com.tournamentmanager.backend.model.PlayerLink;
import com.tournamentmanager.backend.model.User;
import com.tournamentmanager.backend.model.Link;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerLinkRepository extends JpaRepository<PlayerLink, Long> {
    List<PlayerLink> findByUser(User user);
    Optional<PlayerLink> findByUserAndLink(User user, Link link);
}