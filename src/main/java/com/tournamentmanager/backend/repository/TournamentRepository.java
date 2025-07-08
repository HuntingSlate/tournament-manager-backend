package com.tournamentmanager.backend.repository;

import com.tournamentmanager.backend.model.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {
    List<Tournament> findByNameContainingIgnoreCase(String name);
    List<Tournament> findByLocationCityContainingIgnoreCase(String city);
    List<Tournament> findByStartDateBetween(LocalDate startDate, LocalDate endDate);
    List<Tournament> findByOrganizerNicknameContainingIgnoreCase(String organizerNickname);
    List<Tournament> findByNameContainingIgnoreCaseAndLocationCityContainingIgnoreCase(String name, String city);
}