"use client";

import { ApiError, riderApi } from "@riding-platform/api-client";
import { Button, Card, Field, SelectInput, TextArea, TextInput } from "@riding-platform/ui-kit";
import { useMemo, useState } from "react";
import { useRiderSession } from "../hooks/use-rider-session";
import type {
  FareEstimateRequest,
  FareEstimateResponse,
  RideBookingResponse,
  RideStopDraft,
  RideStopRequest,
  RideType,
  VehicleType,
} from "../lib/types";
import { FareEstimatePanel } from "./fare-estimate-panel";
import { RideTypeSelector } from "./ride-type-selector";
import { StopEditor } from "./stop-editor";

type BookingFormState = {
  rideType: RideType;
  seatCount: number;
  requestedVehicleType: VehicleType;
  pickupAddress: string;
  pickupLatitude: number;
  pickupLongitude: number;
  dropAddress: string;
  dropLatitude: number;
  dropLongitude: number;
  stops: RideStopDraft[];
  paymentMethodId: string;
  notes: string;
};

const INITIAL_FORM: BookingFormState = {
  rideType: "STANDARD",
  seatCount: 1,
  requestedVehicleType: "SEDAN",
  pickupAddress: "Connaught Place, New Delhi",
  pickupLatitude: 28.6315,
  pickupLongitude: 77.2167,
  dropAddress: "IGI Airport Terminal 3, New Delhi",
  dropLatitude: 28.5562,
  dropLongitude: 77.1,
  stops: [],
  paymentMethodId: "11111111-1111-1111-1111-111111111111",
  notes: "",
};

function toStopRequests(stops: RideStopDraft[]): RideStopRequest[] {
  return stops
    .filter((stop) => stop.address.trim().length > 0)
    .map((stop) => ({
      stopType: "INTERMEDIATE",
      latitude: stop.latitude,
      longitude: stop.longitude,
      address: stop.address,
      locality: stop.locality,
    }));
}

function validateForm(state: BookingFormState): string | null {
  if (!state.pickupAddress.trim() || !state.dropAddress.trim()) {
    return "Pickup and drop are required.";
  }
  if (!state.paymentMethodId.trim()) {
    return "A payment method reference is required for booking.";
  }
  return null;
}

