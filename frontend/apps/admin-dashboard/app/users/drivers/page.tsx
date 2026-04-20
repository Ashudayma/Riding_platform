import { SectionTitle } from "@riding-platform/ui-kit";
import { AdminAuthGuard } from "../../../components/admin-auth-guard";
import { ProfileManagementPage } from "../../../components/profile-management-page";

export default function AdminDriversPage() {
  return (
    <AdminAuthGuard roles={["PLATFORM_ADMIN", "OPS_ADMIN", "SUPPORT_AGENT"]}>
      <main>
        <SectionTitle
          eyebrow="Drivers"
          title="Driver management"
          description="Operations can review driver status, risk posture, and take manual block or unblock actions directly from the dashboard."
        />
        <ProfileManagementPage subject="drivers" />
      </main>
    </AdminAuthGuard>
  );
}
