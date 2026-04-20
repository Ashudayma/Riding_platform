"use client";

import { ApiError, adminApi } from "@riding-platform/api-client";
import { Button, Card, EmptyState, Field, LoadingState, SelectInput, StatusBadge, TableShell, TextInput } from "@riding-platform/ui-kit";
import { useEffect, useMemo, useState } from "react";
import { useAdminSession } from "../hooks/use-admin-session";
import { formatDateTime, statusTone } from "../lib/format";
import { buildQuery } from "../lib/query";
import type { AdminPage, AdminRide } from "../lib/types";
import { PaginationControls } from "./pagination-controls";

type RideFilterState = {
  rideType: string;
  requestStatus: string;
  lifecycleStatus: string;
  riderProfileId: string;
  driverProfileId: string;
};

const INITIAL_FILTERS: RideFilterState = {
  rideType: "",
  requestStatus: "",
  lifecycleStatus: "",
  riderProfileId: "",
  driverProfileId: "",
};

export function RidesPageContent() {
  const auth = useAdminSession(["PLATFORM_ADMIN", "OPS_ADMIN"]);
  const [filters, setFilters] = useState<RideFilterState>(INITIAL_FILTERS);
  const [page, setPage] = useState(0);
  const [data, setData] = useState<AdminPage<AdminRide> | null>(null);
  const [selectedRideId, setSelectedRideId] = useState<string | null>(null);
  const [timeline, setTimeline] = useState<Array<{ currentStatus: string; changedAt: string; note: string | null }>>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isTimelineLoading, setIsTimelineLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const apiOptions = useMemo(() => ({ accessToken: auth.user?.accessToken }), [auth.user?.accessToken]);

  const load = async () => {
    if (!auth.user?.accessToken) {
      return;
    }
    setIsLoading(true);
    try {
      const response = await adminApi.rides(
        buildQuery({
          page,
          size: 10,
          sort: "requestedAt,desc",
          rideType: filters.rideType || undefined,
          requestStatus: filters.requestStatus || undefined,
          lifecycleStatus: filters.lifecycleStatus || undefined,
          riderProfileId: filters.riderProfileId || undefined,
          driverProfileId: filters.driverProfileId || undefined,
        }),
        apiOptions,
      );
      setData(response as AdminPage<AdminRide>);
      setError(null);
    } catch (caught) {
      setError(caught instanceof ApiError ? `Rides query failed with status ${caught.status}.` : "Unable to load rides.");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [apiOptions, auth.user?.accessToken, page]);

  const loadTimeline = async (rideRequestId: string) => {
    setSelectedRideId(rideRequestId);
    setIsTimelineLoading(true);
    try {
      const response = await adminApi.rideTimeline(rideRequestId, apiOptions);
      setTimeline(response as Array<{ currentStatus: string; changedAt: string; note: string | null }>);
    } finally {
      setIsTimelineLoading(false);
    }
  };

  if (isLoading && !data) {
    return <LoadingState label="Loading rides..." />;
  }

  if (error && !data) {
    return <EmptyState title="Ride operations unavailable" description={error} />;
  }

  return (
    <div style={{ display: "grid", gap: 16 }}>
      <Card title="Ride filters">
        <div style={{ display: "grid", gap: 14, gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))" }}>
          <Field label="Ride type">
            <SelectInput value={filters.rideType} onChange={(event) => setFilters((current) => ({ ...current, rideType: event.target.value }))}>
              <option value="">All</option>
              <option value="STANDARD">STANDARD</option>
              <option value="SHARED">SHARED</option>
            </SelectInput>
          </Field>
          <Field label="Request status">
            <SelectInput value={filters.requestStatus} onChange={(event) => setFilters((current) => ({ ...current, requestStatus: event.target.value }))}>
              <option value="">All</option>
              <option value="REQUESTED">REQUESTED</option>
              <option value="SEARCHING_DRIVER">SEARCHING_DRIVER</option>
              <option value="DRIVER_ASSIGNED">DRIVER_ASSIGNED</option>
              <option value="IN_PROGRESS">IN_PROGRESS</option>
              <option value="COMPLETED">COMPLETED</option>
              <option value="CANCELLED">CANCELLED</option>
            </SelectInput>
          </Field>
          <Field label="Lifecycle status">
            <SelectInput value={filters.lifecycleStatus} onChange={(event) => setFilters((current) => ({ ...current, lifecycleStatus: event.target.value }))}>
              <option value="">All</option>
              <option value="SEARCHING_DRIVER">SEARCHING_DRIVER</option>
              <option value="DRIVER_ASSIGNED">DRIVER_ASSIGNED</option>
              <option value="IN_PROGRESS">IN_PROGRESS</option>
              <option value="COMPLETED">COMPLETED</option>
              <option value="FAILED">FAILED</option>
            </SelectInput>
          </Field>
          <Field label="Rider profile ID">
            <TextInput value={filters.riderProfileId} onChange={(event) => setFilters((current) => ({ ...current, riderProfileId: event.target.value }))} />
          </Field>
          <Field label="Driver profile ID">
            <TextInput value={filters.driverProfileId} onChange={(event) => setFilters((current) => ({ ...current, driverProfileId: event.target.value }))} />
          </Field>
        </div>
        <div style={{ display: "flex", gap: 12, marginTop: 14, flexWrap: "wrap" }}>
          <Button onClick={() => { setPage(0); void load(); }}>Apply filters</Button>
          <Button variant="ghost" onClick={() => { setFilters(INITIAL_FILTERS); setPage(0); }}>
            Reset
          </Button>
        </div>
      </Card>
      <div style={{ display: "grid", gap: 16, gridTemplateColumns: "1.7fr 1fr" }}>
        <Card title="Rides table">
          <TableShell
            columns={["Request", "Type", "Request Status", "Lifecycle", "Origin", "Destination", "Requested", "Timeline"]}
            rows={(data?.items ?? []).map((ride) => [
              ride.rideRequestId.slice(0, 8),
              ride.rideType,
              ride.requestStatus,
              ride.lifecycleStatus ?? "-",
              ride.originAddress,
              ride.destinationAddress,
              formatDateTime(ride.requestedAt),
              "View",
            ])}
          />
          <div style={{ display: "grid", gap: 10, marginTop: 14 }}>
            {(data?.items ?? []).map((ride) => (
              <div key={ride.rideRequestId} style={{ display: "flex", justifyContent: "space-between", gap: 12, flexWrap: "wrap", alignItems: "center" }}>
                <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                  <StatusBadge label={ride.requestStatus} tone={statusTone(ride.requestStatus)} />
                  <StatusBadge label={ride.lifecycleStatus ?? "NO_RIDE"} tone={statusTone(ride.lifecycleStatus ?? "PENDING")} />
                </div>
                <Button variant="ghost" onClick={() => void loadTimeline(ride.rideRequestId)}>
                  View timeline
                </Button>
              </div>
            ))}
          </div>
          {data ? <PaginationControls page={data.page} totalPages={data.totalPages} onChange={setPage} /> : null}
        </Card>
        <Card title="Ride timeline">
          {isTimelineLoading ? <LoadingState label="Loading timeline..." /> : null}
          {!isTimelineLoading && !selectedRideId ? (
            <p style={{ margin: 0, color: "#5d6975" }}>Select a ride to inspect its status timeline.</p>
          ) : null}
          {!isTimelineLoading && selectedRideId ? (
            <div style={{ display: "grid", gap: 10 }}>
              <div style={{ color: "#5d6975" }}>Ride request: {selectedRideId}</div>
              {timeline.map((item, index) => (
                <div key={`${item.changedAt}-${index}`} style={{ paddingBottom: 10, borderBottom: "1px solid rgba(0,0,0,0.06)" }}>
                  <StatusBadge label={item.currentStatus} tone={statusTone(item.currentStatus)} />
                  <div style={{ marginTop: 8, color: "#5d6975" }}>{formatDateTime(item.changedAt)}</div>
                  {item.note ? <div style={{ color: "#5d6975" }}>{item.note}</div> : null}
                </div>
              ))}
            </div>
          ) : null}
        </Card>
      </div>
    </div>
  );
}
