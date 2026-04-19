# NetCloud Application Load Balancer - Agent Guide

## Repository Structure

```
netcloud/
├── netcloud-core/           # Shared modules (version 1.0)
│   ├── netcloud-commons/    # SecretGenerator, JWT, error handling, GCP service
│   ├── netcloud-core-model/ # Model classes
│   └── netcloud-core-dtos/  # DTOs
├── ApplicationLoadBalancer/
│   ├── control-plane/       # Autodiscovery & healthcheck service (port 9004)
│   ├── loadbalancer/        # Load balancing service (port 9342)
│   └── autodiscovery-client/# Server autodiscovery client
├── tenant_ws/               # Tenant workspace service
├── client-app/             # Client application
└── NetCloudUI/              # Node.js UI (net-ui/)
```

## Build Commands

```bash
# Control-plane (Spring Boot 4.0.1, Java 17)
cd ApplicationLoadBalancer/control-plane
./mvnw clean package  # Windows: mvnw.cmd

# Loadbalancer
cd ApplicationLoadBalancer/application-load-balancer/loadbalancer
./mvnw clean package

# Netcloud-core modules (must build before dependent modules)
cd netcloud-core
./mvnw clean install

# UI
cd NetCloudUI/net-ui && npm install && npm run dev
```

## Important Ports

| Service | Port |
|---------|------|
| control-plane | 9004 |
| loadbalancer | 9342 |
| autodiscovery-client | Inherits from parent |
| UI dev server | Check package.json |

## Shared Dependencies

All services depend on `netcloud-core` modules (version 1.0). **Build netcloud-core first** before building any dependent module:

```bash
cd netcloud-core && ./mvnw clean install
```

## Key Architecture Notes

- **control-plane** uses SHA-256 hashing for distributed health-check assignment across CP nodes
- **autodiscovery-client** pings `http://localhost:9004/lb/{tenantSecret}` (see `application.properties`)
- Redis configuration is commented out in YAML files (expects external Valkey/Redis at `10.160.0.5:6379`)
- `netcloud.secrets.secret` is shared across all services (hardcoded in YAML)

## Package Convention

Control-plane uses `com.netcloud.control_plane` (note underscore), not `com.netcloud.control-plane`.
