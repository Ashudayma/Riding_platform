package com.ridingplatform.rider.interfaces;

import com.ridingplatform.security.application.SecurityContextFacade;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rider")
public class RiderProfileController {

    private final SecurityContextFacade securityContextFacade;

    public RiderProfileController(SecurityContextFacade securityContextFacade) {
        this.securityContextFacade = securityContextFacade;
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('RIDER', 'SUPPORT_AGENT', 'OPS_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<Map<String, Object>> me() {
        var actor = securityContextFacade.currentActor().orElseThrow();
        return ResponseEntity.ok(Map.of(
                "subject", actor.subject(),
                "userProfileId", actor.userProfileId(),
                "roles", actor.roles()
        ));
    }
}
