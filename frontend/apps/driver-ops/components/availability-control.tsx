"use client";

import { ApiError, driverApi } from "@riding-platform/api-client";
import { Button, Card, Field, SelectInput, StatusBadge, TextInput } from "@riding-platform/ui-kit";
import { useMemo, useState } from "react";
import { useLocationStreamer } from "../hooks/use-location-streamer";
import { useDriverSession } from "../hooks/use-driver-session";
import { formatDateTime, statusTone } from "../lib/format";
import type { DriverAvailabilityResponse } from "../lib/types";

function defaultSessionId(): string {
  return `driver-session-${Date.now()}`;
}

export function AvailabilityControl() {
  const auth = useDriverSession();
  const [sessionId] = useState(defaultSessionId);
  const [availability, setAvailability] = useState<DriverAvailabilityResponse | null>(null);
  const [seatCount, setSeatCount] = useState("4");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const apiOptions = useMemo(() => ({ accessToken: auth.user?.accessToken }), [auth.user?.accessToken]);
  const isOnline = availability?.onlineStatus === "ONLINE";
  const { lastLocation, locationError } = useLocationStreamer(auth.user?.accessToken, isOnline);

  const updateAvailability = async (online: boolean) => {
    setIsSubmitting(true);
    try {
      const response = await driverApi.updateAvailability<
        {
          online: boolean;
          availableSeatCount: number;
          sessionId: string;
          appVersion: string;
          devicePlatform: string;
        },
        DriverAvailabilityResponse
      >(
        {
          online,
          availableSeatCount: Number(seatCount),
          sessionId,
          appVersion: "driver-web-0.1.0",
          devicePlatform: "WEB",
        },
        apiOptions,
      );
      setAvailability(response);
      setError(null);
    } catch (caught) {
      setError(caught instanceof ApiError ? `Availability update failed with status ${caught.status}.` : "Availability update failed.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Card title="Availability and live location">
      <div style={{ display: "grid", gap: 14 }}>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <StatusBadge label={availability?.onlineStatus ?? "OFFLINE"} tone={statusTone(availability?.onlineStatus ?? "OFFLINE")} />
          <StatusBadge label={availability?.availabilityStatus ?? "UNAVAILABLE"} tone={statusTone(availability?.availabilityStatus ?? "UNAVAILABLE")} />
        </div>
        <div style={{ display: "grid", gap: 12, gridTemplateColumns: "repeat(auto-fit, minmax(160px, 1fr))" }}>
          <Field label="Available seats">
            <SelectInput value={seatCount} onChange={(event) => setSeatCount(event.target.value)}>
              {[1, 2, 3, 4, 5, 6].map((count) => (
                <option key={count} value={count}>
                  {count}
                </option>
              ))}
            </SelectInput>
          </Field>
          <Field label="Session ID">
            <TextInput value={sessionId} readOnly />
          </Field>
        </div>
        <div style={{ display: "flex", gap: 12, flexWrap: "wrap" }}>
          <Button onClick={() => updateAvailability(true)} disabled={isSubmitting}>
            {isSubmitting && !isOnline ? "Switching..." : "Go online"}
          </Button>
          <Button variant="secondary" onClick={() => updateAvailability(false)} disabled={isSubmitting}>
            {isSubmitting && isOnline ? "Switching..." : "Go offline"}
          </Button>
        </div>
        {availability ? (
          <div style={{ color: "#5d6975" }}>Last heartbeat: {formatDateTime(availability.lastHeartbeatAt)}</div>
        ) : null}
        {lastLocation ? (
          <div style={{ color: "#5d6975" }}>
            Live location synced at {formatDateTime(lastLocation.observedAt)} from {lastLocation.latitude.toFixed(5)}, {lastLocation.longitude.toFixed(5)}
          </div>
        ) : null}
        {error ? <p style={{ margin: 0, color: "#8b2d2a" }}>{error}</p> : null}
        {locationError ? <p style={{ margin: 0, color: "#8b4d12" }}>{locationError}</p> : null}
      </div>
    </Card>
  );
}
