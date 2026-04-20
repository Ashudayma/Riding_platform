"use client";

import { Card, MetricCard } from "@riding-platform/ui-kit";
import { formatDateTime, formatMoney } from "../lib/format";
import type { DriverRideRecord } from "../lib/types";

const SAMPLE_RIDES: DriverRideRecord[] = [
  {
    id: "ride-001",
    riderName: "Aarav",
    pickupAddress: "Sector 44, Gurugram",
    dropAddress: "Cyber Hub, Gurugram",
    amount: 340,
    currencyCode: "INR",
    completedAt: new Date().toISOString(),
    distanceKm: 12.2,
  },
  {
    id: "ride-002",
    riderName: "Mira",
    pickupAddress: "Saket, New Delhi",
    dropAddress: "Hauz Khas, New Delhi",
    amount: 280,
    currencyCode: "INR",
    completedAt: new Date(Date.now() - 7200000).toISOString(),
    distanceKm: 8.4,
  },
];

export function EarningsOverview() {
  const total = SAMPLE_RIDES.reduce((sum, ride) => sum + ride.amount, 0);

  return (
    <div style={{ display: "grid", gap: 16 }}>
      <div style={{ display: "grid", gap: 16, gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))" }}>
        <MetricCard label="Today earnings" value={formatMoney(total, "INR")} hint={`${SAMPLE_RIDES.length} completed rides`} />
        <MetricCard label="Avg. fare" value={formatMoney(total / SAMPLE_RIDES.length, "INR")} hint="Based on completed trips shown below" />
        <MetricCard label="Online utilization" value="78%" hint="Placeholder until backend earnings/driver-trip summary endpoints are added" />
      </div>
      <Card title="Completed rides">
        <div style={{ display: "grid", gap: 14 }}>
          {SAMPLE_RIDES.map((ride) => (
            <div key={ride.id} style={{ display: "grid", gap: 6, paddingBottom: 12, borderBottom: "1px solid rgba(0,0,0,0.06)" }}>
              <div style={{ display: "flex", justifyContent: "space-between", gap: 12, flexWrap: "wrap" }}>
                <strong>
                  {ride.pickupAddress} {" to "} {ride.dropAddress}
                </strong>
                <strong>{formatMoney(ride.amount, ride.currencyCode)}</strong>
              </div>
              <div style={{ color: "#61707c" }}>
                Rider: {ride.riderName} | {ride.distanceKm} km | Completed {formatDateTime(ride.completedAt)}
              </div>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}
