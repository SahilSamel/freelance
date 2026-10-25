/**
 * <fl-login-page> Native Web Component
 * Clean Operator & Administrator Authentication Page without hardcoded credentials.
 */

import { Icons } from '../../assets/icons/icons.js';
import { api } from '../../core/api/client.js';
import { SessionManager } from '../../core/auth/session.js';
import { AppConfig } from '../../core/config/app-config.js';
import { ErrorMapper } from '../../core/error/error-mapper.js';
import '../../core/components/forms/fl-input.js';

export class FlLoginPage extends HTMLElement {
  async connectedCallback() {
    this.brandListener = () => this.render();
    window.addEventListener('fl:config-changed', this.brandListener);

    // Automatically resolve tenant from hostname and load branding
    const tenant = SessionManager.getTenantId();
    if (tenant) {
      try {
        await AppConfig.loadFromBackend(tenant);
      } catch (_) {}
    }

    this.render();
  }

  disconnectedCallback() {
    if (this.brandListener) {
      window.removeEventListener('fl:config-changed', this.brandListener);
    }
  }

  render() {
    const gymName = AppConfig.getBrandName();
    const gymSubtitle = AppConfig.getBrandSubtitle() || 'Sign in to access your operations dashboard';
    const logoUrl = AppConfig.getBrandLogoUrl();

    this.style.display = 'flex';
    this.style.minHeight = '100vh';
    this.style.alignItems = 'center';
    this.style.justifyContent = 'center';
    this.style.backgroundColor = '#f8fafc';
    this.style.padding = '20px';

    this.innerHTML = `
      <div style="
        width: 100%;
        max-width: 420px;
        background: #ffffff;
        border: 1px solid var(--fl-border, #e2e8f0);
        border-radius: var(--fl-radius-xl, 16px);
        padding: 36px 32px;
        box-shadow: var(--fl-shadow-lg);
        display: flex;
        flex-direction: column;
        animation: flFadeIn 250ms ease-out forwards;
      ">
        <!-- Brand Header -->
        <div style="display: flex; flex-direction: column; align-items: center; text-align: center; margin-bottom: 28px;">
          <div style="
            width: 56px;
            height: 56px;
            border-radius: var(--fl-radius-lg, 12px);
            background: #ffffff;
            border: 1px solid var(--fl-border, #e2e8f0);
            display: flex;
            align-items: center;
            justify-content: center;
            margin-bottom: 14px;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
            overflow: hidden;
            padding: 4px;
          ">
            ${logoUrl ? `<img src="${logoUrl}" alt="${gymName}" style="width:100%; height:100%; object-fit:contain;" />` : Icons.logo}
          </div>
          <h1 style="font-size: 22px; font-weight: 700; color: var(--fl-text, #0f172a);">${gymName}</h1>
          <p style="font-size: 13px; color: var(--fl-text-secondary, #64748b); margin-top: 4px;">
            ${gymSubtitle}
          </p>
        </div>

        <!-- Login Form -->
        <form id="fl-login-form" novalidate>
          <fl-input 
            label="Username" 
            name="username" 
            value=""
            placeholder="Enter your username"
            required
          ></fl-input>

          <fl-input 
            label="Password" 
            name="password" 
            type="password" 
            value=""
            placeholder="Enter your password"
            required
          ></fl-input>

          <button type="submit" id="fl-login-submit-btn" class="fl-btn fl-btn-primary" style="
            width: 100%;
            height: 42px;
            margin-top: 8px;
            font-size: 14px;
            box-shadow: 0 2px 8px rgba(15, 23, 42, 0.15);
          ">
            <span>Sign In</span>
          </button>
        </form>
      </div>
    `;

    const form = this.querySelector('#fl-login-form');
    const submitBtn = this.querySelector('#fl-login-submit-btn');

    if (form) {
      form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const usernameInput = form.querySelector('[name="username"]');
        const passwordInput = form.querySelector('[name="password"]');

        const tenant = SessionManager.getTenantId();
        const username = usernameInput ? usernameInput.getValue().trim() : '';
        const password = passwordInput ? passwordInput.getValue() : '';

        if (!username) {
          if (usernameInput) usernameInput.setError('Username is required');
          return;
        }
        if (!password) {
          if (passwordInput) passwordInput.setError('Password is required');
          return;
        }

        submitBtn.disabled = true;
        submitBtn.innerHTML = `<span>Signing In...</span>`;

        try {
          if (tenant) {
            SessionManager.setTenantId(tenant);
          }
          
          // Authenticate with live backend
          const response = await api.post('/auth/login', { username, password });
          if (!response || !response.token) {
            throw new Error('Invalid response from authentication server');
          }

          const token = response.token;
          const user = {
            username,
            name: username
          };

          SessionManager.setSession(token, user, tenant);
          sessionStorage.setItem('fl_just_logged_in', 'true');
          
          // Refresh branding for active tenant
          await AppConfig.loadFromBackend(tenant);

          ErrorMapper.notifySuccess(`Welcome back, ${user.name}!`, 'Signed In');

          const hashQuery = window.location.hash.includes('?') ? window.location.hash.split('?')[1] : '';
          const params = new URLSearchParams(window.location.search || hashQuery);
          const redirect = params.get('redirect') || '/';
          
          if (window.fl && window.fl.router) {
            window.fl.router.navigate(redirect);
          } else {
            window.location.hash = redirect.startsWith('/') ? redirect : `/${redirect}`;
          }

        } catch (err) {
          ErrorMapper.handle(err, form);
        } finally {
          submitBtn.disabled = false;
          submitBtn.innerHTML = `<span>Sign In</span>`;
        }
      });
    }
  }
}

customElements.define('fl-login-page', FlLoginPage);
