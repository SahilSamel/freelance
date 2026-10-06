/**
 * <fl-error-window-container> & <fl-error-window> Native Web Components
 * Top-center floating red error window/banner for server, network, and operational errors.
 */

import { Icons } from '../../../assets/icons/icons.js';

export class FlErrorWindowContainer extends HTMLElement {
  connectedCallback() {
    this.className = 'fl-error-window-container';

    this._handler = (e) => {
      this.showError(e.detail);
    };
    window.addEventListener('fl:error-window', this._handler);
  }

  disconnectedCallback() {
    if (this._handler) {
      window.removeEventListener('fl:error-window', this._handler);
    }
  }

  showError({ title = 'Error', message = '', errorCode = '', duration = 6000 }) {
    const errorEl = document.createElement('div');
    errorEl.className = 'fl-error-window';
    errorEl.setAttribute('role', 'alert');

    errorEl.innerHTML = `
      <div style="color: var(--fl-danger, #e53e3e); flex-shrink: 0; margin-top: 2px;">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10"></circle>
          <line x1="12" y1="8" x2="12" y2="12"></line>
          <line x1="12" y1="16" x2="12.01" y2="16"></line>
        </svg>
      </div>
      <div style="flex: 1; min-width: 0;">
        <div style="display: flex; align-items: center; justify-content: space-between; gap: 8px;">
          <h4 style="margin: 0; font-size: 14px; font-weight: 700; color: #991b1b; display: flex; align-items: center; gap: 6px;">
            ${title}
            ${errorCode ? `<span style="font-size: 11px; background: #fee2e2; color: #991b1b; padding: 2px 6px; border-radius: 4px; font-weight: 600;">${errorCode}</span>` : ''}
          </h4>
        </div>
        <div style="font-size: 13px; color: #7f1d1d; margin-top: 4px; line-height: 1.45; word-break: break-word;">
          ${message}
        </div>
      </div>
      <button type="button" class="fl-error-close-btn" aria-label="Close error" style="
        background: transparent;
        border: none;
        cursor: pointer;
        padding: 2px;
        color: #991b1b;
        opacity: 0.7;
        transition: opacity 0.15s ease;
        display: flex;
        align-items: center;
        justify-content: center;
      ">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <line x1="18" y1="6" x2="6" y2="18"></line>
          <line x1="6" y1="6" x2="18" y2="18"></line>
        </svg>
      </button>
    `;

    const closeBtn = errorEl.querySelector('.fl-error-close-btn');
    const dismiss = () => {
      errorEl.style.transition = 'opacity 0.2s ease, transform 0.2s ease';
      errorEl.style.opacity = '0';
      errorEl.style.transform = 'translateY(-12px)';
      setTimeout(() => errorEl.remove(), 200);
    };

    closeBtn.addEventListener('click', dismiss);

    if (duration > 0) {
      setTimeout(dismiss, duration);
    }

    this.prepend(errorEl);
  }
}

customElements.define('fl-error-window-container', FlErrorWindowContainer);
