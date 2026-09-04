import { buildScenarios } from '../../config/profiles.js';
import { buildEndpointThresholds, buildThresholds } from '../../config/thresholds.js';
import { resolveAccessToken } from '../../lib/auth.js';
import { assertSafeExecution } from '../../lib/guards.js';
import {
  executeReadJourney,
  nonNegativeNumberEnv,
  positiveIntegerEnv,
} from '../../lib/read-journey.js';

const pageSize = positiveIntegerEnv('DOMAIN_READ_PAGE_SIZE', 10);
const thinkTimeSeconds = nonNegativeNumberEnv('DOMAIN_READ_THINK_TIME_SECONDS', 0.5);
const requests = [
  {
    path: `/api/rewards/products?size=${pageSize}`,
    domain: 'reward',
    endpoint: 'reward-products',
  },
  {
    path: '/api/rewards/products/preview',
    domain: 'reward',
    endpoint: 'reward-preview',
  },
  {
    path: `/api/rewards/redemptions?size=${pageSize}`,
    domain: 'reward',
    endpoint: 'reward-redemptions',
  },
];

export const options = {
  scenarios: buildScenarios(),
  thresholds: buildThresholds(buildEndpointThresholds(
    requests.map((request) => request.endpoint)
  )),
  systemTags: ['status', 'method', 'name', 'scenario', 'expected_response', 'error', 'error_code'],
};

export function setup() {
  const baseUrl = assertSafeExecution();
  return { baseUrl, accessToken: resolveAccessToken(baseUrl) };
}

export default function (data) {
  executeReadJourney(data, requests, thinkTimeSeconds);
}
