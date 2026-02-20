# NeoChat Data Model Blueprint

## 1) Scope and Decision

This document defines the target data model strategy for NeoChat with clear separation between application data and identity provider data.

### Decision Summary
- Keep **NeoChat core application data** in a single PostgreSQL database.
- Keep **Keycloak data** in a **separate PostgreSQL database**.
- Keep MongoDB and Kafka as independent data systems (not schema-bound to PostgreSQL).

This enables switching from Keycloak to another OIDC provider (Entra ID, Okta, Ping, etc.) without leaving identity tables in the application database.

---

## 2) Data Boundary Model

## 2.1 NeoChat Application Data Plane
- PostgreSQL DB: `neochat_app` (single DB for app-owned relational data)
- MongoDB DB(s): app-owned document data (conversation/user-profile cache or app metadata)
- Kafka topics: app-owned event streams

## 2.2 Identity Provider Data Plane
- PostgreSQL DB: `keycloak` (Keycloak-owned relational data)
- Managed exclusively by Keycloak migrations/versioning
- No application service writes directly to Keycloak DB tables

---

## 3) PostgreSQL Logical Model (NeoChat App DB)

## 3.1 Database
- Database name: `neochat_app`

## 3.2 Schemas
- `chat` — chat event and conversation-adjacent relational data
- `admin` — administrative and audit data

### Recommended Baseline DDL
```sql
CREATE DATABASE neochat_app;

\c neochat_app;

CREATE SCHEMA IF NOT EXISTS chat;
CREATE SCHEMA IF NOT EXISTS admin;
```

## 3.3 Core Tables (Current/Planned)

### chat.chat_events
Purpose: immutable event ledger for long-term chat history

```sql
CREATE TABLE IF NOT EXISTS chat.chat_events (
    id UUID PRIMARY KEY,
    conversation_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    payload TEXT,
    user_id VARCHAR(255) NOT NULL,
    timestamp TIMESTAMP NOT NULL
) PARTITION BY RANGE (timestamp);

CREATE TABLE IF NOT EXISTS chat.chat_events_default
    PARTITION OF chat.chat_events DEFAULT;

CREATE INDEX IF NOT EXISTS idx_chat_events_conversation
    ON chat.chat_events(conversation_id);
CREATE INDEX IF NOT EXISTS idx_chat_events_timestamp
    ON chat.chat_events(timestamp);
CREATE INDEX IF NOT EXISTS idx_chat_events_user
    ON chat.chat_events(user_id);
```

### admin.audit_logs
Purpose: audit trail for administrative actions

```sql
CREATE TABLE IF NOT EXISTS admin.audit_logs (
    id UUID PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent TEXT,
    status VARCHAR(50) NOT NULL,
    details TEXT
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_user
    ON admin.audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp
    ON admin.audit_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_logs_resource
    ON admin.audit_logs(resource_type, resource_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action
    ON admin.audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_status
    ON admin.audit_logs(status);
```

---

## 4) Keycloak PostgreSQL Model

## 4.1 Database
- Database name: `keycloak`
- Ownership: Keycloak runtime and migration engine

## 4.2 Schema Strategy
- Default schema `public` is acceptable for Keycloak because DB is dedicated.
- Optional: isolate further with `keycloak` schema if required by governance.

## 4.3 Contract
- NeoChat services interact with Keycloak via OIDC/JWKS/Admin API only.
- NeoChat services do **not** query Keycloak DB tables directly.

---

## 5) MongoDB Model (Non-Relational)

MongoDB is not schema-based like PostgreSQL. Isolation is done by database + collection boundaries.

### Recommended Mongo DB Layout
- `neochat_chat`: conversations and hot/recent chat documents
- `neochat_admin`: optional admin app documents (if retained)

### Typical Collections
- `conversations`
- `users` (only if app-owned profile data remains outside IdP)
- `chat_events_hot` (optional naming for clarity)

### Retention Guidance
- If hot event retention is required (e.g., 30 days), enforce with TTL indexes and/or scheduled cleanup.
- Do not rely on comments/documentation-only TTL assumptions.

---

## 6) Kafka Event Model (Non-Relational)

Kafka does not use DB schemas; isolation uses topic namespace conventions and ACLs.

### Recommended Topic Namespace
- `neochat.chat.events`
- `neochat.admin.audit`
- Environment prefixes (optional): `dev.neochat.*`, `stg.neochat.*`, `prod.neochat.*`

### Governance
- Partition by `conversation_id` for ordering guarantees.
- Retention and compaction policies defined per topic.

---

## 7) Environment Blueprint

## 7.1 Development
- `neochat_app` DB + `keycloak` DB on same Postgres instance acceptable.
- Separate init scripts:
  - `init-neochat-app.sql`
  - `init-keycloak-db.sql`

## 7.2 Staging/Production
- Keep separate DBs at minimum.
- Prefer separate Postgres instances for stricter blast-radius and compliance boundaries.
- Enforce independent backup/restore, credentials, and retention policies.

---

## 8) Initialization and Migration Contract

## 8.1 NeoChat App Init Responsibilities
- Create `chat` and `admin` schemas in `neochat_app`.
- Create app-owned tables, indexes, and partition functions.

## 8.2 Keycloak Init Responsibilities
- Create `keycloak` database and dedicated DB user.
- Let Keycloak bootstrap and migrate its own schema/tables.

## 8.3 Decommission Path (If Switching IdP)
- Disable Keycloak integration in app config.
- Preserve/export realm config if needed.
- Drop/decommission the dedicated `keycloak` DB without touching `neochat_app`.

---

## 9) Architecture Constraints and Guardrails

- No cross-database foreign keys between `neochat_app` and `keycloak`.
- No direct reads from Keycloak tables by application services.
- Identities are linked by stable OIDC subject identifiers (`sub`) and issuer (`iss`), not by DB joins.
- Any app-specific identity metadata is stored in app-owned stores (Postgres/Mongo) with explicit ownership.

---

## 10) Open Implementation Items (Current Repository)

- Align service config defaults so app services target `neochat_app` consistently in all profiles.
- Keep Keycloak JDBC URL pointed to `keycloak` DB only.
- Validate Mongo retention behavior with explicit TTL index creation where intended.
- Namespace Kafka topics for environment and domain clarity.
