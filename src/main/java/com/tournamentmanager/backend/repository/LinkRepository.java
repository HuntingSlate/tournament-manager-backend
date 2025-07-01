package com.tournamentmanager.backend.repository;

import com.tournamentmanager.backend.model.Link;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LinkRepository extends JpaRepository<Link, Long> {
    Link findByName(String name);
}