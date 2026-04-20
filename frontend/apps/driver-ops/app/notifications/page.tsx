import { SectionTitle } from "@riding-platform/ui-kit";
import { DriverAuthGuard } from "../../components/driver-auth-guard";
import { DriverNotificationsPanel } from "../../components/notifications-panel";

export default function DriverNotificationsPage() {
  return (
    <DriverAuthGuard>
      <main>
        <SectionTitle
          eyebrow="Notifications"
          title="Driver alerts and ride updates"
          description="Operational and ride lifecycle notifications are surfaced here from the shared notification subsystem."
        />
        <DriverNotificationsPanel />
      </main>
    </DriverAuthGuard>
  );
}
