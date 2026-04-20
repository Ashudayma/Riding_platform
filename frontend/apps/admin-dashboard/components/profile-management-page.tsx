"use client";

import { ApiError, adminApi } from "@riding-platform/api-client";
import { Button, Card, EmptyState, Field, LoadingState, SelectInput, StatusBadge, TableShell } from "@riding-platform/ui-kit";
import { useEffect, useMemo, useState } from "react";
import { useAdminSession } from "../hooks/use-admin-session";
import { statusTone } from "../lib/format";
import { buildQuery } from "../lib/query";
import type { AdminPage, AdminProfile } from "../lib/types";
import { PaginationControls } from "./pagination-controls";

export function ProfileManagementPage({
  subject,
}: {
  subject: "riders" | "drivers";
}) {
  const auth = useAdminSession(["PLATFORM_ADMIN", "OPS_ADMIN", "SUPPORT_AGENT"]);
  const [status, setStatus] = useState("");
  const [fraudFilter, setFraudFilter] = useState("");
  const [page, setPage] = useState(0);
  const [data, setData] = useState<AdminPage<AdminProfile> | null>(null);
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
      const query = buildQuery({
        page,
        size: 10,
        sort: "createdAt,desc",
        status: status || undefined,
        [subject === "riders" ? "fraudHold" : "fraudBlocked"]: fraudFilter === "" ? undefined : fraudFilter === "true",
      });
      const response =
        subject === "riders" ? await adminApi.riders(query, apiOptions) : await adminApi.drivers(query, apiOptions);
      setData(response as AdminPage<AdminProfile>);
      setError(null);
    } catch (caught) {
      setError(caught instanceof ApiError ? `Profiles failed with status ${caught.status}.` : "Unable to load profiles.");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [apiOptions, auth.user?.accessToken, page]);

  const toggleBlock = async (profile: AdminProfile) => {
    if (!auth.user?.accessToken) {
      return;
    }
    try {
      const body = { reason: `${profile.blocked ? "Unblocking" : "Blocking"} through admin dashboard` };
      if (subject === "riders") {
        if (profile.blocked) {
          await adminApi.unblockRider(profile.profileId, body, apiOptions);
        } else {
          await adminApi.blockRider(profile.profileId, body, apiOptions);
        }
      } else if (profile.blocked) {
        await adminApi.unblockDriver(profile.profileId, body, apiOptions);
      } else {
        await adminApi.blockDriver(profile.profileId, body, apiOptions);
      }
      setActionError(null);
      await load();
    } catch (caught) {
      setActionError(caught instanceof ApiError ? `Profile action failed with status ${caught.status}.` : "Unable to update profile.");
    }
  };

  if (isLoading && !data) {
    return <LoadingState label={`Loading ${subject}...`} />;
  }

  if (error && !data) {
    return <EmptyState title="Profile management unavailable" description={error} />;
  }

  return (
    <div style={{ display: "grid", gap: 16 }}>
      <Card title={`${subject === "riders" ? "Rider" : "Driver"} filters`}>
        <div style={{ display: "grid", gap: 14, gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))" }}>
          <Field label="Status">
            <SelectInput value={status} onChange={(event) => setStatus(event.target.value)}>
              <option value="">All</option>
              <option value="ACTIVE">ACTIVE</option>
              <option value="BLOCKED">BLOCKED</option>
            </SelectInput>
          </Field>
          <Field label={subject === "riders" ? "Fraud hold" : "Fraud blocked"}>
            <SelectInput value={fraudFilter} onChange={(event) => setFraudFilter(event.target.value)}>
              <option value="">All</option>
              <option value="true">True</option>
              <option value="false">False</option>
            </SelectInput>
          </Field>
        </div>
        <div style={{ display: "flex", gap: 12, marginTop: 14 }}>
          <Button onClick={() => { setPage(0); void load(); }}>Apply filters</Button>
          <Button variant="ghost" onClick={() => { setStatus(""); setFraudFilter(""); setPage(0); }}>
            Reset
          </Button>
        </div>
      </Card>
      <Card title={`${subject === "riders" ? "Rider" : "Driver"} management`}>
        <TableShell
          columns={["Code", "Name", "Email", "Status", "Rating", "Risk", "Flags"]}
          rows={(data?.items ?? []).map((item) => [
            item.code,
            item.displayName,
            item.email,
            item.status,
            String(item.averageRating ?? 0),
            String(item.riskScore ?? 0),
            [item.fraudHold ? "fraud-hold" : "", item.fraudBlocked ? "fraud-blocked" : "", item.blocked ? "blocked" : ""]
              .filter(Boolean)
              .join(", ") || "-",
          ])}
        />
        <div style={{ display: "grid", gap: 10, marginTop: 14 }}>
          {(data?.items ?? []).map((profile) => (
            <div key={profile.profileId} style={{ display: "flex", justifyContent: "space-between", gap: 12, flexWrap: "wrap", alignItems: "center" }}>
              <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                <StatusBadge label={profile.status} tone={statusTone(profile.status)} />
                {profile.fraudHold ? <StatusBadge label="FRAUD_HOLD" tone="warning" /> : null}
                {profile.fraudBlocked ? <StatusBadge label="FRAUD_BLOCKED" tone="danger" /> : null}
              </div>
              <Button variant={profile.blocked ? "secondary" : "ghost"} onClick={() => void toggleBlock(profile)}>
                {profile.blocked ? "Unblock" : "Block"}
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
