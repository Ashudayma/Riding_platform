export type AdminRole = "PLATFORM_ADMIN" | "OPS_ADMIN" | "SUPPORT_AGENT" | "FRAUD_ANALYST";

export type AdminPage<T> = {
  items: T[];
  totalElements: number;
  page: number;
  size: number;
  totalPages: number;
};

export type AdminOverviewMetrics = {
  ridesInProgress: number;
  ridesSearchingDriver: number;
  availableDrivers: number;
  blockedDrivers: number;
  blockedRiders: number;
  openFraudAlerts: number;
  openSharedRideGroups: number;
};

export type AdminRide = {
  rideRequestId: string;
  rideId: string | null;
  riderProfileId: string;
  driverProfileId: string | null;
  rideType: string;
  requestStatus: string;
  lifecycleStatus: string | null;
  originAddress: string;
  destinationAddress: string;
  requestedAt: string;
  assignedAt: string | null;
  completedAt: string | null;
};

export type AdminRideTimelineItem = {
  historyId: string;
  previousStatus: string | null;
  currentStatus: string;
  sourceType: string;
  actorType: string | null;
  note: string | null;
  changedAt: string;
};

export type AdminProfile = {
  profileId: string;
  userProfileId: string;
  code: string;
  status: string;
  averageRating: number;
  riskScore: number;
  blocked: boolean;
  fraudHold: boolean;
  fraudBlocked: boolean;
  displayName: string;
  email: string;
};

export type AdminFraudAlert = {
  flagId: string;
  subjectType: string;
  subjectId: string;
  severity: string;
  flagStatus: string;
  ruleCode: string;
  riskScore: number;
  title: string;
  createdAt: string;
};

export type AdminPricingRule = {
  pricingRuleId: string;
  cityCode: string;
  zoneCode: string | null;
  rideType: string;
  vehicleType: string | null;
  pricingVersion: number;
  active: boolean;
  baseFare: number;
  perKmRate: number;
  perMinuteRate: number;
  sharedDiscountFactor: number;
  effectiveFrom: string;
  effectiveTo: string | null;
};

export type AdminOperationalMetrics = {
  ridesInProgress: number;
  ridesSearchingDriver: number;
  availableDrivers: number;
  blockedDrivers: number;
  blockedRiders: number;
  openFraudAlerts: number;
  openSharedRideGroups: number;
};

export type AdminSharedRidePerformance = {
  totalGroups: number;
  openGroups: number;
  completedGroups: number;
  averageSeatUtilization: number;
  totalPoolingSavings: number;
};

export type AdminDispatchStats = {
  totalAttempts: number;
  acceptedAttempts: number;
  rejectedAttempts: number;
  timedOutAttempts: number;
  failedAttempts: number;
};

export type AdminAuditLog = {
  auditLogId: string;
  actionCode: string;
  targetType: string;
  targetId: string;
  resultStatus: string;
  requestId: string | null;
  traceId: string | null;
  occurredAt: string;
};
