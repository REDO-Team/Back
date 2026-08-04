import { check } from 'k6';
import http from 'k6/http';
import { Counter, Rate } from 'k6/metrics';

import { buildThresholds } from '../../config/thresholds.js';
import { resolveAccessToken } from '../../lib/auth.js';
import { apiRequest, checkApiSuccess, jsonValue } from '../../lib/client.js';
import { assertSafeExecution, requireEnv } from '../../lib/guards.js';

const concurrency = Number.parseInt(__ENV.REWARD_CONCURRENCY || '10', 10);
if (!Number.isInteger(concurrency) || concurrency < 2) {
  throw new Error('REWARD_CONCURRENCY must be an integer greater than or equal to 2.');
}

const expectedRedemptionStatuses = http.expectedStatuses(200, 409);
const redemptionSuccesses = new Counter('reward_redemption_successes');
const redemptionDuplicates = new Counter('reward_redemption_duplicates');
const redemptionExpectedResponses = new Rate('reward_redemption_expected_responses');
const rewardPointConsistency = new Rate('reward_point_consistency');
const rewardStockConsistency = new Rate('reward_stock_consistency');

export const options = {
  scenarios: {
    rewardIdempotency: {
      executor: 'per-vu-iterations',
      vus: concurrency,
      iterations: 1,
      maxDuration: '1m',
      tags: { profile: 'idempotency' },
    },
  },
  thresholds: buildThresholds({
    reward_redemption_successes: ['count==1'],
    reward_redemption_duplicates: [`count==${concurrency - 1}`],
    reward_redemption_expected_responses: ['rate==1'],
    reward_point_consistency: ['rate==1'],
    reward_stock_consistency: ['rate==1'],
  }),
  systemTags: ['status', 'method', 'name', 'scenario', 'expected_response', 'error', 'error_code'],
};

function readPoint(baseUrl, accessToken) {
  return apiRequest('GET', baseUrl, '/api/rewards/points', {
    token: accessToken,
    domain: 'reward',
    endpoint: 'point-balance-verification',
  });
}

function readProduct(baseUrl, accessToken, rewardProductId) {
  return apiRequest('GET', baseUrl, `/api/rewards/products/${rewardProductId}`, {
    token: accessToken,
    domain: 'reward',
    endpoint: 'reward-product-verification',
  });
}

export function setup() {
  const baseUrl = assertSafeExecution({ write: true });
  const accessToken = resolveAccessToken(baseUrl);
  const rewardProductId = requireEnv('REWARD_PRODUCT_ID');
  const receiverName = requireEnv('REWARD_RECEIVER_NAME');
  const receiverPhone = requireEnv('REWARD_RECEIVER_PHONE');
  const pointResponse = readPoint(baseUrl, accessToken);
  const productResponse = readProduct(baseUrl, accessToken, rewardProductId);

  if (!checkApiSuccess(pointResponse, 'initial point balance')
      || !checkApiSuccess(productResponse, 'initial reward product')) {
    throw new Error('Failed to load initial reward state.');
  }

  const initialPoint = jsonValue(pointResponse, 'result.totalPoint');
  const initialStock = jsonValue(productResponse, 'result.stockQuantity');
  const pricePoint = jsonValue(productResponse, 'result.pricePoint');

  if (![initialPoint, initialStock, pricePoint].every(Number.isFinite)) {
    throw new Error('Initial reward state contains invalid point or stock values.');
  }
  if (initialPoint < pricePoint || initialStock < 1) {
    throw new Error('Test user point or reward product stock is insufficient.');
  }

  return {
    baseUrl,
    accessToken,
    rewardProductId: Number(rewardProductId),
    receiverName,
    receiverPhone,
    idempotencyKey: `${requireEnv('TEST_ID')}-same-request`,
    initialPoint,
    initialStock,
    pricePoint,
  };
}

export default function (data) {
  const response = apiRequest('POST', data.baseUrl, '/api/rewards/redemptions', {
    token: data.accessToken,
    body: {
      rewardProductId: data.rewardProductId,
      shippingAddressId: null,
      receiverName: data.receiverName,
      receiverPhone: data.receiverPhone,
    },
    headers: {
      'Content-Type': 'application/json',
      'Idempotency-Key': data.idempotencyKey,
    },
    domain: 'reward',
    endpoint: 'idempotent-redemption',
    responseCallback: expectedRedemptionStatuses,
  });

  const isSuccess = response.status === 200 && jsonValue(response, 'isSuccess') === true;
  const isDuplicate = response.status === 409
    && jsonValue(response, 'code') === 'REWARD_409_001';

  redemptionSuccesses.add(isSuccess ? 1 : 0);
  redemptionDuplicates.add(isDuplicate ? 1 : 0);
  redemptionExpectedResponses.add(isSuccess || isDuplicate);
  check(response, {
    'redemption is success or duplicate': () => isSuccess || isDuplicate,
  });
}

export function teardown(data) {
  const pointResponse = readPoint(data.baseUrl, data.accessToken);
  const productResponse = readProduct(data.baseUrl, data.accessToken, data.rewardProductId);
  const finalPoint = jsonValue(pointResponse, 'result.totalPoint');
  const finalStock = jsonValue(productResponse, 'result.stockQuantity');

  rewardPointConsistency.add(finalPoint === data.initialPoint - data.pricePoint);
  rewardStockConsistency.add(finalStock === data.initialStock - 1);
  check({ finalPoint, finalStock }, {
    'point is deducted exactly once': (state) =>
      state.finalPoint === data.initialPoint - data.pricePoint,
    'stock is deducted exactly once': (state) =>
      state.finalStock === data.initialStock - 1,
  });
}
