# NeoChat Architecture

## Overview

NeoChat is a secure, auditable tutoring platform that connects students with tutors through real-time chat. The backend is built with Quarkus, using a microservices architecture with event-driven communication.

For the authoritative data boundary and schema blueprint, see [DATA_MODEL.md](DATA_MODEL.md).

## System Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Ingress Layer                         │
│                     (NGINX/Kubernetes)                       │
└────────────────────────┬────────────────────────────────────┘
                         │
          ┌──────────────┴──────────────┐
          │                             │
┌─────────▼─────────┐         ┌────────▼────────┐
│  Chat Service     │         │  Admin Service  │
│  (Port 8080)      │         │  (Port 8081)    │
│  - WebSocket      │         │  - REST API     │
│  - Kafka Producer │         │  - User Mgmt    │
│  - Event Consumer │         │  - Audit Logs   │
└───────────────────┘         └─────────────────┘
          │                             │
          └──────────────┬──────────────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
   ┌────▼────┐      ┌────▼────┐     ┌────▼────┐
   │MongoDB  │      │PostgreSQL│     │ Kafka   │
   │(Hot)    │      │(Cold)   │     │(Events) │
   └─────────┘      └─────────┘     └─────────┘
```

### Components

#### 1. Chat Service
- **WebSocket Endpoint**: Stateless real-time communication
- **Kafka Producer**: Publishes chat events to Kafka topics
- **Kafka Consumer**: Consumes events and persists to storage
- **Event Repository**: Manages dual storage (MongoDB + PostgreSQL)
- **Conversation Repository**: Manages conversation metadata

#### 2. Admin Service
- **Admin REST API**: User management operations
- **Audit REST API**: Audit log queries
- **User Management Service**: CRUD operations for users
- **Audit Service**: Comprehensive audit logging

#### 3. Common Module
- **Security Filters**: JWT authorization and identity token authentication
- **Token Services**: JWT signing and validation
- **Vault Integration**: Key management (filesystem, HashiCorp, Azure)
- **Storage Abstractions**: Event store interfaces

## Security Architecture

### Authentication & Authorization Flow

```
1. Client connects with Bearer token (OIDC/Keycloak)
2. JWTAuthZFilter validates token (Priority: AUTHENTICATION)
3. IdentityTokenAuthNFilter validates identity (Priority: AUTHENTICATION + 1)
4. Request proceeds to endpoint with TokenClaims attached
```

### Security Layers

1. **JWTAuthZFilter** (Priority 1000)
   - Validates Bearer token from Authorization header
   - Checks JWT signature and scopes
   - Returns 401 if token missing/invalid
   - Returns 403 if scopes insufficient

2. **IdentityTokenAuthNFilter** (Priority 1001)
   - Extracts identity_token from request body/header
   - Validates against OIDC ID token
   - Regenerates token if expired
   - Checks role-based access (@RolesAllowed)

3. **Token Service**
   - Signs identity tokens with RS256
   - Minimal payload: sub, email, roles, groups
   - Integrates with Vault for key management

4. **Vault Key Provider**
   - Supports multiple backends:
     - Filesystem: `/keys/private.pem`
     - HashiCorp Vault: KV secrets engine
     - Azure Key Vault: Managed keys
   - Caches keys in memory for performance

## Data Architecture

### Storage Strategy

#### Hot Storage (MongoDB)
- **Purpose**: Recent events (last 30 days)
- **TTL**: Automatic expiration after 30 days
- **Collections**:
  - `conversations`: Conversation metadata
  - `mongoeventdocument`: Recent chat events
  - `users`: User profiles

#### Cold Storage (PostgreSQL)
- **Purpose**: Historical events (permanent)
- **Partitioning**: Monthly partitions by timestamp
- **Tables**:
  - `chat_events`: All chat events (partitioned)
  - `audit_logs`: Comprehensive audit trail

### Event Flow

```
1. WebSocket message received → ChatEventService validates
2. ChatEventService → KafkaEventProducer publishes event
3. Kafka topic: chat-events (partitioned by conversation_id)
4. ChatEventConsumer subscribes to topic
5. Event stored in both MongoDB (hot) and PostgreSQL (cold)
6. WebSocket clients receive broadcast
```

## Message Flow

### Chat Message Lifecycle

1. **Client Sends Message**
   ```json
   {
     "conversation_id": "uuid",
     "identity_token": "jwt",
     "content": "Hello",
     "message_type": "text"
   }
   ```

2. **Server Processing**
   - Validate identity token
   - Check conversation access
   - Create immutable ChatEvent
   - Publish to Kafka with conversation_id as partition key

3. **Event Persistence**
   - Consumer writes to MongoDB (TTL 30 days)
   - Consumer writes to PostgreSQL (permanent)
   - Broadcast to WebSocket subscribers

4. **Client Receives**
   ```json
   {
     "event_id": "uuid",
     "conversation_id": "uuid",
     "event_type": "message",
     "payload": "Hello",
     "user_id": "uuid",
     "timestamp": "2024-01-01T12:00:00Z"
   }
   ```

## Kafka Architecture

### Topics

- **chat-events**
  - Partitions: 10
  - Replication Factor: 3
  - Retention: 30 days
  - Partition Key: `conversation_id` (ensures ordering per conversation)

### Consumer Groups

- **chat-service-consumer**: Persists events to storage

## Deployment Architecture

### Kubernetes Resources

1. **Deployments**
   - `chat-service`: 3 replicas (stateless)
   - `admin-service`: 2 replicas

2. **Services**
   - `chat-service`: ClusterIP on port 8080
   - `admin-service`: ClusterIP on port 8081

3. **Ingress**
   - `/api/chat` → chat-service
   - `/api/admin` → admin-service
   - `/api/audit` → admin-service
   - WebSocket support enabled

4. **Infrastructure** (Helm Charts)
   - PostgreSQL (Bitnami): Persistent storage with partitioning
   - MongoDB (Bitnami): Replica set with TTL indexes
   - Kafka (Strimzi): 3-broker cluster with Zookeeper
   - Vault (HashiCorp): Secret management
   - Keycloak (Bitnami): OIDC provider

### Monitoring

- **Health Checks**: `/q/health/live` and `/q/health/ready`
- **Metrics**: Prometheus endpoint at `/q/metrics`
- **Logging**: JSON-formatted logs in production

## Development Workflow

### Local Development

1. **Start infrastructure**:
   ```bash
   docker-compose -f docker-compose.dev.yml up -d
   ```

2. **Generate keys**:
   ```bash
   ./scripts/generate-keys.sh
   ```

3. **Initialize database**:
   ```bash
   ./scripts/init-db.sh
   ```

4. **Run services**:
   ```bash
   mvn quarkus:dev -pl chat-service
   mvn quarkus:dev -pl admin-service
   ```

### Kubernetes Development (Tilt)

```bash
tilt up
```

Tilt provides:
- Live code reloading
- Resource dashboard
- Log streaming
- Port forwarding

## CI/CD Pipeline

### Build Pipeline
1. Checkout code
2. Set up JDK 21
3. Build with Maven
4. Run tests
5. Build Docker images
6. Upload artifacts

### Deploy Pipeline
1. Build production images
2. Push to Docker registry
3. Update Kubernetes manifests
4. Rolling deployment
5. Health check verification

## Configuration

### Environment Variables

#### Chat Service
- `MONGODB_URL`: MongoDB connection string
- `POSTGRES_URL`: PostgreSQL connection string
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka brokers
- `OIDC_AUTH_SERVER_URL`: Keycloak URL
- `VAULT_URL`: Vault server URL
- `VAULT_PROVIDER`: Key provider (filesystem/hashicorp/azure)

#### Admin Service
- Same as chat-service (no Kafka required)

## Scalability Considerations

1. **Horizontal Scaling**
   - Stateless WebSocket endpoints
   - Kafka partitioning for parallel processing
   - Read replicas for databases

2. **Performance Optimization**
   - Key caching in VaultKeyProvider
   - MongoDB for hot data access
   - Kafka for asynchronous processing

3. **Data Retention**
   - Automatic TTL in MongoDB (30 days)
   - Partitioned PostgreSQL for efficient queries
   - Regular cleanup jobs for audit logs

## Security Best Practices

1. **Token Management**
   - RS256 signatures (asymmetric)
   - Short-lived tokens
   - Secure key storage in Vault

2. **Network Security**
   - TLS for all external communication
   - mTLS for inter-service communication (future)
   - Network policies in Kubernetes

3. **Access Control**
   - Role-based access control (RBAC)
   - Conversation-level authorization
   - Comprehensive audit logging

## Future Enhancements

1. **Observability**
   - Distributed tracing (OpenTelemetry)
   - Service mesh (Istio)
   - Advanced metrics dashboards

2. **Features**
   - File sharing support
   - Video call integration
   - Enhanced notification system

3. **Performance**
   - Redis caching layer
   - GraphQL API
   - Native compilation (GraalVM)
