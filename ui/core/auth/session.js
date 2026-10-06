/**
 * Freelance Frontend Framework - Session & Token Lifecycle Manager
 * Handles local storage, session state, request activity tracking, and "Stay Connected" prompts.
 */

const TOKEN_KEY = 'fl_access_token';
const USER_KEY = 'fl_auth_user';
const TENANT_KEY = 'fl_active_tenant';

// Timeouts matching backend SessionConfigKeys.SESSION_TTL_SECONDS (600s = 10 minutes)
const SESSION_TTL_MS = 10 * 60 * 1000;          // 10 minutes server session TTL
const INACTIVITY_WARNING_MS = 8 * 60 * 1000;    // 8 minutes since last backend hit before warning

class SessionManagerClass {
  constructor() {
    this.lastRequestTime = Date.now();
    this.inactivityInterval = null;
    this.isPromptOpen = false;
    this.modalEl = null;

    const token = localStorage.getItem(TOKEN_KEY);
    if (token && token.startsWith('demo_token_')) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
    }

    this.startInactivityWatcher();
  }

  getTenantId() {
    if (typeof window !== 'undefined' && window.location) {
      const host = window.location.hostname;
      if (host && !host.startsWith('localhost') && !/^[0-9.]+$/.test(host)) {
        const parts = host.split('.');
        if (parts.length >= 3 && parts[0] !== 'www' && parts[0] !== 'api') {
          return parts[0].toLowerCase();
        }
      }

      const urlParams = new URLSearchParams(window.location.search);
      const paramTenant = urlParams.get('tenant');
      if (paramTenant) {
        this.setTenantId(paramTenant);
        return paramTenant.toLowerCase();
      }
    }

    return localStorage.getItem(TENANT_KEY) || '';
  }

  setTenantId(tenantId) {
    if (tenantId) {
      localStorage.setItem(TENANT_KEY, tenantId.trim().toLowerCase());
    } else {
      localStorage.removeItem(TENANT_KEY);
    }
  }

  getToken() {
    return localStorage.getItem(TOKEN_KEY);
  }

  setToken(token) {
    if (token) {
      localStorage.setItem(TOKEN_KEY, token);
      this.recordRequest();
    } else {
      localStorage.removeItem(TOKEN_KEY);
    }
  }

  getUser() {
    try {
      const raw = localStorage.getItem(USER_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }

  setUser(user) {
    if (user) {
      localStorage.setItem(USER_KEY, JSON.stringify(user));
    } else {
      localStorage.removeItem(USER_KEY);
    }
  }

  setSession(token, user, tenantId = null) {
    if (tenantId) {
      this.setTenantId(tenantId);
    }
    this.setToken(token);
    this.setUser(user);
    this.recordRequest();
    window.dispatchEvent(new CustomEvent('fl:auth-changed', { detail: { isAuthenticated: true, user } }));
  }

  clearSession() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.hideInactivityModal();
    window.dispatchEvent(new CustomEvent('fl:auth-changed', { detail: { isAuthenticated: false, user: null } }));
  }

  isAuthenticated() {
    return Boolean(this.getToken());
  }

  /**
   * Called whenever an authenticated request hits the backend.
   * Since each hit slides the server session expiry by 10 minutes,
   * we update lastRequestTime to reflect the server's renewed window.
   */
  recordRequest() {
    this.lastRequestTime = Date.now();
  }

  recordActivity() {
    this.recordRequest();
  }

  /**
   * Refreshes the session with the backend by calling POST /auth/refresh.
   * Rotates the sessionKey on the backend and updates the client with the new token.
   */
  async refreshSession() {
    try {
      const { api } = await import('../api/client.js');
      const res = await api.post('/auth/refresh', {}, { silentError: false });
      if (res && res.token) {
        this.setToken(res.token);
        this.recordRequest();
        this.hideInactivityModal();
        if (window.fl && window.fl.errors && window.fl.errors.toast) {
          window.fl.errors.toast('Your session has been extended.', 'success', 'Session Active');
        }
        return true;
      }
    } catch (err) {
      console.warn('[SessionManager] Session refresh failed:', err);
      this.hideInactivityModal();
      this.clearSession();
      if (typeof window !== 'undefined') {
        window.location.hash = '/login';
      }
      return false;
    }
  }

  /**
   * Gracefully invalidates the session on the backend and logs the user out locally.
   */
  async logout() {
    this.hideInactivityModal();
    try {
      const { api } = await import('../api/client.js');
      await api.post('/auth/logout', {}, { silentError: true });
    } catch {
      // Best-effort logout
    }
    this.clearSession();
    if (typeof window !== 'undefined') {
      window.location.hash = '/login';
    }
  }

  /**
   * Monitors elapsed time since the last backend API request.
   */
  startInactivityWatcher() {
    if (typeof window === 'undefined') return;

    if (this.inactivityInterval) {
      clearInterval(this.inactivityInterval);
    }

    this.inactivityInterval = setInterval(() => {
      if (!this.isAuthenticated()) {
        if (this.isPromptOpen) {
          this.hideInactivityModal();
        }
        return;
      }

      const idleMs = Date.now() - this.lastRequestTime;

      // 1. Hard server timeout reached -> Force logout
      if (idleMs >= SESSION_TTL_MS) {
        this.logout();
        return;
      }

      // 2. 8 minutes of inactivity reached -> Prompt user to stay connected
      if (idleMs >= INACTIVITY_WARNING_MS) {
        const remainingSec = Math.max(0, Math.ceil((SESSION_TTL_MS - idleMs) / 1000));
        this.showInactivityModal(remainingSec);
      } else if (this.isPromptOpen) {
        this.hideInactivityModal();
      }
    }, 1000);
  }

  /**
   * Displays the "Stay Connected" dialog with an active countdown.
   */
  showInactivityModal(remainingSeconds) {
    this.isPromptOpen = true;

    if (!this.modalEl) {
      this.modalEl = document.createElement('div');
      this.modalEl.id = 'fl-inactivity-modal';
      this.modalEl.style.position = 'fixed';
      this.modalEl.style.top = '0';
      this.modalEl.style.left = '0';
      this.modalEl.style.right = '0';
      this.modalEl.style.bottom = '0';
      this.modalEl.style.backgroundColor = 'rgba(15, 23, 42, 0.65)';
      this.modalEl.style.backdropFilter = 'blur(4px)';
      this.modalEl.style.zIndex = '99999';
      this.modalEl.style.display = 'flex';
      this.modalEl.style.alignItems = 'center';
      this.modalEl.style.justifyContent = 'center';
      this.modalEl.style.padding = '20px';

      this.modalEl.innerHTML = `
        <div style="
          background: #ffffff;
          border-radius: 14px;
          max-width: 440px;
          width: 100%;
          padding: 28px 24px;
          box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
          text-align: center;
          font-family: inherit;
        ">
          <!-- Warning Icon -->
          <div style="
            width: 48px;
            height: 48px;
            margin: 0 auto 16px;
            border-radius: 50%;
            background: #fef3c7;
            color: #d97706;
            display: flex;
            align-items: center;
            justify-content: center;
          ">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"></circle>
              <polyline points="12 6 12 12 16 14"></polyline>
            </svg>
          </div>

          <h3 style="font-size: 18px; font-weight: 700; color: #0f172a; margin: 0 0 8px;">
            Are you still there?
          </h3>
          <p style="font-size: 14px; color: #64748b; line-height: 1.5; margin: 0 0 20px;">
            You have been inactive for a while. For your security, your session will automatically end in
            <strong id="fl-inactivity-sec" style="color: #d97706; font-size: 15px;">${remainingSeconds}</strong> seconds.
          </p>

          <div style="display: flex; gap: 12px; justify-content: center;">
            <button id="fl-inactivity-logout-btn" type="button" style="
              flex: 1;
              padding: 10px 16px;
              border: 1px solid #e2e8f0;
              background: #ffffff;
              color: #475569;
              font-size: 14px;
              font-weight: 600;
              border-radius: 8px;
              cursor: pointer;
            ">
              Log Out
            </button>
            <button id="fl-inactivity-stay-btn" type="button" style="
              flex: 1;
              padding: 10px 16px;
              border: none;
              background: #2563eb;
              color: #ffffff;
              font-size: 14px;
              font-weight: 600;
              border-radius: 8px;
              cursor: pointer;
            ">
              Stay Connected
            </button>
          </div>
        </div>
      `;

      document.body.appendChild(this.modalEl);

      const stayBtn = this.modalEl.querySelector('#fl-inactivity-stay-btn');
      const logoutBtn = this.modalEl.querySelector('#fl-inactivity-logout-btn');

      stayBtn.addEventListener('click', async () => {
        stayBtn.disabled = true;
        stayBtn.innerText = 'Connecting...';
        await this.refreshSession();
        stayBtn.disabled = false;
        stayBtn.innerText = 'Stay Connected';
      });

      logoutBtn.addEventListener('click', async () => {
        logoutBtn.disabled = true;
        logoutBtn.innerText = 'Signing out...';
        await this.logout();
      });
    }

    const secEl = this.modalEl.querySelector('#fl-inactivity-sec');
    if (secEl) {
      secEl.innerText = remainingSeconds;
    }
  }

  /**
   * Hides and removes the inactivity warning modal.
   */
  hideInactivityModal() {
    this.isPromptOpen = false;
    if (this.modalEl && this.modalEl.parentNode) {
      this.modalEl.parentNode.removeChild(this.modalEl);
      this.modalEl = null;
    }
  }
}

export const SessionManager = new SessionManagerClass();
