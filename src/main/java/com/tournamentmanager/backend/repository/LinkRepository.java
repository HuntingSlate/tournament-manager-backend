package com.tournamentmanager.backend.repository;

import com.tournamentmanager.backend.model.Link;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LinkRepository extends JpaRepository<Link, Long> {
    Optional<Link> findByUrl(String url);
    Optional<Link> findByNameAndUrl(String name, String url);
}