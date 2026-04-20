"use client";

import { Button, Card, LoadingState } from "@riding-platform/ui-kit";
import type { ReactNode } from "react";
import { useRiderSession } from "../hooks/use-rider-session";

export function RiderAuthGuard({ children }: { children: ReactNode }) {
  const auth = useRiderSession();

  if (auth.isLoading) {
    return <LoadingState label="Restoring secure rider session..." />;
  }

  if (!auth.isAuthenticated) {
    return (
      <Card title="Sign in required">
        <p style={{ marginTop: 0, color: "#5f6e7b" }}>
          The rider experience is protected with Keycloak-backed access tokens. Use the secure login flow to continue.
        </p>
        <div style={{ display: "flex", gap: 12, flexWrap: "wrap" }}>
          <Button onClick={auth.login}>Continue with login</Button>
          <Button variant="secondary" onClick={auth.signup}>
            Create account
          </Button>
        </div>
      </Card>
    );
  }

  return <>{children}</>;
}
