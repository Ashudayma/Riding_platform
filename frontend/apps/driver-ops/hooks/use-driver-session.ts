"use client";

import { useAuth, type AuthUser } from "@riding-platform/auth";
import { useEffect } from "react";

const DEMO_DRIVER: AuthUser = {
  subject: "demo-driver-subject",
  userProfileId: "00000000-0000-0000-0000-000000000002",
  username: "demo.driver",
  roles: ["DRIVER"],
  accessToken: "demo-driver-token",
};

export function useDriverSession() {
  const auth = useAuth();

  useEffect(() => {
    if (!auth.isLoading && !auth.user && typeof window !== "undefined") {
      const allowDemoSession = window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1";
      if (allowDemoSession) {
        auth.setSession(DEMO_DRIVER);
      }
    }
  }, [auth]);

  return auth;
}
