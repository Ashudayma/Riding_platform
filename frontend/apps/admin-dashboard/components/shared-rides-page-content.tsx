"use client";

import { ApiError, adminApi } from "@riding-platform/api-client";
import { Card, EmptyState, LoadingState, MetricCard } from "@riding-platform/ui-kit";
import { useEffect, useMemo, useState } from "react";
import { useAdminSession } from "../hooks/use-admin-session";
import { formatMoney, percent } from "../lib/format";
import type { AdminSharedRidePerformance } from "../lib/types";
import { ChartCard } from "./chart-card";

export function SharedRidesPageContent() {
  const auth = useAdminSession(["PLATFORM_ADMIN", "OPS_ADMIN"]);
  const [performance, setPerformance] = useState<AdminSharedRidePerformance | null>(null);
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
        const response = await adminApi.sharedRidePerformance(apiOptions);
        setPerformance(response as AdminSharedRidePerformance);
        setError(null);
      } catch (caught) {
        setError(caught instanceof ApiError ? `Shared ride analytics failed with status ${caught.status}.` : "Unable to load shared ride analytics.");
      } finally {
        setIsLoading(false);
      }
    };

    void load();
  }, [apiOptions, auth.user?.accessToken]);

  if (isLoading) {
    return <LoadingState label="Loading shared ride analytics..." />;
  }

  if (!performance || error) {
    return <EmptyState title="Shared ride analytics unavailable" description={error ?? "Shared ride performance is unavailable."} />;
  }

  return (
    <div style={{ display: "grid", gap: 18 }}>
      <div style={{ display: "grid", gap: 16, gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))" }}>
        <MetricCard label="Total groups" value={String(performance.totalGroups)} hint="All pooled ride groups tracked" />
        <MetricCard label="Open groups" value={String(performance.openGroups)} hint="Still forming or active" />
        <MetricCard label="Completed groups" value={String(performance.completedGroups)} hint="Finished pooled rides" />
        <MetricCard label="Seat utilization" value={percent(performance.averageSeatUtilization)} hint="Average seat fill ratio" />
      </div>
      <div style={{ display: "grid", gap: 16, gridTemplateColumns: "1.3fr 1fr" }}>
        <ChartCard
          title="Pooling group outcomes"
          values={[performance.openGroups, performance.completedGroups]}
          labels={["Open", "Completed"]}
          accent="#0c7f71"
        />
        <Card title="Pooling savings">
          <div style={{ fontSize: 34, fontWeight: 800 }}>{formatMoney(performance.totalPoolingSavings)}</div>
          <p style={{ color: "#5d6975" }}>Total savings attributed to shared ride grouping across tracked records.</p>
        </Card>
      </div>
    </div>
  );
}
