import React from "react";

type NavItem = {
  href: string;
  label: string;
};

export function AppShell({
  brand,
  navItems,
  children,
  sidebar = false,
}: {
  brand: string;
  navItems: NavItem[];
  children: React.ReactNode;
  sidebar?: boolean;
}) {
  return (
    <div style={{ minHeight: "100vh" }}>
      <header
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          padding: "16px 24px",
          borderBottom: "1px solid rgba(0,0,0,0.08)",
          background: "rgba(255,255,255,0.72)",
          backdropFilter: "blur(10px)",
          position: "sticky",
          top: 0,
          zIndex: 10,
        }}
      >
        <strong style={{ letterSpacing: "0.04em" }}>{brand}</strong>
        <nav style={{ display: "flex", gap: 14, flexWrap: "wrap" }}>
          {navItems.map((item) => (
            <a key={item.href} href={item.href} style={{ color: "inherit", opacity: 0.9 }}>
              {item.label}
            </a>
          ))}
        </nav>
      </header>
      <div
        style={{
          display: "grid",
          gridTemplateColumns: sidebar ? "220px minmax(0, 1fr)" : "minmax(0, 1fr)",
          minHeight: "calc(100vh - 69px)",
        }}
      >
        {sidebar ? (
          <aside style={{ borderRight: "1px solid rgba(0,0,0,0.06)", padding: 24, background: "rgba(255,255,255,0.45)" }}>
            <p style={{ marginTop: 0, color: "#5b6a79", fontSize: 13, textTransform: "uppercase", letterSpacing: "0.08em" }}>Operations</p>
            <div style={{ display: "grid", gap: 10 }}>
              {navItems.map((item) => (
                <a key={item.href} href={item.href}>
                  {item.label}
                </a>
              ))}
            </div>
          </aside>
        ) : null}
        <div style={{ padding: 24, width: "100%", maxWidth: 1280, margin: "0 auto" }}>{children}</div>
      </div>
    </div>
  );
}

export function SectionTitle({
  eyebrow,
  title,
  description,
}: {
  eyebrow: string;
  title: string;
  description: string;
}) {
  return (
    <section style={{ marginBottom: 24 }}>
      <div style={{ textTransform: "uppercase", letterSpacing: "0.12em", fontSize: 12, opacity: 0.7 }}>{eyebrow}</div>
      <h1 style={{ margin: "8px 0", fontSize: "clamp(1.9rem, 3vw, 3rem)", lineHeight: 1.05 }}>{title}</h1>
      <p style={{ margin: 0, maxWidth: 760, color: "#5e5a52" }}>{description}</p>
    </section>
  );
}

export function Card({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section
      style={{
        background: "rgba(255,255,255,0.82)",
        border: "1px solid rgba(0,0,0,0.08)",
        borderRadius: 20,
        padding: 20,
        boxShadow: "0 12px 40px rgba(0,0,0,0.04)",
      }}
    >
      <h2 style={{ marginTop: 0, marginBottom: 12, fontSize: 18 }}>{title}</h2>
      <div>{children}</div>
    </section>
  );
}

export function MetricCard({ label, value, hint }: { label: string; value: string; hint: string }) {
  return (
    <Card title={label}>
      <div style={{ fontSize: 32, fontWeight: 700, marginBottom: 8 }}>{value}</div>
      <div style={{ color: "#65727f" }}>{hint}</div>
    </Card>
  );
}

export function StatusBadge({ label, tone }: { label: string; tone: "neutral" | "success" | "warning" | "danger" }) {
  const palette = {
    neutral: { bg: "#eef1f4", fg: "#304050" },
    success: { bg: "#dff4e8", fg: "#1f6a43" },
    warning: { bg: "#fde8cf", fg: "#8b4d12" },
    danger: { bg: "#fde0df", fg: "#8d2d2a" },
  }[tone];
  return (
    <span
      style={{
        display: "inline-flex",
        alignItems: "center",
        padding: "6px 10px",
        borderRadius: 999,
        background: palette.bg,
        color: palette.fg,
        fontSize: 13,
        fontWeight: 600,
      }}
    >
      {label}
    </span>
  );
}

