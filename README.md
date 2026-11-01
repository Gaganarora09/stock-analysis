# StockAnalysis — Intelligent Equity Analytics & Forecasting Platform

A full-stack Spring Boot 3 web application providing automated technical indicator analysis, momentum-based BUY/HOLD/SELL signals, 7-day predictive linear regression price forecasting, per-user watchlists, admin equity management, and live market data synchronization with safe fallback for top Indian equities.

---

## Tech Stack

| Component | Technology | Version / Details |
|---|---|---|
| **Runtime & Language** | Java (JDK) | 17 LTS |
| **Framework** | Spring Boot | 3.2.5 |
| **Security** | Spring Security | HTTP Basic Authentication & BCrypt Hashing |
| **Persistence & ORM** | Spring Data JPA / Hibernate | 6.4.x |
| **Database (Dev/Local)** | H2 Database | File-based mode (`./data/stockdb`) |
| **Database (Prod)** | PostgreSQL | Configured via `application-prod.properties` |
| **External Market Data** | Alpha Vantage API | REST client with resilient offline fallback |
| **API Documentation** | Springdoc OpenAPI (Swagger UI) | 2.3.0 |
| **Validation** | Jakarta Bean Validation | Hibernate Validator |
| **Frontend** | Vanilla HTML5 / CSS3 / ES6+ JavaScript | Modern dark theme, zero build dependencies |
| **Data Visualization** | Chart.js | 4.4.1 (UMD) |
| **Containerization** | Docker | Multi-stage build (`maven:3.9` + `eclipse-temurin:17-jre`) |

---

## Key Features

- **Algorithmic Signal Engine**: Evaluates 5-day vs 20-day Simple Moving Average (SMA) crossovers combined with 14-period Relative Strength Index (RSI) to generate actionable **BUY**, **HOLD**, and **SELL** recommendations.
- **7-Day Price Forecast**: Computes ordinary least squares linear regression trends over the past 20 trading closes and projects the next 7 market days, skipping Saturdays and Sundays.
- **Equities Dashboard**: Real-time summary metrics across all tracked equities, instant search, and one-click filtering by signal (All / BUY / HOLD / SELL / Watchlist). Backed by a high-performance single-request aggregated endpoint (`GET /api/stocks/summary`) with 60-second in-memory caching and batched eager price fetching.
- **Interactive Multi-Chart View**:
  - Main price chart overlaying historical closes, SMA5, SMA20, and dashed 7-day forecast.
  - RSI(14) oscillator chart with 30 (oversold) and 70 (overbought) threshold bounds.
  - Side-by-side stock comparison overlaying a secondary ticker onto a distinct right-hand Y-axis.
- **Per-User Watchlist**: Personalized stock watchlists per user, with one-click star toggle on the dashboard, dedicated "My Watchlist" filter tab, and header controls on the analytics page.
- **Live Market Data with Safe Fallback**: Alpha Vantage API integration with upsert capability for daily closes, intelligent daily rate-limit throttling (25 requests/day), automated `@Scheduled` background sync, and 100% resilient fallback to sample data on errors or missing API keys.
- **Admin Management Console (`admin.html`)**: Dedicated console for `ROLE_ADMIN` users to provision new equities with automated 60-day historical price synthesis, trigger on-demand market data sync, delete assets with full cascade purging, and review registered platform accounts.
- **Dual Database Architecture**: Zero-setup embedded H2 database for local development and PostgreSQL-ready configuration for cloud deployments.

---

## Tracked Equities

The application tracks 10 prominent Indian equities across key sectors:

1. **HCLTECH** — HCL Technologies Ltd (*Information Technology*)
2. **TCS** — Tata Consultancy Services Ltd (*Information Technology*)
3. **INFY** — Infosys Ltd (*Information Technology*)
4. **WIPRO** — Wipro Ltd (*Information Technology*)
5. **RELIANCE** — Reliance Industries Ltd (*Energy & Petrochemicals*)
6. **HDFCBANK** — HDFC Bank Ltd (*Banking & Financial Services*)
7. **ICICIBANK** — ICICI Bank Ltd (*Banking & Financial Services*)
8. **SBIN** — State Bank of India (*Banking & Financial Services*)
9. **ITC** — ITC Ltd (*Consumer Goods*)
10. **TECHM** — Tech Mahindra Ltd (*Information Technology*)

