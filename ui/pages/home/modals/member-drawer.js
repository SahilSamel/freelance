/**
 * <fl-member-drawer> Native Web Component
 * Slide-out drawer displaying Member 360 profile, active plans, and installment breakdown.
 */

import { HomeStore } from '../home-store.js';
import { Icons } from '../../../assets/icons/icons.js';
import { AppConfig } from '../../../core/config/app-config.js';
import '../../../core/components/drawer/fl-drawer.js';
import '../../../core/components/table/fl-badge.js';

export class FlMemberDrawer extends HTMLElement {
  connectedCallback() {
    this.unsubscribe = HomeStore.subscribe(() => this.render());
    this.render();
  }

  disconnectedCallback() {
    if (this.unsubscribe) this.unsubscribe();
  }

  render() {
    const { memberDrawer } = HomeStore.getState();
    const { isOpen, member } = memberDrawer;

    if (!isOpen || !member) {
      this.innerHTML = '';
      return;
    }

    const planBatches = member.planBatches || [];
    const isExpired = member.status === 'expired';
    const isRenewals = member.status === 'renewals';

    let badgeVariant = 'success';
    if (isExpired) badgeVariant = 'danger';
    else if (isRenewals) badgeVariant = 'warning';

    this.innerHTML = `
      <fl-drawer title="Member Profile" width="520px">
        <div style="display: flex; flex-direction: column; gap: 20px;">
          
          <!-- Member Profile Header Card -->
          <div style="background: #f8fafc; border: 1px solid var(--fl-border); border-radius: 12px; padding: 18px; display: flex; align-items: center; gap: 16px;">
            <div style="
              width: 52px;
              height: 52px;
              border-radius: 50%;
              background: var(--fl-primary-light, #fee2e2);
              color: var(--fl-primary, #dc2626);
              display: flex;
              align-items: center;
              justify-content: center;
              font-weight: 700;
              font-size: 18px;
            ">
              ${(member.name || 'M').charAt(0).toUpperCase()}
            </div>
            <div style="flex: 1; min-width: 0;">
              <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 4px;">
                <h3 style="margin: 0; font-size: 17px; font-weight: 700; color: var(--fl-text);">${member.name}</h3>
                <fl-badge variant="${badgeVariant}" text="${member.statusLabel || member.status}"></fl-badge>
              </div>
              <div style="font-size: 13px; color: var(--fl-text-secondary); display: flex; gap: 12px; flex-wrap: wrap;">
                <span>📞 ${member.phone}</span>
                ${member.memberSince ? `<span>🗓️ Member since ${HomeStore.formatDateDisplay(member.memberSince)}</span>` : ''}
              </div>
            </div>
          </div>

          <!-- Quick Actions Bar -->
          <div style="display: flex; gap: 10px;">
            <button type="button" id="fl-drawer-wa-btn" style="
              flex: 1;
              background: #25d366;
              color: #ffffff;
              border: none;
              border-radius: 8px;
              padding: 9px 14px;
              font-size: 13px;
              font-weight: 600;
              display: flex;
              align-items: center;
              justify-content: center;
              gap: 8px;
              cursor: pointer;
            ">
              ${Icons.whatsapp || '💬'} WhatsApp Message
            </button>
            <button type="button" id="fl-drawer-renew-btn" style="
              flex: 1;
              background: var(--fl-primary, #dc2626);
              color: #ffffff;
              border: none;
              border-radius: 8px;
              padding: 9px 14px;
              font-size: 13px;
              font-weight: 600;
              cursor: pointer;
            ">
              Renew Membership
            </button>
          </div>

          <!-- Financial Snapshot -->
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
            <div style="background: #ffffff; border: 1px solid var(--fl-border); border-radius: 10px; padding: 12px;">
              <span style="font-size: 11px; text-transform: uppercase; font-weight: 600; color: var(--fl-text-muted);">Total Paid</span>
              <div style="font-size: 18px; font-weight: 800; color: #16a34a; margin-top: 2px;">
                ${AppConfig.formatCurrency(member.totalSpent || 0)}
              </div>
            </div>
            <div style="background: #ffffff; border: 1px solid var(--fl-border); border-radius: 10px; padding: 12px;">
              <span style="font-size: 11px; text-transform: uppercase; font-weight: 600; color: var(--fl-text-muted);">Pending Dues</span>
              <div style="font-size: 18px; font-weight: 800; color: ${(member.pendingAmount || 0) > 0 ? '#dc2626' : '#16a34a'}; margin-top: 2px;">
                ${AppConfig.formatCurrency(member.pendingAmount || 0)}
              </div>
            </div>
          </div>

          <!-- Membership Plans & Installments -->
          <div>
            <h4 style="font-size: 14px; font-weight: 700; color: var(--fl-text); margin: 0 0 10px 0;">
              Membership Subscriptions & Installments
            </h4>

            ${planBatches.length === 0 ? `
              <div style="padding: 24px; text-align: center; color: var(--fl-text-muted); font-size: 13px; background: #f8fafc; border-radius: 8px;">
                No active membership plan records found.
              </div>
            ` : planBatches.map((batch) => `
              <div style="border: 1px solid var(--fl-border); border-radius: 10px; margin-bottom: 12px; overflow: hidden; background: #ffffff;">
                <div style="background: #f1f5f9; padding: 10px 14px; display: flex; justify-content: space-between; align-items: center;">
                  <div>
                    <div style="font-weight: 700; font-size: 13.5px; color: var(--fl-text);">${batch.plans}</div>
                    <div style="font-size: 11.5px; color: var(--fl-text-secondary);">${batch.period}</div>
                  </div>
                  <div style="text-align: right;">
                    <div style="font-weight: 700; font-size: 13px; color: var(--fl-text);">${batch.totalAmountFormatted}</div>
                    ${batch.pendingAmount > 0 ? `<div style="font-size: 11px; font-weight: 600; color: var(--fl-danger);">${batch.pendingAmountFormatted}</div>` : ''}
                  </div>
                </div>

                <!-- Installments Table -->
                <div style="padding: 8px 12px;">
                  ${(batch.installments || []).map(inst => `
                    <div style="display: flex; justify-content: space-between; align-items: center; padding: 8px 0; border-bottom: 1px solid #f1f5f9; font-size: 12.5px;">
                      <div>
                        <div style="font-weight: 600; color: var(--fl-text);">${inst.note || 'Installment'}</div>
                        <div style="font-size: 11px; color: var(--fl-text-muted);">Due: ${inst.dueDate}</div>
                      </div>
                      <div style="display: flex; align-items: center; gap: 8px;">
                        <span style="font-weight: 700;">${inst.amountFormatted}</span>
                        <fl-badge variant="${inst.isPaid ? 'success' : (inst.payStatus === 'overdue' ? 'danger' : 'warning')}" text="${inst.statusLabel}"></fl-badge>
                      </div>
                    </div>
                  `).join('')}
                </div>
              </div>
            `).join('')}
          </div>

        </div>
      </fl-drawer>
    `;

    const drawer = this.querySelector('fl-drawer');
    if (drawer) {
      drawer.addEventListener('close', () => HomeStore.closeMemberDrawer());
    }

    const waBtn = this.querySelector('#fl-drawer-wa-btn');
    if (waBtn) {
      waBtn.addEventListener('click', () => {
        HomeStore.openWhatsAppModal(member);
      });
    }

    const renewBtn = this.querySelector('#fl-drawer-renew-btn');
    if (renewBtn) {
      renewBtn.addEventListener('click', () => {
        HomeStore.openWhatsAppModal(member, 'EXPIRY_REMINDER');
      });
    }
  }
}

customElements.define('fl-member-drawer', FlMemberDrawer);
