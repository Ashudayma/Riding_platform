package com.ridingplatform.admin.interfaces;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/support")
public class SupportOperationsController {

    @GetMapping("/rides/{rideId}")
    @PreAuthorize("hasAnyRole('SUPPORT_AGENT', 'OPS_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<Map<String, Object>> rideSupportView(
            @PathVariable UUID rideId,
            @RequestHeader(value = "X-Target-Id", required = false) String ignoredTargetId
    ) {
        return ResponseEntity.ok(Map.of(
                "rideId", rideId,
                "view", "support"
        ));
    }
}
