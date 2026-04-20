package com.ridingplatform.security.application;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextFacade {

    public Optional<CurrentActor> currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }
        Set<String> roles = extractRoles(authentication.getAuthorities());
        UUID userProfileId = null;
        Object claim = jwt.getClaims().get("user_profile_id");
        if (claim instanceof String value && !value.isBlank()) {
            userProfileId = UUID.fromString(value);
        }
        return Optional.of(new CurrentActor(
                jwt.getSubject(),
                userProfileId,
                jwt.getClaimAsString("preferred_username"),
                roles
        ));
    }

    private Set<String> extractRoles(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .collect(Collectors.toSet());
    }
}
