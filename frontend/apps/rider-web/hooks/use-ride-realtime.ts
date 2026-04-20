"use client";

import { connectRealtimeWithReconnect } from "@riding-platform/realtime";
import { useEffect, useState } from "react";
import type { RideRealtimeSnapshot } from "../lib/types";

export function useRideRealtime(accessToken: string | undefined, rideRequestId: string) {
  const [lastEvent, setLastEvent] = useState<RideRealtimeSnapshot | null>(null);
  const [connectionState, setConnectionState] = useState<"idle" | "connecting" | "connected" | "disconnected">("idle");

  useEffect(() => {
    if (!accessToken || !rideRequestId) {
      return;
    }

    setConnectionState("connecting");

    const connection = connectRealtimeWithReconnect({
      accessToken,
      onOpen: () => setConnectionState("connected"),
      onClose: () => setConnectionState("disconnected"),
      onMessage: (payload) => {
        if (!payload || typeof payload !== "object") {
          return;
        }
        const event = payload as Record<string, unknown>;
        const eventRideRequestId = String(event.rideRequestId ?? event.aggregateId ?? "");
        if (eventRideRequestId && eventRideRequestId !== rideRequestId) {
          return;
        }
        setLastEvent({
          rideRequestId,
          status: typeof event.status === "string" ? (event.status as RideRealtimeSnapshot["status"]) : undefined,
          driverLatitude: typeof event.driverLatitude === "number" ? event.driverLatitude : undefined,
          driverLongitude: typeof event.driverLongitude === "number" ? event.driverLongitude : undefined,
          driverHeadingDegrees: typeof event.driverHeadingDegrees === "number" ? event.driverHeadingDegrees : undefined,
          driverEtaSeconds: typeof event.driverEtaSeconds === "number" ? event.driverEtaSeconds : undefined,
          messageType: typeof event.messageType === "string" ? event.messageType : undefined,
          occurredAt: typeof event.occurredAt === "string" ? event.occurredAt : undefined,
        });
      },
    });

    return () => connection.disconnect();
  }, [accessToken, rideRequestId]);

  return {
    lastEvent,
    connectionState,
  };
}
