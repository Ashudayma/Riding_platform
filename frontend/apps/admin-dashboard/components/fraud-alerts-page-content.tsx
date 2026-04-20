"use client";

import { ApiError, adminApi } from "@riding-platform/api-client";
import { Button, Card, EmptyState, Field, LoadingState, SelectInput, StatusBadge, TableShell } from "@riding-platform/ui-kit";
import { useEffect, useMemo, useState } from "react";
import { useAdminSession } from "../hooks/use-admin-session";
import { formatDateTime, statusTone } from "../lib/format";
import { buildQuery } from "../lib/query";
import type { AdminFraudAlert, AdminPage } from "../lib/types";
import { PaginationControls } from "./pagination-controls";

export function FraudAlertsPageContent() {
  const auth = useAdminSession(["PLATFORM_ADMIN", "OPS_ADMIN", "FRAUD_ANALYST"]);
  const [subjectType, setSubjectType] = useState("");
  const [flagStatus, setFlagStatus] = useState("");
  const [page, setPage] = useState(0);
  const [data, setData] = useState<AdminPage<AdminFraudAlert> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const apiOptions = useMemo(() => ({ accessToken: auth.user?.accessToken }), [auth.user?.accessToken]);

  const load = async () => {
    if (!auth.user?.accessToken) {
      return;
    }
    setIsLoading(true);
    try {
      const response = await adminApi.fraudAlerts(
        buildQuery({
          page,
          size: 10,
          sort: "createdAt,desc",
          subjectType: subjectType || undefined,
          flagStatus: flagStatus || undefined,
        }),
        apiOptions,
      );
      setData(response as AdminPage<AdminFraudAlert>);
      setError(null);
    } catch (caught) {
      setError(caught instanceof ApiError ? `Fraud alerts failed with status ${caught.status}.` : "Unable to load fraud alerts.");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [apiOptions, auth.user?.accessToken, page]);

  if (isLoading && !data) {
    return <LoadingState label="Loading fraud alerts..." />;
  }

  if (error && !data) {
    return <EmptyState title="Fraud alerts unavailable" description={error} />;
  }

  return (
    <div style={{ display: "grid", gap: 16 }}>
      <Card title="Alert filters">
        <div style={{ display: "grid", gap: 14, gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))" }}>
          <Field label="Subject type">
            <SelectInput value={subjectType} onChange={(event) => setSubjectType(event.target.value)}>
              <option value="">All</option>
              <option value="RIDER">RIDER</option>
              <option value="DRIVER">DRIVER</option>
            </SelectInput>
          </Field>
          <Field label="Flag status">
            <SelectInput value={flagStatus} onChange={(event) => setFlagStatus(event.target.value)}>
              <option value="">All</option>
              <option value="OPEN">OPEN</option>
              <option value="UNDER_REVIEW">UNDER_REVIEW</option>
              <option value="RESOLVED">RESOLVED</option>
            </SelectInput>
          </Field>
        </div>
        <div style={{ display: "flex", gap: 12, marginTop: 14 }}>
          <Button onClick={() => { setPage(0); void load(); }}>Apply filters</Button>
        </div>
      </Card>
      <Card title="Fraud alerts">
        <TableShell
          columns={["Alert", "Subject", "Severity", "Status", "Rule", "Risk Score", "Created"]}
          rows={(data?.items ?? []).map((item) => [
            item.flagId.slice(0, 8),
            `${item.subjectType}:${item.subjectId.slice(0, 8)}`,
            item.severity,
            item.flagStatus,
            item.ruleCode,
            String(item.riskScore ?? 0),
            formatDateTime(item.createdAt),
          ])}
        />
        <div style={{ display: "grid", gap: 10, marginTop: 14 }}>
          {(data?.items ?? []).map((item) => (
            <div key={item.flagId} style={{ display: "flex", justifyContent: "space-between", gap: 12, alignItems: "center", flexWrap: "wrap" }}>
              <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                <StatusBadge label={item.severity} tone={item.severity === "CRITICAL" ? "danger" : "warning"} />
                <StatusBadge label={item.flagStatus} tone={statusTone(item.flagStatus)} />
              </div>
              <div style={{ color: "#5d6975" }}>{item.title}</div>
            </div>
          ))}
        </div>
        {data ? <PaginationControls page={data.page} totalPages={data.totalPages} onChange={setPage} /> : null}
      </Card>
    </div>
  );
}
