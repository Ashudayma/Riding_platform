"use client";

import { Card, MetricCard, SectionTitle, StatusBadge } from "@riding-platform/ui-kit";
import { AssignmentInbox } from "../../components/assignment-inbox";
import { AvailabilityControl } from "../../components/availability-control";
import { DriverAuthGuard } from "../../components/driver-auth-guard";
import { useDriverRealtime } from "../../hooks/use-driver-realtime";
import { useDriverSession } from "../../hooks/use-driver-session";

export default function DriverHomePage() {
  const auth = useDriverSession();
  const realtime = useDriverRealtime(auth.user?.accessToken);

  return (
    <DriverAuthGuard>
      <main>
        <SectionTitle
          eyebrow="Driver App"
          title="Stay online, receive dispatches live, and manage the trip flow."
          description="This driver surface is wired for secure auth, availability updates, browser geolocation streaming, STOMP-based assignment delivery, and explicit trip execution states."
        />
        <div style={{ display: "grid", gap: 16, gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))" }}>
          <MetricCard label="Realtime status" value={realtime.connectionState} hint="Secured user-queue subscription state" />
          <MetricCard label="Live assignments" value={String(realtime.assignmentEvents.length)} hint="Assignment events retained in local inbox" />
          <MetricCard label="Ride updates" value={String(realtime.rideEvents.length)} hint="Driver ride and location events received" />
        </div>
        <div style={{ marginTop: 20, display: "grid", gap: 16, gridTemplateColumns: "1.35fr 1fr" }}>
          <AvailabilityControl />
          <Card title="Operational shortcuts">
            <div style={{ display: "grid", gap: 12 }}>
              <StatusBadge label="Realtime dispatch ready" tone="success" />
              <a href="/assignments" style={{ color: "#136545", fontWeight: 700 }}>
                Open assignment inbox
              </a>
              <a href="/earnings" style={{ color: "#136545", fontWeight: 700 }}>
                Review completed rides and earnings
              </a>
              <a href="/profile" style={{ color: "#136545", fontWeight: 700 }}>
                Inspect secure driver identity
              </a>
            </div>
          </Card>
        </div>
        <div style={{ marginTop: 20 }}>
          <AssignmentInbox />
        </div>
      </main>
    </DriverAuthGuard>
  );
}
