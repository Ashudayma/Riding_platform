import { SectionTitle } from "@riding-platform/ui-kit";
import { AdminAuthGuard } from "../../components/admin-auth-guard";
import { OverviewPageContent } from "../../components/overview-page-content";

export default function AdminOverviewPage() {
  return (
    <AdminAuthGuard roles={["PLATFORM_ADMIN", "OPS_ADMIN"]}>
      <main>
        <SectionTitle
          eyebrow="Admin Dashboard"
          title="Monitor rides, risk, supply, pricing, and dispatch from one control plane."
          description="The overview combines live operational metrics, governance posture, and demand-supply pressure into a production-style admin control surface."
        />
        <OverviewPageContent />
      </main>
    </AdminAuthGuard>
  );
}
