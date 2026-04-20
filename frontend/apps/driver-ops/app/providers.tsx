"use client";

import { AuthProvider } from "@riding-platform/auth";
import type { ReactNode } from "react";

export function DriverAppProviders({ children }: { children: ReactNode }) {
  return <AuthProvider>{children}</AuthProvider>;
}
