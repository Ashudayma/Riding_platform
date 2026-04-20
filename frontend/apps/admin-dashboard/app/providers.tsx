"use client";

import { AuthProvider } from "@riding-platform/auth";
import type { ReactNode } from "react";

export function AdminAppProviders({ children }: { children: ReactNode }) {
  return <AuthProvider>{children}</AuthProvider>;
}
