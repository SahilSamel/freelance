/**
 * Freelance Frontend Framework - Automated HTTP API Client
 * Coordinates multi-tenant headers, auth bearer tokens, trace IDs, and response handling.
 */

import { SessionManager } from '../auth/session.js';
import { ErrorMapper } from '../error/error-mapper.js';

export class ApiClient {
  constructor(baseUrl = '') {
    this.baseUrl = baseUrl;
    this.mockMode = false;
  }

  setBaseUrl(url) {
    this.baseUrl = url;
  }

  setMockMode(enabled) {
    this.mockMode = enabled;
  }

  generateTraceId() {
    return 'trc_' + Math.random().toString(36).substring(2, 11) + '_' + Date.now();
  }

  async request(endpoint, options = {}) {
    const url = endpoint.startsWith('http') ? endpoint : `${this.baseUrl}${endpoint}`;
    const method = (options.method || 'GET').toUpperCase();
    const headers = new Headers(options.headers || {});

    if (!headers.has('Content-Type') && method !== 'GET' && !(options.body instanceof FormData)) {
      headers.set('Content-Type', 'application/json');
    }

    // Auto-inject Multi-Tenant Identifier
    const tenantId = SessionManager.getTenantId();
    if (tenantId && !headers.has('X-Tenant-ID')) {
      headers.set('X-Tenant-ID', tenantId);
    }

    // Auto-inject Bearer Token
    const token = SessionManager.getToken();
    if (token && !headers.has('Authorization')) {
      headers.set('Authorization', `Bearer ${token}`);
      SessionManager.recordActivity();
    }

    // Auto-inject Trace ID
    if (!headers.has('X-Trace-ID')) {
      headers.set('X-Trace-ID', this.generateTraceId());
    }

    let requestBody = options.body;
    if (requestBody && typeof requestBody === 'object' && !(requestBody instanceof FormData) && !(requestBody instanceof Blob)) {
      requestBody = JSON.stringify(requestBody);
    }

    // Abort controller with timeout protection (defaults to 10s)
    const timeoutMs = options.timeout || 10000;
    const controller = new AbortController();
    const timeoutTimer = setTimeout(() => controller.abort(), timeoutMs);

    try {
      const response = await fetch(url, {
        ...options,
        signal: controller.signal,
        method,
        headers,
        body: requestBody
      });

      clearTimeout(timeoutTimer);

      // Handle 401 Unauthorized globally
      if (response.status === 401) {
        SessionManager.clearSession();
        ErrorMapper.notifyUnauthorized();
        const err = new Error('Session expired. Please log in again.');
        err.status = 401;
        throw err;
      }

      const contentType = response.headers.get('content-type') || '';
      let data = null;

      if (contentType.includes('application/json')) {
        data = await response.json();
      } else {
        const text = await response.text();
        try {
          data = JSON.parse(text);
        } catch {
          data = text;
        }
      }

      if (!response.ok) {
        const error = new Error((data && data.message) || (data && data.error) || response.statusText || 'API Request Failed');
        error.status = response.status;
        error.data = data;
        error.endpoint = endpoint;

        // Auto-delegate to global error mapper
        if (!options.silentError) {
          ErrorMapper.handle(error, options.formElement);
        }

        throw error;
      }

      return data;
    } catch (err) {
      clearTimeout(timeoutTimer);

      if (err.name === 'AbortError') {
        const timeoutErr = new Error(`Request to ${endpoint} timed out after ${timeoutMs}ms.`);
        timeoutErr.status = 408;
        if (!options.silentError) {
          ErrorMapper.handle(timeoutErr, options.formElement);
        }
        throw timeoutErr;
      }

      if (!options.silentError && err.status !== 401) {
        ErrorMapper.handle(err, options.formElement);
      }
      throw err;
    }
  }

  get(endpoint, options = {}) {
    return this.request(endpoint, { ...options, method: 'GET' });
  }

  post(endpoint, body, options = {}) {
    return this.request(endpoint, { ...options, method: 'POST', body });
  }

  put(endpoint, body, options = {}) {
    return this.request(endpoint, { ...options, method: 'PUT', body });
  }

  delete(endpoint, options = {}) {
    return this.request(endpoint, { ...options, method: 'DELETE' });
  }
}

export const api = new ApiClient();
