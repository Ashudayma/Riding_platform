import { SectionTitle } from "@riding-platform/ui-kit";
import { RiderAuthGuard } from "../../components/auth-guard";
import { BookingFlow } from "../../components/booking-flow";

export default function RiderBookPage() {
  return (
    <RiderAuthGuard>
      <main>
        <SectionTitle
          eyebrow="Booking"
          title="Request a ride with standard or pooled pricing."
          description="Pickup and drop are mandatory, optional stops are supported, and booking uses the same auth and API layer that will power production mobile and web rider flows."
        />
        <BookingFlow />
      </main>
    </RiderAuthGuard>
  );
}
