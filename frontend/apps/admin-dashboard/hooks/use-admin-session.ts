"use client";

import { hasAnyRole, useAuth, type AuthUser } from "@riding-platform/auth";
import { useEffect } from "react";
import type { AdminRole } from "../lib/types";

const DEMO_ADMIN: AuthUser = {
  subject: "demo-platform-admin",
  userProfileId: "00000000-0000-0000-0000-000000000010",
  username: "demo.admin",
  roles: ["PLATFORM_ADMIN", "OPS_ADMIN"],
  accessToken: "demo-admin-token",
};

export function useAdminSession(roles?: AdminRole[]) {
  const auth = useAuth();

  useEffect(() => {
    if (!auth.isLoading && !auth.user && typeof window !== "undefined") {
      const allowDemoSession = window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1";
      if (allowDemoSession) {
        auth.setSession(DEMO_ADMIN);
      }
    }
  }, [auth]);

  return {
    ...auth,
    hasRequiredRole: roles ? hasAnyRole(auth.user, roles) : true,
  };
}
