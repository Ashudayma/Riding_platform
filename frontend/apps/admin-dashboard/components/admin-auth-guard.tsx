"use client";

import { Button, Card, LoadingState } from "@riding-platform/ui-kit";
import type { ReactNode } from "react";
import { useAdminSession } from "../hooks/use-admin-session";
import type { AdminRole } from "../lib/types";

export function AdminAuthGuard({
  children,
  roles = ["PLATFORM_ADMIN", "OPS_ADMIN"],
}: {
  children: ReactNode;
  roles?: AdminRole[];
}) {
  const auth = useAdminSession(roles);

  if (auth.isLoading) {
    return <LoadingState label="Restoring secure admin session..." />;
  }

  if (!auth.isAuthenticated) {
    return (
      <Card title="Admin sign in required">
        <p style={{ marginTop: 0, color: "#5f6e7b" }}>
          This control plane requires a Keycloak-backed admin session before operational and governance data is displayed.
        </p>
        <div style={{ display: "flex", gap: 12, flexWrap: "wrap" }}>
          <Button onClick={auth.login}>Continue with login</Button>
          <Button variant="secondary" onClick={auth.signup}>
            Create admin account
          </Button>
        </div>
      </Card>
    );
  }

  if (!auth.hasRequiredRole) {
    return (
      <Card title="Insufficient role">
        <p style={{ margin: 0, color: "#8b2d2a" }}>
          Your current session does not have the role grants required to open this admin view.
        </p>
      </Card>
    );
  }

  return <>{children}</>;
}
