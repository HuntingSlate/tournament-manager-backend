package com.tournamentmanager.backend.repository;

import com.tournamentmanager.backend.model.Match;
import com.tournamentmanager.backend.model.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByTournament(Tournament tournament);

    List<Match> findByStartDatetimeBetween(LocalDateTime start, LocalDateTime end);
}