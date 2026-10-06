/**
 * Freelance Frontend Framework - Unified Global Runtime (window.fl)
 */

import { api, ApiClient } from './api/client.js';
import { SessionManager } from './auth/session.js';
import { PermissionManager } from './auth/permissions.js';
import { Router } from './router/router.js';
import { ThemeEngine } from './theme/theme.js';
import { ErrorMapper } from './error/error-mapper.js';
import { FormBinder } from './error/form-binder.js';
import { AppConfig } from './config/app-config.js';
import { Icons } from '../assets/icons/icons.js';

export const fl = {
  api,
  ApiClient,
  config: AppConfig,
  auth: {
    session: SessionManager,
    permissions: PermissionManager,
    getToken: () => SessionManager.getToken(),
    getTenantId: () => SessionManager.getTenantId(),
    getUser: () => SessionManager.getUser(),
    isAuthenticated: () => SessionManager.isAuthenticated(),
    login: (token, user, tenantId) => SessionManager.setSession(token, user, tenantId),
    logout: () => SessionManager.clearSession(),
    can: (perm) => PermissionManager.can(perm),
    hasRole: (role) => PermissionManager.hasRole(role)
  },
  router: Router,
  theme: ThemeEngine,
  errors: {
    mapper: ErrorMapper,
    forms: FormBinder,
    toast: (msg, type, title, duration) => ErrorMapper.toast(msg, type, title, duration),
    success: (msg, title) => ErrorMapper.notifySuccess(msg, title),
    warning: (msg, title) => ErrorMapper.notifyWarning(msg, title),
    danger: (msg, title) => ErrorMapper.toast(msg, 'danger', title),
    showErrorWindow: (msg, title, code, duration) => ErrorMapper.showErrorWindow(msg, title, code, duration),
    handle: (err, formEl) => ErrorMapper.handle(err, formEl)
  },
  icons: Icons,

  init({ outlet = '#app-outlet', routes = [], baseUrl = '' } = {}) {
    if (baseUrl) {
      this.api.setBaseUrl(baseUrl);
    }
    this.router.setOutlet(outlet);
    if (routes.length > 0) {
      this.router.register(routes);
    }
    this.theme.init();
    this.router.handleRouteChange();
  }
};

// Bind to global window object
if (typeof window !== 'undefined') {
  window.fl = fl;
}
