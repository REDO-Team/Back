function positiveNumber(name, fallback) {
  const value = Number(__ENV[name] || fallback);
  if (!Number.isFinite(value) || value <= 0) {
    throw new Error(`${name} must be greater than 0.`);
  }
  return value;
}

export function buildThresholds(overrides = {}) {
  const maxP95 = positiveNumber('MAX_P95_MS', 500);
  const maxP99 = positiveNumber('MAX_P99_MS', 1000);
  const maxErrorRate = positiveNumber('MAX_ERROR_RATE', 0.01);

  if (maxErrorRate >= 1) {
    throw new Error('MAX_ERROR_RATE must be less than 1.');
  }

  return {
    checks: [`rate>${1 - maxErrorRate}`],
    'http_req_failed{phase:measurement}': [`rate<${maxErrorRate}`],
    'http_req_duration{phase:measurement}': [
      `p(95)<${maxP95}`,
      `p(99)<${maxP99}`,
    ],
    ...overrides,
  };
}

export function buildEndpointThresholds(endpoints) {
  const maxP95 = positiveNumber('MAX_P95_MS', 500);
  const maxP99 = positiveNumber('MAX_P99_MS', 1000);
  const maxErrorRate = positiveNumber('MAX_ERROR_RATE', 0.01);

  if (maxErrorRate >= 1) {
    throw new Error('MAX_ERROR_RATE must be less than 1.');
  }

  return Object.fromEntries(endpoints.flatMap((endpoint) => [
    [
      `http_req_failed{phase:measurement,endpoint:${endpoint}}`,
      [`rate<${maxErrorRate}`],
    ],
    [
      `http_req_duration{phase:measurement,endpoint:${endpoint}}`,
      [`p(95)<${maxP95}`, `p(99)<${maxP99}`],
    ],
  ]));
}
