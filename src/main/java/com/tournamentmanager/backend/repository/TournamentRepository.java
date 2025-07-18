package com.tournamentmanager.backend.repository;

import com.tournamentmanager.backend.model.Team;
import com.tournamentmanager.backend.model.TeamApplication;
import com.tournamentmanager.backend.model.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long>, JpaSpecificationExecutor<Tournament> {
    List<Tournament> findByNameContainingIgnoreCase(String name);
    List<Tournament> findByLocationCityContainingIgnoreCase(String city);
    List<Tournament> findByStartDateBetween(LocalDate startDate, LocalDate endDate);
    List<Tournament> findByOrganizerNicknameContainingIgnoreCase(String organizerNickname);
    Optional<Tournament> findByName(String tournamentName);
    @Query("SELECT t FROM Tournament t LEFT JOIN FETCH t.participatingTeams pt WHERE t.id = :id")
    Optional<Tournament> findByIdWithParticipatingTeams(@Param("id") Long id);
    List<Tournament> findByNameContainingIgnoreCaseAndLocationCityContainingIgnoreCase(String name, String city);
    Optional<Tournament> findByParticipatingTeamsContains(Team team);
    Optional<Tournament> findByIdAndParticipatingTeamsContains(Long tournamentId, Team team);
}