import { SectionTitle } from "@riding-platform/ui-kit";
import { RiderAuthGuard } from "../../../components/auth-guard";
import { LiveRideView } from "../../../components/live-ride-view";

export default function RideTrackingPage({ params }: { params: { rideRequestId: string } }) {
  return (
    <RiderAuthGuard>
      <main>
        <SectionTitle
          eyebrow="Live Ride"
          title={`Tracking ride ${params.rideRequestId}`}
          description="Live trip state is loaded from the backend and refreshed with websocket-delivered updates so the rider can follow assignment, arrival, and trip progress in real time."
        />
        <LiveRideView rideRequestId={params.rideRequestId} />
      </main>
    </RiderAuthGuard>
  );
}
