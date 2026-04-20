import { SectionTitle } from "@riding-platform/ui-kit";
import { AdminAuthGuard } from "../../components/admin-auth-guard";
import { AuditLogsPageContent } from "../../components/audit-logs-page-content";

export default function AuditLogsPage() {
  return (
    <AdminAuthGuard roles={["PLATFORM_ADMIN", "OPS_ADMIN"]}>
      <main>
        <SectionTitle
          eyebrow="Audit"
          title="Admin audit log viewer"
          description="Critical admin actions are filterable by target type so governance and change history stay visible to operations leadership."
        />
        <AuditLogsPageContent />
      </main>
    </AdminAuthGuard>
  );
}
