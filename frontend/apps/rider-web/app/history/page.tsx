import { SectionTitle } from "@riding-platform/ui-kit";
import { RiderAuthGuard } from "../../components/auth-guard";
import { HistoryPageContent } from "../../components/history-page-content";

export default function RiderHistoryPage() {
  return (
    <RiderAuthGuard>
      <main>
        <SectionTitle
          eyebrow="History"
          title="Recent rides"
          description="Past bookings, outcomes, and amount paid are loaded from the ride history API with clear loading and failure states."
        />
        <HistoryPageContent />
      </main>
    </RiderAuthGuard>
  );
}
