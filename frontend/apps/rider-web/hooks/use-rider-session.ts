"use client";

import { useAuth, type AuthUser } from "@riding-platform/auth";
import { useEffect } from "react";

const DEMO_RIDER: AuthUser = {
  subject: "demo-rider-subject",
  userProfileId: "00000000-0000-0000-0000-000000000001",
  username: "demo.rider",
  roles: ["RIDER"],
  accessToken: "demo-access-token",
};

export function useRiderSession() {
  const auth = useAuth();

  useEffect(() => {
    if (!auth.isLoading && !auth.user && typeof window !== "undefined") {
      const allowDemoSession = window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1";
      if (allowDemoSession) {
        auth.setSession(DEMO_RIDER);
      }
    }
  }, [auth]);

  return auth;
}
