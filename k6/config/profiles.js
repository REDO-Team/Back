function positiveInteger(name, fallback) {
  const value = Number.parseInt(__ENV[name] || `${fallback}`, 10);
  if (!Number.isInteger(value) || value < 1) {
    throw new Error(`${name} must be a positive integer.`);
  }
  return value;
}

export function selectedProfile() {
  return __ENV.PROFILE || 'smoke';
}

export function buildScenarios() {
  const profile = selectedProfile();

  if (profile === 'smoke') {
    return {
      smoke: {
        executor: 'shared-iterations',
        vus: positiveInteger('SMOKE_VUS', 1),
        iterations: positiveInteger('SMOKE_ITERATIONS', 1),
        maxDuration: '1m',
        tags: { profile: 'smoke' },
      },
    };
  }

  if (profile === 'load') {
    const maxVus = positiveInteger('LOAD_MAX_VUS', 20);
    return {
      load: {
        executor: 'ramping-vus',
        startVUs: 0,
        stages: [
          { duration: '30s', target: Math.max(1, Math.ceil(maxVus / 4)) },
          { duration: '1m', target: maxVus },
          { duration: '1m', target: maxVus },
          { duration: '30s', target: 0 },
        ],
        gracefulRampDown: '10s',
        tags: { profile: 'load' },
      },
    };
  }

  if (profile === 'stress') {
    const maxVus = positiveInteger('STRESS_MAX_VUS', 50);
    return {
      stress: {
        executor: 'ramping-vus',
        startVUs: 0,
        stages: [
          { duration: '30s', target: Math.max(1, Math.ceil(maxVus / 4)) },
          { duration: '1m', target: Math.max(1, Math.ceil(maxVus / 2)) },
          { duration: '1m', target: maxVus },
          { duration: '30s', target: 0 },
        ],
        gracefulRampDown: '10s',
        tags: { profile: 'stress' },
      },
    };
  }

  throw new Error(`Unsupported PROFILE: ${profile} (smoke | load | stress)`);
}
