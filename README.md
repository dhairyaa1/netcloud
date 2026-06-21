# NetCloud — Application Load Balancer

A **distributed, cloud-native application load balancer** built with Spring Boot 4 and Java 17. Features intelligent routing algorithms, automated server health monitoring, consistent hash-based edge proxying, and a real-time pool management system backed by Redis pub/sub.

---

## Architecture Overview

```
                         ┌──────────────────┐
                         │   NetCloud UI    │
                         │  (Angular, :80)  │
                         └────────┬─────────┘
                                  │
                                  ▼
┌──────────────────────────────────────────────────────┐
│              netcloud-edge-service (:9679)            │
│         Consistent Hash Ring → Round Robin           │
│     (subscribes to `lb:active` Redis channel)        │
└──────────────────────┬───────────────────────────────┘
                       │ HTTP
                       ▼
┌──────────────────────────────────────────────────────┐
│             loadbalancer (:80 → :9342)                │
│    Round Robin / Least Connections routing           │
│    Local cache synced via Valkey pub/sub             │
│    Failure detection → deadpool promotion            │
└──┬───────────────────────────────┬───────────────────┘
   │  HTTP (proxy to backend)      │  Redis pub/sub
   ▼                               ▼
┌──────────────┐   ┌──────────────────────┐   ┌──────────────────┐
│  client-app  │   │   control-plane      │   │  loadbalancer_ws │
│  (:8080)     │   │   (:9004)            │   │  (:9090)         │
│              │   │                      │   │                  │
│ Registers    │◄──│ Health checks        │   │ LB config CRUD   │
│ via HTTP     │   │ Deadpool monitoring  │   │ Secret generation│
│ ping to CP   │   │ SHA-256 partitioning │   │ MongoDB + Redis  │
└──────┬───────┘   └─────────┬────────────┘   └────────┬─────────┘
       │                     │                          │
       ▼                     ▼                          ▼
┌──────────────────────────────────────────────────────────────┐
│                    Valkey / Redis (:6379)                     │
│                                                              │
│  Hashes:  activepool:{lbId},  deadpool:{lbId}               │
│  Strings: lb:{lbId}:details                                  │
│  Sets:    lb:active                                           │
│  Pub/Sub: activepool:*, deadpool:*, lb:active                │
└──────────────────────────────────────────────────────────────┘
                              │
                              ▼
                     ┌────────────────┐
                     │  MongoDB Atlas │
                     │                │
                     │ loadbalancer_  │
                     │   config       │
                     │ tenant         │
                     └────────────────┘
```

---

## Services

### 1. `control-plane` (:9004)
**Auto-discovery & Health Check Service**

- Backend servers ping `/{tenantSecret}` on startup to register themselves
- Decrypts the tenant secret to determine `tenantId`, `lbId`, and health check interval
- Registers the server in Redis active pool and publishes events to loadbalancers
- Monitors dead pool via SHA-256 consistent partitioning across CP nodes
- Exponential backoff health checks (30s → 300s), promotes servers back on 3 consecutive successes

### 2. `loadbalancer` (:80 → :9342)
**Core Request Routing Engine**

- Routes incoming `/{tenantSecret}/**` requests to healthy backend servers
- **Round Robin**: Atomic counters per tenant, modulo server selection, retry on failure
- **Least Connections**: Sorts by `connectionCount`, connection tracking via `AtomicLong`
- Extensible — supports `WEIGHTED_ROUND_ROBIN`, `IP_HASH`, `ANT_COLONY`, `WEIGHTED_LEAST_CONNECTION`
- Local cache (`ValkeyCache`) synced via Redis pub/sub — avoids Redis hits per request
- **Failure detection**: 2 consecutive failures → deadpool; 2 consecutive successes → recovery (weight 0.1)

### 3. `loadbalancer_ws` (:9090)
**Load Balancer Configuration API**

- CRUD for load balancer configurations (persisted in MongoDB)
- Generates AES-encrypted URL secret (`SecretGenerator`) containing `tenantId|interval|lbId`
- Writes configs to Redis `lb:{lbId}:details` for real-time consumption by loadbalancer

### 4. `tenant_ws` (:9020)
**Tenant Management Service**

- Registration, login, and profile management
- JWT authentication (HMAC-SHA256, 24h expiry)
- BCrypt password hashing
- Spring Security with stateless sessions

### 5. `client-app` (:8080)
**Demo Backend Application**

- Sample backend that registers with control-plane on startup
- Exposes `/api/health`, `/api/request`, `/api/slow` endpoints
- Uses `autodiscovery-client` library for auto-registration with retry logic

### 6. `netcloud-edge-service` (:9679)
**Edge Proxy with Consistent Hashing**

- Entry point for all external traffic
- Builds a consistent hash ring from the global pool of active loadbalancer instances
- SHA-256 virtual node placement on a `TreeMap<Long, Server>`
- Each tenant gets `serversPerTenant` (default: 2) LB instances with max 150 virtual nodes
- Request routed to selected LB via round-robin among replicas (default: 1)
- Listens on Redis `lb:active` channel for LB instance lifecycle events

