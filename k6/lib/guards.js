import { URL } from 'https://jslib.k6.io/url/1.0.0/index.js';

const LOCAL_HOSTS = new Set([
  'localhost',
  '127.0.0.1',
  'host.docker.internal',
  'app',
]);

export function isEnabled(value) {
  return String(value || '').toLowerCase() === 'true';
}

export function requireEnv(name) {
  const value = __ENV[name];
  if (!value || !String(value).trim()) {
    throw new Error(`${name} is required.`);
  }
  return String(value).trim();
}

function parseHttpUrl(value, label) {
  let parsedUrl;
  try {
    parsedUrl = new URL(String(value));
  } catch (error) {
    throw new Error(`${label} must be a valid http or https URL.`);
  }

  if (parsedUrl.protocol !== 'http:' && parsedUrl.protocol !== 'https:') {
    throw new Error(`${label} must use http or https.`);
  }

  if (parsedUrl.username || parsedUrl.password) {
    throw new Error(`${label} must not include username or password.`);
  }

  return parsedUrl;
}

function hostnameOf(baseUrl) {
  return parseHttpUrl(baseUrl, 'BASE_URL').hostname.toLowerCase();
}

export function assertAuthenticationTransport(requestUrl) {
  const parsedUrl = parseHttpUrl(requestUrl, 'Authenticated request URL');
  if (parsedUrl.protocol === 'https:') {
    return;
  }

  const hostname = parsedUrl.hostname.toLowerCase();
  if (LOCAL_HOSTS.has(hostname) && isEnabled(__ENV.ALLOW_INSECURE_LOCAL_AUTH)) {
    return;
  }

  if (LOCAL_HOSTS.has(hostname)) {
    throw new Error(
      'Local HTTP authentication is blocked. Set ALLOW_INSECURE_LOCAL_AUTH=true only for local testing.'
    );
  }

  throw new Error('Remote authentication requires HTTPS.');
}

export function assertSafeExecution({ write = false } = {}) {
  const baseUrl = requireEnv('BASE_URL').replace(/\/$/, '');
  const hostname = hostnameOf(baseUrl);
  const isRemote = !LOCAL_HOSTS.has(hostname);
  const profile = __ENV.PROFILE || 'smoke';

  if (!isRemote
      && parseHttpUrl(baseUrl, 'BASE_URL').protocol === 'http:'
      && isEnabled(__ENV.ALLOW_INSECURE_LOCAL_AUTH)) {
    console.warn('Local HTTP authentication is enabled. Never use production credentials.');
  }

  if (isRemote && !isEnabled(__ENV.ALLOW_REMOTE)) {
    throw new Error(
      `Remote target ${hostname} is blocked. Set ALLOW_REMOTE=true after confirming the target.`
    );
  }

  if (write && !isEnabled(__ENV.ALLOW_WRITE)) {
    throw new Error('Write scenario is blocked. Set ALLOW_WRITE=true after preparing test data.');
  }

  if (profile === 'stress' && !isEnabled(__ENV.ALLOW_STRESS)) {
    throw new Error('Stress profile is blocked. Set ALLOW_STRESS=true after team approval.');
  }

  return baseUrl;
}
