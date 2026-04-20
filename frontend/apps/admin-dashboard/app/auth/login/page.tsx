import { Button, Card, SectionTitle } from "@riding-platform/ui-kit";

export default function AdminLoginPage() {
  return (
    <main>
      <SectionTitle
        eyebrow="Authentication"
        title="Secure admin sign in"
        description="The admin dashboard is designed for a Keycloak-backed OIDC flow with role-based access checks for platform, ops, support, and fraud teams."
      />
      <Card title="Continue securely">
        <p style={{ marginTop: 0, color: "#5f6d7a" }}>
          Use the hosted identity flow to obtain an admin session before protected dashboard routes are unlocked.
        </p>
        <a href="/overview">
          <Button>Return to dashboard</Button>
        </a>
      </Card>
    </main>
  );
}
