package com.tournamentmanager.backend.repository;

import com.tournamentmanager.backend.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
    Location findByCity(String city);
    Location findByPostalCode(String postalCode);
}