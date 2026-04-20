import { Button, Card, SectionTitle } from "@riding-platform/ui-kit";

export default function RiderLoginPage() {
  return (
    <main>
      <SectionTitle
        eyebrow="Authentication"
        title="Secure rider sign in"
        description="The production path is Keycloak-backed OIDC. This page keeps the browser entry point explicit for future social and passwordless flows."
      />
      <Card title="Continue securely">
        <p style={{ marginTop: 0, color: "#5f6d7a" }}>
          Use the Keycloak-hosted login experience to start a secure rider session. For production hardening, pair this with a backend-for-frontend token exchange and httpOnly cookies.
        </p>
        <a href="/">
          <Button>Return to rider app</Button>
        </a>
      </Card>
    </main>
  );
}
