"use client";

import { ApiError, adminApi } from "@riding-platform/api-client";
import { EmptyState, LoadingState, MetricCard } from "@riding-platform/ui-kit";
import { useEffect, useMemo, useState } from "react";
import { useAdminSession } from "../hooks/use-admin-session";
import type { AdminDispatchStats, AdminOperationalMetrics, AdminSharedRidePerformance } from "../lib/types";
import { ChartCard } from "./chart-card";

export function MetricsPageContent() {
  const auth = useAdminSession(["PLATFORM_ADMIN", "OPS_ADMIN"]);
  const [metrics, setMetrics] = useState<AdminOperationalMetrics | null>(null);
  const [shared, setShared] = useState<AdminSharedRidePerformance | null>(null);
  const [dispatch, setDispatch] = useState<AdminDispatchStats | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const apiOptions = useMemo(() => ({ accessToken: auth.user?.accessToken }), [auth.user?.accessToken]);

  useEffect(() => {
    const load = async () => {
      if (!auth.user?.accessToken) {
        return;
      }
      setIsLoading(true);
      try {
        const [metricsResponse, sharedResponse, dispatchResponse] = await Promise.all([
          adminApi.operationalMetrics(apiOptions),
          adminApi.sharedRidePerformance(apiOptions),
          adminApi.dispatchStats(apiOptions),
        ]);
        setMetrics(metricsResponse as AdminOperationalMetrics);
        setShared(sharedResponse as AdminSharedRidePerformance);
        setDispatch(dispatchResponse as AdminDispatchStats);
        setError(null);
      } catch (caught) {
        setError(caught instanceof ApiError ? `Metrics failed with status ${caught.status}.` : "Unable to load metrics.");
      } finally {
        setIsLoading(false);
      }
    };

    void load();
  }, [apiOptions, auth.user?.accessToken]);

  if (isLoading) {
    return <LoadingState label="Loading metrics..." />;
  }

  if (!metrics || !shared || !dispatch || error) {
    return <EmptyState title="Metrics unavailable" description={error ?? "Metrics are unavailable."} />;
  }

  return (
    <div style={{ display: "grid", gap: 18 }}>
      <div style={{ display: "grid", gap: 16, gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))" }}>
        <MetricCard label="In progress" value={String(metrics.ridesInProgress)} hint="Trips currently active" />
        <MetricCard label="Searching" value={String(metrics.ridesSearchingDriver)} hint="Trips waiting for driver" />
        <MetricCard label="Pooling groups" value={String(shared.totalGroups)} hint="Total shared ride groups" />
        <MetricCard label="Dispatch attempts" value={String(dispatch.totalAttempts)} hint="Driver assignment attempts tracked" />
      </div>
      <div style={{ display: "grid", gap: 16, gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))" }}>
        <ChartCard
          title="Dispatch funnel"
          values={[dispatch.acceptedAttempts, dispatch.rejectedAttempts, dispatch.timedOutAttempts, dispatch.failedAttempts]}
          labels={["Accepted", "Rejected", "Timed out", "Failed"]}
        />
        <ChartCard
          title="Shared ride mix"
          values={[shared.openGroups, shared.completedGroups, metrics.openSharedRideGroups]}
          labels={["Open groups", "Completed groups", "Live open groups"]}
          accent="#0c7f71"
        />
      </div>
    </div>
  );
}
