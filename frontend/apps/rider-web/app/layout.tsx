import type { Metadata } from "next";
import type { ReactNode } from "react";
import "./globals.css";
import { AppShell } from "@riding-platform/ui-kit";
import { RiderAppProviders } from "./providers";

export const metadata: Metadata = {
  title: "Rider App",
  description: "Ride booking and tracking experience for riders.",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en">
      <body>
        <RiderAppProviders>
          <AppShell
            brand="RideNow Rider"
            navItems={[
              { href: "/", label: "Home" },
              { href: "/book", label: "Book" },
              { href: "/history", label: "History" },
              { href: "/notifications", label: "Notifications" },
              { href: "/profile", label: "Profile" },
            ]}
          >
            {children}
          </AppShell>
        </RiderAppProviders>
      </body>
    </html>
  );
}
