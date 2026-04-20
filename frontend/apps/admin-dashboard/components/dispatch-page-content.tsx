"use client";

import { ApiError, adminApi } from "@riding-platform/api-client";
import { Card, EmptyState, LoadingState, MetricCard } from "@riding-platform/ui-kit";
import { useEffect, useMemo, useState } from "react";
import { useAdminSession } from "../hooks/use-admin-session";
import type { AdminDispatchStats } from "../lib/types";
import { ChartCard } from "./chart-card";

export function DispatchPageContent() {
  const auth = useAdminSession(["PLATFORM_ADMIN", "OPS_ADMIN"]);
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
        const response = await adminApi.dispatchStats(apiOptions);
        setDispatch(response as AdminDispatchStats);
        setError(null);
      } catch (caught) {
        setError(caught instanceof ApiError ? `Dispatch stats failed with status ${caught.status}.` : "Unable to load dispatch stats.");
      } finally {
        setIsLoading(false);
      }
    };

    void load();
  }, [apiOptions, auth.user?.accessToken]);

  if (isLoading) {
    return <LoadingState label="Loading dispatch stats..." />;
  }

  if (!dispatch || error) {
    return <EmptyState title="Dispatch stats unavailable" description={error ?? "Dispatch stats are unavailable."} />;
  }

  return (
    <div style={{ display: "grid", gap: 18 }}>
      <div style={{ display: "grid", gap: 16, gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))" }}>
        <MetricCard label="Total attempts" value={String(dispatch.totalAttempts)} hint="Dispatch attempts recorded" />
        <MetricCard label="Accepted" value={String(dispatch.acceptedAttempts)} hint="Drivers accepted these assignments" />
        <MetricCard label="Rejected" value={String(dispatch.rejectedAttempts)} hint="Assignments actively rejected" />
        <MetricCard label="Timed out" value={String(dispatch.timedOutAttempts)} hint="Assignments expired before action" />
      </div>
      <ChartCard
        title="Assignment outcomes"
        values={[dispatch.acceptedAttempts, dispatch.rejectedAttempts, dispatch.timedOutAttempts, dispatch.failedAttempts]}
        labels={["Accepted", "Rejected", "Timed out", "Failed"]}
      />
      <Card title="Operational reading">
        <p style={{ margin: 0, color: "#5d6975" }}>
          This page is tuned for dispatch supervisors. As backend support expands, candidate score distributions, time-to-accept histograms, and city-level dispatch slices can plug into the same layout without changing the route structure.
        </p>
      </Card>
    </div>
  );
}
