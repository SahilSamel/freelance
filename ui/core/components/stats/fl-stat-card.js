/**
 * <fl-stat-card> Native Web Component
 * Metric stat card displaying label, bold numeric counter, and circular colored icon badge.
 */

import { Icons } from '../../../assets/icons/icons.js';

export class FlStatCard extends HTMLElement {
  static get observedAttributes() {
    return ['label', 'value', 'variant', 'icon'];
  }

  connectedCallback() {
    this.render();
  }

  attributeChangedCallback() {
    this.render();
  }

  render() {
    const label = this.getAttribute('label') || 'Metric';
    const value = this.getAttribute('value') || '0';
    const variant = this.getAttribute('variant') || 'success'; // success, warning, attention, danger
    const iconName = this.getAttribute('icon') || 'users';

    const variantStyles = {
      success: {
        bg: 'var(--fl-status-active-bg, #ecfdf5)',
        color: 'var(--fl-status-active-text, #059669)',
        icon: Icons[iconName] || Icons.users
      },
      warning: {
        bg: 'var(--fl-status-expiring-bg, #fefce8)',
        color: 'var(--fl-status-expiring-text, #ca8a04)',
        icon: Icons[iconName] || Icons.warning
      },
      attention: {
        bg: 'var(--fl-status-overdue-bg, #fff7ed)',
        color: 'var(--fl-status-overdue-text, #ea580c)',
        icon: Icons[iconName] || Icons.clock
      },
      danger: {
        bg: 'var(--fl-status-expired-bg, #fef2f2)',
        color: 'var(--fl-status-expired-text, #dc2626)',
        icon: Icons[iconName] || Icons.userX
      }
    };

    const currentStyle = variantStyles[variant] || variantStyles.success;

    this.style.display = 'block';
    this.style.flex = '1';
    this.style.minWidth = '200px';

    this.innerHTML = `
      <div style="
        background: var(--fl-surface, #ffffff);
        border: 1px solid var(--fl-border, #e2e8f0);
        border-radius: var(--fl-radius-lg, 12px);
        padding: 18px 22px;
        display: flex;
        align-items: center;
        justify-content: space-between;
        box-shadow: var(--fl-shadow-xs);
        transition: transform var(--fl-transition-fast), box-shadow var(--fl-transition-fast);
      " class="fl-stat-card-inner">
        <div style="display: flex; flex-direction: column; gap: 4px;">
          <span style="font-size: 13px; font-weight: 500; color: var(--fl-text-secondary, #64748b);">
            ${label}
          </span>
          <span style="font-size: 30px; font-weight: 700; color: var(--fl-text, #0f172a); line-height: 1.1;">
            ${value}
          </span>
        </div>
        <div style="
          width: 44px;
          height: 44px;
          border-radius: 50%;
          background: ${currentStyle.bg};
          color: ${currentStyle.color};
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;
        ">
          ${currentStyle.icon}
        </div>
      </div>
    `;
  }
}

export class FlStatRibbon extends HTMLElement {
  connectedCallback() {
    this.style.display = 'grid';
    this.style.gridTemplateColumns = 'repeat(auto-fit, minmax(220px, 1fr))';
    this.style.gap = '16px';
    this.style.marginBottom = '20px';
  }
}

customElements.define('fl-stat-card', FlStatCard);
customElements.define('fl-stat-ribbon', FlStatRibbon);
