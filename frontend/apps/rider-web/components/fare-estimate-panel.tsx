"use client";

import { Card, LoadingState, StatusBadge } from "@riding-platform/ui-kit";
import { formatDuration, formatMoney } from "../lib/format";
import type { FareEstimateResponse } from "../lib/types";

export function FareEstimatePanel({
  estimate,
  isLoading,
  error,
}: {
  estimate: FareEstimateResponse | null;
  isLoading: boolean;
  error: string | null;
}) {
  if (isLoading) {
    return (
      <Card title="Fare estimate">
        <LoadingState label="Calculating live fare estimate..." />
      </Card>
    );
  }

  if (error) {
    return (
      <Card title="Fare estimate">
        <p style={{ color: "#8b2d2a", margin: 0 }}>{error}</p>
      </Card>
    );
  }

  if (!estimate) {
    return (
      <Card title="Fare estimate">
        <p style={{ color: "#64707d", margin: 0 }}>Enter pickup and drop details to preview pricing before you book.</p>
      </Card>
    );
  }

  return (
    <Card title="Fare estimate">
      <div style={{ display: "grid", gap: 14 }}>
        <div style={{ display: "flex", justifyContent: "space-between", gap: 12, alignItems: "center" }}>
          <div>
            <div style={{ fontSize: 30, fontWeight: 800 }}>{formatMoney(estimate.totalAmount, estimate.currencyCode)}</div>
            <div style={{ color: "#62707e" }}>
              {Math.round(estimate.quotedDistanceMeters / 1000)} km · {formatDuration(estimate.quotedDurationSeconds)}
            </div>
          </div>
          <StatusBadge
            label={estimate.rideType === "SHARED" ? "Shared fare" : "Standard fare"}
            tone={estimate.rideType === "SHARED" ? "success" : "warning"}
          />
        </div>
        <div style={{ display: "grid", gap: 8, color: "#4d5865" }}>
          <div>Base fare: {formatMoney(estimate.baseFare, estimate.currencyCode)}</div>
          <div>Distance fare: {formatMoney(estimate.distanceFare, estimate.currencyCode)}</div>
          <div>Duration fare: {formatMoney(estimate.durationFare, estimate.currencyCode)}</div>
          <div>Booking fee: {formatMoney(estimate.bookingFee, estimate.currencyCode)}</div>
          <div>Tax: {formatMoney(estimate.taxAmount, estimate.currencyCode)}</div>
          {estimate.poolingDiscountAmount > 0 ? (
            <div style={{ color: "#1d7044" }}>
              Shared ride savings: -{formatMoney(estimate.poolingDiscountAmount, estimate.currencyCode)}
            </div>
          ) : null}
        </div>
      </div>
    </Card>
  );
}