export function TableShell({ columns, rows }: { columns: string[]; rows: string[][] }) {
  return (
    <div style={{ overflowX: "auto" }}>
      <table style={{ width: "100%", borderCollapse: "collapse" }}>
        <thead>
          <tr>
            {columns.map((column) => (
              <th
                key={column}
                style={{
                  textAlign: "left",
                  padding: "12px 10px",
                  borderBottom: "1px solid rgba(0,0,0,0.08)",
                  fontSize: 13,
                  color: "#607080",
                }}
              >
                {column}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, index) => (
            <tr key={index}>
              {row.map((cell, cellIndex) => (
                <td key={`${index}-${cellIndex}`} style={{ padding: "12px 10px", borderBottom: "1px solid rgba(0,0,0,0.05)" }}>
                  {cell}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function Button({
  children,
  variant = "primary",
  disabled,
  onClick,
  type = "button",
}: {
  children: React.ReactNode;
  variant?: "primary" | "secondary" | "ghost";
  disabled?: boolean;
  onClick?: () => void;
  type?: "button" | "submit";
}) {
  const palette = {
    primary: { bg: "#1c6ed6", fg: "#fff", border: "#1c6ed6" },
    secondary: { bg: "#f4ede1", fg: "#2b241a", border: "#dbcbb1" },
    ghost: { bg: "transparent", fg: "#2b241a", border: "#d5dbe2" },
  }[variant];
  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled}
      style={{
        borderRadius: 14,
        border: `1px solid ${palette.border}`,
        background: palette.bg,
        color: palette.fg,
        padding: "11px 16px",
        fontWeight: 700,
        cursor: disabled ? "not-allowed" : "pointer",
        opacity: disabled ? 0.6 : 1,
      }}
    >
      {children}
    </button>
  );
}

export function Field({
  label,
  description,
  children,
}: {
  label: string;
  description?: string;
  children: React.ReactNode;
}) {
  return (
    <label style={{ display: "grid", gap: 8 }}>
      <span style={{ fontWeight: 700 }}>{label}</span>
      {description ? <span style={{ fontSize: 13, color: "#677281" }}>{description}</span> : null}
      {children}
    </label>
  );
}

export function TextInput(props: React.InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      {...props}
      style={{
        width: "100%",
        padding: "12px 14px",
        borderRadius: 14,
        border: "1px solid rgba(0,0,0,0.12)",
        background: "rgba(255,255,255,0.92)",
        ...(props.style ?? {}),
      }}
    />
  );
}

export function SelectInput(props: React.SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <select
      {...props}
      style={{
        width: "100%",
        padding: "12px 14px",
        borderRadius: 14,
        border: "1px solid rgba(0,0,0,0.12)",
        background: "rgba(255,255,255,0.92)",
        ...(props.style ?? {}),
      }}
    />
  );
}

export function TextArea(props: React.TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return (
    <textarea
      {...props}
      style={{
        width: "100%",
        padding: "12px 14px",
        borderRadius: 14,
        border: "1px solid rgba(0,0,0,0.12)",
        background: "rgba(255,255,255,0.92)",
        minHeight: 96,
        resize: "vertical",
        ...(props.style ?? {}),
      }}
    />
  );
}

export function LoadingState({ label = "Loading..." }: { label?: string }) {
  return <p style={{ color: "#64707d" }}>{label}</p>;
}

export function ErrorState({ title, description }: { title: string; description: string }) {
  return (
    <Card title={title}>
      <p style={{ margin: 0, color: "#8b2d2a" }}>{description}</p>
    </Card>
  );
}

export function EmptyState({ title, description }: { title: string; description: string }) {
  return (
    <Card title={title}>
      <p style={{ margin: 0, color: "#64707d" }}>{description}</p>
    </Card>
  );
}
