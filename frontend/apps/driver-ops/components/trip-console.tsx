"use client";

import { ApiError, driverApi } from "@riding-platform/api-client";
import { Button, Card, EmptyState, LoadingState, StatusBadge } from "@riding-platform/ui-kit";
import { useEffect, useMemo, useState } from "react";
import { useDriverRealtime } from "../hooks/use-driver-realtime";
import { useDriverSession } from "../hooks/use-driver-session";
import { formatMoney, statusTone } from "../lib/format";
import type { DriverTripState, RideStatus } from "../lib/types";

type TripRecord = {
  rideRequestId: string;
  rideId: string | null;
  rideType: string;
  status: RideStatus;
  estimatedTotalAmount: number;
  currencyCode: string;
  stops: Array<{
    stopId: string;
    stopType: string;
    requestSequenceNo: number;
    address: string;
  }>;
};

const progression: DriverTripState[] = ["EN_ROUTE_PICKUP", "ARRIVED_PICKUP", "TRIP_STARTED", "DROPPED_OFF", "COMPLETED"];

export function TripConsole({ rideId }: { rideId: string }) {
  const auth = useDriverSession();
  const realtime = useDriverRealtime(auth.user?.accessToken);
  const [trip, setTrip] = useState<TripRecord | null>(null);
  const [tripState, setTripState] = useState<DriverTripState>("EN_ROUTE_PICKUP");
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
        const response = await driverApi.rideDetails(rideId, apiOptions);
        setTrip(response as TripRecord);
        setError(null);
      } catch (caught) {
        setError(caught instanceof ApiError ? `Trip details failed with status ${caught.status}.` : "Trip details are unavailable.");
      } finally {
        setIsLoading(false);
      }
    };

    void load();
  }, [apiOptions, auth.user?.accessToken, rideId]);

  useEffect(() => {
    const event = realtime.rideEvents.find((item) => item.rideId === rideId || item.rideRequestId === rideId);
    if (event?.status && trip) {
      setTrip({
        ...trip,
        status: event.status,
      });
    }
  }, [realtime.rideEvents, rideId, trip]);

  if (isLoading) {
    return <LoadingState label="Loading trip console..." />;
  }

  if (error) {
    return <EmptyState title="Trip unavailable" description={error} />;
  }

  if (!trip) {
    return <EmptyState title="Trip not found" description="The requested trip could not be resolved for this driver session." />;
  }

  const currentStep = progression.indexOf(tripState);

  return (
    <div style={{ display: "grid", gap: 18, gridTemplateColumns: "minmax(0, 1.7fr) minmax(320px, 1fr)" }}>
      <Card title="Trip progress">
        <div style={{ display: "grid", gap: 14 }}>
          <div style={{ display: "flex", justifyContent: "space-between", gap: 12, flexWrap: "wrap", alignItems: "center" }}>
            <StatusBadge label={trip.status.replaceAll("_", " ")} tone={statusTone(trip.status)} />
            <strong>{formatMoney(trip.estimatedTotalAmount, trip.currencyCode)}</strong>
          </div>
          {progression.map((step, index) => (
            <div key={step} style={{ display: "flex", gap: 12, alignItems: "center" }}>
              <div
                style={{
                  width: 12,
                  height: 12,
                  borderRadius: 999,
                  background: index <= currentStep ? "#1f8b63" : "#d7dce3",
                  boxShadow: index <= currentStep ? "0 0 0 6px rgba(31,139,99,0.12)" : "none",
                }}
              />
              <span>{step.replaceAll("_", " ")}</span>
            </div>
          ))}
          <div style={{ display: "flex", gap: 12, flexWrap: "wrap" }}>
            {progression.map((step) => (
              <Button key={step} variant={step === tripState ? "primary" : "ghost"} onClick={() => setTripState(step)}>
                {step.replaceAll("_", " ")}
              </Button>
            ))}
          </div>
          <p style={{ margin: 0, color: "#61707c" }}>
            The trip console is ready for backend ride-state transition endpoints. Until those APIs land, the UI keeps the operational state machine explicit and isolated behind this component.
          </p>
        </div>
      </Card>
      <div style={{ display: "grid", gap: 16 }}>
        <Card title="Stop sequence">
          <div style={{ display: "grid", gap: 8 }}>
            {trip.stops.map((stop) => (
              <div key={stop.stopId} style={{ display: "flex", justifyContent: "space-between", gap: 12 }}>
                <span>
                  {stop.requestSequenceNo}. {stop.address}
                </span>
                <StatusBadge label={stop.stopType} tone={stop.stopType === "DROP" ? "success" : "neutral"} />
              </div>
            ))}
          </div>
        </Card>
        <Card title="Realtime feed">
          <div style={{ display: "grid", gap: 8 }}>
            <StatusBadge label={realtime.connectionState} tone={statusTone(realtime.connectionState.toUpperCase())} />
            {realtime.rideEvents.slice(0, 3).map((event, index) => (
              <div key={`${event.rideId ?? event.rideRequestId ?? "event"}-${index}`} style={{ color: "#61707c" }}>
                {event.messageType ?? "RIDE_EVENT"} {event.status ? `- ${event.status}` : ""}
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
}
