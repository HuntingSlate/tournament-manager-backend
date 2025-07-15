package com.tournamentmanager.backend.Specification;

import com.tournamentmanager.backend.model.Tournament;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TournamentSpecification {

    public static Specification<Tournament> findByCriteria(
            String name, String location, LocalDate startDate, LocalDate endDate,
            String organizerNickname, String gameName, Tournament.TournamentStatus status) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (name != null && !name.isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            if (location != null && !location.isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("location").get("city")), "%" + location.toLowerCase() + "%"));
            }
            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("startDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("startDate"), endDate));
            }
            if (organizerNickname != null && !organizerNickname.isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("organizer").get("nickname")), "%" + organizerNickname.toLowerCase() + "%"));
            }
            if (gameName != null && !gameName.isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("game").get("name")), "%" + gameName.toLowerCase() + "%"));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
