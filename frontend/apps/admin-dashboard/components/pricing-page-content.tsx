"use client";

import { ApiError, adminApi } from "@riding-platform/api-client";
import { Button, Card, EmptyState, Field, LoadingState, SelectInput, StatusBadge, TableShell, TextInput } from "@riding-platform/ui-kit";
import { useEffect, useMemo, useState } from "react";
import { useAdminSession } from "../hooks/use-admin-session";
import { formatDateTime, formatMoney, statusTone } from "../lib/format";
import { buildQuery } from "../lib/query";
import type { AdminPage, AdminPricingRule } from "../lib/types";
import { PaginationControls } from "./pagination-controls";

export function PricingPageContent() {
  const auth = useAdminSession(["PLATFORM_ADMIN", "OPS_ADMIN"]);
  const [cityCode, setCityCode] = useState("");
  const [activeFilter, setActiveFilter] = useState("");
  const [page, setPage] = useState(0);
  const [data, setData] = useState<AdminPage<AdminPricingRule> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const apiOptions = useMemo(() => ({ accessToken: auth.user?.accessToken }), [auth.user?.accessToken]);

  const load = async () => {
    if (!auth.user?.accessToken) {
      return;
    }
    setIsLoading(true);
    try {
      const response = await adminApi.pricingRules(
        buildQuery({
          page,
          size: 10,
          sort: "effectiveFrom,desc",
          cityCode: cityCode || undefined,
          active: activeFilter === "" ? undefined : activeFilter === "true",
        }),
        apiOptions,
      );
      setData(response as AdminPage<AdminPricingRule>);
      setError(null);
    } catch (caught) {
      setError(caught instanceof ApiError ? `Pricing rules failed with status ${caught.status}.` : "Unable to load pricing rules.");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [apiOptions, auth.user?.accessToken, page]);

  const toggleActive = async (rule: AdminPricingRule) => {
    try {
      await adminApi.updatePricingRule(
        rule.pricingRuleId,
        { active: !rule.active, reason: `${rule.active ? "Deactivate" : "Activate"} via admin dashboard` },
        apiOptions,
      );
      setActionError(null);
      await load();
    } catch (caught) {
      setActionError(caught instanceof ApiError ? `Pricing update failed with status ${caught.status}.` : "Unable to update pricing rule.");
    }
  };

  if (isLoading && !data) {
    return <LoadingState label="Loading pricing configuration..." />;
  }

  if (error && !data) {
    return <EmptyState title="Pricing unavailable" description={error} />;
  }

  return (
    <div style={{ display: "grid", gap: 16 }}>
      <Card title="Pricing filters">
        <div style={{ display: "grid", gap: 14, gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))" }}>
          <Field label="City code">
            <TextInput value={cityCode} onChange={(event) => setCityCode(event.target.value.toUpperCase())} />
          </Field>
          <Field label="Active">
            <SelectInput value={activeFilter} onChange={(event) => setActiveFilter(event.target.value)}>
              <option value="">All</option>
              <option value="true">True</option>
              <option value="false">False</option>
            </SelectInput>
          </Field>
        </div>
        <div style={{ display: "flex", gap: 12, marginTop: 14 }}>
          <Button onClick={() => { setPage(0); void load(); }}>Apply filters</Button>
        </div>
      </Card>
      <Card title="Pricing rules">
        <TableShell
          columns={["City", "Zone", "Ride Type", "Version", "Active", "Base", "Per KM", "Effective From"]}
          rows={(data?.items ?? []).map((rule) => [
            rule.cityCode,
            rule.zoneCode ?? "-",
            rule.rideType,
            String(rule.pricingVersion),
            String(rule.active),
            formatMoney(rule.baseFare),
            formatMoney(rule.perKmRate),
            formatDateTime(rule.effectiveFrom),
          ])}
        />
        <div style={{ display: "grid", gap: 10, marginTop: 14 }}>
          {(data?.items ?? []).map((rule) => (
            <div key={rule.pricingRuleId} style={{ display: "flex", justifyContent: "space-between", gap: 12, alignItems: "center", flexWrap: "wrap" }}>
              <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                <StatusBadge label={`${rule.cityCode}:${rule.rideType}`} tone="neutral" />
                <StatusBadge label={rule.active ? "ACTIVE" : "INACTIVE"} tone={statusTone(rule.active ? "ACTIVE" : "BLOCKED")} />
              </div>
              <Button variant={rule.active ? "secondary" : "ghost"} onClick={() => void toggleActive(rule)}>
                {rule.active ? "Deactivate" : "Activate"}
              </Button>
            </div>
          ))}
        </div>
        {actionError ? <p style={{ marginTop: 14, color: "#8b2d2a" }}>{actionError}</p> : null}
        {data ? <PaginationControls page={data.page} totalPages={data.totalPages} onChange={setPage} /> : null}
      </Card>
    </div>
  );
}
