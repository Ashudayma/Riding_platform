import { SectionTitle } from "@riding-platform/ui-kit";
import { DriverAuthGuard } from "../../components/driver-auth-guard";
import { EarningsOverview } from "../../components/earnings-overview";

export default function DriverEarningsPage() {
  return (
    <DriverAuthGuard>
      <main>
        <SectionTitle
          eyebrow="Earnings"
          title="Daily and weekly earnings"
          description="Completed rides and summary metrics are centralized here, with a clean seam for future payout, incentives, and reconciliation APIs."
        />
        <EarningsOverview />
      </main>
    </DriverAuthGuard>
  );
}
