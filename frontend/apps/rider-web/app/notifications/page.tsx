import { SectionTitle } from "@riding-platform/ui-kit";
import { RiderAuthGuard } from "../../components/auth-guard";
import { NotificationsPanel } from "../../components/notifications-panel";

export default function RiderNotificationsPage() {
  return (
    <RiderAuthGuard>
      <main>
        <SectionTitle
          eyebrow="Notifications"
          title="Updates and alerts"
          description="Ride lifecycle and account updates are delivered through the notification subsystem and surfaced here for the rider."
        />
        <NotificationsPanel />
      </main>
    </RiderAuthGuard>
  );
}
