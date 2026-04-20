export type DriverProfile = {
  subject: string;
  userProfileId: string;
  roles: string[];
};

export type AvailabilityStatus = "AVAILABLE" | "UNAVAILABLE" | "ON_TRIP";
export type OnlineStatus = "ONLINE" | "OFFLINE";
export type RideStatus =
  | "REQUESTED"
  | "SEARCHING_DRIVER"
  | "DRIVER_ASSIGNED"
  | "DRIVER_ARRIVING"
  | "DRIVER_ARRIVED"
  | "IN_PROGRESS"
  | "COMPLETED"
  | "CANCELLED"
  | "FAILED";

export type DriverAvailabilityResponse = {
  driverProfileId: string;
  availabilityStatus: AvailabilityStatus;
  onlineStatus: OnlineStatus;
  availableSeatCount: number;
  currentRideId: string | null;
  latitude: number;
  longitude: number;
  accuracyMeters: number | null;
  lastHeartbeatAt: string;
};

export type DriverLocationResponse = {
  driverProfileId: string;
  latitude: number;
  longitude: number;
  observedAt: string;
};

export type DriverAssignmentEvent = {
  assignmentAttemptId?: string;
  rideRequestId: string;
  rideId?: string | null;
  driverProfileId?: string;
  assignmentStatus?: string;
  assignmentToken?: string;
  pickupAddress?: string;
  dropAddress?: string;
  estimatedAmount?: number;
  currencyCode?: string;
  occurredAt?: string;
  failureReason?: string | null;
};

export type DriverRideRealtimeEvent = {
  messageType?: string;
  rideRequestId?: string;
  rideId?: string;
  status?: RideStatus;
  message?: string;
  occurredAt?: string;
  driverProfileId?: string;
  latitude?: number;
  longitude?: number;
  headingDegrees?: number;
  speedKph?: number;
  accuracyMeters?: number;
};

export type DriverRideRecord = {
  id: string;
  riderName: string;
  pickupAddress: string;
  dropAddress: string;
  amount: number;
  currencyCode: string;
  completedAt: string;
  distanceKm: number;
};

export type DriverTripState = "EN_ROUTE_PICKUP" | "ARRIVED_PICKUP" | "TRIP_STARTED" | "DROPPED_OFF" | "COMPLETED";

export type NotificationItem = {
  notificationId: string;
  rideId: string | null;
  notificationType: string;
  eventCode: string;
  channel: string;
  deliveryStatus: string;
  title: string;
  body: string;
  sentAt: string;
};
