import { SectionTitle } from "@riding-platform/ui-kit";
import { AdminAuthGuard } from "../../../components/admin-auth-guard";
import { ProfileManagementPage } from "../../../components/profile-management-page";

export default function AdminRidersPage() {
  return (
    <AdminAuthGuard roles={["PLATFORM_ADMIN", "OPS_ADMIN", "SUPPORT_AGENT"]}>
      <main>
        <SectionTitle
          eyebrow="Riders"
          title="Rider management"
          description="Support and operations teams can inspect rider profiles, filter by status, and block or unblock accounts when required."
        />
        <ProfileManagementPage subject="riders" />
      </main>
    </AdminAuthGuard>
  );
}
