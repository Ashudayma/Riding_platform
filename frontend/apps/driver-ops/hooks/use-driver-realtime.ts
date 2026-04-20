"use client";

import { connectStompWithReconnect } from "@riding-platform/realtime";
import { useEffect, useState } from "react";
import type { DriverAssignmentEvent, DriverRideRealtimeEvent } from "../lib/types";

export function useDriverRealtime(accessToken: string | undefined) {
  const [assignmentEvents, setAssignmentEvents] = useState<DriverAssignmentEvent[]>([]);
  const [rideEvents, setRideEvents] = useState<DriverRideRealtimeEvent[]>([]);
  const [connectionState, setConnectionState] = useState<"idle" | "connecting" | "connected" | "disconnected">("idle");

  useEffect(() => {
    if (!accessToken) {
      return;
    }

    setConnectionState("connecting");

    const connection = connectStompWithReconnect({
      accessToken,
      onOpen: () => setConnectionState("connected"),
      onClose: () => setConnectionState("disconnected"),
      onError: () => setConnectionState("disconnected"),
    });

    const assignmentSubscription = connection.subscribe("/user/queue/assignments", (payload) => {
      if (payload && typeof payload === "object") {
        setAssignmentEvents((current) => [payload as DriverAssignmentEvent, ...current].slice(0, 20));
      }
    });

    const rideSubscription = connection.subscribe("/user/queue/driver-rides", (payload) => {
      if (payload && typeof payload === "object") {
        setRideEvents((current) => [payload as DriverRideRealtimeEvent, ...current].slice(0, 50));
      }
    });

    return () => {
      assignmentSubscription.unsubscribe();
      rideSubscription.unsubscribe();
      connection.disconnect();
    };
  }, [accessToken]);

  return {
    assignmentEvents,
    rideEvents,
    connectionState,
  };
}
