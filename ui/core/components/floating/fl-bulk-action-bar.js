/**
 * <fl-bulk-action-bar> & <fl-fab> Native Web Components
 * Bottom floating action bar for bulk operations and primary FAB CTA.
 */

import { Icons } from '../../../assets/icons/icons.js';
import { AppConfig } from '../../config/app-config.js';

export class FlBulkActionBar extends HTMLElement {
  static get observedAttributes() {
    return ['count', 'visible'];
  }

  connectedCallback() {
    this.render();
  }

  attributeChangedCallback() {
    this.render();
  }

  setTemplates(templates = []) {
    this.templates = templates;
    this.render();
  }

  render() {
    const count = parseInt(this.getAttribute('count') || '0', 10);
    const isVisible = count > 0 || this.hasAttribute('visible');
    const selectedTemplate = this.getAttribute('selected-template') || 'Renewal Reminder';

    this.style.position = 'fixed';
    this.style.bottom = '24px';
    this.style.left = '50%';
    this.style.transform = isVisible ? 'translateX(-50%) translateY(0)' : 'translateX(-50%) translateY(100px)';
    this.style.opacity = isVisible ? '1' : '0';
    this.style.pointerEvents = isVisible ? 'auto' : 'none';
    this.style.transition = 'all 300ms cubic-bezier(0.4, 0, 0.2, 1)';
    this.style.zIndex = 'var(--fl-z-floating-bar, 500)';

    this.innerHTML = `
      <div style="
        background-color: #0f172a;
        color: #ffffff;
        border-radius: var(--fl-radius-full, 9999px);
        padding: 8px 12px 8px 16px;
        display: flex;
        align-items: center;
        gap: 14px;
        box-shadow: var(--fl-shadow-floating);
        border: 1px solid rgba(255, 255, 255, 0.1);
      ">
        <!-- Count Badge -->
        <div style="
          display: flex;
          align-items: center;
          gap: 6px;
          background: var(--fl-accent, #f97316);
          color: #ffffff;
          padding: 6px 12px;
          border-radius: var(--fl-radius-full);
          font-size: 13px;
          font-weight: 600;
        ">
          <span>${Icons.check}</span>
          <span>${count} member${count > 1 ? 's' : ''} selected</span>
        </div>

        <!-- Template Selector -->
        <div style="position: relative;">
          <select id="fl-bulk-template-select" style="
            background: rgba(255, 255, 255, 0.08);
            color: #f8fafc;
            border: 1px solid rgba(255, 255, 255, 0.15);
            border-radius: var(--fl-radius-md, 8px);
            padding: 8px 12px;
            font-size: 13px;
            outline: none;
            cursor: pointer;
          ">
            ${((this.templates && this.templates.length > 0 ? this.templates : AppConfig.getQuickTemplates()) || []).length > 0 ?
              ((this.templates && this.templates.length > 0 ? this.templates : AppConfig.getQuickTemplates()) || []).map(t => `
                <option value="${t.title || t.key || t}" style="background:#0f172a; color:#fff;">Template: ${t.title || t.key || t}</option>
              `).join('') : `
                <option value="Direct Notification" style="background:#0f172a; color:#fff;">Template: Direct Notification</option>
              `
            }
          </select>
        </div>

        <!-- Primary Action CTA -->
        <button type="button" id="fl-bulk-send-btn" style="
          background-color: var(--fl-primary, #dc2626);
          color: #ffffff;
          border: none;
          border-radius: var(--fl-radius-full);
          padding: 8px 18px;
          font-size: 13px;
          font-weight: 600;
          display: flex;
          align-items: center;
          gap: 8px;
          cursor: pointer;
          transition: background-color var(--fl-transition-fast);
        " onmouseover="this.style.background='var(--fl-primary-hover)'" onmouseout="this.style.background='var(--fl-primary)'">
          <span style="display:flex;">${Icons.chat}</span>
          <span>Send WhatsApp Message</span>
        </button>
      </div>
    `;

    const sendBtn = this.querySelector('#fl-bulk-send-btn');
    const select = this.querySelector('#fl-bulk-template-select');

    if (sendBtn && select) {
      sendBtn.addEventListener('click', () => {
        this.dispatchEvent(new CustomEvent('fl-bulk-submit', {
          bubbles: true,
          detail: {
            template: select.value,
            count
          }
        }));
      });
    }
  }
}

export class FlFab extends HTMLElement {
  static get observedAttributes() {
    return ['label', 'icon'];
  }

  connectedCallback() {
    this.render();
  }

  attributeChangedCallback() {
    this.render();
  }

  render() {
    const label = this.getAttribute('label') || '+ Add Member';

    this.style.position = 'fixed';
    this.style.bottom = '24px';
    this.style.right = '28px';
    this.style.zIndex = 'var(--fl-z-floating-bar, 500)';

    this.innerHTML = `
      <button type="button" style="
        background-color: var(--fl-primary, #dc2626);
        color: #ffffff;
        border: none;
        border-radius: var(--fl-radius-full, 9999px);
        padding: 12px 22px;
        font-size: 14px;
        font-weight: 600;
        display: flex;
        align-items: center;
        gap: 8px;
        box-shadow: 0 4px 14px rgba(220, 38, 38, 0.35);
        cursor: pointer;
        transition: all var(--fl-transition-fast);
      " id="fl-fab-btn" onmouseover="this.style.background='var(--fl-primary-hover)'; this.style.transform='translateY(-2px)'" onmouseout="this.style.background='var(--fl-primary)'; this.style.transform='translateY(0)'">
        <span style="display:flex;">${Icons.plus}</span>
        <span>${label}</span>
      </button>
    `;

    const btn = this.querySelector('#fl-fab-btn');
    if (btn) {
      btn.addEventListener('click', (e) => {
        this.dispatchEvent(new CustomEvent('fl-fab-click', { bubbles: true }));
      });
    }
  }
}

customElements.define('fl-bulk-action-bar', FlBulkActionBar);
customElements.define('fl-fab', FlFab);
