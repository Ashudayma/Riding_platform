import { SectionTitle } from "@riding-platform/ui-kit";
import { AdminAuthGuard } from "../../components/admin-auth-guard";
import { DispatchPageContent } from "../../components/dispatch-page-content";

export default function DispatchPage() {
  return (
    <AdminAuthGuard roles={["PLATFORM_ADMIN", "OPS_ADMIN"]}>
      <main>
        <SectionTitle
          eyebrow="Dispatch"
          title="Dispatch and assignment stats"
          description="Assignment acceptance, rejection, timeout, and failure patterns are summarized here for dispatch performance review."
        />
        <DispatchPageContent />
      </main>
    </AdminAuthGuard>
  );
}
