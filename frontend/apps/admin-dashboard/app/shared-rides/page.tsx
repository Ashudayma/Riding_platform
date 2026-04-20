import { SectionTitle } from "@riding-platform/ui-kit";
import { AdminAuthGuard } from "../../components/admin-auth-guard";
import { SharedRidesPageContent } from "../../components/shared-rides-page-content";

export default function SharedRidesPage() {
  return (
    <AdminAuthGuard roles={["PLATFORM_ADMIN", "OPS_ADMIN"]}>
      <main>
        <SectionTitle
          eyebrow="Shared Rides"
          title="Pooling performance"
          description="Seat utilization, group completion, and rider savings are surfaced here for shared ride optimization review."
        />
        <SharedRidesPageContent />
      </main>
    </AdminAuthGuard>
  );
}
