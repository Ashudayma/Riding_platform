"use client";

import { Button } from "@riding-platform/ui-kit";

export function PaginationControls({
  page,
  totalPages,
  onChange,
}: {
  page: number;
  totalPages: number;
  onChange: (page: number) => void;
}) {
  return (
    <div style={{ display: "flex", gap: 12, alignItems: "center", justifyContent: "space-between", flexWrap: "wrap" }}>
      <div style={{ color: "#5d6975" }}>
        Page {page + 1} of {Math.max(totalPages, 1)}
      </div>
      <div style={{ display: "flex", gap: 8 }}>
        <Button variant="ghost" onClick={() => onChange(Math.max(0, page - 1))} disabled={page <= 0}>
          Previous
        </Button>
        <Button variant="ghost" onClick={() => onChange(page + 1)} disabled={page + 1 >= totalPages}>
          Next
        </Button>
      </div>
    </div>
  );
}
