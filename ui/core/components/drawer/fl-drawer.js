/**
 * <fl-drawer>, <fl-profile-card>, <fl-key-value-list>, <fl-payment-card>
 * Slide-over detail drawer for viewing and managing member profiles.
 */

import { Icons } from '../../../assets/icons/icons.js';
import { AppConfig } from '../../config/app-config.js';
import '../shell/fl-avatar.js';
import '../table/fl-badge.js';

export class FlDrawer extends HTMLElement {
  static get observedAttributes() {
    return ['open', 'title'];
  }

  constructor() {
    super();
    this.member = null;
  }

  connectedCallback() {
    this.render();
  }

  attributeChangedCallback() {
    this.render();
  }

  setMember(member) {
    this.member = member;
    this.setAttribute('open', '');
    this.render();
  }

  close() {
    this.removeAttribute('open');
    this.member = null;
    this.render();
    this.dispatchEvent(new CustomEvent('fl-drawer-close', { bubbles: true }));
  }

  render() {
    const isOpen = this.hasAttribute('open');
    const title = this.getAttribute('title') || 'Member Profile';
    const m = this.member || {
      name: '',
      id: '',
      status: '',
      phone: '',
      email: '',
      plan: '',
      joinDate: '',
      expiryDate: '',
      paidAmount: '',
      paymentMethod: '',
      paymentDate: '',
      avatar: ''
    };

    this.style.position = 'fixed';
    this.style.top = '0';
    this.style.right = '0';
    this.style.bottom = '0';
    this.style.width = '390px';
    this.style.maxWidth = '100vw';
    this.style.backgroundColor = '#ffffff';
    this.style.borderLeft = '1px solid var(--fl-border, #e2e8f0)';
    this.style.boxShadow = isOpen ? 'var(--fl-shadow-xl)' : 'none';
    this.style.transform = isOpen ? 'translateX(0)' : 'translateX(100%)';
    this.style.transition = 'transform 300ms cubic-bezier(0.4, 0, 0.2, 1)';
    this.style.zIndex = 'var(--fl-z-drawer, 800)';
    this.style.display = 'flex';
    this.style.flexDirection = 'column';
    this.style.overflowY = 'auto';

    this.innerHTML = `
      <!-- Drawer Header -->
      <div style="
        padding: 18px 22px;
        border-bottom: 1px solid var(--fl-border-light, #f1f5f9);
        display: flex;
        align-items: center;
        justify-content: space-between;
      ">
        <h3 style="font-size: 16px; font-weight: 700; color: var(--fl-text, #0f172a);">${title}</h3>
        <button type="button" id="fl-drawer-close-btn" style="
          background: transparent;
          border: none;
          color: var(--fl-text-secondary, #64748b);
          cursor: pointer;
          display: flex;
          align-items: center;
          justify-content: center;
          width: 32px;
          height: 32px;
          border-radius: var(--fl-radius-md);
        " onmouseover="this.style.background='#f1f5f9'" onmouseout="this.style.background='transparent'">
          ${Icons.close}
        </button>
      </div>

      <!-- Drawer Body Content -->
      <div style="padding: 24px 22px; display: flex; flex-direction: column; gap: 24px; flex: 1;">
        <!-- Avatar & Name Profile Center Block -->
        <div style="display: flex; flex-direction: column; align-items: center; text-align: center; gap: 8px;">
          <fl-avatar size="84px" name="${m.name}" src="${m.avatar || ''}"></fl-avatar>
          <h2 style="font-size: 19px; font-weight: 700; color: var(--fl-text); margin-top: 4px;">
            ${m.name}
          </h2>
          <span style="font-size: 12px; font-weight: 600; color: var(--fl-text-muted);">
            ID: #${m.id || 'MEM-2024-001'}
          </span>
          <div style="margin-top: 2px;">
            <fl-badge variant="${m.status}"></fl-badge>
          </div>
        </div>

        <!-- Contact Info Box -->
        <div style="
          background-color: #f8fafc;
          border: 1px solid var(--fl-border, #e2e8f0);
          border-radius: var(--fl-radius-md, 8px);
          padding: 14px 16px;
          display: flex;
          flex-direction: column;
          gap: 8px;
        ">
          <div style="display: flex; justify-content: space-between; font-size: 13px;">
            <span style="font-size: 11px; font-weight: 700; color: var(--fl-text-muted); text-transform: uppercase;">Phone</span>
            <span style="font-weight: 600; color: var(--fl-text);">${m.phone || '-'}</span>
          </div>
          <div style="display: flex; justify-content: space-between; font-size: 13px;">
            <span style="font-size: 11px; font-weight: 700; color: var(--fl-text-muted); text-transform: uppercase;">Email</span>
            <span style="color: var(--fl-text-secondary); font-size: 12px;">${m.email || '-'}</span>
          </div>
        </div>

        <!-- Membership Plan Section -->
        <div>
          <h4 style="font-size: 13px; font-weight: 700; color: var(--fl-text); margin-bottom: 12px;">
            Membership Plan
          </h4>
          <div style="display: flex; flex-direction: column; gap: 10px; font-size: 13px;">
            <div style="display: flex; justify-content: space-between;">
              <span style="color: var(--fl-text-secondary);">Plan Type</span>
              <span style="font-weight: 600; color: var(--fl-text);">${m.plan || 'Annual Gold'}</span>
            </div>
            <div style="display: flex; justify-content: space-between;">
              <span style="color: var(--fl-text-secondary);">Join Date</span>
              <span style="font-weight: 500; color: var(--fl-text);">${m.joinDate || '12 Jan 2024'}</span>
            </div>
            <div style="display: flex; justify-content: space-between;">
              <span style="color: var(--fl-text-secondary);">Expiry Date</span>
              <span style="font-weight: 600; color: ${m.status === 'expired' ? 'var(--fl-danger)' : 'var(--fl-text)'};">
                ${m.expiryDate || '11 Jan 2025'}
              </span>
            </div>
          </div>
        </div>

        <!-- Action CTAs -->
        <div style="display: flex; flex-direction: column; gap: 10px;">
          <!-- Primary CTA Button -->
          <button type="button" id="fl-drawer-renew-btn" style="
            width: 100%;
            padding: 12px;
            background-color: var(--fl-primary, #dc2626);
            color: #ffffff;
            border: none;
            border-radius: var(--fl-radius-md, 8px);
            font-size: 14px;
            font-weight: 600;
            cursor: pointer;
            transition: background var(--fl-transition-fast);
          " onmouseover="this.style.background='var(--fl-primary-hover)'" onmouseout="this.style.background='var(--fl-primary)'">
            Renew Membership
          </button>

          <!-- Secondary Actions Row -->
          <div style="display: flex; gap: 10px;">
            <button type="button" id="fl-drawer-msg-btn" style="
              flex: 1;
              padding: 10px;
              background-color: #ffffff;
              color: var(--fl-text, #0f172a);
              border: 1px solid var(--fl-border, #e2e8f0);
              border-radius: var(--fl-radius-md, 8px);
              font-size: 13px;
              font-weight: 600;
              display: flex;
              align-items: center;
              justify-content: center;
              gap: 6px;
              cursor: pointer;
            " onmouseover="this.style.background='#f8fafc'" onmouseout="this.style.background='#ffffff'">
              ${Icons.chat}
              <span>Send Msg</span>
            </button>

            <button type="button" id="fl-drawer-edit-btn" style="
              flex: 1;
              padding: 10px;
              background-color: #ffffff;
              color: var(--fl-text, #0f172a);
              border: 1px solid var(--fl-border, #e2e8f0);
              border-radius: var(--fl-radius-md, 8px);
              font-size: 13px;
              font-weight: 600;
              display: flex;
              align-items: center;
              justify-content: center;
              gap: 6px;
              cursor: pointer;
            " onmouseover="this.style.background='#f8fafc'" onmouseout="this.style.background='#ffffff'">
              ${Icons.edit}
              <span>Edit Info</span>
            </button>
          </div>
        </div>

        <!-- Payment History Card -->
        <div>
          <h4 style="font-size: 13px; font-weight: 700; color: var(--fl-text); margin-bottom: 12px;">
            Payment History
          </h4>
          <div style="
            background: #ffffff;
            border: 1px solid var(--fl-border, #e2e8f0);
            border-radius: var(--fl-radius-md, 8px);
            padding: 14px 16px;
            display: flex;
            align-items: center;
            justify-content: space-between;
          ">
            <div style="display: flex; flex-direction: column; gap: 2px;">
              <span style="font-size: 14px; font-weight: 700; color: var(--fl-text);">
                ${m.paidAmountFormatted || (m.paidAmount !== undefined && m.paidAmount !== '' ? AppConfig.formatCurrency(m.paidAmount) : '')}
              </span>
              <span style="font-size: 11px; color: var(--fl-text-muted);">
                ${m.paymentMethod || ''}
              </span>
            </div>
            <span style="font-size: 12px; color: var(--fl-text-secondary); font-weight: 500;">
              ${m.paymentDate || ''}
            </span>
          </div>
        </div>
      </div>
    `;

    // Event Listeners
    const closeBtn = this.querySelector('#fl-drawer-close-btn');
    const renewBtn = this.querySelector('#fl-drawer-renew-btn');
    const msgBtn = this.querySelector('#fl-drawer-msg-btn');
    const editBtn = this.querySelector('#fl-drawer-edit-btn');

    if (closeBtn) {
      closeBtn.addEventListener('click', () => this.close());
    }

    if (renewBtn) {
      renewBtn.addEventListener('click', () => {
        this.dispatchEvent(new CustomEvent('fl-member-renew', { bubbles: true, detail: { member: this.member } }));
      });
    }

    if (msgBtn) {
      msgBtn.addEventListener('click', () => {
        this.dispatchEvent(new CustomEvent('fl-member-chat', { bubbles: true, detail: { member: this.member } }));
      });
    }

    if (editBtn) {
      editBtn.addEventListener('click', () => {
        this.dispatchEvent(new CustomEvent('fl-member-edit', { bubbles: true, detail: { member: this.member } }));
      });
    }
  }
}

customElements.define('fl-drawer', FlDrawer);
