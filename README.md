# MedConnect

Medicine stock-out prediction and redistribution platform for primary health centres and district hospitals. Tracks inventory across facilities, forecasts demand from consumption history, flags stock-out and expiry risks, and recommends inter-facility transfers to prevent shortages and wastage.

Built for the GreenOps VIT hackathon.

## What it does

A district health officer can ask: *"Which facilities are about to run out of Paracetamol, and where can we move stock from?"*

The system pulls live stock levels and 30 days of consumption data, runs demand forecasting, and returns something like:

> PHC-A will stock out of Paracetamol in 5.7 days (42/day demand, 240 units left).  
> Transfer 400 units from PHC-D (1200 units, 18 km away) — they have surplus above safety stock.  
> Batch BATCH-PC-001 at PHC-C (500 units, expires in 20 days) won't be consumed in time at current demand.

The dashboard shows risk levels across all facilities, redistribution recommendations with distance and transfer quantities, and expiry alerts at the batch level.

## Architecture

Two Spring Boot microservices + a React frontend:

```
┌─────────────────┐       REST        ┌─────────────────────┐
│  Inventory Svc  │◄────────────────► │   Forecast Svc      │
│  (port 8081)    │                   │   (port 8082)       │
│                 │                   │                     │
│  - Facilities   │                   │  - Demand forecast  │
│  - Medicines    │                   │  - Risk scoring     │
│  - Stock/Batch  │                   │  - Expiry detection │
│  - Consumption  │                   │  - Redistribution   │
│                 │                   │                     │
│  PostgreSQL     │                   │  Stateless          │
└─────────────────┘                   └─────────────────────┘
         ▲                                      ▲
         │              ┌──────────┐            │
         └──────────────│  React   │────────────┘
                        │  UI      │
                        │  :5173   │
                        └──────────┘
```

**Inventory Service** — owns the data. Facilities, medicines, stock batches (with expiry dates), and daily consumption records. Exposes REST endpoints for querying and filtering. Seeds demo data on first startup (5 facilities in Vellore district, 3 essential medicines, 30 days of consumption history).

**Forecast Service** — stateless computation. Pulls data from Inventory Service via REST, runs forecasting and risk analysis, returns results. No database of its own.

**UI Dashboard** — React + TypeScript + Tailwind. Summary KPI cards, filterable analysis table, redistribution recommendations, expiry alerts. Talks to both services directly.

## Forecasting approach

Not a black-box ML model — the logic is transparent and explainable:

- **Demand forecast**: Computes 7-day and 30-day moving averages separately. Uses the 7-day average when it diverges >20% from the 30-day (detects rising/falling trends), otherwise uses the more stable 30-day average.
- **Days-to-stock-out**: `usable stock / forecast daily demand`. Simple division, but handles edge cases (zero stock = 0 days, near-zero demand = no risk).
- **Risk classification**: CRITICAL (0–3 days), HIGH (4–7), MEDIUM (8–14), LOW (>14). Thresholds are configurable in `application.properties`.
- **Expiry detection**: FEFO (First-Expiry-First-Out) allocation. Sorts batches by expiry date, walks through them deducting forecast consumption, flags batches that won't be fully consumed before they expire.
- **Redistribution**: For each facility at HIGH/CRITICAL risk, scans all other facilities for surplus. Calculates transferable stock (current stock minus safety buffer), ranks donors by surplus quantity then distance (Haversine), rounds transfers to pack sizes of 100 units. Never recommends a transfer that would put the donor below their own safety stock.

Every recommendation comes with a plain-English explanation showing the numbers behind it.

## Tech stack

| Layer | Tech |
|---|---|
| Backend | Java, Spring Boot 4.1.1, Spring Data JPA, Hibernate |
| Database | PostgreSQL |
| Frontend | React 19, TypeScript, Vite, Tailwind CSS 4 |
| Inter-service | REST (Spring RestClient) |

## Prerequisites

- Java 21+
- PostgreSQL running locally
- Node.js 18+
- Maven

## Setup

### Database

