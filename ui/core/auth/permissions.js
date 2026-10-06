/**
 * Freelance Frontend Framework - Role-Based Access Control (RBAC) & DOM Observer
 * Provides programmatic permission checks and automated declarative HTML element guarding.
 */

import { SessionManager } from './session.js';

class PermissionManagerClass {
  constructor() {
    this.observer = null;
    this.init();
  }

  getPermissions() {
    const user = SessionManager.getUser();
    if (!user) return [];
    // Permissions may be an array of strings or embedded in roles
    if (Array.isArray(user.permissions)) {
      return user.permissions;
    }
    if (Array.isArray(user.authorizations)) {
      return user.authorizations;
    }
    return [];
  }

  getRoles() {
    const user = SessionManager.getUser();
    if (!user) return [];
    if (Array.isArray(user.roles)) {
      return user.roles;
    }
    if (typeof user.role === 'string') {
      return [user.role];
    }
    return [];
  }

  can(permissionKey) {
    if (!permissionKey) return true;
    if (!SessionManager.isAuthenticated()) return false;

    // Super admin bypass
    const roles = this.getRoles();
    if (roles.includes('ROLE_SUPER_ADMIN') || roles.includes('SUPER_ADMIN') || roles.includes('ROLE_CONTROL_ADMIN')) {
      return true;
    }

    const perms = this.getPermissions();
    return perms.includes(permissionKey);
  }

  hasRole(roleName) {
    if (!roleName) return true;
    if (!SessionManager.isAuthenticated()) return false;
    const roles = this.getRoles();
    return roles.includes(roleName);
  }

  evaluateElement(el) {
    const requiredPermission = el.getAttribute('fl-permission');
    if (!requiredPermission) return;

    const hasAccess = this.can(requiredPermission);
    const action = el.getAttribute('fl-permission-action') || 'hide'; // 'hide' or 'disable'

    if (hasAccess) {
      if (action === 'hide') {
        el.removeAttribute('hidden');
        el.style.display = '';
      } else if (action === 'disable') {
        el.removeAttribute('disabled');
        el.classList.remove('fl-disabled');
      }
    } else {
      if (action === 'hide') {
        el.setAttribute('hidden', '');
        el.style.display = 'none';
      } else if (action === 'disable') {
        el.setAttribute('disabled', 'true');
        el.classList.add('fl-disabled');
      }
    }
  }

  scanDom(root = document) {
    const elements = root.querySelectorAll('[fl-permission]');
    elements.forEach(el => this.evaluateElement(el));
  }

  initDomObserver() {
    if (this.observer) return;

    this.observer = new MutationObserver(mutations => {
      for (const mutation of mutations) {
        if (mutation.type === 'childList') {
          mutation.addedNodes.forEach(node => {
            if (node.nodeType === Node.ELEMENT_NODE) {
              if (node.hasAttribute('fl-permission')) {
                this.evaluateElement(node);
              }
              this.scanDom(node);
            }
          });
        } else if (mutation.type === 'attributes' && mutation.attributeName === 'fl-permission') {
          this.evaluateElement(mutation.target);
        }
      }
    });

    if (document.body) {
      this.observer.observe(document.body, { childList: true, subtree: true, attributes: true, attributeFilter: ['fl-permission'] });
      this.scanDom();
    } else {
      document.addEventListener('DOMContentLoaded', () => {
        this.observer.observe(document.body, { childList: true, subtree: true, attributes: true, attributeFilter: ['fl-permission'] });
        this.scanDom();
      });
    }
  }

  init() {
    this.initDomObserver();
    window.addEventListener('fl:auth-changed', () => {
      this.scanDom();
    });
  }
}

export const PermissionManager = new PermissionManagerClass();