---

## Signal & Prediction Methodology

### 1. Recommendation Scoring System
Recommendations are derived from a mathematical composite score:

$$\text{Score} = S_{\text{SMA}} + S_{\text{RSI}}$$

- **Moving Average Component ($S_{\text{SMA}}$)**:
  - $\text{SMA5} > \text{SMA20} \implies +1$ *(Short-term bullish trend)*
  - $\text{SMA5} < \text{SMA20} \implies -1$ *(Short-term bearish trend)*
  - $\text{SMA5} = \text{SMA20} \implies 0$
- **RSI Momentum Component ($S_{\text{RSI}}$)**:
  - $\text{RSI} < 35 \implies +1$ *(Oversold condition, potential upward reversal)*
  - $\text{RSI} > 65 \implies -1$ *(Overbought condition, potential downward correction)*
  - $35 \le \text{RSI} \le 65 \implies 0$ *(Neutral zone)*

| Composite Score | Signal | Interpretation |
|---|---|---|
| **$\ge +1$** | BUY | Favorable momentum and moving average alignment |
| **$0$** | HOLD | Consolidation or balanced indicator signals |
| **$\le -1$** | SELL | Bearish momentum or overextended indicator conditions |

### 2. 7-Day Linear Regression Prediction
The predictive model calculates the ordinary least squares (OLS) linear trend on the closing prices of the most recent $N=20$ trading sessions:

$$\text{Slope } m = \frac{N \sum (xy) - \sum x \sum y}{N \sum x^2 - (\sum x)^2}, \quad \text{Intercept } c = \frac{\sum y - m \sum x}{N}$$

For day steps $k = 1, \dots, 7$, the projection $\hat{y}_k = m \cdot (N - 1 + k) + c$ is plotted against target calendar dates **skipping Saturdays and Sundays**, producing realistic market trading projections.

---

## Live Market Data & Safe Fallback Engine (Alpha Vantage)

The platform supports live market data integration with Alpha Vantage while maintaining a 100% resilient fallback guarantee:

### 1. Configuration & Secrets Handling
API configuration is managed via environment variables and application properties without hardcoded secrets:

| Property | Environment Variable | Default | Description |
|---|---|---|---|
| `app.marketdata.enabled` | `MARKETDATA_ENABLED` | `false` | Master toggle for live market queries |
| `app.marketdata.api-key` | `ALPHAVANTAGE_API_KEY` | `""` | Alpha Vantage API key (never logged or exposed) |
| `app.marketdata.symbol-suffix` | — | `.BSE` | Exchange suffix for Indian tickers (e.g. `HCLTECH.BSE`) |
| `app.marketdata.cron` | — | `0 0 18 * * MON-FRI` | Automated weekday closing bell refresh schedule |

To activate live feeds locally or in production, set the environment variable:
```bash
export ALPHAVANTAGE_API_KEY=your_alpha_vantage_api_key_here
```

### 2. Safe Fallback & Rate Limit Resilience
- **Zero Startup Blocking**: The service checks for stale data asynchronously on startup without blocking server boot.
- **Graceful Error Recovery**: If the API key is missing, network is unreachable, or response is invalid, the engine logs a brief one-line warning and immediately retains existing prices. It never crashes, throws unhandled 500 errors, or deletes database records.
- **25 Requests/Day Free-Tier Protection**: Alpha Vantage free tiers allow 25 calls per day. On the first `"Note"` or `"Information"` rate-limit payload from Alpha Vantage, the engine catches `RateLimitExceededException` and halts all subsequent calls for the rest of the day, preserving existing data.
- **Daily Throttling**: Stocks are updated at most once per calendar day unless an admin explicitly requests a force refresh.
- **Transparent Data Provenance**: Every equity and analysis response includes `dataSource` (`"LIVE"` vs `"SAMPLE"`) and `lastUpdated`, which are displayed as visual badges on the dashboard, detail pages, and admin console.

---

## API Endpoints

### Authentication & User
- `POST /api/auth/register` — Register a new account (`ROLE_USER` assigned automatically).
- `POST /api/auth/login` — Authenticate and retrieve user profile.
- `GET /api/auth/me` — Inspect currently authenticated user session.

