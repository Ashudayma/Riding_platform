import { Button, Card, SectionTitle } from "@riding-platform/ui-kit";

export default function RiderSignupPage() {
  return (
    <main>
      <SectionTitle
        eyebrow="Authentication"
        title="Create a rider account"
        description="This route is reserved for Keycloak registration, passwordless entry, and future social identity providers without changing the rider app page structure."
      />
      <Card title="Registration flow">
        <p style={{ marginTop: 0, color: "#5f6d7a" }}>
          Complete signup through the identity provider, then return to the rider app with the same secure token handling contract.
        </p>
        <a href="/">
          <Button variant="secondary">Return to rider app</Button>
        </a>
      </Card>
    </main>
  );
}
