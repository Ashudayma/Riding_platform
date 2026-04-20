"use client";

import { ApiError, adminApi } from "@riding-platform/api-client";
import { Card, EmptyState, LoadingState, MetricCard, StatusBadge } from "@riding-platform/ui-kit";
import { useEffect, useMemo, useState } from "react";
import { useAdminSession } from "../hooks/use-admin-session";
import type { AdminOverviewMetrics, AdminOperationalMetrics } from "../lib/types";
import { ChartCard } from "./chart-card";

export function OverviewPageContent() {
  const auth = useAdminSession();
  const [overview, setOverview] = useState<AdminOverviewMetrics | null>(null);
  const [metrics, setMetrics] = useState<AdminOperationalMetrics | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const apiOptions = useMemo(() => ({ accessToken: auth.user?.accessToken }), [auth.user?.accessToken]);

  useEffect(() => {
    const load = async () => {
      if (!auth.user?.accessToken) {
        return;
      }
      setIsLoading(true);
      try {
        const [overviewResponse, metricsResponse] = await Promise.all([
          adminApi.overview(apiOptions),
          adminApi.operationalMetrics(apiOptions),
        ]);
        setOverview(overviewResponse as AdminOverviewMetrics);
        setMetrics(metricsResponse as AdminOperationalMetrics);
        setError(null);
      } catch (caught) {
        setError(caught instanceof ApiError ? `Overview failed with status ${caught.status}.` : "Unable to load overview.");
      } finally {
        setIsLoading(false);
      }
    };

    void load();
  }, [apiOptions, auth.user?.accessToken]);

  if (isLoading) {
    return <LoadingState label="Loading operational overview..." />;
  }

  if (error || !overview || !metrics) {
    return <EmptyState title="Overview unavailable" description={error ?? "Operational metrics are unavailable."} />;
  }

  return (
    <div style={{ display: "grid", gap: 18 }}>
      <div style={{ display: "grid", gap: 16, gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))" }}>
        <MetricCard label="Rides in progress" value={String(overview.ridesInProgress)} hint="Live ride execution load" />
        <MetricCard label="Searching rides" value={String(overview.ridesSearchingDriver)} hint="Demand waiting for supply" />
        <MetricCard label="Available drivers" value={String(overview.availableDrivers)} hint="Ready for assignment" />
        <MetricCard label="Open fraud alerts" value={String(overview.openFraudAlerts)} hint="Needs analyst review" />
      </div>
      <div style={{ display: "grid", gap: 16, gridTemplateColumns: "1.4fr 1fr" }}>
        <ChartCard
          title="Operational pressure"
          values={[metrics.ridesInProgress, metrics.ridesSearchingDriver, metrics.availableDrivers, metrics.openFraudAlerts]}
          labels={["In progress", "Searching", "Available drivers", "Fraud alerts"]}
        />
        <Card title="Governance snapshot">
          <div style={{ display: "grid", gap: 12 }}>
            <div style={{ display: "flex", justifyContent: "space-between", gap: 12 }}>
              <span>Blocked drivers</span>
              <StatusBadge label={String(metrics.blockedDrivers)} tone="danger" />
            </div>
            <div style={{ display: "flex", justifyContent: "space-between", gap: 12 }}>
              <span>Blocked riders</span>
              <StatusBadge label={String(metrics.blockedRiders)} tone="danger" />
            </div>
            <div style={{ display: "flex", justifyContent: "space-between", gap: 12 }}>
              <span>Open shared groups</span>
              <StatusBadge label={String(metrics.openSharedRideGroups)} tone="success" />
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
}
