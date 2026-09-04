import { sleep } from 'k6';

import { apiRequest, checkApiSuccess } from './client.js';

export function positiveIntegerEnv(name, fallback) {
  const rawValue = String(__ENV[name] || fallback);
  if (!/^[1-9]\d*$/.test(rawValue)) {
    throw new Error(`${name} must be a positive integer.`);
  }

  const value = Number(rawValue);
  if (!Number.isSafeInteger(value)) {
    throw new Error(`${name} must be a positive integer.`);
  }

  return value;
}

export function nonNegativeNumberEnv(name, fallback) {
  const value = Number(__ENV[name] || fallback);
  if (!Number.isFinite(value) || value < 0) {
    throw new Error(`${name} must be zero or greater.`);
  }

  return value;
}

export function executeReadJourney(data, requests, thinkTimeSeconds) {
  for (const request of requests) {
    const response = apiRequest('GET', data.baseUrl, request.path, {
      token: data.accessToken,
      domain: request.domain,
      endpoint: request.endpoint,
    });

    checkApiSuccess(response, request.endpoint);
    sleep(thinkTimeSeconds);
  }
}
