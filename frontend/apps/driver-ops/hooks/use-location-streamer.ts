"use client";

import { ApiError, driverApi } from "@riding-platform/api-client";
import { useEffect, useRef, useState } from "react";
import type { DriverLocationResponse } from "../lib/types";

export function useLocationStreamer(accessToken: string | undefined, isOnline: boolean) {
  const watchIdRef = useRef<number | null>(null);
  const [lastLocation, setLastLocation] = useState<DriverLocationResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isOnline || !accessToken || typeof window === "undefined" || !navigator.geolocation) {
      if (watchIdRef.current !== null && typeof navigator !== "undefined" && navigator.geolocation) {
        navigator.geolocation.clearWatch(watchIdRef.current);
        watchIdRef.current = null;
      }
      return;
    }

    watchIdRef.current = navigator.geolocation.watchPosition(
      async (position) => {
        try {
          const response = await driverApi.updateLocation<
            {
              latitude: number;
              longitude: number;
              headingDegrees: number | null;
              speedKph: number | null;
              accuracyMeters: number;
              locationProvider: "GPS";
            },
            DriverLocationResponse
          >(
            {
              latitude: position.coords.latitude,
              longitude: position.coords.longitude,
              headingDegrees: position.coords.heading ?? null,
              speedKph: position.coords.speed ? Number((position.coords.speed * 3.6).toFixed(2)) : null,
              accuracyMeters: position.coords.accuracy,
              locationProvider: "GPS",
            },
            { accessToken },
          );
          setLastLocation(response);
          setError(null);
        } catch (caught) {
          setError(caught instanceof ApiError ? `Location update failed with status ${caught.status}.` : "Location update failed.");
        }
      },
      (positionError) => {
        setError(positionError.message);
      },
      {
        enableHighAccuracy: true,
        maximumAge: 5000,
        timeout: 15000,
      },
    );

    return () => {
      if (watchIdRef.current !== null && navigator.geolocation) {
        navigator.geolocation.clearWatch(watchIdRef.current);
        watchIdRef.current = null;
      }
    };
  }, [accessToken, isOnline]);

  return {
    lastLocation,
    locationError: error,
  };
}
