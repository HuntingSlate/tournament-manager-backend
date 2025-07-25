package com.tournamentmanager.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "player")
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 40)
    private String email;

    @Column(nullable = false, length = 128)
    private String password;

    @Column(unique = true, nullable = false, length = 40)
    private String nickname;

    @Column(length = 50)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Roles role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PlayerTeam> playerTeams = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PlayerLink> playerLinks = new HashSet<>();

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PlayerStatistics> playerStatistics = new HashSet<>();

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MatchStatistics> matchStatisticsEntries = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    public User(String email, String password, String nickname, String fullName) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.fullName = fullName;
        this.role = Roles.ROLE_USER;
        this.status = AccountStatus.ACTIVE;
    }
    public enum AccountStatus {
        ACTIVE,
        INACTIVE
    }

}