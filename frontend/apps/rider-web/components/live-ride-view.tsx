"use client";

import { ApiError, riderApi } from "@riding-platform/api-client";
import { Button, Card, EmptyState, LoadingState, StatusBadge } from "@riding-platform/ui-kit";
import { useEffect, useMemo, useState } from "react";
import { useRideRealtime } from "../hooks/use-ride-realtime";
import { useRiderSession } from "../hooks/use-rider-session";
import { formatDateTime, formatMoney, statusTone } from "../lib/format";
import type { RideBookingResponse } from "../lib/types";
import { RideStatusTimeline } from "./ride-status-timeline";

export function LiveRideView({ rideRequestId }: { rideRequestId: string }) {
  const auth = useRiderSession();
  const [ride, setRide] = useState<RideBookingResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isCancelling, setIsCancelling] = useState(false);

  const apiOptions = useMemo(() => ({ accessToken: auth.user?.accessToken }), [auth.user?.accessToken]);
  const realtime = useRideRealtime(auth.user?.accessToken, rideRequestId);

  useEffect(() => {
    const loadRide = async () => {
      if (!auth.user?.accessToken) {
        return;
      }
      setIsLoading(true);
      try {
        const response = await riderApi.rideDetails(rideRequestId, apiOptions);
        setRide(response as RideBookingResponse);
        setError(null);
      } catch (caught) {
        const message = caught instanceof ApiError ? `Ride details failed with status ${caught.status}.` : "Unable to load ride.";
        setError(message);
      } finally {
        setIsLoading(false);
      }
    };

    void loadRide();
  }, [auth.user?.accessToken, apiOptions, rideRequestId]);

  useEffect(() => {
    if (!realtime.lastEvent) {
      return;
    }
    setRide((current) =>
      current
        ? {
            ...current,
            status: realtime.lastEvent?.status ?? current.status,
          }
        : current,
    );
  }, [realtime.lastEvent]);

  const cancelRide = async () => {
    if (!ride) {
      return;
    }
    setIsCancelling(true);
    try {
      const response = await riderApi.cancelRide<{ cancelReason: string }, RideBookingResponse>(
        ride.rideRequestId,
        { cancelReason: "Rider requested cancellation from rider app" },
        apiOptions,
      );
      setRide(response);
    } catch (caught) {
      const message = caught instanceof ApiError ? `Cancellation failed with status ${caught.status}.` : "Unable to cancel ride.";
      setError(message);
    } finally {
      setIsCancelling(false);
    }
  };

  if (isLoading) {
    return <LoadingState label="Loading live ride details..." />;
  }

  if (error) {
    return <EmptyState title="Ride unavailable" description={error} />;
  }

  if (!ride) {
    return <EmptyState title="Ride not found" description="The requested trip could not be loaded for this rider session." />;
  }

  return (
    <div style={{ display: "grid", gap: 18, gridTemplateColumns: "minmax(0, 1.7fr) minmax(320px, 1fr)" }}>
      <Card title="Live tracking">
        <div
          style={{
            minHeight: 320,
            borderRadius: 22,
            padding: 24,
            background:
              "linear-gradient(135deg, rgba(214,107,45,0.18), rgba(255,255,255,0.6)), repeating-linear-gradient(45deg, rgba(255,255,255,0.4), rgba(255,255,255,0.4) 12px, rgba(249,233,207,0.55) 12px, rgba(249,233,207,0.55) 24px)",
            border: "1px solid rgba(0,0,0,0.08)",
            display: "grid",
            alignContent: "space-between",
          }}
        >
          <div>
            <div style={{ fontSize: 13, letterSpacing: "0.12em", textTransform: "uppercase", color: "#6c6558" }}>Realtime trip map</div>
            <h3 style={{ marginBottom: 8 }}>Driver and route updates stream here</h3>
            <p style={{ maxWidth: 560, color: "#5a6470" }}>
              Plug your preferred map renderer into this container. The page already avoids polling by listening for websocket events and only refreshing ride state when necessary.
            </p>
          </div>
          <div style={{ display: "grid", gap: 8 }}>
            <div>Pickup: {ride.stops.find((stop) => stop.stopType === "PICKUP")?.address ?? "Pending"}</div>
            <div>Drop: {ride.stops.find((stop) => stop.stopType === "DROP")?.address ?? "Pending"}</div>
            <div>
              Driver ETA:{" "}
              {typeof realtime.lastEvent?.driverEtaSeconds === "number"
                ? `${Math.round(realtime.lastEvent.driverEtaSeconds / 60)} min`
                : "Awaiting live assignment"}
            </div>
          </div>
        </div>
      </Card>
      <div style={{ display: "grid", gap: 16 }}>
        <Card title="Ride state">
          <div style={{ display: "grid", gap: 14 }}>
            <div style={{ display: "flex", justifyContent: "space-between", gap: 12, flexWrap: "wrap", alignItems: "center" }}>
              <StatusBadge label={ride.status.replaceAll("_", " ")} tone={statusTone(ride.status)} />
              <span style={{ color: "#62707c" }}>Booked {formatDateTime(ride.requestedAt)}</span>
            </div>
            <div style={{ fontSize: 28, fontWeight: 800 }}>{formatMoney(ride.estimatedTotalAmount, ride.currencyCode)}</div>
            <div style={{ color: "#5d6975" }}>Realtime connection: {realtime.connectionState}</div>
            <RideStatusTimeline status={ride.status} />
            {ride.status === "REQUESTED" || ride.status === "SEARCHING_DRIVER" || ride.status === "DRIVER_ASSIGNED" ? (
              <Button variant="ghost" onClick={cancelRide} disabled={isCancelling}>
                {isCancelling ? "Cancelling..." : "Cancel ride"}
              </Button>
            ) : null}
          </div>
        </Card>
        <Card title="Stop sequence">
          <div style={{ display: "grid", gap: 8 }}>
            {ride.stops.map((stop) => (
              <div key={stop.stopId} style={{ display: "flex", justifyContent: "space-between", gap: 12 }}>
                <span>
                  {stop.requestSequenceNo}. {stop.address}
                </span>
                <StatusBadge label={stop.stopType} tone={stop.stopType === "DROP" ? "success" : "neutral"} />
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
}