### Equities & Technical Analysis (Requires Auth)
- `GET /api/stocks` — List all registered equities.
- `GET /api/stocks/summary` — Aggregated dashboard summary returning all equities, latest prices, daily percent changes, BUY/HOLD/SELL signals, reasons, and data provenance in a single payload (60s TTL cache with eager batched fetching).
- `GET /api/stocks/{symbol}` — Retrieve metadata for a single equity.
- `GET /api/stocks/{symbol}/prices` — Historical OHLCV price series.
- `GET /api/stocks/{symbol}/analysis` — Technical indicators (SMA5, SMA20, RSI14, volatility, BUY/HOLD/SELL recommendation, and data source provenance).
- `GET /api/stocks/{symbol}/prediction` — 7-day linear regression forecast (skips weekends).

### Watchlist (Requires Auth)
- `GET /api/watchlist` — Retrieve the current user's personal watchlist.
- `POST /api/watchlist/{symbol}` — Add equity to current user's watchlist (idempotent).
- `DELETE /api/watchlist/{symbol}` — Remove equity from current user's watchlist.

### Admin Management (Requires `ROLE_ADMIN`)
- `POST /api/admin/stocks` — Provision a new equity asset and synthesize 60 days of continuous price data.
- `DELETE /api/admin/stocks/{symbol}` — Cascade delete an equity, its historical prices, and referencing watchlist records.
- `GET /api/admin/users` — List platform user accounts (passwords never exposed).
- `POST /api/admin/market/refresh` — Trigger an on-demand market data sync with Alpha Vantage across all equities.

---

## Test Accounts & Credentials

The application initializes with three pre-configured accounts in `data.sql`:

| Username | Password | Role | Description |
|---|---|---|---|
| `admin` | `admin123` | `ROLE_ADMIN` | Platform administrator with asset & sync management |
| `alice` | `alice123` | `ROLE_USER` | Regular market analyst account |
| `bob` | `bob123` | `ROLE_USER` | Regular market analyst account |

---

## How to Run Locally

### Prerequisites
- Java 17 LTS or higher
- Apache Maven 3.8+

### Execution

```bash
# Navigate to the project directory
cd stock-analysis

# Compile, run tests, and package
mvn clean package

# Run the Spring Boot application
java -jar target/stock-analysis-0.0.1-SNAPSHOT.jar
```

Access the application in your browser:
- **Web App**: http://localhost:8080/login.html
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs

---

## Automated Testing

The project includes an automated test suite combining unit tests with Mockito and full-context integration tests with Spring Boot Test and MockMvc. All tests execute against an isolated in-memory H2 database (`application-test.properties`), ensuring determinism without impacting local or production databases.

### Running Tests

```bash
mvn clean test
```

### Coverage Overview (54 Automated Tests)

1. **`AnalysisServiceTest` (6 unit tests)**:
   - Technical indicators: SMA5, SMA20, RSI(14), daily % change, historical high/low, and volatility.
   - Signal scoring logic: verified across uptrend (BUY), downtrend (SELL), flat price series, and small datasets (< 20 records yielding HOLD).
   - Exception handling: missing stock symbols and empty price histories throw `ResourceNotFoundException`.
2. **`UserServiceTest` (6 unit tests)**:
   - Password encryption: verifies BCrypt hashing before persistence, ensuring plain text is never stored.
   - Duplicate registration prevention: duplicate usernames or emails throw `UserAlreadyExistsException`.
   - Authentication verification: valid logins return `AuthResponse`; invalid passwords or unknown users throw `BadCredentialsException`.
3. **`WatchlistServiceTest` (6 unit tests)**:
   - Watchlist additions and idempotent duplicate requests (no duplicate entries saved).
   - Watchlist deletions and unknown stock symbol handling.
   - User isolation: ensures each user only accesses their own watchlist items.
4. **`MarketDataServiceTest` (5 unit tests)**:
   - New date upserting: inserts new daily close rows when price dates do not yet exist.
   - Existing date updating: updates existing closing price records without creating duplicates.
   - Failure fallback: retains existing prices when client throws connection/parsing exceptions.
   - Rate limit halting: immediately breaks loop and skips subsequent equities upon encountering rate limits (25 req/day).
   - Disabled/missing key check: performs zero external network calls when disabled or unconfigured.
