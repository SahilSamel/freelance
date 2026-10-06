/**
 * <fl-access-denied> Native Web Component
 * Framework-level 403 Forbidden Access Denied view with generic navigation actions.
 */

import { Icons } from '../../../assets/icons/icons.js';

export class FlAccessDenied extends HTMLElement {
  connectedCallback() {
    this.style.display = 'flex';
    this.style.flexDirection = 'column';
    this.style.alignItems = 'center';
    this.style.justifyContent = 'center';
    this.style.minHeight = '60vh';
    this.style.padding = '60px 20px';
    this.style.textAlign = 'center';

    this.innerHTML = `
      <div style="
        width: 64px;
        height: 64px;
        border-radius: 50%;
        background: #fee2e2;
        color: var(--fl-danger, #ef4444);
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 20px;
      ">
        ${Icons.userX || `
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
            <circle cx="8.5" cy="7" r="4"></circle>
            <line x1="18" y1="8" x2="23" y2="13"></line>
            <line x1="23" y1="8" x2="18" y2="13"></line>
          </svg>
        `}
      </div>
      <h2 style="font-size: 24px; font-weight: 700; color: var(--fl-text); margin-bottom: 8px;">
        Access Denied (403)
      </h2>
      <p style="font-size: 14px; color: var(--fl-text-secondary); max-width: 440px; margin-bottom: 28px; line-height: 1.5;">
        You do not have the required permissions to view this section. If you believe this is an error, please contact your administrator.
      </p>
      <div style="display: flex; gap: 12px; align-items: center;">
        <button type="button" id="fl-back-prev-btn" style="
          background: #ffffff;
          color: var(--fl-text, #0f172a);
          border: 1px solid var(--fl-border, #cbd5e1);
          border-radius: var(--fl-radius-md, 8px);
          padding: 10px 20px;
          font-size: 13.5px;
          font-weight: 600;
          cursor: pointer;
        ">
          ← Go Back
        </button>
        <button type="button" id="fl-back-home-btn" style="
          background: var(--fl-primary, #dc2626);
          color: #ffffff;
          border: none;
          border-radius: var(--fl-radius-md, 8px);
          padding: 10px 20px;
          font-size: 13.5px;
          font-weight: 600;
          cursor: pointer;
        ">
          Go to Home
        </button>
      </div>
    `;

    const backBtn = this.querySelector('#fl-back-prev-btn');
    if (backBtn) {
      backBtn.addEventListener('click', () => {
        window.history.back();
      });
    }

    const homeBtn = this.querySelector('#fl-back-home-btn');
    if (homeBtn) {
      homeBtn.addEventListener('click', () => {
        if (window.fl && window.fl.router) {
          window.fl.router.navigate('/');
        } else {
          window.location.hash = '#/';
        }
      });
    }
  }
}

customElements.define('fl-access-denied', FlAccessDenied);
