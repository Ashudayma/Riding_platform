"use client";

import { Card } from "@riding-platform/ui-kit";

export function ChartCard({
  title,
  values,
  labels,
  accent = "#1254a1",
}: {
  title: string;
  values: number[];
  labels: string[];
  accent?: string;
}) {
  const max = Math.max(...values, 1);

  return (
    <Card title={title}>
      <div style={{ display: "grid", gap: 12 }}>
        <div style={{ display: "flex", alignItems: "end", gap: 12, minHeight: 220 }}>
          {values.map((value, index) => (
            <div key={`${labels[index]}-${value}`} style={{ flex: 1, display: "grid", gap: 8, justifyItems: "center" }}>
              <div style={{ color: "#5d6975", fontSize: 13 }}>{value}</div>
              <div
                style={{
                  width: "100%",
                  height: `${Math.max(16, (value / max) * 180)}px`,
                  borderRadius: 16,
                  background: `linear-gradient(180deg, ${accent}, rgba(18,84,161,0.28))`,
                }}
              />
              <div style={{ fontSize: 12, color: "#5d6975", textAlign: "center" }}>{labels[index]}</div>
            </div>
          ))}
        </div>
      </div>
    </Card>
  );
}
