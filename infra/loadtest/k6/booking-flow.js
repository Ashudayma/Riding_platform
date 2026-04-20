import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  scenarios: {
    booking_flow: {
      executor: "ramping-arrival-rate",
      startRate: 5,
      timeUnit: "1s",
      preAllocatedVUs: 20,
      maxVUs: 200,
      stages: [
        { target: 20, duration: "1m" },
        { target: 50, duration: "2m" },
        { target: 50, duration: "2m" },
        { target: 0, duration: "30s" },
      ],
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<800", "p(99)<1500"],
  },
};

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const accessToken = __ENV.ACCESS_TOKEN || "replace-me";

function authHeaders(extraHeaders = {}) {
  return {
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
      "X-Correlation-Id": `k6-booking-${__VU}-${Date.now()}`,
      ...extraHeaders,
    },
  };
}

function randomCoordinate(base, spread) {
  return Number((base + (Math.random() - 0.5) * spread).toFixed(6));
}

export default function () {
  const payload = {
    rideType: Math.random() > 0.7 ? "SHARED" : "STANDARD",
    seatCount: 1,
    requestedVehicleType: "SEDAN",
    pickupLatitude: randomCoordinate(28.6139, 0.08),
    pickupLongitude: randomCoordinate(77.209, 0.08),
    pickupAddress: "Connaught Place, New Delhi",
    dropLatitude: randomCoordinate(28.5562, 0.08),
    dropLongitude: randomCoordinate(77.1, 0.08),
    dropAddress: "IGI Airport Terminal 3, New Delhi",
    stops: [],
    paymentMethodId: "11111111-1111-1111-1111-111111111111",
    notes: "k6 booking performance test",
  };

  const estimateResponse = http.post(
    `${baseUrl}/api/v1/rides/estimate`,
    JSON.stringify({
      rideType: payload.rideType,
      seatCount: payload.seatCount,
      requestedVehicleType: payload.requestedVehicleType,
      pickupLatitude: payload.pickupLatitude,
      pickupLongitude: payload.pickupLongitude,
      pickupAddress: payload.pickupAddress,
      dropLatitude: payload.dropLatitude,
      dropLongitude: payload.dropLongitude,
      dropAddress: payload.dropAddress,
      stops: payload.stops,
    }),
    authHeaders(),
  );

  check(estimateResponse, {
    "estimate status 200": (response) => response.status === 200,
  });

  const bookingResponse = http.post(
    `${baseUrl}/api/v1/rides`,
    JSON.stringify(payload),
    authHeaders({
      "X-Idempotency-Key": `booking-${__VU}-${__ITER}`,
    }),
  );

  check(bookingResponse, {
    "booking status 200": (response) => response.status === 200,
    "booking has rideRequestId": (response) => {
      try {
        return !!response.json("rideRequestId");
      } catch (_) {
        return false;
      }
    },
  });

  sleep(1);
}
