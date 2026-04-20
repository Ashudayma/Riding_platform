"use client";

import { ApiError, driverApi } from "@riding-platform/api-client";
import { Card, EmptyState, LoadingState, StatusBadge } from "@riding-platform/ui-kit";
import { useEffect, useMemo, useState } from "react";
import { useDriverSession } from "../hooks/use-driver-session";
import { formatDateTime } from "../lib/format";
import type { NotificationItem } from "../lib/types";

export function DriverNotificationsPanel() {
  const auth = useDriverSession();
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const apiOptions = useMemo(() => ({ accessToken: auth.user?.accessToken }), [auth.user?.accessToken]);

  useEffect(() => {
    const load = async () => {
      if (!auth.user?.accessToken) {
        return;
      }
      try {
        const response = await driverApi.notifications(apiOptions);
        setNotifications(response as NotificationItem[]);
        setError(null);
      } catch (caught) {
        setError(caught instanceof ApiError ? `Notifications failed with status ${caught.status}.` : "Unable to load notifications.");
      } finally {
        setIsLoading(false);
      }
    };

    void load();
  }, [apiOptions, auth.user?.accessToken]);

  if (isLoading) {
    return <LoadingState label="Loading notifications..." />;
  }

  if (error) {
    return <EmptyState title="Notifications unavailable" description={error} />;
  }

  if (notifications.length === 0) {
    return <EmptyState title="No notifications" description="Assignment and ride updates will appear here once the backend sends them." />;
  }

  return (
    <div style={{ display: "grid", gap: 14 }}>
      {notifications.map((notification) => (
        <Card key={notification.notificationId} title={notification.title}>
          <div style={{ display: "grid", gap: 8 }}>
            <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
              <StatusBadge label={notification.deliveryStatus} tone={notification.deliveryStatus === "FAILED" ? "danger" : "success"} />
              <StatusBadge label={notification.channel} tone="neutral" />
            </div>
            <div style={{ color: "#61707c" }}>{notification.body}</div>
            <div style={{ color: "#61707c" }}>Sent {formatDateTime(notification.sentAt)}</div>
          </div>
        </Card>
      ))}
    </div>
  );
}
