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

function hostnameOf(baseUrl) {
  const match = String(baseUrl).match(/^https?:\/\/([^/:?#]+)/i);
  if (!match) {
    throw new Error('BASE_URL must start with http:// or https://.');
  }
  return match[1].toLowerCase();
}

export function assertSafeExecution({ write = false } = {}) {
  const baseUrl = requireEnv('BASE_URL').replace(/\/$/, '');
  const hostname = hostnameOf(baseUrl);
  const isRemote = !LOCAL_HOSTS.has(hostname);
  const profile = __ENV.PROFILE || 'smoke';

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
