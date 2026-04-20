package com.ridingplatform.notification.interfaces;

import com.ridingplatform.notification.application.NotificationApplicationService;
import com.ridingplatform.notification.application.NotificationNotFoundException;
import com.ridingplatform.security.application.SecurityContextFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationApplicationService notificationApplicationService;
    private final SecurityContextFacade securityContextFacade;

    public NotificationController(
            NotificationApplicationService notificationApplicationService,
            SecurityContextFacade securityContextFacade
    ) {
        this.notificationApplicationService = notificationApplicationService;
        this.securityContextFacade = securityContextFacade;
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('RIDER', 'DRIVER', 'SUPPORT_AGENT', 'OPS_ADMIN', 'PLATFORM_ADMIN', 'FRAUD_ANALYST')")
    @Operation(summary = "Get in-app notification history for the authenticated user")
    public ResponseEntity<List<NotificationHttpResponse>> myNotifications() {
        return ResponseEntity.ok(notificationApplicationService.listForUser(currentUserProfileId()).stream()
                .map(view -> new NotificationHttpResponse(
                        view.notificationId(),
                        view.rideId(),
                        view.notificationType(),
                        view.eventCode(),
                        view.channel(),
                        view.deliveryStatus(),
                        view.title(),
                        view.body(),
                        view.sentAt(),
                        view.deliveredAt(),
                        view.readAt(),
                        view.failureReason()
                ))
                .toList());
    }

    @PatchMapping("/{notificationId}/read")
    @PreAuthorize("hasAnyRole('RIDER', 'DRIVER', 'SUPPORT_AGENT', 'OPS_ADMIN', 'PLATFORM_ADMIN', 'FRAUD_ANALYST')")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<NotificationHttpResponse> markRead(@PathVariable("notificationId") UUID notificationId) {
        var view = notificationApplicationService.markRead(notificationId, currentUserProfileId());
        return ResponseEntity.ok(new NotificationHttpResponse(
                view.notificationId(),
                view.rideId(),
                view.notificationType(),
                view.eventCode(),
                view.channel(),
                view.deliveryStatus(),
                view.title(),
                view.body(),
                view.sentAt(),
                view.deliveredAt(),
                view.readAt(),
                view.failureReason()
        ));
    }

    @GetMapping("/preferences")
    @PreAuthorize("hasAnyRole('RIDER', 'DRIVER', 'SUPPORT_AGENT', 'OPS_ADMIN', 'PLATFORM_ADMIN', 'FRAUD_ANALYST')")
    @Operation(summary = "Get notification preferences for the authenticated user")
    public ResponseEntity<List<NotificationPreferenceHttpResponse>> preferences() {
        return ResponseEntity.ok(notificationApplicationService.listPreferences(currentUserProfileId()).stream()
                .map(preference -> new NotificationPreferenceHttpResponse(
                        preference.preferenceId(),
                        preference.eventCode(),
                        preference.channel(),
                        preference.enabled()
                ))
                .toList());
    }

    @PutMapping("/preferences")
    @PreAuthorize("hasAnyRole('RIDER', 'DRIVER', 'SUPPORT_AGENT', 'OPS_ADMIN', 'PLATFORM_ADMIN', 'FRAUD_ANALYST')")
    @Operation(summary = "Update notification preference for the authenticated user")
    public ResponseEntity<NotificationPreferenceHttpResponse> updatePreference(@Valid @RequestBody NotificationPreferenceHttpRequest request) {
        var preference = notificationApplicationService.updatePreference(currentUserProfileId(), request.eventCode(), request.channel(), request.enabled());
        return ResponseEntity.ok(new NotificationPreferenceHttpResponse(
                preference.preferenceId(),
                preference.eventCode(),
                preference.channel(),
                preference.enabled()
        ));
    }

    private UUID currentUserProfileId() {
        UUID userProfileId = securityContextFacade.currentActor()
                .orElseThrow(() -> new NotificationNotFoundException("Authenticated actor not found"))
                .userProfileId();
        if (userProfileId == null) {
            throw new NotificationNotFoundException("user_profile_id claim is required");
        }
        return userProfileId;
    }
}
