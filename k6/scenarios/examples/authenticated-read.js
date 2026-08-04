import { sleep } from 'k6';

import { buildScenarios } from '../../config/profiles.js';
import { buildThresholds } from '../../config/thresholds.js';
import { resolveAccessToken } from '../../lib/auth.js';
import { apiRequest, checkApiSuccess } from '../../lib/client.js';
import { assertSafeExecution, requireEnv } from '../../lib/guards.js';

export const options = {
  scenarios: buildScenarios(),
  thresholds: buildThresholds(),
  systemTags: ['status', 'method', 'name', 'scenario', 'expected_response', 'error', 'error_code'],
};

export function setup() {
  const baseUrl = assertSafeExecution();
  return {
    baseUrl,
    accessToken: resolveAccessToken(baseUrl),
  };
}

export default function (data) {
  const path = requireEnv('ENDPOINT_PATH');
  const endpoint = __ENV.ENDPOINT_NAME || path;
  const domain = __ENV.DOMAIN || 'common';
  const response = apiRequest('GET', data.baseUrl, path, {
    token: data.accessToken,
    domain,
    endpoint,
  });

  checkApiSuccess(response, endpoint);
  sleep(Number(__ENV.THINK_TIME_SECONDS || 1));
}
