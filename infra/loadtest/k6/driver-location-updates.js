import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  scenarios: {
    driver_location_updates: {
      executor: "constant-arrival-rate",
      rate: 200,
      timeUnit: "1s",
      duration: "3m",
      preAllocatedVUs: 100,
      maxVUs: 500,
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.02"],
    http_req_duration: ["p(95)<300", "p(99)<700"],
  },
};

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const accessToken = __ENV.ACCESS_TOKEN || "replace-me";
const locationProvider = __ENV.LOCATION_PROVIDER || "GPS";

function authHeaders() {
  return {
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
      "X-Correlation-Id": `k6-driver-location-${__VU}-${Date.now()}`,
    },
  };
}

function nextPoint(driverIndex) {
  const offset = driverIndex * 0.0001;
  return {
    latitude: Number((28.6139 + offset + Math.random() * 0.001).toFixed(6)),
    longitude: Number((77.209 + offset + Math.random() * 0.001).toFixed(6)),
    headingDegrees: Number((Math.random() * 360).toFixed(2)),
    speedKph: Number((25 + Math.random() * 20).toFixed(2)),
    accuracyMeters: Number((5 + Math.random() * 15).toFixed(2)),
    locationProvider,
  };
}

export default function () {
  const payload = nextPoint(__VU % 1000);
  const response = http.post(`${baseUrl}/api/v1/driver/location`, JSON.stringify(payload), authHeaders());

  check(response, {
    "location status 200": (result) => result.status === 200,
  });

  sleep(0.2);
}
