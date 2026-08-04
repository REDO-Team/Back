import { check } from 'k6';
import http from 'k6/http';

function responseIsSuccess(response) {
  try {
    return response.json('isSuccess') === true;
  } catch (error) {
    return false;
  }
}

export function apiRequest(
  method,
  baseUrl,
  path,
  {
    token = null,
    body = null,
    headers = {},
    domain = 'common',
    endpoint,
    responseCallback,
    tags = {},
  } = {}
) {
  const normalizedMethod = method.toUpperCase();
  const endpointName = endpoint || path;
  const requestHeaders = {
    Accept: 'application/json',
    ...headers,
  };

  if (token) {
    requestHeaders.Authorization = `Bearer ${token}`;
  }

  const params = {
    headers: requestHeaders,
    tags: {
      name: `${normalizedMethod} ${endpointName}`,
      domain,
      endpoint: endpointName,
      ...tags,
    },
  };

  if (responseCallback) {
    params.responseCallback = responseCallback;
  }

  const payload = body === null ? null : JSON.stringify(body);
  return http.request(normalizedMethod, `${baseUrl}${path}`, payload, params);
}

export function checkApiSuccess(response, label, expectedStatus = 200) {
  return check(response, {
    [`${label}: status is ${expectedStatus}`]: (result) => result.status === expectedStatus,
    [`${label}: isSuccess is true`]: responseIsSuccess,
  });
}

export function jsonValue(response, selector) {
  try {
    return response.json(selector);
  } catch (error) {
    return null;
  }
}
