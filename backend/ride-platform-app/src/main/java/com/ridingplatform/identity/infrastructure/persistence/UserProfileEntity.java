package com.ridingplatform.identity.infrastructure.persistence;

import com.ridingplatform.common.persistence.AbstractJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_profile", schema = "identity")
public class UserProfileEntity extends AbstractJpaEntity {

    @Column(name = "keycloak_user_id", nullable = false, updatable = false)
    private UUID keycloakUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false, length = 32)
    private UserStatus userStatus;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(name = "phone_country_code", length = 8)
    private String phoneCountryCode;

    @Column(name = "phone_number", length = 32)
    private String phoneNumber;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "display_name", length = 150)
    private String displayName;

    @Column(name = "profile_photo_url", length = 500)
    private String profilePhotoUrl;

    @Column(name = "country_code", length = 3)
    private String countryCode;

    @Column(length = 64)
    private String timezone;

    @Column(name = "default_locale", length = 16)
    private String defaultLocale;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private String metadataJson;
}
