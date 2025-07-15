package com.tournamentmanager.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "match")
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Column(nullable = false)
    private Integer roundNumber;

    @Column(nullable = false)
    private Integer matchNumberInRound;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_1_id")
    private Team team1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_2_id")
    private Team team2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prev_match_1_id")
    private Match prevMatch1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prev_match_2_id")
    private Match prevMatch2;

    @Column(nullable = false)
    private LocalDateTime startDatetime;

    private LocalDateTime endDatetime;

    @Column
    private Integer scoreTeam1;

    @Column
    private Integer scoreTeam2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winning_team_id")
    private Team winningTeam;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status;

    public enum MatchStatus {
        SCHEDULED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }
}