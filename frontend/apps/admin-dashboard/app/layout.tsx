import type { Metadata } from "next";
import type { ReactNode } from "react";
import "./globals.css";
import { AppShell } from "@riding-platform/ui-kit";
import { AdminAppProviders } from "./providers";

export const metadata: Metadata = {
  title: "Admin Dashboard",
  description: "Operational and governance dashboard for the ride platform.",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en">
      <body>
        <AdminAppProviders>
          <AppShell
            brand="RideNow Admin"
            navItems={[
              { href: "/overview", label: "Overview" },
              { href: "/rides", label: "Rides" },
              { href: "/users/riders", label: "Riders" },
              { href: "/users/drivers", label: "Drivers" },
              { href: "/fraud-alerts", label: "Fraud" },
              { href: "/pricing", label: "Pricing" },
              { href: "/metrics", label: "Metrics" },
              { href: "/shared-rides", label: "Shared" },
              { href: "/dispatch", label: "Dispatch" },
              { href: "/audit-logs", label: "Audit" },
            ]}
            sidebar
          >
            {children}
          </AppShell>
        </AdminAppProviders>
      </body>
    </html>
  );
}
