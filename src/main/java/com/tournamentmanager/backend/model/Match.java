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
    private Integer bracketLevel;
    @Column(nullable = false)
    private Integer matchNumberInRound;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_1_id")
    private Team firstTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_2_id")
    private Team secondTeam;

    @Column(nullable = false)
    private LocalDateTime startDatetime;

    private LocalDateTime endDatetime;

    @Column
    private Integer firstTeamScore;

    @Column
    private Integer secondTeamScore;

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