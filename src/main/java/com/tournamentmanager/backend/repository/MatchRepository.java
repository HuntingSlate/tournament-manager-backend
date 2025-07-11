package com.tournamentmanager.backend.repository;

import com.tournamentmanager.backend.model.Match;
import com.tournamentmanager.backend.model.Tournament;
import com.tournamentmanager.backend.model.Match.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByTournament(Tournament tournament);

    List<Match> findByStartDatetimeBetween(LocalDateTime start, LocalDateTime end);

    List<Match> findByTournamentAndRoundNumber(Tournament tournament, Integer roundNumber);

    List<Match> findByTournamentAndStatus(Tournament tournament, MatchStatus status);

    Optional<Match> findByPrevMatch1OrPrevMatch2(Match prevMatch1, Match prevMatch2);
}