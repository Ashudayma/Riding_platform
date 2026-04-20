"use client";

import { Card, EmptyState, StatusBadge } from "@riding-platform/ui-kit";
import { formatDateTime, formatMoney, statusTone } from "../lib/format";
import type { RideHistoryItem } from "../lib/types";

export function RideHistoryList({ rides }: { rides: RideHistoryItem[] }) {
  if (rides.length === 0) {
    return <EmptyState title="No rides yet" description="Your recent bookings will show up here once you start taking trips." />;
  }

  return (
    <div style={{ display: "grid", gap: 16 }}>
      {rides.map((ride) => (
        <Card key={ride.rideRequestId} title={`${ride.pickupAddress} -> ${ride.dropAddress}`}>
          <div style={{ display: "grid", gap: 10 }}>
            <div style={{ display: "flex", justifyContent: "space-between", gap: 12, flexWrap: "wrap", alignItems: "center" }}>
              <StatusBadge label={ride.status.replaceAll("_", " ")} tone={statusTone(ride.status)} />
              <strong>{formatMoney(ride.amount, ride.currencyCode)}</strong>
            </div>
            <div style={{ color: "#5f6e7b" }}>Booked {formatDateTime(ride.requestedAt)}</div>
            <div style={{ color: "#5f6e7b" }}>Ride type: {ride.rideType}</div>
            <a href={`/ride/${ride.rideRequestId}`} style={{ color: "#b94f14", fontWeight: 700 }}>
              Open live ride details
            </a>
          </div>
        </Card>
      ))}
    </div>
  );
}
