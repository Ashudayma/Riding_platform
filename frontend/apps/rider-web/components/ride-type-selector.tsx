"use client";

import { StatusBadge } from "@riding-platform/ui-kit";
import type { RideType } from "../lib/types";

export function RideTypeSelector({
  value,
  onChange,
}: {
  value: RideType;
  onChange: (value: RideType) => void;
}) {
  const options: Array<{
    type: RideType;
    title: string;
    description: string;
    badge: string;
    tone: "warning" | "success";
  }> = [
    {
      type: "STANDARD",
      title: "Standard ride",
      description: "Fastest pickup and direct route with no extra pooled stops.",
      badge: "Priority pickup",
      tone: "warning",
    },
    {
      type: "SHARED",
      title: "Shared ride",
      description: "Lower fare with smart pooling, seat-aware routing, and detour controls.",
      badge: "Save more",
      tone: "success",
    },
  ];

  return (
    <div style={{ display: "grid", gap: 12, gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))" }}>
      {options.map((option) => {
        const active = option.type === value;
        return (
          <button
            key={option.type}
            type="button"
            onClick={() => onChange(option.type)}
            style={{
              textAlign: "left",
              borderRadius: 20,
              border: active ? "2px solid #d66b2d" : "1px solid rgba(0,0,0,0.08)",
              background: active ? "rgba(249, 233, 207, 0.9)" : "rgba(255,255,255,0.82)",
              padding: 18,
              cursor: "pointer",
              boxShadow: active ? "0 14px 30px rgba(214,107,45,0.12)" : "0 10px 28px rgba(0,0,0,0.04)",
            }}
          >
            <div style={{ display: "flex", justifyContent: "space-between", gap: 12, alignItems: "center" }}>
              <strong>{option.title}</strong>
              <StatusBadge label={option.badge} tone={option.tone} />
            </div>
            <p style={{ marginBottom: 0, color: "#5c6876" }}>{option.description}</p>
          </button>
        );
      })}
    </div>
  );
}
