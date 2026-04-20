import { SectionTitle } from "@riding-platform/ui-kit";
import { DriverAuthGuard } from "../../components/driver-auth-guard";
import { DriverProfilePanel } from "../../components/profile-panel";

export default function DriverProfilePage() {
  return (
    <DriverAuthGuard>
      <main>
        <SectionTitle
          eyebrow="Profile"
          title="Driver identity and session posture"
          description="Your protected identity, role grants, and operational connectivity assumptions are summarized here."
        />
        <DriverProfilePanel />
      </main>
    </DriverAuthGuard>
  );
}
