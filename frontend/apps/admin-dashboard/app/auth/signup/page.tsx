import { Button, Card, SectionTitle } from "@riding-platform/ui-kit";

export default function AdminSignupPage() {
  return (
    <main>
      <SectionTitle
        eyebrow="Authentication"
        title="Provision an admin account"
        description="This route keeps the identity entrypoint explicit for controlled admin onboarding and future access-governance workflows."
      />
      <Card title="Admin onboarding">
        <p style={{ marginTop: 0, color: "#5f6d7a" }}>
          Complete registration through the identity provider, then return with role grants appropriate for your admin responsibilities.
        </p>
        <a href="/overview">
          <Button variant="secondary">Return to dashboard</Button>
        </a>
      </Card>
    </main>
  );
}
