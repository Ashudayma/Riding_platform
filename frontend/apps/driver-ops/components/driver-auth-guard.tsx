"use client";

import { Button, Card, LoadingState } from "@riding-platform/ui-kit";
import type { ReactNode } from "react";
import { useDriverSession } from "../hooks/use-driver-session";

export function DriverAuthGuard({ children }: { children: ReactNode }) {
  const auth = useDriverSession();

  if (auth.isLoading) {
    return <LoadingState label="Restoring secure driver session..." />;
  }

  if (!auth.isAuthenticated) {
    return (
      <Card title="Driver sign in required">
        <p style={{ marginTop: 0, color: "#5f6e7b" }}>
          This operations surface expects a Keycloak-backed driver session before availability, assignments, and live trip actions are enabled.
        </p>
        <div style={{ display: "flex", gap: 12, flexWrap: "wrap" }}>
          <Button onClick={auth.login}>Continue with login</Button>
          <Button variant="secondary" onClick={auth.signup}>
            Create driver account
          </Button>
        </div>
      </Card>
    );
  }

  return <>{children}</>;
}
