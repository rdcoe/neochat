# Tiltfile for local Kubernetes development with Tilt

# Load Kubernetes manifests
k8s_yaml([
  'k8s/namespace.yml',
  'k8s/neochat-backend-deployment.yml',
  'k8s/neochat-backend-service.yml',
  'k8s/ingress.yml',
])

# Build Docker images locally
docker_build('neochat/chat-service', '.', 
  dockerfile='docker/Dockerfile',
  target='chat-service',
  live_update=[
    sync('./chat-service/src', '/app/chat-service/src'),
    run('cd /app && mvn compile -pl chat-service', trigger=['./chat-service/src/**/*.java']),
  ]
)

docker_build('neochat/admin-service', '.', 
  dockerfile='docker/Dockerfile',
  target='admin-service',
  live_update=[
    sync('./admin-service/src', '/app/admin-service/src'),
    run('cd /app && mvn compile -pl admin-service', trigger=['./admin-service/src/**/*.java']),
  ]
)

# Define Kubernetes resources
k8s_resource('chat-service',
  port_forwards=['8080:8080'],
  labels=['backend'],
  resource_deps=['postgres', 'mongodb', 'kafka']
)

k8s_resource('admin-service',
  port_forwards=['8081:8081'],
  labels=['backend'],
  resource_deps=['postgres', 'mongodb']
)

# Local services (via docker-compose)
local_resource('dependencies',
  cmd='docker-compose -f docker-compose.dev.yml up -d',
  labels=['infrastructure']
)

# Generate keys if they don't exist
local_resource('generate-keys',
  cmd='./scripts/generate-keys.sh /tmp/keys',
  labels=['setup']
)

# Initialize database
local_resource('init-db',
  cmd='sleep 10 && ./scripts/init-db.sh',
  resource_deps=['dependencies'],
  labels=['setup']
)
