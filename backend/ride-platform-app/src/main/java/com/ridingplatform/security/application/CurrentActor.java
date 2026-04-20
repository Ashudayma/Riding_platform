package com.ridingplatform.security.application;

import java.util.Set;
import java.util.UUID;

public record CurrentActor(
        String subject,
        UUID userProfileId,
        String username,
        Set<String> roles
) {

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
