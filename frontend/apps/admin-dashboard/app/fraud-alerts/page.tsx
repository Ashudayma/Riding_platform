import { SectionTitle } from "@riding-platform/ui-kit";
import { AdminAuthGuard } from "../../components/admin-auth-guard";
import { FraudAlertsPageContent } from "../../components/fraud-alerts-page-content";

export default function AdminFraudAlertsPage() {
  return (
    <AdminAuthGuard roles={["PLATFORM_ADMIN", "OPS_ADMIN", "FRAUD_ANALYST"]}>
      <main>
        <SectionTitle
          eyebrow="Fraud"
          title="Fraud alerts and risk review"
          description="Analysts and operations admins can filter rule-based alerts, inspect severity, and review flagged rider or driver subjects."
        />
        <FraudAlertsPageContent />
      </main>
    </AdminAuthGuard>
  );
}
