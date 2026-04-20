import { SectionTitle } from "@riding-platform/ui-kit";
import { RiderAuthGuard } from "../../components/auth-guard";
import { ProfilePageContent } from "../../components/profile-page-content";

export default function RiderProfilePage() {
  return (
    <RiderAuthGuard>
      <main>
        <SectionTitle
          eyebrow="Profile"
          title="Account and security posture"
          description="Your rider identity, role grants, and secure session strategy are surfaced here through the protected rider profile endpoint."
        />
        <ProfilePageContent />
      </main>
    </RiderAuthGuard>
  );
}
