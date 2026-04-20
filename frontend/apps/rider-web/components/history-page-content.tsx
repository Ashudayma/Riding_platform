"use client";

import { ApiError, riderApi } from "@riding-platform/api-client";
import { EmptyState, LoadingState } from "@riding-platform/ui-kit";
import { useEffect, useMemo, useState } from "react";
import { useRiderSession } from "../hooks/use-rider-session";
import type { RideHistoryItem } from "../lib/types";
import { RideHistoryList } from "./history-list";

export function HistoryPageContent() {
  const auth = useRiderSession();
  const [rides, setRides] = useState<RideHistoryItem[]>([]);
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
        const response = await riderApi.history(apiOptions);
        setRides(response as RideHistoryItem[]);
        setError(null);
      } catch (caught) {
        const message = caught instanceof ApiError ? `Ride history failed with status ${caught.status}.` : "Unable to load ride history.";
        setError(message);
      } finally {
        setIsLoading(false);
      }
    };

    void load();
  }, [apiOptions, auth.user?.accessToken]);

  if (isLoading) {
    return <LoadingState label="Loading ride history..." />;
  }

  if (error) {
    return <EmptyState title="History unavailable" description={error} />;
  }

  return <RideHistoryList rides={rides} />;
}
