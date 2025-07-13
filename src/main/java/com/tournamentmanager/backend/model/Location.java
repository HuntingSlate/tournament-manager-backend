package com.tournamentmanager.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "location")
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 10)
    private String postalCode;

    @Column(length = 50)
    private String city;

    @Column(length = 50)
    private String street;

    private Integer buildingNumber;

    @Column(nullable = true)
    private Double latitude;

    @Column(nullable = true)
    private Double longitude;

}