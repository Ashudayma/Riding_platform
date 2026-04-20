import { SectionTitle } from "@riding-platform/ui-kit";
import { DriverAuthGuard } from "../../../components/driver-auth-guard";
import { TripConsole } from "../../../components/trip-console";

export default function DriverTripPage({ params }: { params: { rideId: string } }) {
  return (
    <DriverAuthGuard>
      <main>
        <SectionTitle
          eyebrow="Trip Flow"
          title={`Trip ${params.rideId}`}
          description="The driver trip console combines current ride state, stop ordering, realtime updates, and explicit pickup/drop completion controls."
        />
        <TripConsole rideId={params.rideId} />
      </main>
    </DriverAuthGuard>
  );
}