Create a PostgreSQL database called `mediconnect`. The default config expects postgres on `localhost:5432` with user `postgres`. Update credentials in `inventory-service/src/main/resources/application.properties` if yours differ.

```sql
CREATE DATABASE mediconnect;
```

Tables are auto-created by Hibernate on startup (`ddl-auto=update`), and demo data is seeded automatically when the database is empty.

### Inventory Service

```bash
cd inventory-service
./mvnw spring-boot:run
```

Runs on port 8081. On first launch, seeds 5 facilities, 3 medicines, stock batches, and 30 days of consumption records.

### Forecast Service

```bash
cd forecast-service
./mvnw spring-boot:run
```

Runs on port 8082. Needs the Inventory Service to be up — it calls it at startup to fetch data.

### Frontend

```bash
cd UI-Dashboard
npm install
npm run dev
```

Opens on `http://localhost:5173`. Talks to both backend services directly (CORS is configured).

## API endpoints

### Inventory Service (8081)

| Endpoint | Description |
|---|---|
| `GET /api/facilities?district=` | List facilities, optionally filtered by district |
| `GET /api/medicines` | List all medicines |
| `GET /api/inventory?district=&facilityId=&medicineId=` | Aggregated usable stock per facility+medicine |
| `GET /api/batches?facilityId=&medicineId=&expiredOnly=&expiringWithinDays=` | Individual batches with expiry info |
| `GET /api/consumption?facilityId=&medicineId=&from=&to=` | Daily consumption records (defaults to last 30 days) |

### Forecast Service (8082)

| Endpoint | Description |
|---|---|
| `GET /api/analysis?district=&medicineId=&forecastHorizonDays=` | Full analysis — risk, forecast, redistribution, expiry alerts per facility |
| `GET /api/dashboard/summary` | Aggregate KPIs — facilities monitored, at-risk count, expiry risks, pending transfers |

## Demo data

The seeder creates a scenario in Vellore district designed to show all features working:

| Facility | Paracetamol stock | Demand/day | Scenario |
|---|---|---|---|
| PHC-A | 240 | ~42 | **Critical stock-out** (~5.7 days left) |
| PHC-B | 600 | ~30 | Moderate — OK for now |
| PHC-C | 900 (two batches) | ~21 | **Expiry risk** — 500-unit batch expires in 20 days |
| PHC-D | 1200 | ~30 | **Surplus** — viable donor for PHC-A |
| District Hospital | 2000 | ~70 | Large stock, high demand |

Three medicines are seeded: Paracetamol 500mg, Amoxicillin 250mg, ORS Sachet.

## Project structure

```
MedConnect/
├── inventory-service/          # Spring Boot — data + persistence
│   └── src/main/java/com/mediconnect/inventory_service/
│       ├── controller/         # REST endpoints (5 controllers)
│       ├── entity/             # JPA entities (Facility, Medicine, StockBatch, ConsumptionRecord)
│       ├── repository/         # Spring Data repositories with custom JPQL queries
│       ├── service/            # Business logic
│       ├── dto/                # Response DTOs (Java records)
│       └── config/             # CORS config, data seeder
│
├── forecast-service/           # Spring Boot — stateless computation
│   └── src/main/java/com/mediconnect/forecast_service/
│       ├── controller/         # /api/analysis, /api/dashboard/summary
│       ├── service/            # ForecastEngine, RedistributionEngine, AnalysisService
│       ├── client/             # REST client for Inventory Service
│       ├── model/              # Analysis result types, RiskLevel enum
│       └── dto/                # DTOs matching Inventory Service responses
│
└── UI-Dashboard/               # React + TypeScript + Tailwind
    └── src/
        ├── App.tsx             # Main dashboard component
        └── types.ts            # TypeScript interfaces
```

## Configuration

Risk thresholds and redistribution parameters are in `forecast-service/src/main/resources/application.properties`:

```properties
forecast.risk.critical-days=3
forecast.risk.high-days=7
forecast.risk.medium-days=14
forecast.redistribution.target-coverage-days=14
forecast.redistribution.safety-stock-days=7
```