export function BookingFlow() {
  const auth = useRiderSession();
  const [form, setForm] = useState<BookingFormState>(INITIAL_FORM);
  const [estimate, setEstimate] = useState<FareEstimateResponse | null>(null);
  const [booking, setBooking] = useState<RideBookingResponse | null>(null);
  const [isEstimating, setIsEstimating] = useState(false);
  const [isBooking, setIsBooking] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [estimateError, setEstimateError] = useState<string | null>(null);

  const apiOptions = useMemo(() => ({ accessToken: auth.user?.accessToken }), [auth.user?.accessToken]);

  const buildEstimateRequest = (): FareEstimateRequest => ({
    rideType: form.rideType,
    seatCount: form.seatCount,
    requestedVehicleType: form.requestedVehicleType,
    pickupLatitude: form.pickupLatitude,
    pickupLongitude: form.pickupLongitude,
    pickupAddress: form.pickupAddress,
    dropLatitude: form.dropLatitude,
    dropLongitude: form.dropLongitude,
    dropAddress: form.dropAddress,
    stops: toStopRequests(form.stops),
  });

  const requestEstimate = async () => {
    const validationError = validateForm(form);
    if (validationError) {
      setEstimateError(validationError);
      return;
    }
    setIsEstimating(true);
    setEstimateError(null);
    try {
      const response = await riderApi.estimateFare<FareEstimateRequest, FareEstimateResponse>(buildEstimateRequest(), apiOptions);
      setEstimate(response);
    } catch (caught) {
      const message = caught instanceof ApiError ? `Estimate failed with status ${caught.status}.` : "Unable to estimate fare.";
      setEstimateError(message);
    } finally {
      setIsEstimating(false);
    }
  };

  const confirmBooking = async () => {
    const validationError = validateForm(form);
    if (validationError) {
      setError(validationError);
      return;
    }
    setIsBooking(true);
    setError(null);
    try {
      const response = await riderApi.bookRide<
        FareEstimateRequest & { paymentMethodId: string; notes: string },
        RideBookingResponse
      >(
        {
          ...buildEstimateRequest(),
          paymentMethodId: form.paymentMethodId,
          notes: form.notes,
        },
        {
          ...apiOptions,
          correlationId: `rider-booking-${Date.now()}`,
        },
      );
      setBooking(response);
    } catch (caught) {
      const message = caught instanceof ApiError ? `Booking failed with status ${caught.status}.` : "Unable to confirm ride.";
      setError(message);
    } finally {
      setIsBooking(false);
    }
  };

  return (
    <div style={{ display: "grid", gap: 18, gridTemplateColumns: "minmax(0, 1.8fr) minmax(320px, 1fr)" }}>
      <Card title="Plan your ride">
        <div style={{ display: "grid", gap: 18 }}>
          <RideTypeSelector
            value={form.rideType}
            onChange={(rideType) => setForm((current) => ({ ...current, rideType }))}
          />
          <div style={{ display: "grid", gap: 14, gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))" }}>
            <Field label="Pickup address">
              <TextInput
                value={form.pickupAddress}
                onChange={(event) => setForm((current) => ({ ...current, pickupAddress: event.target.value }))}
              />
            </Field>
            <Field label="Drop address">
              <TextInput
                value={form.dropAddress}
                onChange={(event) => setForm((current) => ({ ...current, dropAddress: event.target.value }))}
              />
            </Field>
          </div>
          <div style={{ display: "grid", gap: 14, gridTemplateColumns: "repeat(auto-fit, minmax(160px, 1fr))" }}>
            <Field label="Seats">
              <SelectInput
                value={String(form.seatCount)}
                onChange={(event) => setForm((current) => ({ ...current, seatCount: Number(event.target.value) }))}
              >
                {[1, 2, 3, 4].map((count) => (
                  <option key={count} value={count}>
                    {count}
                  </option>
                ))}
              </SelectInput>
            </Field>
            <Field label="Vehicle preference">
              <SelectInput
                value={form.requestedVehicleType}
                onChange={(event) =>
                  setForm((current) => ({ ...current, requestedVehicleType: event.target.value as VehicleType }))
                }
              >
                {["SEDAN", "SUV", "AUTO", "MOTORBIKE", "VAN"].map((vehicleType) => (
                  <option key={vehicleType} value={vehicleType}>
                    {vehicleType}
                  </option>
                ))}
              </SelectInput>
            </Field>
          </div>
          <div style={{ display: "grid", gap: 14, gridTemplateColumns: "repeat(auto-fit, minmax(160px, 1fr))" }}>
            <Field label="Pickup latitude">
              <TextInput
                type="number"
                value={form.pickupLatitude}
                onChange={(event) => setForm((current) => ({ ...current, pickupLatitude: Number(event.target.value) }))}
              />
            </Field>
            <Field label="Pickup longitude">
              <TextInput
                type="number"
                value={form.pickupLongitude}
                onChange={(event) => setForm((current) => ({ ...current, pickupLongitude: Number(event.target.value) }))}
              />
            </Field>
            <Field label="Drop latitude">
              <TextInput
                type="number"
                value={form.dropLatitude}
                onChange={(event) => setForm((current) => ({ ...current, dropLatitude: Number(event.target.value) }))}
              />
            </Field>
            <Field label="Drop longitude">
              <TextInput
                type="number"
                value={form.dropLongitude}
                onChange={(event) => setForm((current) => ({ ...current, dropLongitude: Number(event.target.value) }))}
              />
            </Field>
          </div>
          <Field label="Additional stops" description="Optional intermediate stops are inserted into the route in order.">
            <StopEditor stops={form.stops} onChange={(stops) => setForm((current) => ({ ...current, stops }))} />
          </Field>
          <Field label="Payment method reference">
            <TextInput
              value={form.paymentMethodId}
              onChange={(event) => setForm((current) => ({ ...current, paymentMethodId: event.target.value }))}
            />
          </Field>
          <Field label="Ride notes" description="Optional notes for pickup instructions or building access.">
            <TextArea value={form.notes} onChange={(event) => setForm((current) => ({ ...current, notes: event.target.value }))} />
          </Field>
          {error ? <p style={{ margin: 0, color: "#8b2d2a" }}>{error}</p> : null}
          <div style={{ display: "flex", gap: 12, flexWrap: "wrap" }}>
            <Button onClick={requestEstimate} disabled={isEstimating}>
              {isEstimating ? "Estimating..." : "Get fare estimate"}
            </Button>
            <Button variant="secondary" onClick={confirmBooking} disabled={isBooking}>
              {isBooking ? "Booking..." : "Confirm booking"}
            </Button>
            {booking ? (
              <a href={`/ride/${booking.rideRequestId}`} style={{ color: "#b94f14", fontWeight: 700, alignSelf: "center" }}>
                Open live trip tracking
              </a>
            ) : null}
          </div>
        </div>
      </Card>
      <div style={{ display: "grid", gap: 16 }}>
        <FareEstimatePanel estimate={estimate} isLoading={isEstimating} error={estimateError} />
        <Card title="Booking status">
          {booking ? (
            <div style={{ display: "grid", gap: 10 }}>
              <div style={{ fontWeight: 800 }}>Ride request created</div>
              <div>Ride request ID: {booking.rideRequestId}</div>
              <div>Status: {booking.status}</div>
              <div>Ride type: {booking.rideType}</div>
              <div>Stops in route: {booking.stops.length}</div>
            </div>
          ) : (
            <p style={{ margin: 0, color: "#62707c" }}>
              Booking confirmation, driver assignment, and realtime trip updates appear here after the request is submitted.
            </p>
          )}
        </Card>
      </div>
    </div>
  );
}
