"use client";

import { ApiError, driverApi } from "@riding-platform/api-client";
import { Card, EmptyState, LoadingState, MetricCard, StatusBadge } from "@riding-platform/ui-kit";
import { useEffect, useMemo, useState } from "react";
import { useDriverSession } from "../hooks/use-driver-session";
import type { DriverProfile } from "../lib/types";

export function DriverProfilePanel() {
  const auth = useDriverSession();
  const [profile, setProfile] = useState<DriverProfile | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const apiOptions = useMemo(() => ({ accessToken: auth.user?.accessToken }), [auth.user?.accessToken]);

  useEffect(() => {
    const load = async () => {
      if (!auth.user?.accessToken) {
        return;
      }
      try {
        const response = await driverApi.me(apiOptions);
        setProfile(response as DriverProfile);
        setError(null);
      } catch (caught) {
        setError(caught instanceof ApiError ? `Profile failed with status ${caught.status}.` : "Unable to load driver profile.");
      } finally {
        setIsLoading(false);
      }
    };

    void load();
  }, [apiOptions, auth.user?.accessToken]);

  if (isLoading) {
    return <LoadingState label="Loading driver profile..." />;
  }

  if (error) {
    return <EmptyState title="Profile unavailable" description={error} />;
  }

  if (!profile) {
    return <EmptyState title="Profile not found" description="The authenticated driver profile could not be resolved." />;
  }

  return (
    <div style={{ display: "grid", gap: 16 }}>
      <Card title="Driver identity">
        <div style={{ display: "grid", gap: 12 }}>
          <div>
            <div style={{ fontSize: 28, fontWeight: 800 }}>{profile.subject}</div>
            <div style={{ color: "#61707c" }}>User profile ID: {profile.userProfileId}</div>
          </div>
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
            {profile.roles.map((role) => (
              <StatusBadge key={role} label={role} tone={role === "DRIVER" ? "success" : "neutral"} />
            ))}
          </div>
        </div>
      </Card>
      <div style={{ display: "grid", gap: 16, gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))" }}>
        <MetricCard label="Auth model" value="OIDC" hint="Keycloak-backed JWT session for driver operations" />
        <MetricCard label="Realtime" value="STOMP" hint="Assignments and trip updates are subscribed over secured user queues" />
        <MetricCard label="Location sync" value="GPS" hint="Browser geolocation is streamed only while driver is online" />
      </div>
    </div>
  );
}
