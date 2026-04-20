package com.ridingplatform.fraud.interfaces;

import com.ridingplatform.fraud.application.FraudRiskService;
import com.ridingplatform.fraud.infrastructure.persistence.FraudFlagEntity;
import com.ridingplatform.fraud.infrastructure.persistence.FraudFlagStatus;
import com.ridingplatform.fraud.infrastructure.persistence.FraudSubjectType;
import com.ridingplatform.security.application.AdminAuditService;
import com.ridingplatform.security.application.SecurityContextFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fraud")
@Tag(name = "Fraud Operations")
public class FraudOperationsController {

    private final FraudRiskService fraudRiskService;
    private final SecurityContextFacade securityContextFacade;
    private final AdminAuditService adminAuditService;

    public FraudOperationsController(
            FraudRiskService fraudRiskService,
            SecurityContextFacade securityContextFacade,
            AdminAuditService adminAuditService
    ) {
        this.fraudRiskService = fraudRiskService;
        this.securityContextFacade = securityContextFacade;
        this.adminAuditService = adminAuditService;
    }

    @GetMapping("/flags/open")
    @PreAuthorize("hasAnyRole('FRAUD_ANALYST', 'OPS_ADMIN', 'PLATFORM_ADMIN')")
    @Operation(summary = "List open and under-review fraud flags")
    public ResponseEntity<Map<String, Object>> openFlags() {
        List<FraudFlagHttpResponse> openFlags = fraudRiskService.openFlags().stream()
                .map(this::toFlagResponse)
                .toList();
        return ResponseEntity.ok(Map.of("openFlags", openFlags, "count", openFlags.size()));
    }

    @GetMapping("/flags")
    @PreAuthorize("hasAnyRole('FRAUD_ANALYST', 'OPS_ADMIN', 'PLATFORM_ADMIN')")
    @Operation(summary = "List fraud flags for a subject")
    public ResponseEntity<List<FraudFlagHttpResponse>> subjectFlags(
            @RequestParam("subjectType") FraudSubjectType subjectType,
            @RequestParam("subjectId") UUID subjectId
    ) {
        return ResponseEntity.ok(fraudRiskService.findFlags(subjectType, subjectId).stream()
                .map(this::toFlagResponse)
                .toList());
    }

    @GetMapping("/profiles/{subjectType}/{subjectId}")
    @PreAuthorize("hasAnyRole('FRAUD_ANALYST', 'OPS_ADMIN', 'PLATFORM_ADMIN')")
    @Operation(summary = "Get fraud risk profile with recent signals")
    public ResponseEntity<Map<String, Object>> profile(
            @PathVariable("subjectType") FraudSubjectType subjectType,
            @PathVariable("subjectId") UUID subjectId
    ) {
        var profile = fraudRiskService.getProfile(subjectType, subjectId);
        var signals = fraudRiskService.recentSignals(subjectType, subjectId).stream()
                .map(signal -> new FraudSignalHttpResponse(
                        signal.getId(),
                        signal.getSignalType(),
                        signal.getOccurredAt(),
                        signal.getSourceTopic(),
                        signal.getTriggeredRulesJson()
                ))
                .toList();
        return ResponseEntity.ok(Map.of(
                "profile", new FraudProfileHttpResponse(
                        profile.profileId(),
                        profile.subjectType(),
                        profile.subjectId(),
                        profile.aggregateScore(),
                        profile.riskLevel().name(),
                        profile.activeFlagCount(),
                        profile.derivedBlocked(),
                        profile.manualBlocked(),
                        profile.blocked(),
                        profile.blockedReason(),
                        profile.lastSignalAt(),
                        profile.lastAssessedAt()
                ),
                "recentSignals", signals
        ));
    }

    @PostMapping("/flags/{flagId}/review")
    @PreAuthorize("hasAnyRole('FRAUD_ANALYST', 'OPS_ADMIN', 'PLATFORM_ADMIN')")
    @Operation(summary = "Review, confirm, dismiss, resolve, or manually block a fraud flag")
    public ResponseEntity<FraudProfileHttpResponse> reviewFlag(
            @PathVariable("flagId") UUID flagId,
            @Valid @RequestBody FraudFlagReviewHttpRequest request,
            HttpServletRequest httpServletRequest
    ) {
        UUID actorUserProfileId = securityContextFacade.currentActor()
                .map(actor -> actor.userProfileId())
                .orElse(null);
        var summary = fraudRiskService.reviewFlag(
                flagId,
                request.flagStatus(),
                request.note(),
                request.manualBlock(),
                actorUserProfileId
        );
        adminAuditService.log(
                securityContextFacade.currentActor(),
                "fraud.flag.review",
                "FRAUD_FLAG",
                flagId,
                com.ridingplatform.admin.infrastructure.persistence.AuditResultStatus.SUCCESS,
                httpServletRequest.getHeader("X-Request-Id"),
                httpServletRequest.getHeader("X-Trace-Id"),
                httpServletRequest.getRemoteAddr(),
                httpServletRequest.getHeader("User-Agent"),
                "{\"targetStatus\":\"" + request.flagStatus().name() + "\"}"
        );
        return ResponseEntity.ok(new FraudProfileHttpResponse(
                summary.profileId(),
                summary.subjectType(),
                summary.subjectId(),
                summary.aggregateScore(),
                summary.riskLevel().name(),
                summary.activeFlagCount(),
                summary.derivedBlocked(),
                summary.manualBlocked(),
                summary.blocked(),
                summary.blockedReason(),
                summary.lastSignalAt(),
                summary.lastAssessedAt()
        ));
    }

    private FraudFlagHttpResponse toFlagResponse(FraudFlagEntity flag) {
        return new FraudFlagHttpResponse(
                flag.getId(),
                flag.getSubjectType(),
                flag.getSubjectId(),
                flag.getSeverity().name(),
                flag.getFlagStatus(),
                flag.getRuleCode(),
                flag.getRiskScore(),
                flag.getTitle(),
                flag.getDescription(),
                flag.getCreatedAt(),
                flag.getResolvedAt()
        );
    }
}
