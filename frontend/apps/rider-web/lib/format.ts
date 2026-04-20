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

export function formatDuration(seconds: number): string {
  const minutes = Math.max(1, Math.round(seconds / 60));
  if (minutes < 60) {
    return `${minutes} min`;
  }
  const hours = Math.floor(minutes / 60);
  const remainingMinutes = minutes % 60;
  return remainingMinutes > 0 ? `${hours}h ${remainingMinutes}m` : `${hours}h`;
}

export function statusTone(status: string): "neutral" | "success" | "warning" | "danger" {
  switch (status) {
    case "COMPLETED":
    case "DRIVER_ASSIGNED":
    case "DRIVER_ARRIVING":
    case "DRIVER_ARRIVED":
    case "IN_PROGRESS":
      return "success";
    case "CANCELLED":
    case "FAILED":
      return "danger";
    case "REQUESTED":
    case "SEARCHING_DRIVER":
      return "warning";
    default:
      return "neutral";
  }
}
