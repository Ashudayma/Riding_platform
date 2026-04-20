import { defaultConfig } from "@riding-platform/config";

export type ApiClientOptions = {
  accessToken?: string;
  correlationId?: string;
};

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly payload?: unknown,
  ) {
    super(message);
  }
}

export async function apiRequest<T>(
  path: string,
  init: RequestInit = {},
  options: ApiClientOptions = {},
): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set("Content-Type", "application/json");
  if (options.accessToken) {
    headers.set("Authorization", `Bearer ${options.accessToken}`);
  }
  if (options.correlationId) {
    headers.set("X-Correlation-Id", options.correlationId);
  }

  const response = await fetch(`${defaultConfig.apiBaseUrl}${path}`, {
    ...init,
    headers,
    cache: "no-store",
  });

  if (!response.ok) {
    let payload: unknown = null;
    try {
      payload = await response.json();
    } catch {
      payload = await response.text();
    }
    throw new ApiError(`Request failed for ${path}`, response.status, payload);
  }

  return response.json() as Promise<T>;
}

export const adminApi = {
  overview: (options?: ApiClientOptions) => apiRequest("/api/v1/admin/overview", { method: "GET" }, options),
  rides: (query = "", options?: ApiClientOptions) => apiRequest(`/api/v1/admin/rides${query}`, { method: "GET" }, options),
  ride: (rideRequestId: string, options?: ApiClientOptions) =>
    apiRequest(`/api/v1/admin/rides/${rideRequestId}`, { method: "GET" }, options),
  rideTimeline: (rideRequestId: string, options?: ApiClientOptions) =>
    apiRequest(`/api/v1/admin/rides/${rideRequestId}/timeline`, { method: "GET" }, options),
  riders: (query = "", options?: ApiClientOptions) => apiRequest(`/api/v1/admin/riders${query}`, { method: "GET" }, options),
  drivers: (query = "", options?: ApiClientOptions) => apiRequest(`/api/v1/admin/drivers${query}`, { method: "GET" }, options),
  fraudAlerts: (query = "", options?: ApiClientOptions) =>
    apiRequest(`/api/v1/admin/fraud/alerts${query}`, { method: "GET" }, options),
  blockRider: <TBody, TResponse>(riderProfileId: string, body: TBody, options?: ApiClientOptions) =>
    apiRequest<TResponse>(`/api/v1/admin/riders/${riderProfileId}/block`, { method: "PATCH", body: JSON.stringify(body) }, options),
  unblockRider: <TBody, TResponse>(riderProfileId: string, body: TBody, options?: ApiClientOptions) =>
    apiRequest<TResponse>(`/api/v1/admin/riders/${riderProfileId}/unblock`, { method: "PATCH", body: JSON.stringify(body) }, options),
  blockDriver: <TBody, TResponse>(driverProfileId: string, body: TBody, options?: ApiClientOptions) =>
    apiRequest<TResponse>(`/api/v1/admin/drivers/${driverProfileId}/block`, { method: "PATCH", body: JSON.stringify(body) }, options),
  unblockDriver: <TBody, TResponse>(driverProfileId: string, body: TBody, options?: ApiClientOptions) =>
    apiRequest<TResponse>(`/api/v1/admin/drivers/${driverProfileId}/unblock`, { method: "PATCH", body: JSON.stringify(body) }, options),
  pricingRules: (query = "", options?: ApiClientOptions) =>
    apiRequest(`/api/v1/admin/pricing/rules${query}`, { method: "GET" }, options),
  updatePricingRule: <TBody, TResponse>(pricingRuleId: string, body: TBody, options?: ApiClientOptions) =>
    apiRequest<TResponse>(`/api/v1/admin/pricing/rules/${pricingRuleId}`, { method: "PUT", body: JSON.stringify(body) }, options),
  operationalMetrics: (options?: ApiClientOptions) => apiRequest("/api/v1/admin/metrics/operations", { method: "GET" }, options),
  sharedRidePerformance: (options?: ApiClientOptions) =>
    apiRequest("/api/v1/admin/shared-rides/performance", { method: "GET" }, options),
  dispatchStats: (options?: ApiClientOptions) => apiRequest("/api/v1/admin/dispatch/stats", { method: "GET" }, options),
  auditLogs: (query = "", options?: ApiClientOptions) => apiRequest(`/api/v1/admin/audit-logs${query}`, { method: "GET" }, options),
};

export const riderApi = {
  estimateFare: <TBody, TResponse>(body: TBody, options?: ApiClientOptions) =>
    apiRequest<TResponse>("/api/v1/rides/estimate", { method: "POST", body: JSON.stringify(body) }, options),
  bookRide: <TBody, TResponse>(body: TBody, options?: ApiClientOptions) =>
    apiRequest<TResponse>("/api/v1/rides", { method: "POST", body: JSON.stringify(body) }, options),
  history: (options?: ApiClientOptions) => apiRequest("/api/v1/rides/history", { method: "GET" }, options),
  rideDetails: (rideRequestId: string, options?: ApiClientOptions) =>
    apiRequest(`/api/v1/rides/${rideRequestId}`, { method: "GET" }, options),
  cancelRide: <TBody, TResponse>(rideRequestId: string, body: TBody, options?: ApiClientOptions) =>
    apiRequest<TResponse>(`/api/v1/rides/${rideRequestId}/cancel`, { method: "PATCH", body: JSON.stringify(body) }, options),
  notifications: (options?: ApiClientOptions) => apiRequest("/api/v1/notifications/me", { method: "GET" }, options),
  profile: (options?: ApiClientOptions) => apiRequest("/api/v1/rider/me", { method: "GET" }, options),
};

export const driverApi = {
  me: (options?: ApiClientOptions) => apiRequest("/api/v1/driver/me", { method: "GET" }, options),
  updateAvailability: <TBody, TResponse>(body: TBody, options?: ApiClientOptions) =>
    apiRequest<TResponse>("/api/v1/driver/availability", { method: "PATCH", body: JSON.stringify(body) }, options),
  updateLocation: <TBody, TResponse>(body: TBody, options?: ApiClientOptions) =>
    apiRequest<TResponse>("/api/v1/driver/location", { method: "POST", body: JSON.stringify(body) }, options),
  rideDetails: (rideRequestId: string, options?: ApiClientOptions) =>
    apiRequest(`/api/v1/rides/${rideRequestId}`, { method: "GET" }, options),
  notifications: (options?: ApiClientOptions) => apiRequest("/api/v1/notifications/me", { method: "GET" }, options),
};
