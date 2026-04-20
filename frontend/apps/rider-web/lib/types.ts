export type RideType = "STANDARD" | "SHARED";
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
export type StopType = "PICKUP" | "INTERMEDIATE" | "DROP";
export type VehicleType = "MOTORBIKE" | "SEDAN" | "SUV" | "AUTO" | "VAN";

export type RideStopDraft = {
  id: string;
  address: string;
  latitude: number;
  longitude: number;
  locality?: string;
};

export type RideStopRequest = {
  stopType: StopType;
  address: string;
  latitude: number;
  longitude: number;
  locality?: string;
};

export type FareEstimateRequest = {
  rideType: RideType;
  seatCount: number;
  requestedVehicleType?: VehicleType;
  pickupLatitude: number;
  pickupLongitude: number;
  pickupAddress: string;
  dropLatitude: number;
  dropLongitude: number;
  dropAddress: string;
  stops: RideStopRequest[];
};

export type FareEstimateResponse = {
  fareQuoteId: string;
  rideType: RideType;
  totalAmount: number;
  currencyCode: string;
  baseFare: number;
  distanceFare: number;
  durationFare: number;
  surgeMultiplier: number;
  bookingFee: number;
  taxAmount: number;
  poolingDiscountAmount: number;
  quotedDistanceMeters: number;
  quotedDurationSeconds: number;
  expiresAt: string;
};

export type RideStopResponse = {
  stopId: string;
  stopType: StopType;
  requestSequenceNo: number;
  rideSequenceNo: number | null;
  address: string;
};

export type RideBookingResponse = {
  rideRequestId: string;
  rideId: string | null;
  riderProfileId: string;
  rideType: RideType;
  status: RideStatus;
  fareQuoteId: string;
  estimatedTotalAmount: number;
  currencyCode: string;
  requestedAt: string;
  stops: RideStopResponse[];
};

export type RideHistoryItem = {
  rideRequestId: string;
  rideId: string | null;
  rideType: RideType;
  status: RideStatus;
  pickupAddress: string;
  dropAddress: string;
  amount: number;
  currencyCode: string;
  requestedAt: string;
};

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
  deliveredAt: string | null;
  readAt: string | null;
  failureReason: string | null;
};

export type RiderProfile = {
  subject: string;
  userProfileId: string;
  roles: string[];
};

export type RideRealtimeSnapshot = {
  rideRequestId: string;
  status?: RideStatus;
  driverLatitude?: number;
  driverLongitude?: number;
  driverHeadingDegrees?: number;
  driverEtaSeconds?: number;
  messageType?: string;
  occurredAt?: string;
};
