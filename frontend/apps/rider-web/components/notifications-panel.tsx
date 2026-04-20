"use client";

import { ApiError, riderApi } from "@riding-platform/api-client";
import { Card, EmptyState, LoadingState, StatusBadge } from "@riding-platform/ui-kit";
import { useEffect, useMemo, useState } from "react";
import { useRiderSession } from "../hooks/use-rider-session";
import { formatDateTime } from "../lib/format";
import type { NotificationItem } from "../lib/types";

export function NotificationsPanel() {
  const auth = useRiderSession();
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const apiOptions = useMemo(() => ({ accessToken: auth.user?.accessToken }), [auth.user?.accessToken]);

  useEffect(() => {
    const load = async () => {
      if (!auth.user?.accessToken) {
        return;
      }
      setIsLoading(true);
      try {
        const response = await riderApi.notifications(apiOptions);
        setNotifications(response as NotificationItem[]);
        setError(null);
      } catch (caught) {
        const message =
          caught instanceof ApiError ? `Notifications failed with status ${caught.status}.` : "Unable to load notifications.";
        setError(message);
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
    return <EmptyState title="No notifications" description="Ride and account updates will appear here once events are delivered." />;
  }

  return (
    <div style={{ display: "grid", gap: 14 }}>
      {notifications.map((notification) => (
        <Card key={notification.notificationId} title={notification.title}>
          <div style={{ display: "grid", gap: 10 }}>
            <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
              <StatusBadge label={notification.deliveryStatus} tone={notification.deliveryStatus === "FAILED" ? "danger" : "success"} />
              <StatusBadge label={notification.channel} tone="neutral" />
            </div>
            <p style={{ margin: 0, color: "#586574" }}>{notification.body}</p>
            <div style={{ color: "#667380" }}>Sent {formatDateTime(notification.sentAt)}</div>
          </div>
        </Card>
      ))}
    </div>
  );
}
