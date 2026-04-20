import { SectionTitle } from "@riding-platform/ui-kit";
import { AdminAuthGuard } from "../../components/admin-auth-guard";
import { RidesPageContent } from "../../components/rides-page-content";

export default function AdminRidesPage() {
  return (
    <AdminAuthGuard roles={["PLATFORM_ADMIN", "OPS_ADMIN"]}>
      <main>
        <SectionTitle
          eyebrow="Rides"
          title="Rides table with timeline inspection"
          description="Operations teams can filter rides, inspect status progression, and follow request-to-completion behavior through the admin ride APIs."
        />
        <RidesPageContent />
      </main>
    </AdminAuthGuard>
  );
}
