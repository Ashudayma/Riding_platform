package com.ridingplatform.config;

import com.ridingplatform.security.web.CorrelationIdFilter;
import com.ridingplatform.security.web.ErrorResponseWriter;
import com.ridingplatform.security.web.IdempotencyFilter;
import com.ridingplatform.security.web.RateLimitingFilter;
import com.ridingplatform.security.web.SecureRequestLoggingFilter;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.access.AccessDeniedHandler;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    private final CorrelationIdFilter correlationIdFilter;
    private final SecureRequestLoggingFilter secureRequestLoggingFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final IdempotencyFilter idempotencyFilter;
    private final ErrorResponseWriter errorResponseWriter;

    public SecurityConfiguration(
            CorrelationIdFilter correlationIdFilter,
            SecureRequestLoggingFilter secureRequestLoggingFilter,
            RateLimitingFilter rateLimitingFilter,
            IdempotencyFilter idempotencyFilter,
            ErrorResponseWriter errorResponseWriter
    ) {
        this.correlationIdFilter = correlationIdFilter;
        this.secureRequestLoggingFilter = secureRequestLoggingFilter;
        this.rateLimitingFilter = rateLimitingFilter;
        this.idempotencyFilter = idempotencyFilter;
        this.errorResponseWriter = errorResponseWriter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health/**",
                                "/actuator/prometheus",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/api/v1/system/health"
                        ).permitAll()
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("PLATFORM_ADMIN", "OPS_ADMIN")
                        .requestMatchers("/api/v1/support/**").hasAnyRole("SUPPORT_AGENT", "OPS_ADMIN", "PLATFORM_ADMIN")
                        .requestMatchers("/api/v1/fraud/**").hasAnyRole("FRAUD_ANALYST", "OPS_ADMIN", "PLATFORM_ADMIN")
                        .requestMatchers("/api/v1/notifications/**").hasAnyRole("RIDER", "DRIVER", "SUPPORT_AGENT", "FRAUD_ANALYST", "OPS_ADMIN", "PLATFORM_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/pricing/estimate").hasRole("RIDER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/pricing/*/finalize").hasAnyRole("SUPPORT_AGENT", "OPS_ADMIN", "PLATFORM_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/driver/search/nearby").hasAnyRole("SUPPORT_AGENT", "OPS_ADMIN", "PLATFORM_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/driver/**").hasAnyRole("DRIVER", "SUPPORT_AGENT", "OPS_ADMIN", "PLATFORM_ADMIN")
                        .requestMatchers("/api/v1/driver/**").hasAnyRole("DRIVER", "OPS_ADMIN")
                        .requestMatchers("/api/v1/shared-rides/**").hasAnyRole("RIDER", "OPS_ADMIN", "PLATFORM_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/rider/**").hasAnyRole("RIDER", "SUPPORT_AGENT", "OPS_ADMIN", "PLATFORM_ADMIN")
                        .requestMatchers("/api/v1/rider/**").hasAnyRole("RIDER", "OPS_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/rides/**").hasAnyRole("RIDER", "OPS_ADMIN", "PLATFORM_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/rides/**").hasAnyRole("RIDER", "DRIVER", "SUPPORT_AGENT", "FRAUD_ANALYST", "OPS_ADMIN", "PLATFORM_ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler(accessDeniedHandler()))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(secureRequestLoggingFilter, CorrelationIdFilter.class)
                .addFilterAfter(rateLimitingFilter, SecureRequestLoggingFilter.class)
                .addFilterAfter(idempotencyFilter, RateLimitingFilter.class);
        return http.build();
    }

    @Bean
    public Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return jwt -> new JwtAuthenticationToken(jwt, extractAuthorities(jwt));
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) ->
                errorResponseWriter.write(request, response, HttpStatus.FORBIDDEN, "Access denied");
    }

    @SuppressWarnings("unchecked")
    private Collection<? extends GrantedAuthority> extractAuthorities(Jwt jwt) {
        Set<String> authorities = new HashSet<>();
        var realmAccess = (Map<String, Object>) jwt.getClaims().getOrDefault("realm_access", Map.of());
        var roles = (Collection<String>) realmAccess.getOrDefault("roles", List.of());
        roles.stream()
                .map(role -> "ROLE_" + role.toUpperCase().replace('-', '_'))
                .forEach(authorities::add);
        var resourceAccess = (Map<String, Object>) jwt.getClaims().getOrDefault("resource_access", Map.of());
        resourceAccess.values().stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(entry -> (Collection<String>) entry.getOrDefault("roles", List.of()))
                .flatMap(Collection::stream)
                .map(role -> "ROLE_" + role.toUpperCase().replace('-', '_'))
                .forEach(authorities::add);
        var scopeClaim = jwt.getClaimAsString("scope");
        if (scopeClaim != null && !scopeClaim.isBlank()) {
            for (String scope : scopeClaim.split(" ")) {
                authorities.add("SCOPE_" + scope);
            }
        }
        return authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }
}
