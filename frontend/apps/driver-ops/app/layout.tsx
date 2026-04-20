import type { Metadata } from "next";
import type { ReactNode } from "react";
import "./globals.css";
import { AppShell } from "@riding-platform/ui-kit";
import { DriverAppProviders } from "./providers";

export const metadata: Metadata = {
  title: "Driver Ops",
  description: "Driver operations console",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en">
      <body>
        <DriverAppProviders>
          <AppShell
            brand="RideNow Driver"
            navItems={[
              { href: "/home", label: "Home" },
              { href: "/assignments", label: "Assignments" },
              { href: "/earnings", label: "Earnings" },
              { href: "/notifications", label: "Notifications" },
              { href: "/profile", label: "Profile" },
            ]}
          >
            {children}
          </AppShell>
        </DriverAppProviders>
      </body>
    </html>
  );
}
