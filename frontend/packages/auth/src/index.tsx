import { defaultConfig, type AppRole } from "@riding-platform/config";
import React, { createContext, useContext, useEffect, useMemo, useState } from "react";

export type AuthUser = {
  subject: string;
  userProfileId?: string;
  username?: string;
  roles: AppRole[];
  accessToken?: string;
};

type AuthContextValue = {
  user: AuthUser | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: () => void;
  signup: () => void;
  logout: () => void;
  setSession: (user: AuthUser | null) => void;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

const AUTH_STORAGE_KEY = "riding-platform:session";

function readStoredSession(): AuthUser | null {
  if (typeof window === "undefined") {
    return null;
  }
  const raw = window.sessionStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as AuthUser;
  } catch {
    return null;
  }
}

function writeStoredSession(user: AuthUser | null): void {
  if (typeof window === "undefined") {
    return;
  }
  if (!user) {
    window.sessionStorage.removeItem(AUTH_STORAGE_KEY);
    return;
  }
  window.sessionStorage.setItem(
    AUTH_STORAGE_KEY,
    JSON.stringify({
      subject: user.subject,
      userProfileId: user.userProfileId,
      username: user.username,
      roles: user.roles,
      accessToken: user.accessToken,
    }),
  );
}

function keycloakAuthorizeUrl(mode: "login" | "signup"): string {
  const redirectUri = typeof window === "undefined" ? "" : window.location.origin;
  const url = new URL(
    `${defaultConfig.keycloakFrontendBaseUrl}/realms/${defaultConfig.keycloakRealm}/protocol/openid-connect/auth`,
  );
  url.searchParams.set("client_id", defaultConfig.keycloakClientId);
  url.searchParams.set("redirect_uri", redirectUri);
  url.searchParams.set("response_type", "code");
  url.searchParams.set("scope", "openid profile email");
  if (mode === "signup") {
    url.searchParams.set("kc_action", "register");
  }
  return url.toString();
}

export function AuthProvider({
  initialUser = null,
  children,
}: {
  initialUser?: AuthUser | null;
  children: React.ReactNode;
}) {
  const [user, setUser] = useState<AuthUser | null>(initialUser);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    setUser((current) => current ?? readStoredSession());
    setIsLoading(false);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthenticated: !!user?.accessToken,
      isLoading,
      login: () => {
        if (typeof window !== "undefined") {
          window.location.href = keycloakAuthorizeUrl("login");
        }
      },
      signup: () => {
        if (typeof window !== "undefined") {
          window.location.href = keycloakAuthorizeUrl("signup");
        }
      },
      logout: () => {
        writeStoredSession(null);
        setUser(null);
      },
      setSession: (nextUser) => {
        writeStoredSession(nextUser);
        setUser(nextUser);
      },
    }),
    [user, isLoading],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }
  return context;
}

export function hasRole(user: AuthUser | null | undefined, role: AppRole): boolean {
  return !!user?.roles.includes(role);
}

export function hasAnyRole(user: AuthUser | null | undefined, roles: AppRole[]): boolean {
  return !!user && roles.some((role) => user.roles.includes(role));
}

export function RequireRole({
  user,
  roles,
  children,
  fallback,
}: {
  user: AuthUser | null;
  roles: AppRole[];
  children: React.ReactNode;
  fallback?: React.ReactNode;
}) {
  return hasAnyRole(user, roles) ? <>{children}</> : <>{fallback ?? null}</>;
}

export function RequireAuth({
  children,
  fallback,
}: {
  children: React.ReactNode;
  fallback?: React.ReactNode;
}) {
  const { isAuthenticated, isLoading } = useAuth();
  if (isLoading) {
    return <>{fallback ?? null}</>;
  }
  return isAuthenticated ? <>{children}</> : <>{fallback ?? null}</>;
}
