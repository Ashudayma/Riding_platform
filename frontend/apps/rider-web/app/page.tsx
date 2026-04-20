import { Card, MetricCard, SectionTitle, StatusBadge } from "@riding-platform/ui-kit";
import { RiderAuthGuard } from "../components/auth-guard";

export default function RiderHomePage() {
  return (
    <RiderAuthGuard>
      <main>
        <SectionTitle
          eyebrow="Rider App"
          title="Book fast, track live, and compare standard vs pooled rides."
          description="The rider experience is wired for secure auth, booking orchestration, realtime trip visibility, and a clear path to production token hardening."
        />
        <div style={{ display: "grid", gap: 16, gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))" }}>
          <MetricCard label="Ride modes" value="2" hint="Standard and shared ride flows" />
          <MetricCard label="Realtime channel" value="Live" hint="Driver location and trip state over WebSocket" />
          <MetricCard label="Booking safety" value="Idempotent" hint="Client correlation id supports resilient booking retries" />
        </div>
        <div style={{ display: "grid", gap: 16, gridTemplateColumns: "2fr 1fr", marginTop: 20 }}>
          <Card title="Next rider actions">
            <div style={{ display: "grid", gap: 12 }}>
              <a href="/book" style={{ color: "#b94f14", fontWeight: 700 }}>
                Start a new booking
              </a>
              <a href="/history" style={{ color: "#b94f14", fontWeight: 700 }}>
                Review completed and cancelled trips
              </a>
              <a href="/profile" style={{ color: "#b94f14", fontWeight: 700 }}>
                Inspect your secured rider profile
              </a>
            </div>
          </Card>
          <Card title="Shared ride readiness">
            <p style={{ marginTop: 0 }}>The frontend supports pooled selection before booking and is ready for richer matching feedback from the shared-ride engine.</p>
            <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
              <StatusBadge label="Standard" tone="warning" />
              <StatusBadge label="Shared optimized" tone="success" />
            </div>
          </Card>
        </div>
      </main>
    </RiderAuthGuard>
  );
}