5. **`StockSummaryServiceTest` (4 unit tests)**:
   - Batch summary computation: maps multiple stocks and prices in 2 database queries without N+1 calls.
   - 60-second in-memory caching: guarantees subsequent requests do not re-query the database while cache is fresh.
   - Cache eviction: verifies explicit cache invalidation when stocks are created, deleted, or refreshed.
   - Empty dataset resilience: returns an empty list gracefully when no stocks exist in the database.
6. **`SequenceSyncIT` (4 integration tests)**:
   - Dynamic sequence synchronization: adding stocks after seed data succeeds without primary key collisions and creates 60 price rows.
   - Multi-stock sequence progression: adding consecutive stocks in a row assigns monotonically increasing IDs without collisions.
   - Post-seed user registration: registering users after seeding assigns valid consecutive user IDs.
   - Idempotency & resilience: running the sequence synchronizer multiple times consecutively changes nothing and subsequent inserts continue to succeed.
7. **`AuthAndSecurityIT` (23 integration tests)**:
   - Security boundaries: `/api/stocks`, `/api/stocks/summary`, and `/api/watchlist` require authentication (return 401 unauthenticated).
   - HTTP Basic auth: valid credentials (`admin:admin123`) return 200 OK.
   - Aggregated summary: authenticated `GET /api/stocks/summary` returns all stock metrics and signals in a single payload.
   - Public web resources: `/login.html` is accessible without authentication (returns 200 OK).
   - Form validation: valid registration succeeds (201 Created); invalid payloads return 400 Bad Request with field errors.
   - Role boundaries: `admin` gets 200 OK and `alice` gets 403 Forbidden on `/api/admin/users` and `/api/admin/market/refresh`; unauthenticated requests get 401.
   - Registration role lock: registrations always assign `ROLE_USER` and cannot create admin accounts.
   - Admin stock management: `POST /api/admin/stocks` creates stock and 60 daily prices; duplicate symbols rejected with 409 Conflict.
   - Symbol format validation: rejects lowercase letters, spaces, and invalid characters with 400 Bad Request; accepts uppercase symbols with dots and hyphens.
   - Sequence advancement: adding stocks and registering users after database seeding succeeds without primary key sequence collisions.
   - Cascade deletion: `DELETE /api/admin/stocks/{symbol}` cleanly purges stock, its prices, and watchlist records without orphan rows.

---

## Deployment Steps

### 1. Containerized Deployment with Docker

A production-ready multi-stage `Dockerfile` is included in the project root:

```bash
# Build the Docker image
docker build -t stock-analysis .

# Run the container (defaulting to port 8080)
docker run -d -p 8080:8080 --name stock-analysis-app stock-analysis
```

### 2. Production Deployment with PostgreSQL

When deploying to container cloud providers (Render, Railway, Fly.io, AWS ECS, or Kubernetes):

1. Set the active Spring profile to `prod`:
   ```bash
   SPRING_PROFILES_ACTIVE=prod
   ```
2. Provide the following database and API environment variables:
   ```bash
   DB_URL=jdbc:postgresql://<db-host>:<db-port>/<database-name>
   DB_USERNAME=<db-username>
   DB_PASSWORD=<db-password>
   PORT=8080
   ALPHAVANTAGE_API_KEY=<optional-api-key>
   ```
3. Run the container or JAR artifact:
   ```bash
   java -jar app.jar --spring.profiles.active=prod
   ```

---

## Future Improvements

- **Google OAuth2 / Social Login**: Integrate Spring Security OAuth2 Client for single-sign-on (SSO) with Google and GitHub accounts alongside the current username/password authentication.
- **WebSocket Price Ticker**: Stream tick-by-tick real-time price updates directly to the client browser.
- **Advanced Technical Indicators**: Expand analytical suite with MACD (Moving Average Convergence Divergence) and Bollinger Bands.

---

## Disclaimer

**Educational purposes only, not financial advice.** All analyses, calculations, and price projections are algorithmic estimations based on sample and market data and should not be used as investment advice.
