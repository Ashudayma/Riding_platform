import { Button, Card, SectionTitle } from "@riding-platform/ui-kit";

export default function DriverLoginPage() {
  return (
    <main>
      <SectionTitle
        eyebrow="Authentication"
        title="Secure driver sign in"
        description="The production path is a Keycloak-backed OIDC login that can later support device trust, passwordless flows, and social identity if needed."
      />
      <Card title="Continue securely">
        <p style={{ marginTop: 0, color: "#5f6d7a" }}>
          Use the hosted identity flow to obtain a secure driver session before availability and trip actions are enabled.
        </p>
        <a href="/home">
          <Button>Return to driver app</Button>
        </a>
      </Card>
    </main>
  );
}
