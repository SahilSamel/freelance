/**
 * <fl-toast-container> & <fl-toast> Native Web Components
 * Floating notification toast stack for success, warning, and error alerts.
 */

import { Icons } from '../../../assets/icons/icons.js';

export class FlToastContainer extends HTMLElement {
  connectedCallback() {
    this.style.position = 'fixed';
    this.style.top = '20px';
    this.style.right = '24px';
    this.style.zIndex = 'var(--fl-z-toast, 2000)';
    this.style.display = 'flex';
    this.style.flexDirection = 'column';
    this.style.gap = '10px';
    this.style.pointerEvents = 'none';

    window.addEventListener('fl:toast', (e) => {
      this.addToast(e.detail);
    });
  }

  addToast({ message, type = 'info', title = '', duration = 4000 }) {
    const toast = document.createElement('fl-toast');
    toast.setAttribute('type', type);
    if (title) toast.setAttribute('title', title);
    toast.setAttribute('message', message);
    toast.setAttribute('duration', duration.toString());
    this.appendChild(toast);
  }
}

export class FlToast extends HTMLElement {
  connectedCallback() {
    const type = this.getAttribute('type') || 'info';
    const title = this.getAttribute('title') || '';
    const message = this.getAttribute('message') || '';
    const duration = parseInt(this.getAttribute('duration') || '4000', 10);

    const typeConfig = {
      success: { bg: '#ecfdf5', border: '#a7f3d0', text: '#065f46', icon: Icons.check },
      danger: { bg: '#fef2f2', border: '#fecaca', text: '#991b1b', icon: Icons.warning },
      warning: { bg: '#fffbeb', border: '#fde68a', text: '#92400e', icon: Icons.warning },
      info: { bg: '#f0f9ff', border: '#bae6fd', text: '#075985', icon: Icons.bell }
    };

    const cfg = typeConfig[type] || typeConfig.info;

    this.style.pointerEvents = 'auto';
    this.style.minWidth = '280px';
    this.style.maxWidth = '400px';
    this.style.backgroundColor = cfg.bg;
    this.style.border = `1px solid ${cfg.border}`;
    this.style.color = cfg.text;
    this.style.borderRadius = 'var(--fl-radius-md, 8px)';
    this.style.padding = '12px 16px';
    this.style.boxShadow = 'var(--fl-shadow-lg)';
    this.style.display = 'flex';
    this.style.alignItems = 'flex-start';
    this.style.gap = '12px';
    this.style.animation = 'flFadeIn 200ms ease-out forwards';

    this.innerHTML = `
      <div style="flex-shrink: 0; margin-top: 1px;">${cfg.icon}</div>
      <div style="flex: 1; min-width: 0;">
        ${title ? `<div style="font-weight: 700; font-size: 13px; margin-bottom: 2px;">${title}</div>` : ''}
        <div style="font-size: 12.5px; line-height: 1.4;">${message}</div>
      </div>
      <button type="button" class="fl-toast-close" style="
        background: transparent;
        border: none;
        color: currentColor;
        opacity: 0.6;
        cursor: pointer;
        padding: 2px;
        display: flex;
      " onmouseover="this.style.opacity='1'" onmouseout="this.style.opacity='0.6'">
        ${Icons.close}
      </button>
    `;

    const closeBtn = this.querySelector('.fl-toast-close');
    if (closeBtn) {
      closeBtn.addEventListener('click', () => this.dismiss());
    }

    if (duration > 0) {
      setTimeout(() => this.dismiss(), duration);
    }
  }

  dismiss() {
    this.style.opacity = '0';
    this.style.transform = 'translateY(-6px)';
    this.style.transition = 'all 200ms ease-in';
    setTimeout(() => this.remove(), 200);
  }
}

customElements.define('fl-toast-container', FlToastContainer);
customElements.define('fl-toast', FlToast);
