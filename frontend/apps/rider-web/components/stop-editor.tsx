"use client";

import { Button, Field, TextInput } from "@riding-platform/ui-kit";
import type { RideStopDraft } from "../lib/types";

type StopEditorProps = {
  stops: RideStopDraft[];
  onChange: (stops: RideStopDraft[]) => void;
};

function createEmptyStop(index: number): RideStopDraft {
  return {
    id: `stop-${Date.now()}-${index}`,
    address: "",
    latitude: 28.6139 + index * 0.01,
    longitude: 77.209 + index * 0.01,
    locality: "",
  };
}

export function StopEditor({ stops, onChange }: StopEditorProps) {
  const updateStop = (id: string, field: keyof RideStopDraft, value: string | number) => {
    onChange(
      stops.map((stop) =>
        stop.id === id
          ? {
              ...stop,
              [field]: value,
            }
          : stop,
      ),
    );
  };

  const removeStop = (id: string) => onChange(stops.filter((stop) => stop.id !== id));

  return (
    <div style={{ display: "grid", gap: 14 }}>
      {stops.map((stop, index) => (
        <div
          key={stop.id}
          style={{
            padding: 16,
            borderRadius: 18,
            border: "1px solid rgba(0,0,0,0.08)",
            background: "rgba(255,255,255,0.76)",
            display: "grid",
            gap: 12,
          }}
        >
          <div style={{ display: "flex", justifyContent: "space-between", gap: 12, alignItems: "center" }}>
            <strong>Stop {index + 1}</strong>
            <Button variant="ghost" onClick={() => removeStop(stop.id)}>
              Remove
            </Button>
          </div>
          <Field label="Address">
            <TextInput
              value={stop.address}
              placeholder="Enter stop address"
              onChange={(event) => updateStop(stop.id, "address", event.target.value)}
            />
          </Field>
          <div style={{ display: "grid", gap: 12, gridTemplateColumns: "repeat(auto-fit, minmax(160px, 1fr))" }}>
            <Field label="Latitude">
              <TextInput
                type="number"
                value={stop.latitude}
                onChange={(event) => updateStop(stop.id, "latitude", Number(event.target.value))}
              />
            </Field>
            <Field label="Longitude">
              <TextInput
                type="number"
                value={stop.longitude}
                onChange={(event) => updateStop(stop.id, "longitude", Number(event.target.value))}
              />
            </Field>
          </div>
        </div>
      ))}
      <div>
        <Button variant="secondary" onClick={() => onChange([...stops, createEmptyStop(stops.length + 1)])}>
          Add stop
        </Button>
      </div>
    </div>
  );
}
