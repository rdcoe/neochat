<!-- markdownlint-disable MD022 MD031 MD032 MD034 MD040 MD060 -->

# NeoChat Backend

A secure, auditable tutoring platform that connects students with tutors through real-time chat.

## Features

- **Real-time Chat**: WebSocket-based messaging with Kafka event streaming
- **Dual Storage**: Hot storage (MongoDB) for recent data, cold storage (PostgreSQL) for historical data
- **Security**: JWT-based authentication with OIDC integration (Keycloak)
- **Audit Trail**: Comprehensive logging of all administrative actions
- **Scalable**: Kubernetes-native with horizontal scaling support
- **Event-Driven**: Kafka-based event processing for reliability

## Architecture

NeoChat uses a microservices architecture built with Quarkus:

- **Chat Service**: Real-time WebSocket chat with Kafka event processing
- **Admin Service**: User management and audit logging
- **Common Module**: Shared security and storage components

For detailed architecture documentation, see [ARCHITECTURE.md](ARCHITECTURE.md).

## Technology Stack

- **Framework**: Quarkus 3.15.1
- **Language**: Java 21
- **Databases**: PostgreSQL (cold storage), MongoDB (hot storage)
- **Message Broker**: Apache Kafka
- **Authentication**: Keycloak (OIDC)
- **Secret Management**: HashiCorp Vault
- **Container Orchestration**: Kubernetes
- **Build Tool**: Maven

## Project Structure

```
neochat/
├── common/              # Shared security and storage components
├── chat-service/        # Real-time chat service
├── admin-service/       # Administration and audit service
├── k8s/                 # Kubernetes manifests
├── docker/              # Docker configurations
├── scripts/             # Utility scripts
└── .github/workflows/   # CI/CD pipelines
```

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose
- kubectl (for Kubernetes deployment)

### Local Development

1. **Start infrastructure services**:
   ```bash
   docker-compose -f docker-compose.dev.yml up -d
   ```

2. **Generate RSA keys for JWT signing**:
   ```bash
   ./scripts/generate-keys.sh
   ```

3. **Initialize PostgreSQL database**:
   ```bash
   ./scripts/init-db.sh
   ```

4. **Run chat service**:
   ```bash
   mvn quarkus:dev -pl chat-service
   ```

5. **Run admin service** (in another terminal):
   ```bash
   mvn quarkus:dev -pl admin-service
   ```

The services will be available at:
- Chat Service: http://localhost:8080
- Admin Service: http://localhost:8081
- Keycloak: http://localhost:8180

### Building the Project

```bash
# Build all modules
mvn clean package

# Skip tests
mvn clean package -DskipTests

# Build Docker images
docker build -f docker/Dockerfile -t neochat/chat-service:latest .
docker build -f docker/Dockerfile -t neochat/admin-service:latest .

# Build native images (requires GraalVM)
docker build -f docker/Dockerfile.native -t neochat/chat-service:native .
```

### Running Tests

```bash
# Run all tests
mvn test

# Run tests for specific module
mvn test -pl chat-service
mvn test -pl admin-service
```

## Kubernetes Deployment

### Using Kind (Local)

1. **Create Kind cluster**:
   ```bash
   kind create cluster --config k8s/kind-config.yml
   ```

2. **Install infrastructure** (Helm charts):
   ```bash
   # Add Helm repositories
   helm repo add bitnami https://charts.bitnami.com/bitnami
   helm repo add hashicorp https://helm.releases.hashicorp.com
   helm repo update

   # Install PostgreSQL
   helm install postgresql bitnami/postgresql -f k8s/postgres-helm.yml -n neochat --create-namespace

   # Install MongoDB
   helm install mongodb bitnami/mongodb -f k8s/mongodb-helm.yml -n neochat

   # Install Strimzi Kafka Operator
   kubectl create -f 'https://strimzi.io/install/latest?namespace=neochat' -n neochat
   kubectl apply -f k8s/kafka-strimzi.yml -n neochat

   # Install Vault
   helm install vault hashicorp/vault -f k8s/vault-helm.yml -n neochat

   # Install Keycloak
   helm install keycloak bitnami/keycloak -f k8s/keycloak-helm.yml -n neochat
   ```

3. **Deploy NeoChat services**:
   ```bash
   kubectl apply -k k8s/
   ```

### Using Tilt (Development)

For rapid development with live reloading:

```bash
tilt up
```

Access Tilt dashboard at http://localhost:10350

## API Documentation

### Chat Service

#### WebSocket Endpoint

Connect to: `ws://localhost:8080/api/chat/{conversationId}?authorization=Bearer%20{token}`

Send message:
```json
{
  "conversation_id": "uuid",
  "identity_token": "jwt-token",
  "content": "Hello!",
  "message_type": "text"
}
```

### Admin Service

#### User Management

- `POST /api/admin/users` - Create user
- `GET /api/admin/users/{userId}` - Get user
- `GET /api/admin/users` - List users
- `PUT /api/admin/users/{userId}/roles` - Update roles
- `PUT /api/admin/users/{userId}/status` - Update status
- `DELETE /api/admin/users/{userId}` - Delete user

#### Audit Logs

- `GET /api/audit/users/{userId}` - Get user audit logs
- `GET /api/audit/resources/{type}/{id}` - Get resource audit logs
- `GET /api/audit/time-range` - Get logs by time range
- `GET /api/audit/actions/{action}` - Get logs by action
- `GET /api/audit/failed` - Get failed actions

All admin endpoints require authentication with appropriate roles (admin/overseer).

## Configuration

### Environment Variables

Key configuration options (see `application.yml` for full list):

| Variable | Description | Default |
|----------|-------------|---------|
| `MONGODB_URL` | MongoDB connection string | mongodb://localhost:27017 |
| `POSTGRES_URL` | PostgreSQL connection string | postgresql://localhost:5432/neochat |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka brokers | localhost:9092 |
| `OIDC_AUTH_SERVER_URL` | Keycloak server URL | http://localhost:8180/realms/neochat |
| `VAULT_URL` | Vault server URL | http://localhost:8200 |
| `VAULT_PROVIDER` | Key provider type | filesystem |

### Profiles

- **dev**: Development profile (application-dev.yml)
- **prod**: Production profile (application-prod.yml)

Activate profile:
```bash
mvn quarkus:dev -Dquarkus.profile=dev
```

## Security

### Key Management

Generate RSA keys for JWT signing:
```bash
./scripts/generate-keys.sh /path/to/keys
```

Keys can be stored in:
- **Filesystem**: Local files (development)
- **HashiCorp Vault**: KV secrets engine (recommended)
- **Azure Key Vault**: Managed keys (cloud)

### Authentication Flow

1. User authenticates with Keycloak (OIDC)
2. Receives JWT access token
3. Client sends token in Authorization header
4. JWTAuthZFilter validates token
5. IdentityTokenAuthNFilter validates identity
6. Request processed with user context

## Monitoring

### Health Checks

- Liveness: `/q/health/live`
- Readiness: `/q/health/ready`

### Metrics

Prometheus metrics: `/q/metrics`

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests: `mvn test`
5. Submit a pull request

## License

This project is licensed under the MIT License.

## Support

For issues and questions:
- GitHub Issues: https://github.com/rdcoe/neochat/issues
- Documentation: [ARCHITECTURE.md](ARCHITECTURE.md)