### 7. `netcloud-core` (shared library)
Three shared modules:

| Module | Purpose |
|--------|---------|
| `netcloud-commons` | `SecretGenerator` (AES), `JwtUtil`, `GCPUtil`, `HostUtil`, `LRUCache`, `IdGenerator`, error handling |
| `netcloud-core-dtos` | `Server`, `LoadBalancerDto`, `LbConfig`, `Tenant` DTOs, enums (`RoutingAlgorithm`, `ServerState`, etc.) |
| `netcloud-core-model` | MongoDB `@Document` classes: `LoadBalacerConfig`, `Tenant` |

---

## Data Flow

### Request Routing
```
Client → edge-service → consistent hash → loadbalancer → algorithm → backend server
```

### Auto-Discovery
```
Backend starts → pings control-plane /{tenantSecret} → CP registers in Redis
→ Pub/sub notifies loadbalancer → local cache updated
```

### Health Check / Recovery
```
Loadbalancer detects failure → moves server to deadpool (Redis + pub/sub)
→ Control-plane picks up event (SHA-256 partitioned) → monitors with backoff
→ 3 successes → promoted back to active pool
```

### Load Balancer Configuration
```
UI/API → loadbalancer_ws → MongoDB (persist) + Redis lb:{lbId}:details (cache)
→ Loadbalancer reads from Redis per request
```

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| **Runtime** | Java 17 |
| **Framework** | Spring Boot 4.0.1 / 3.4.4 |
| **Database** | MongoDB Atlas |
| **Cache & Pub/Sub** | Valkey / Redis 7.2 |
| **Build** | Maven (multi-module) |
| **Security** | AES-256, JWT (HMAC-SHA256), BCrypt |
| **Mapping** | MapStruct |
| **Frontend** | Angular (NetCloud UI) |
| **Container** | Docker, docker-compose |
| **Cloud** | Google Cloud Run (edge-service) |

---

## Port Map

| Service | Internal | Docker |
|---------|----------|--------|
| control-plane | 9004 | 9004 |
| loadbalancer | 80 | 9342 |
| loadbalancer_ws | 9090 | — |
| tenant_ws | 9020 | — |
| client-app | 8080 | 8080 |
| netcloud-edge-service | 9679 | — |
| NetCloud UI | 80 | 80 |
| Valkey/Redis | 6379 | 6379 |

---

## Getting Started

### Prerequisites
- Java 17+
- Docker & docker-compose
- MongoDB Atlas (or local instance)

### Build & Run

```bash
# 1. Build shared libraries first
cd netcloud-core
./mvnw clean install

# 2. Build services (examples)
cd ../ApplicationLoadBalancer/control-plane
./mvnw clean package

cd ../application-load-balancer/loadbalancer
./mvnw clean package

# 3. Run with Docker
docker-compose up -d
```

---

## API Reference

### control-plane (:9004)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/{tenantSecret}` | Register backend server for LB |

### loadbalancer (:9342)
| Method | Path | Description |
|--------|------|-------------|
| ANY | `/{tenantSecret}/**` | Route request to backend |
| GET | `/` | Health probe |

### loadbalancer_ws (:9090)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/lb/save` | tenant-id header | Create LB config |
| PUT | `/lb` | tenant-id header | Update LB config |
| GET | `/lb` | tenant-id header | List LB configs |
| GET | `/lb/{lbId}` | tenant-id header | Get LB config |
| DELETE | `/lb/{lbId}` | tenant-id header | Delete LB config |
| GET | `/lb/interval/{tenantSecret}` | — | Decode health interval |
| GET | `/lb/tenant/{tenantSecret}` | — | Decode tenant secret |

### tenant_ws (:9020)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/tenant/register` | — | Register tenant |
| POST | `/tenant/login` | — | Login, returns JWT |
| GET | `/tenant/getTenant` | JWT | Get tenant profile |
| POST | `/tenant/save` | JWT | Create tenant |

### client-app (:8080)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/request` | Returns instance ID |
| POST | `/api/request` | Echoes body |
| GET | `/api/health` | Health check |
| GET | `/api/slow` | Simulated slow response |

---

## Algorithms

| Algorithm | Component | Details |
|-----------|-----------|---------|
| **Round Robin** | loadbalancer | Atomic counter per tenant, modulo selection |
| **Least Connections** | loadbalancer | Sort by connection count, connection tracking |
| **Consistent Hashing** | edge-service | SHA-256 virtual nodes on `TreeMap`, replica selection |
| **SHA-256 Partitioning** | control-plane | Distributes health check responsibility across CP nodes |
| **Exponential Backoff** | control-plane | 30s × 2^level, capped at 300s for dead pool checks |
| **AES-256** | commons | Tenant URL secret encryption/decryption |
| **JWT HMAC-SHA256** | commons | Token generation & validation |
