package com.ridingplatform.rating.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatingReviewJpaRepository extends JpaRepository<RatingReviewEntity, UUID> {

    List<RatingReviewEntity> findByRideIdOrderBySubmittedAtAsc(UUID rideId);
}
