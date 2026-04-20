export function formatMoney(amount: number, currencyCode: string): string {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: currencyCode || "INR",
    maximumFractionDigits: 0,
  }).format(amount);
}

export function formatDateTime(value: string): string {
  return new Intl.DateTimeFormat("en-IN", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

export function statusTone(status: string): "neutral" | "success" | "warning" | "danger" {
  switch (status) {
    case "ONLINE":
    case "AVAILABLE":
    case "CONNECTED":
    case "DRIVER_ASSIGNED":
    case "DRIVER_ARRIVING":
    case "DRIVER_ARRIVED":
    case "IN_PROGRESS":
    case "COMPLETED":
      return "success";
    case "OFFLINE":
    case "DISCONNECTED":
    case "FAILED":
    case "CANCELLED":
      return "danger";
    case "CONNECTING":
      return "warning";
    case "REQUESTED":
    case "SEARCHING_DRIVER":
    default:
      return "warning";
  }
}
