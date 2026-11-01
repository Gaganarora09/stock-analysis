# StockAnalysis — Intelligent Equity Analytics & Forecasting Platform

A full-stack Spring Boot 3 web application providing automated technical indicator analysis, momentum-based BUY/HOLD/SELL signals, and 7-day predictive linear regression price forecasting for top Indian equities.

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
| **API Documentation** | Springdoc OpenAPI (Swagger UI) | 2.3.0 |
| **Validation** | Jakarta Bean Validation | Hibernate Validator |
| **Frontend** | Vanilla HTML5 / CSS3 / ES6+ JavaScript | Modern dark theme, zero build dependencies |
| **Data Visualization** | Chart.js | 4.4.1 (UMD) |
| **Containerization** | Docker | Multi-stage build (`maven:3.9` + `eclipse-temurin:17-jre`) |

---

## Key Features

- **Algorithmic Signal Engine**: Evaluates 5-day vs 20-day Simple Moving Average (SMA) crossovers combined with 14-period Relative Strength Index (RSI) to generate actionable **BUY**, **HOLD**, and **SELL** recommendations.
- **7-Day Price Forecast**: Computes linear regression trends over the past 20 trading closes and projects the next 7 market days, skipping Saturdays and Sundays.
- **Equities Dashboard**: Real-time summary metrics across all 10 tracked equities, instant search, and one-click filtering by signal (All / BUY / HOLD / SELL).
- **Interactive Multi-Chart View**:
  - Main price chart overlaying historical closes, SMA5, SMA20, and dashed 7-day forecast.
  - RSI(14) oscillator chart with 30 (oversold) and 70 (overbought) threshold bounds.
  - Side-by-side stock comparison overlaying a secondary ticker onto a distinct right-hand Y-axis.
- **User Authentication**: Secure BCrypt-hashed password storage, HTTP Basic authentication with popup-suppressed 401 handling, and session persistence.
- **Per-User Watchlist**: Personalized stock watchlists per user, with one-click star toggle on the dashboard, dedicated "My Watchlist" filter tab, and header controls on the analytics page.
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

> **Note on Data**: The 60 days of historical Open, High, Low, Close, and Volume (OHLCV) price points populated in `data.sql` are realistic sample datasets generated for educational and evaluation purposes. They do not represent live market feeds.

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
| **$\ge +1$** | <span style="color:#10b981;font-weight:bold;">BUY</span> | Favorable momentum and moving average alignment |
| **$0$** | <span style="color:#f59e0b;font-weight:bold;">HOLD</span> | Consolidation or balanced indicator signals |
| **$\le -1$** | <span style="color:#ef4444;font-weight:bold;">SELL</span> | Bearish momentum or overextended indicator conditions |

### 2. 7-Day Linear Regression Prediction
The predictive model calculates the ordinary least squares (OLS) linear trend on the closing prices of the most recent $N=20$ trading sessions:

$$\text{Slope } m = \frac{N \sum (xy) - \sum x \sum y}{N \sum x^2 - (\sum x)^2}, \quad \text{Intercept } c = \frac{\sum y - m \sum x}{N}$$

For day steps $k = 1, \dots, 7$, the projection $\hat{y}_k = m \cdot (N - 1 + k) + c$ is plotted against target calendar dates **skipping Saturdays and Sundays**, producing realistic market trading projections.

---

## How to Run Locally

### Prerequisites
- Java 17 or higher
- Apache Maven 3.8+ (or use the configured wrapper)

### Execution

```bash
# Clone and navigate into the project directory
cd stock-analysis

# Build and run with Spring Boot
mvn spring-boot:run
```

Once started, open your web browser:
- **Application Portal**: [http://localhost:8080/login.html](http://localhost:8080/login.html)
- **Interactive Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI Schema**: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)
- **H2 Database Console**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
  - JDBC URL: `jdbc:h2:file:./data/stockdb`
  - Username: `sa`
  - Password: *(leave blank)*

---

## Test Accounts

The local database initializes with three pre-configured accounts:

| Username | Password | Role | Description |
|---|---|---|---|
| `admin` | `admin123` | `ROLE_ADMIN` | Administrator account |
| `alice` | `alice123` | `ROLE_USER` | Standard trader account |
| `bob` | `bob123` | `ROLE_USER` | Standard analyst account |

