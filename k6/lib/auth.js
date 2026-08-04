import http from 'k6/http';

import { assertAuthenticationTransport, isEnabled, requireEnv } from './guards.js';

export function resolveAccessToken(baseUrl) {
  if (__ENV.ACCESS_TOKEN) {
    return __ENV.ACCESS_TOKEN.trim();
  }

  const authRequired = !__ENV.AUTH_REQUIRED || isEnabled(__ENV.AUTH_REQUIRED);
  if (!authRequired && (!__ENV.TEST_LOGIN_ID || !__ENV.TEST_PASSWORD)) {
    return null;
  }

  const loginId = requireEnv('TEST_LOGIN_ID');
  const password = requireEnv('TEST_PASSWORD');
  const loginUrl = `${baseUrl}/api/auth/login`;
  assertAuthenticationTransport(loginUrl);
  const response = http.post(
    loginUrl,
    JSON.stringify({ loginId, password }),
    {
      headers: { 'Content-Type': 'application/json' },
      tags: {
        name: 'POST /api/auth/login',
        domain: 'auth',
        endpoint: 'login-setup',
      },
    }
  );

  if (response.status !== 200) {
    throw new Error(`Login failed with status ${response.status}.`);
  }

  const accessToken = response.json('result.accessToken');
  if (!accessToken) {
    throw new Error('Login response does not contain result.accessToken.');
  }

  return accessToken;
}
