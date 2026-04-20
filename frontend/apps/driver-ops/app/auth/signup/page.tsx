import { Button, Card, SectionTitle } from "@riding-platform/ui-kit";

export default function DriverSignupPage() {
  return (
    <main>
      <SectionTitle
        eyebrow="Authentication"
        title="Create a driver account"
        description="This route is kept explicit so the driver app can expand into onboarding, verification, and role-based identity flows without route churn."
      />
      <Card title="Driver onboarding">
        <p style={{ marginTop: 0, color: "#5f6d7a" }}>
          Complete registration in the identity provider, then return to the driver app with the same secure session boundary.
        </p>
        <a href="/home">
          <Button variant="secondary">Return to driver app</Button>
        </a>
      </Card>
    </main>
  );
}