New users can also self-register at [http://localhost:8080/register.html](http://localhost:8080/register.html).

---

## REST API Reference

All `/api/stocks/**` endpoints require HTTP Basic authentication (`username:password`).

### 1. Authentication Endpoints (Public)

#### Register User
- **URL**: `POST /api/auth/register`
- **Request Body**:
  ```json
  {
    "username": "newtrader",
    "password": "password123",
    "email": "trader@example.com"
  }
  ```
- **Response** (`201 Created`):
  ```json
  {
    "message": "User registered successfully",
    "username": "newtrader",
    "role": "ROLE_USER"
  }
  ```

#### Login User
- **URL**: `POST /api/auth/login`
- **Request Body**:
  ```json
  {
    "username": "alice",
    "password": "alice123"
  }
  ```
- **Response** (`200 OK`):
  ```json
  {
    "message": "Login successful",
    "username": "alice",
    "role": "ROLE_USER"
  }
  ```

---

### 2. Stocks & Analysis Endpoints (Protected)

#### Get All Stocks
- **URL**: `GET /api/stocks`
- **Response** (`200 OK`):
  ```json
  [
    {
      "id": 1,
      "symbol": "HCLTECH",
      "companyName": "HCL Technologies Ltd",
      "sector": "Information Technology"
    },
    ...
  ]
  ```

#### Get Stock Historical Prices
- **URL**: `GET /api/stocks/{symbol}/prices`
- **Response** (`200 OK`): Array of 60 OHLCV price records ordered by date descending.

#### Get Technical Analysis & Signals
- **URL**: `GET /api/stocks/{symbol}/analysis`
- **Sample Response** (`200 OK`):
  ```json
  {
    "symbol": "HCLTECH",
    "companyName": "HCL Technologies Ltd",
    "latestClosePrice": 1229.21,
    "percentageChange": -0.14,
    "movingAverage5Day": 1227.04,
    "movingAverage20Day": 1215.20,
    "highestPrice": 1246.23,
    "lowestPrice": 1196.70,
    "rsi14": 64.62,
    "volatility": 0.29,
    "dates": ["2024-08-02", ..., "2024-09-30"],
    "closePrices": [1242.54, ..., 1229.21],
    "sma5Series": [1242.54, ..., 1227.04],
    "sma20Series": [1242.54, ..., 1215.20],
    "signal": "BUY",
    "signalReason": "SMA5 above SMA20 and RSI 64.62 (neutral) (Score 1)"
  }
  ```

#### Get 7-Day Linear Price Prediction
- **URL**: `GET /api/stocks/{symbol}/prediction`
- **Sample Response** (`200 OK`):
  ```json
  {
    "symbol": "HCLTECH",
    "companyName": "HCL Technologies Ltd",
    "trend": "UP",
    "currentPrice": 1229.21,
    "predictedFinalPrice": 1242.76,
    "predictedChangePercent": 1.10,
    "predictions": [
      { "date": "2024-10-01", "predictedPrice": 1232.74 },
      { "date": "2024-10-02", "predictedPrice": 1234.41 },
      { "date": "2024-10-03", "predictedPrice": 1236.08 },
      { "date": "2024-10-04", "predictedPrice": 1237.75 },
      { "date": "2024-10-07", "predictedPrice": 1239.42 },
      { "date": "2024-10-08", "predictedPrice": 1241.09 },
      { "date": "2024-10-09", "predictedPrice": 1242.76 }
    ]
  }
  ```

---

### 3. Watchlist Endpoints (Protected)

#### Get Current User Watchlist
- **URL**: `GET /api/watchlist`
- **Description**: Returns all equities currently saved in the authenticated user's watchlist.
- **Response** (`200 OK`):
  ```json
  [
    {
      "symbol": "HCLTECH",
      "companyName": "HCL Technologies Ltd",
      "sector": "Information Technology"
    },
    {
      "symbol": "TCS",
      "companyName": "Tata Consultancy Services Ltd",
      "sector": "Information Technology"
    }
  ]
  ```

#### Add Stock to Watchlist
- **URL**: `POST /api/watchlist/{symbol}`
- **Description**: Adds the specified stock to the authenticated user's watchlist. Idempotent (succeeds with 200 OK if already present). Returns 404 if the symbol is not recognized.
- **Response** (`200 OK`)

#### Remove Stock from Watchlist
- **URL**: `DELETE /api/watchlist/{symbol}`
- **Description**: Removes the specified stock from the authenticated user's watchlist.
- **Response** (`204 No Content`)

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
2. Provide the following database environment variables:
   ```bash
   DB_URL=jdbc:postgresql://<db-host>:<db-port>/<database-name>
   DB_USERNAME=<db-username>
   DB_PASSWORD=<db-password>
   PORT=8080
   ```
3. Run the container or JAR artifact:
   ```bash
   java -jar app.jar --spring.profiles.active=prod
   ```

---

## Future Improvements

- **Google OAuth2 / Social Login**: Integrate Spring Security OAuth2 Client for single-sign-on (SSO) with Google and GitHub accounts alongside the current username/password authentication.
- **Live Stock API Integration**: Connect to live market data providers (e.g., Alpha Vantage, Yahoo Finance, or NSE/BSE real-time feeds) via WebSockets.
- **Advanced Technical Indicators**: Expand analytical suite with MACD (Moving Average Convergence Divergence), Bollinger Bands, and Stochastic Oscillators.
- **User Watchlist & Portfolio Tracking**: Enable registered users to maintain custom portfolios and price alert notifications.

---

## Disclaimer

**Educational purposes only, not financial advice.** All analyses, calculations, and price projections are algorithmic estimations based on sample data and should not be used as investment advice.
