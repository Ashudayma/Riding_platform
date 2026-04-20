# Riding Platform Frontend

Production-grade frontend monorepo foundation for a ride-hailing and ride-sharing platform using:

- Next.js
- React
- TypeScript
- role-based apps for rider, driver, and admin
- shared packages for UI, auth, API access, realtime, and runtime config

## Architecture

### App boundaries

- `apps/rider-web`
  Rider booking, fare estimate, ride tracking, history, ratings, notifications.
- `apps/driver-ops`
  Driver availability, live location sharing, assignment handling, trip flow, earnings.
- `apps/admin-dashboard`
  Operational dashboard, rides table, fraud alerts, pricing management, metrics, user management.

### Shared packages

- `packages/ui-kit`
  Shared design tokens, layout primitives, cards, tables, forms, badges, buttons.
- `packages/api-client`
  Typed fetch client, auth-aware request wrapper, REST modules by domain.
- `packages/auth`
  Keycloak/OIDC integration strategy, role guards, token helpers, session bridge.
- `packages/realtime`
  WebSocket/STOMP strategy, subscription helpers, reconnect policy.
- `packages/config`
  Environment parsing and runtime config.

## Routing Structure

### Rider app

- `/`
- `/auth/login`
- `/auth/signup`
- `/book`
- `/book/confirm`
- `/ride/[rideRequestId]`
- `/history`
- `/ratings`
- `/notifications`
- `/profile`

### Driver app

- `/`
- `/auth/login`
- `/home`
- `/assignments`
- `/trip/[rideId]`
- `/earnings`
- `/notifications`
- `/profile`

### Admin app

- `/`
- `/auth/login`
- `/overview`
- `/rides`
- `/rides/[rideRequestId]`
- `/fraud-alerts`
- `/pricing`
- `/metrics`
- `/shared-rides`
- `/users/drivers`
- `/users/riders`
- `/dispatch`
- `/audit-logs`

## State Management Recommendation

Use a layered approach instead of one global store for everything:

- Server state: `@tanstack/react-query`
  Best for REST APIs, caching, invalidation, pagination, background refresh.
- Local UI state: React state/hooks
  Best for drawers, modals, filters, local forms.
- Session/auth state: shared auth provider in `packages/auth`
  Handles current user, role checks, token lifecycle.
- Realtime transient state: small per-feature stores or hooks
  Best for ride tracking, driver location, dispatch updates, notifications.

This keeps the app scalable and avoids a giant Redux-style global state tree for mostly server-driven data.

## API Client Strategy

Use a thin shared HTTP client with:

- typed request wrappers
- automatic bearer token injection
- correlation id support
- safe parsing of Spring error payloads
- per-domain API modules:
  - rides
  - pricing
  - driver
  - admin
  - fraud
  - notifications
  - shared rides

React Query hooks should live close to each app feature, while the raw client stays in `packages/api-client`.

## Auth Integration with Keycloak

Frontend auth should be handled through Keycloak/OIDC:

- browser apps authenticate against Keycloak directly
- store access token in memory where possible
- use refresh token flow through a secure auth provider strategy
- decode roles from JWT for page and component guards
- use route-level role guards per app
- support future social login through Keycloak flows without app rewrites

Recommended first pass:

- `react-oidc-context` or custom OIDC adapter in `packages/auth`
- one provider per app
- shared `RoleGate` and `RequireAuth` wrappers

## WebSocket Integration Strategy

The backend currently exposes a secure WebSocket endpoint at `/ws`.

Use:

- STOMP over SockJS/WebSocket for compatibility with the backend today
- JWT passed during socket connect
- reconnect with backoff
- app-specific subscription hooks

Primary topics:

- rider ride updates
- rider driver location updates
- driver assignment updates
- driver trip status updates
- in-app notifications
- admin operational event streams later

## UI Component Plan

Shared UI kit should cover:

- app shells and top navigation
- sidebar navigation for admin and driver
- buttons, inputs, selects, textareas
- cards and stat tiles
- tables with empty/loading/error states
- filter bars
- status badges
- map container wrappers
- timeline component
- ride summary cards
- notification list items

## Folder Structure

```text
frontend/
|-- apps/
|   |-- rider-web/
|   |-- driver-ops/
|   `-- admin-dashboard/
|-- packages/
|   |-- api-client/
|   |-- auth/
|   |-- config/
|   |-- realtime/
|   `-- ui-kit/
|-- package.json
`-- tsconfig.base.json
```

## Starter Code Notes

This starter is intentionally architecture-first:

- workspace structure is in place
- each app has layouts and representative pages
- shared packages define the integration seams
- UI is responsive and role-specific
- backend contracts are represented in the shared API client and realtime package

## Next Steps

1. Install dependencies in `frontend`.
2. Connect Keycloak credentials and app URLs through env vars.
3. Add React Query and OIDC libraries.
4. Replace placeholder map blocks with a real maps provider.
5. Connect live pages to the backend APIs and `/ws`.
