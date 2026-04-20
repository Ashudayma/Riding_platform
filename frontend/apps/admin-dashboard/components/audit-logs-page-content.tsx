"use client";

import { ApiError, adminApi } from "@riding-platform/api-client";
import { Button, Card, EmptyState, Field, LoadingState, SelectInput, TableShell } from "@riding-platform/ui-kit";
import { useEffect, useMemo, useState } from "react";
import { useAdminSession } from "../hooks/use-admin-session";
import { formatDateTime } from "../lib/format";
import { buildQuery } from "../lib/query";
import type { AdminAuditLog, AdminPage } from "../lib/types";
import { PaginationControls } from "./pagination-controls";

export function AuditLogsPageContent() {
  const auth = useAdminSession(["PLATFORM_ADMIN", "OPS_ADMIN"]);
  const [targetType, setTargetType] = useState("RIDER_PROFILE");
  const [page, setPage] = useState(0);
  const [data, setData] = useState<AdminPage<AdminAuditLog> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const apiOptions = useMemo(() => ({ accessToken: auth.user?.accessToken }), [auth.user?.accessToken]);

  const load = async () => {
    if (!auth.user?.accessToken) {
      return;
    }
    setIsLoading(true);
    try {
      const response = await adminApi.auditLogs(buildQuery({ targetType, page, size: 10 }), apiOptions);
      setData(response as AdminPage<AdminAuditLog>);
      setError(null);
    } catch (caught) {
      setError(caught instanceof ApiError ? `Audit log query failed with status ${caught.status}.` : "Unable to load audit logs.");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [apiOptions, auth.user?.accessToken, page]);

  if (isLoading && !data) {
    return <LoadingState label="Loading audit logs..." />;
  }

  if (error && !data) {
    return <EmptyState title="Audit logs unavailable" description={error} />;
  }

  return (
    <div style={{ display: "grid", gap: 16 }}>
      <Card title="Audit filters">
        <div style={{ display: "grid", gap: 14, gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))" }}>
          <Field label="Target type">
            <SelectInput value={targetType} onChange={(event) => setTargetType(event.target.value)}>
              <option value="RIDER_PROFILE">RIDER_PROFILE</option>
              <option value="DRIVER_PROFILE">DRIVER_PROFILE</option>
              <option value="PRICING_RULE">PRICING_RULE</option>
            </SelectInput>
          </Field>
        </div>
        <div style={{ display: "flex", gap: 12, marginTop: 14 }}>
          <Button onClick={() => { setPage(0); void load(); }}>Apply filters</Button>
        </div>
      </Card>
      <Card title="Audit log viewer">
        <TableShell
          columns={["Action", "Target Type", "Target", "Result", "Request ID", "Occurred At"]}
          rows={(data?.items ?? []).map((item) => [
            item.actionCode,
            item.targetType,
            item.targetId.slice(0, 8),
            item.resultStatus,
            item.requestId ?? "-",
            formatDateTime(item.occurredAt),
          ])}
        />
        {data ? <PaginationControls page={data.page} totalPages={data.totalPages} onChange={setPage} /> : null}
      </Card>
    </div>
  );
}
