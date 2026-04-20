"use client";

import { Button, Card, EmptyState, StatusBadge } from "@riding-platform/ui-kit";
import { useMemo, useState } from "react";
import { useDriverRealtime } from "../hooks/use-driver-realtime";
import { useDriverSession } from "../hooks/use-driver-session";
import { formatDateTime, formatMoney, statusTone } from "../lib/format";
import type { DriverAssignmentEvent } from "../lib/types";

function fallbackAssignments(): DriverAssignmentEvent[] {
  return [
    {
      rideRequestId: "sample-ride-request-1",
      rideId: "sample-ride-1",
      assignmentStatus: "DRIVER_ASSIGNED",
      pickupAddress: "Cyber City, Gurugram",
      dropAddress: "Terminal 3, IGI Airport",
      estimatedAmount: 420,
      currencyCode: "INR",
      occurredAt: new Date().toISOString(),
    },
  ];
}

export function AssignmentInbox() {
  const auth = useDriverSession();
  const realtime = useDriverRealtime(auth.user?.accessToken);
  const [decisionByRideRequest, setDecisionByRideRequest] = useState<Record<string, "accepted" | "rejected">>({});

  const assignments = useMemo(() => {
    return realtime.assignmentEvents.length > 0 ? realtime.assignmentEvents : fallbackAssignments();
  }, [realtime.assignmentEvents]);

  const markDecision = (rideRequestId: string, decision: "accepted" | "rejected") => {
    setDecisionByRideRequest((current) => ({
      ...current,
      [rideRequestId]: decision,
    }));
  };

  if (assignments.length === 0) {
    return <EmptyState title="No incoming requests" description="When dispatch pushes a nearby ride request, it will appear here in real time." />;
  }

  return (
    <div style={{ display: "grid", gap: 16 }}>
      <Card title="Assignment connection">
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <StatusBadge label={realtime.connectionState} tone={statusTone(realtime.connectionState.toUpperCase())} />
          <span style={{ color: "#61707c" }}>Live assignment events are subscribed through STOMP user queues.</span>
        </div>
      </Card>
      {assignments.map((assignment) => {
        const decision = decisionByRideRequest[assignment.rideRequestId];
        return (
          <Card key={assignment.rideRequestId} title={assignment.pickupAddress ?? assignment.rideRequestId}>
            <div style={{ display: "grid", gap: 12 }}>
              <div style={{ display: "flex", justifyContent: "space-between", gap: 12, flexWrap: "wrap", alignItems: "center" }}>
                <StatusBadge label={assignment.assignmentStatus ?? "PENDING"} tone={statusTone(assignment.assignmentStatus ?? "REQUESTED")} />
                {assignment.estimatedAmount ? (
                  <strong>{formatMoney(assignment.estimatedAmount, assignment.currencyCode ?? "INR")}</strong>
                ) : null}
              </div>
              <div>Pickup: {assignment.pickupAddress ?? "Awaiting backend payload enrichment"}</div>
              <div>Drop: {assignment.dropAddress ?? "Awaiting backend payload enrichment"}</div>
              <div style={{ color: "#61707c" }}>Event time: {assignment.occurredAt ? formatDateTime(assignment.occurredAt) : "Pending"}</div>
              <div style={{ display: "flex", gap: 12, flexWrap: "wrap" }}>
                <Button onClick={() => markDecision(assignment.rideRequestId, "accepted")}>Accept ride</Button>
                <Button variant="secondary" onClick={() => markDecision(assignment.rideRequestId, "rejected")}>
                  Reject ride
                </Button>
                <a href={`/trip/${assignment.rideRequestId}`} style={{ color: "#136545", fontWeight: 700, alignSelf: "center" }}>
                  Open trip console
                </a>
              </div>
              {decision ? (
                <p style={{ margin: 0, color: decision === "accepted" ? "#1f6a43" : "#8b4d12" }}>
                  Frontend decision recorded as {decision}. Connect this action to the forthcoming driver assignment acceptance API without changing the page contract.
                </p>
              ) : null}
            </div>
          </Card>
        );
      })}
    </div>
  );
}
