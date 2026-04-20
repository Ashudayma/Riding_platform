package com.ridingplatform.rating.infrastructure.persistence;

import com.ridingplatform.common.persistence.AbstractJpaEntity;
import com.ridingplatform.identity.infrastructure.persistence.UserProfileEntity;
import com.ridingplatform.ride.infrastructure.persistence.RideEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "rating_review", schema = "rating")
public class RatingReviewEntity extends AbstractJpaEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ride_id", nullable = false)
    private RideEntity ride;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewer_user_profile_id", nullable = false)
    private UserProfileEntity reviewerUserProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewed_user_profile_id", nullable = false)
    private UserProfileEntity reviewedUserProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "reviewer_type", nullable = false, length = 32)
    private ReviewerType reviewerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "reviewed_type", nullable = false, length = 32)
    private ReviewerType reviewedType;

    @Column(name = "rating_value", nullable = false)
    private short ratingValue;

    @Column(name = "review_text", length = 1000)
    private String reviewText;

    @Column(name = "tags_json", columnDefinition = "jsonb")
    private String tagsJson;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;
}
