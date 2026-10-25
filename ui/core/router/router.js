/**
 * Freelance Frontend Framework - Client-Side SPA Router with RBAC Route Guarding
 */

import { SessionManager } from '../auth/session.js';
import { PermissionManager } from '../auth/permissions.js';
import { AppConfig } from '../config/app-config.js';

class RouterClass {
  constructor() {
    this.routes = [];
    this.outlet = null;
    this.currentRoute = null;
    this.init();
  }

  setOutlet(elementOrSelector) {
    this.outlet = typeof elementOrSelector === 'string' 
      ? document.querySelector(elementOrSelector) 
      : elementOrSelector;
  }

  register(routeConfig) {
    // Array of { path, component, requiresAuth, permission, title }
    if (Array.isArray(routeConfig)) {
      this.routes.push(...routeConfig);
    } else {
      this.routes.push(routeConfig);
    }
  }

  getCurrentPath() {
    const hash = window.location.hash;
    if (!hash || hash === '#' || hash === '#/') return '/';
    const clean = hash.startsWith('#') ? hash.substring(1) : hash;
    return clean.startsWith('/') ? clean : `/${clean}`;
  }

  matchRoute(path) {
    const cleanPath = path.split('?')[0];

    for (const route of this.routes) {
      if (route.path === cleanPath) {
        return { route, params: {} };
      }

      // Check parameterized paths like /chat/:id
      if (route.path.includes(':')) {
        const routeParts = route.path.split('/');
        const pathParts = cleanPath.split('/');

        if (routeParts.length === pathParts.length) {
          const params = {};
          let match = true;

          for (let i = 0; i < routeParts.length; i++) {
            if (routeParts[i].startsWith(':')) {
              params[routeParts[i].substring(1)] = pathParts[i];
            } else if (routeParts[i] !== pathParts[i]) {
              match = false;
              break;
            }
          }

          if (match) {
            return { route, params };
          }
        }
      }
    }

    // Check fallback 404
    const notFoundRoute = this.routes.find(r => r.path === '*');
    return notFoundRoute ? { route: notFoundRoute, params: {} } : null;
  }

  async navigate(path) {
    const targetHash = path.startsWith('/') ? `#${path}` : `#/${path}`;
    if (window.location.hash === targetHash) {
      this.handleRouteChange();
    } else {
      window.location.hash = targetHash;
    }
  }

  async handleRouteChange() {
    const path = this.getCurrentPath();
    const match = this.matchRoute(path);

    if (!match) {
      if (this.outlet) {
        this.outlet.innerHTML = `<fl-not-found-page></fl-not-found-page>`;
      }
      return;
    }

    const { route, params } = match;
    const requiresAuth = route.requiresAuth !== false;

    const cleanPath = path.split('?')[0];

    // 1. Guard: Authentication Check
    if (requiresAuth && !SessionManager.isAuthenticated()) {
      if (cleanPath !== '/login') {
        window.location.hash = `/login?redirect=${encodeURIComponent(path)}`;
        return;
      }
    }

    // 2. Guard: If already logged in and visiting /login, redirect to /
    if (!requiresAuth && cleanPath === '/login' && SessionManager.isAuthenticated()) {
      window.location.hash = '/';
      return;
    }

    // 3. Guard: Permission Check
    if (route.permission && !PermissionManager.can(route.permission)) {
      if (this.outlet) {
        this.outlet.innerHTML = `<fl-access-denied></fl-access-denied>`;
      }
      return;
    }

    this.currentRoute = { ...route, params, path };

    if (route.title) {
      document.title = `${route.title} - ${AppConfig.getBrandName()}`;
    }

    // Render Component into Outlet
    if (this.outlet) {
      const existingElement = this.outlet.firstElementChild;
      const targetTag = typeof route.component === 'string' ? route.component.toLowerCase() : null;

      if (existingElement && targetTag && existingElement.tagName.toLowerCase() === targetTag) {
        existingElement.routeParams = params;
        window.dispatchEvent(new CustomEvent('fl:route-changed', { detail: this.currentRoute }));
        return;
      }

      this.outlet.innerHTML = '';
      let element;
      if (typeof route.component === 'string') {
        element = document.createElement(route.component);
      } else if (typeof route.component === 'function') {
        element = new route.component();
      }

      if (element) {
        element.routeParams = params;
        this.outlet.appendChild(element);
      }
    }

    // Emit route changed event
    window.dispatchEvent(new CustomEvent('fl:route-changed', { detail: this.currentRoute }));
  }

  init() {
    window.addEventListener('hashchange', () => this.handleRouteChange());
    window.addEventListener('fl:unauthorized', () => {
      this.navigate('/login');
    });
  }
}

export const Router = new RouterClass();
