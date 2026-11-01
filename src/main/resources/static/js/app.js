// Shared Application and Technical Analysis Utilities

function saveCredentials(username, password) {
  sessionStorage.setItem('stock_username', username);
  sessionStorage.setItem('stock_password', password);
  sessionStorage.setItem('stock_auth', btoa(username + ':' + password));
}

function getCredentials() {
  const username = sessionStorage.getItem('stock_username');
  const password = sessionStorage.getItem('stock_password');
  if (!username || !password) return null;
  return { username, password };
}

function getAuthHeader() {
  const auth = sessionStorage.getItem('stock_auth');
  return auth ? 'Basic ' + auth : null;
}

function getUsername() {
  return sessionStorage.getItem('stock_username') || '';
}

function isLoggedIn() {
  return !!sessionStorage.getItem('stock_username') && !!sessionStorage.getItem('stock_password');
}

function requireAuth() {
  if (!isLoggedIn()) {
    window.location.href = '/login.html';
  }
}

function logout() {
  sessionStorage.removeItem('stock_username');
  sessionStorage.removeItem('stock_password');
  sessionStorage.removeItem('stock_auth');
  sessionStorage.removeItem('stock_user');
  window.location.href = '/login.html';
}

async function fetchWithAuth(url, options = {}) {
  const headers = options.headers || {};
  const authHeader = getAuthHeader();
  if (authHeader) {
    headers['Authorization'] = authHeader;
  }
  options.headers = headers;

  const res = await fetch(url, options);
  if (res.status === 401) {
    logout();
    throw new Error('Unauthorized');
  }
  return res;
}

// Technical Indicators Calculation Helpers

function calculateSMA(closes, period) {
  const sma = [];
  for (let i = 0; i < closes.length; i++) {
    const start = Math.max(0, i - period + 1);
    let sum = 0;
    let count = 0;
    for (let j = start; j <= i; j++) {
      sum += closes[j];
      count++;
    }
    sma.push(Math.round((sum / count) * 100) / 100);
  }
  return sma;
}

function calculateRSI(closes, period = 14) {
  const rsi = [];
  const n = closes.length;
  for (let i = 0; i < n; i++) {
    if (i < period) {
      rsi.push(50.0);
    } else {
      let gainSum = 0;
      let lossSum = 0;
      for (let j = i - period + 1; j <= i; j++) {
        const diff = closes[j] - closes[j - 1];
        if (diff >= 0) gainSum += diff;
        else lossSum += Math.abs(diff);
      }
      const avgLoss = lossSum / period;
      if (avgLoss === 0) {
        rsi.push(100.0);
      } else {
        const rs = (gainSum / period) / avgLoss;
        const val = 100 - (100 / (1 + rs));
        rsi.push(Math.round(val * 100) / 100);
      }
    }
  }
  return rsi;
}

// Linear Regression on last 20 closes forecasting 7 days skipping weekends
function linearRegression7Days(closes, lastDateStr, windowSize = 20) {
  const n = closes.length;
  const window = Math.min(windowSize, n);
  const subset = closes.slice(n - window);

  let sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
  for (let i = 0; i < window; i++) {
    const x = i;
    const y = subset[i];
    sumX += x;
    sumY += y;
    sumXY += (x * y);
    sumX2 += (x * x);
  }

  const denominator = (window * sumX2 - sumX * sumX);
  const slope = (denominator !== 0) ? (window * sumXY - sumX * sumY) / denominator : 0;
  const intercept = (sumY - slope * sumX) / window;

  const lastDate = new Date(lastDateStr);
  const predictions = [];
  let curDate = new Date(lastDate);

  for (let k = 1; k <= 7; k++) {
    curDate.setDate(curDate.getDate() + 1);
    while (curDate.getDay() === 0 || curDate.getDay() === 6) { // Sunday=0, Saturday=6
      curDate.setDate(curDate.getDate() + 1);
    }
    const xPred = (window - 1) + k;
    let yPred = slope * xPred + intercept;
    if (yPred < 1.0) yPred = 1.0;
    const dateFormatted = curDate.toISOString().split('T')[0];
    predictions.push({
      date: dateFormatted,
      price: Math.round(yPred * 100) / 100
    });
  }

  const trend = slope >= 0 ? 'UP' : 'DOWN';
  return { slope, intercept, trend, predictions };
}

// Signal Calculation: SMA5 > SMA20 (+1, -1), RSI < 35 (+1), RSI > 65 (-1). Score >= 1: BUY, <= -1: SELL, 0: HOLD
function calculateSignal(sma5, sma20, rsi) {
  let score = 0;
  const reasons = [];

  if (sma5 > sma20) {
    score += 1;
    reasons.push('SMA5 above SMA20 (+1)');
  } else if (sma5 < sma20) {
    score -= 1;
    reasons.push('SMA5 below SMA20 (-1)');
  } else {
    reasons.push('SMA5 equal to SMA20 (0)');
  }

  if (rsi < 35) {
    score += 1;
    reasons.push(`RSI ${rsi.toFixed(1)} < 35 (+1)`);
  } else if (rsi > 65) {
    score -= 1;
    reasons.push(`RSI ${rsi.toFixed(1)} > 65 (-1)`);
  } else {
    reasons.push(`RSI ${rsi.toFixed(1)} neutral (0)`);
  }

  let signal = 'HOLD';
  let badgeClass = 'badge-hold';
  if (score >= 1) {
    signal = 'BUY';
    badgeClass = 'badge-buy';
  } else if (score <= -1) {
    signal = 'SELL';
    badgeClass = 'badge-sell';
  }

  const reason = reasons.join(', ') + ` [Score: ${score}]`;
  return { score, signal, reason, badgeClass };
}

// UI Formatting Helpers
function formatCurrency(val) {
  if (val === null || val === undefined) return '₹0.00';
  const num = typeof val === 'number' ? val : parseFloat(val);
  return '₹' + num.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function formatPercent(val) {
  if (val === null || val === undefined) return '0.00%';
  const num = typeof val === 'number' ? val : parseFloat(val);
  const sign = num > 0 ? '+' : '';
  return `${sign}${num.toFixed(2)}%`;
}

function getSignalBadge(signal) {
  const s = (signal || 'HOLD').toUpperCase();
  let cls = 'badge-hold';
  if (s === 'BUY') cls = 'badge-buy';
  if (s === 'SELL') cls = 'badge-sell';
  return `<span class="badge ${cls}">${s}</span>`;
}
