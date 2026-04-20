"use client";

import { Card, StatusBadge } from "@riding-platform/ui-kit";
import type { RiderProfile } from "../lib/types";

export function ProfileSummaryCard({ profile }: { profile: RiderProfile }) {
  return (
    <Card title="Profile">
      <div style={{ display: "grid", gap: 12 }}>
        <div>
          <div style={{ fontSize: 28, fontWeight: 800 }}>{profile.subject}</div>
          <div style={{ color: "#61707c" }}>User profile ID: {profile.userProfileId}</div>
        </div>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          {profile.roles.map((role) => (
            <StatusBadge key={role} label={role} tone={role === "RIDER" ? "success" : "neutral"} />
          ))}
        </div>
      </div>
    </Card>
  );
}
