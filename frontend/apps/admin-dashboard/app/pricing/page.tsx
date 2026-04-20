import { SectionTitle } from "@riding-platform/ui-kit";
import { AdminAuthGuard } from "../../components/admin-auth-guard";
import { PricingPageContent } from "../../components/pricing-page-content";

export default function AdminPricingPage() {
  return (
    <AdminAuthGuard roles={["PLATFORM_ADMIN", "OPS_ADMIN"]}>
      <main>
        <SectionTitle
          eyebrow="Pricing"
          title="Pricing configuration"
          description="Pricing rule sets can be filtered by city and active state, then activated or deactivated directly from the dashboard."
        />
        <PricingPageContent />
      </main>
    </AdminAuthGuard>
  );
}
