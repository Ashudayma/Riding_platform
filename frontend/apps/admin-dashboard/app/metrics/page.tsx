import { SectionTitle } from "@riding-platform/ui-kit";
import { AdminAuthGuard } from "../../components/admin-auth-guard";
import { MetricsPageContent } from "../../components/metrics-page-content";

export default function AdminMetricsPage() {
  return (
    <AdminAuthGuard roles={["PLATFORM_ADMIN", "OPS_ADMIN"]}>
      <main>
        <SectionTitle
          eyebrow="Metrics"
          title="Operational metrics"
          description="The metrics workspace combines throughput, shared-ride behavior, and dispatch outcomes into chart-ready admin panels."
        />
        <MetricsPageContent />
      </main>
    </AdminAuthGuard>
  );
}
