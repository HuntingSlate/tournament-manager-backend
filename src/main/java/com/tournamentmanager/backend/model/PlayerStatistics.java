package com.tournamentmanager.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "player_statistics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer kills;
    private Integer deaths;
    private Integer assists;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private User player;

    private Double averageKills;
    private Double averageDeaths;
    private Double averageAssists;

    private Integer matchesPlayed;

    @PrePersist
    @PreUpdate
    public void calculateAverages() {
        if (matchesPlayed == null || matchesPlayed == 0) {
            this.averageKills = 0.0;
            this.averageDeaths = 0.0;
            this.averageAssists = 0.0;
        } else {
            this.averageKills = (double) kills / matchesPlayed;
            this.averageDeaths = (double) deaths / matchesPlayed;
            this.averageAssists = (double) assists / matchesPlayed;
        }
    }
}