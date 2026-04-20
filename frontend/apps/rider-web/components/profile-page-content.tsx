"use client";

import { ApiError, riderApi } from "@riding-platform/api-client";
import { Card, EmptyState, LoadingState, MetricCard } from "@riding-platform/ui-kit";
import { useEffect, useMemo, useState } from "react";
import { useRiderSession } from "../hooks/use-rider-session";
import type { RiderProfile } from "../lib/types";
import { ProfileSummaryCard } from "./profile-summary-card";

export function ProfilePageContent() {
  const auth = useRiderSession();
  const [profile, setProfile] = useState<RiderProfile | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const apiOptions = useMemo(() => ({ accessToken: auth.user?.accessToken }), [auth.user?.accessToken]);

  useEffect(() => {
    const load = async () => {
      if (!auth.user?.accessToken) {
        return;
      }
      setIsLoading(true);
      try {
        const response = await riderApi.profile(apiOptions);
        setProfile(response as RiderProfile);
        setError(null);
      } catch (caught) {
        const message = caught instanceof ApiError ? `Profile failed with status ${caught.status}.` : "Unable to load profile.";
        setError(message);
      } finally {
        setIsLoading(false);
      }
    };

    void load();
  }, [apiOptions, auth.user?.accessToken]);

  if (isLoading) {
    return <LoadingState label="Loading rider profile..." />;
  }

  if (error) {
    return <EmptyState title="Profile unavailable" description={error} />;
  }

  if (!profile) {
    return <EmptyState title="Profile not found" description="The authenticated rider profile could not be resolved." />;
  }

  return (
    <div style={{ display: "grid", gap: 16 }}>
      <ProfileSummaryCard profile={profile} />
      <div style={{ display: "grid", gap: 16, gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))" }}>
        <MetricCard label="Secure session" value="JWT" hint="Short-lived access token plus refresh-ready Keycloak flow" />
        <MetricCard label="Realtime" value="Socket" hint="Ride state and driver location are streamed without page polling" />
        <MetricCard label="Account role" value="Rider" hint="Role-based access checks are enforced server-side" />
      </div>
      <Card title="Token handling strategy">
        <p style={{ margin: 0, color: "#5d6975" }}>
          This frontend foundation keeps the access token in session storage for local development and isolates auth logic behind a provider. For production hardening, move token exchange to a backend-for-frontend or secure httpOnly cookie boundary while preserving the same client interface.
        </p>
      </Card>
    </div>
  );
}
