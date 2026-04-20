export type AppRole = "RIDER" | "DRIVER" | "PLATFORM_ADMIN" | "OPS_ADMIN" | "SUPPORT_AGENT" | "FRAUD_ANALYST";

export type FrontendRuntimeConfig = {
  apiBaseUrl: string;
  websocketUrl: string;
  keycloakIssuer: string;
  keycloakClientId: string;
  keycloakRealm: string;
  keycloakFrontendBaseUrl: string;
};

export const defaultConfig: FrontendRuntimeConfig = {
  apiBaseUrl: process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080",
  websocketUrl: process.env.NEXT_PUBLIC_WS_URL ?? "http://localhost:8080/ws",
  keycloakIssuer: process.env.NEXT_PUBLIC_KEYCLOAK_ISSUER ?? "http://localhost:8081/realms/riding-platform",
  keycloakClientId: process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT_ID ?? "riding-platform-api",
  keycloakRealm: process.env.NEXT_PUBLIC_KEYCLOAK_REALM ?? "riding-platform",
  keycloakFrontendBaseUrl: process.env.NEXT_PUBLIC_KEYCLOAK_BASE_URL ?? "http://localhost:8081",
};
