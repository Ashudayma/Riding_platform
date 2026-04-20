# Keycloak Setup Guide

## Realm

- Realm name: `riding-platform`

## Clients

- `riding-platform-api`
  Type: confidential for server-side integration and audience mapping
- `riding-platform-web`
  Type: public, Authorization Code + PKCE
- `riding-platform-rider-mobile`
  Type: public, Authorization Code + PKCE
- `riding-platform-driver-mobile`
  Type: public, Authorization Code + PKCE
- `riding-platform-admin`
  Type: confidential or public depending on deployment model
- `riding-platform-gateway`
  Type: confidential if an API gateway/BFF validates sessions and forwards JWTs

## Realm Roles

- `rider`
- `driver`
- `platform-admin`
- `ops-admin`
- `support-agent`
- `fraud-analyst`

## Recommended Client Scopes

- `profile`
- `email`
- `roles`
- `offline_access`
- custom optional scope: `platform-api`

## Token Claims

Add protocol mappers so access tokens include:

- `realm_access.roles`
- `resource_access.riding-platform-api.roles`
- `preferred_username`
- `email`
- `user_profile_id`

`user_profile_id` should map to the platform’s internal `identity.user_profile.id` after account bootstrap/sync.

## Login Flows

### Rider and Driver apps

- Use Authorization Code + PKCE
- Store refresh token only in secure platform-managed storage
- Rotate refresh tokens in Keycloak

### Admin and Support

- Prefer Authorization Code + PKCE behind HTTPS only
- Enforce MFA in Keycloak
- Restrict login by group/network if possible

## Passwordless and Social Login Extensibility

Use Keycloak identity brokering and authentication flows:

- Social login: Google, Apple, Facebook
- Passwordless: Email OTP, WebAuthn, SMS OTP
- Step-up MFA for:
  - admin access
  - fraud review actions
  - payout/payment changes
  - suspicious rider/driver activity

## Refresh Token and Session Strategy

The Spring Boot API is a stateless JWT resource server. It does **not** issue or persist sessions.

- Frontends/mobile apps obtain and refresh tokens directly with Keycloak
- Access token lifetime:
  - rider/driver: 5 to 10 minutes
  - admin/support/fraud: 5 minutes
- Refresh token lifetime:
  - rider/driver: 7 to 30 days depending on device trust
  - admin/support/fraud: shorter lifetime, ideally 8 to 12 hours
- Enable refresh token rotation
- Revoke sessions centrally in Keycloak for account lock or fraud response

## API Gateway Ready Design

Two good patterns:

1. Gateway validates JWT and forwards identity headers plus original token for downstream authz/audit
2. Gateway passes the bearer token through and Spring Boot validates it locally

For the current codebase, prefer:

- gateway performs coarse routing, WAF, and rate controls
- backend still validates JWT locally
- backend remains the source of authorization truth

## Endpoint Authorization Matrix

- `/api/v1/rider/**`
  `rider`, `support-agent`, `ops-admin`, `platform-admin` for read access
  `rider`, `ops-admin` for write access
- `/api/v1/driver/**`
  `driver`, `support-agent`, `ops-admin`, `platform-admin` for read access
  `driver`, `ops-admin` for write access
- `/api/v1/admin/**`
  `platform-admin`, `ops-admin`
- `/api/v1/support/**`
  `support-agent`, `ops-admin`, `platform-admin`
- `/api/v1/fraud/**`
  `fraud-analyst`, `ops-admin`, `platform-admin`
- `/api/v1/rides`
  `rider`, `ops-admin`, `platform-admin` for booking

## Secure Coding Checklist

- Never trust role names from client-side state; trust JWT claims only after signature and issuer validation
- Keep APIs stateless; do not create server sessions for bearer-token flows
- Require HTTPS everywhere, including internal ingress in production
- Do not log bearer tokens, refresh tokens, OTPs, card data, or PII-heavy request bodies
- Enforce least privilege at both route and method level
- Use idempotency keys for booking/payment-like write operations
- Apply Redis-backed rate limiting at gateway and application layers
- Enable MFA for privileged operator accounts
- Rotate Keycloak client secrets and restrict admin console access
- Audit sensitive actions such as admin investigations, fraud reviews, booking overrides, and payment operations
- Use short-lived access tokens and rotated refresh tokens
- Validate issuer and expected audience
- Add anti-automation protections for public auth entry points
- Prefer passwordless/social login through Keycloak flows rather than homegrown auth logic
