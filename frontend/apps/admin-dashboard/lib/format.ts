export function formatMoney(amount: number, currencyCode = "INR"): string {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: currencyCode,
    maximumFractionDigits: 0,
  }).format(amount);
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) {
    return "-";
  }
  return new Intl.DateTimeFormat("en-IN", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

export function statusTone(status: string): "neutral" | "success" | "warning" | "danger" {
  switch (status) {
    case "ACTIVE":
    case "AVAILABLE":
    case "COMPLETED":
    case "SUCCESS":
    case "OPEN":
    case "IN_PROGRESS":
    case "DRIVER_ASSIGNED":
    case "ONLINE":
      return "success";
    case "BLOCKED":
    case "FAILED":
    case "REJECTED":
    case "TIMED_OUT":
    case "CANCELLED":
      return "danger";
    case "SEARCHING_DRIVER":
    case "UNDER_REVIEW":
    case "PENDING":
      return "warning";
    default:
      return "neutral";
  }
}

export function percent(value: number): string {
  return `${(value * 100).toFixed(1)}%`;
}
