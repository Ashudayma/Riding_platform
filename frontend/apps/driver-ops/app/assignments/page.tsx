import { SectionTitle } from "@riding-platform/ui-kit";
import { AssignmentInbox } from "../../components/assignment-inbox";
import { DriverAuthGuard } from "../../components/driver-auth-guard";

export default function DriverAssignmentsPage() {
  return (
    <DriverAuthGuard>
      <main>
        <SectionTitle
          eyebrow="Assignments"
          title="Incoming ride requests"
          description="Low-latency assignment cards are subscribed from the secured driver queue so acceptance and rejection workflows can stay fast and isolated from the rest of the app."
        />
        <AssignmentInbox />
      </main>
    </DriverAuthGuard>
  );
}
