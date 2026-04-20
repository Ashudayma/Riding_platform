"use client";

import { StatusBadge } from "@riding-platform/ui-kit";
import { statusTone } from "../lib/format";
import type { RideStatus } from "../lib/types";

const TIMELINE: RideStatus[] = [
  "REQUESTED",
  "SEARCHING_DRIVER",
  "DRIVER_ASSIGNED",
  "DRIVER_ARRIVING",
  "DRIVER_ARRIVED",
  "IN_PROGRESS",
  "COMPLETED",
];

export function RideStatusTimeline({ status }: { status: RideStatus }) {
  const currentIndex = TIMELINE.indexOf(status);

  return (
    <div style={{ display: "grid", gap: 10 }}>
      {TIMELINE.map((item, index) => {
        const active = currentIndex >= index || item === status;
        return (
          <div key={item} style={{ display: "flex", alignItems: "center", gap: 12 }}>
            <div
              style={{
                width: 12,
                height: 12,
                borderRadius: 999,
                background: active ? "#d66b2d" : "#d7dce3",
                boxShadow: active ? "0 0 0 6px rgba(214,107,45,0.12)" : "none",
              }}
            />
            <StatusBadge label={item.replaceAll("_", " ")} tone={active ? statusTone(item) : "neutral"} />
          </div>
        );
      })}
    </div>
  );
}
