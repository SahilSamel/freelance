/**
 * <fl-renew-member-modal> Native Web Component
 * Renew Membership or Add Plans for an existing member.
 * Smartly sets contiguous start dates for active plans or today for expired ones,
 * supports flexible duration editing, flexible payment schedules, and clean styling.
 */

import { HomeStore } from '../home-store.js';
import { FormBinder } from '../../../core/error/form-binder.js';
import { ErrorMapper } from '../../../core/error/error-mapper.js';
import { AppConfig } from '../../../core/config/app-config.js';
import '../../../core/components/feedback/fl-modal.js';

export class FlRenewMemberModal extends HTMLElement {
  constructor() {
    super();
    this.planRows = [];
    this.paymentRecords = [];
    this.activeMemberId = null;
  }

  connectedCallback() {
    this.unsubscribe = HomeStore.subscribe(() => {
      const { renewMemberModal } = HomeStore.getState();
      if (renewMemberModal.isOpen && (!this.activeMemberId || this.activeMemberId !== renewMemberModal.member?.id)) {
        this.activeMemberId = renewMemberModal.member?.id;
        this.initPlanRows();
      } else if (!renewMemberModal.isOpen) {
        this.activeMemberId = null;
        this.planRows = [];
        this.paymentRecords = [];
      }
      this.render();
    });
    this.render();
  }

  disconnectedCallback() {
    if (this.unsubscribe) this.unsubscribe();
  }

  getContiguousStartDate(planId, member) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const pad = (n) => String(n).padStart(2, '0');
    const toIso = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

    if (!member || !member.subscriptions || !planId) {
      return toIso(today);
    }

    const matchingSubs = member.subscriptions.filter(s => s.planId === planId);
    if (matchingSubs.length === 0) {
      return toIso(today);
    }

    matchingSubs.sort((a, b) => {
      const dateA = a.endDate ? new Date(a.endDate).getTime() : 0;
      const dateB = b.endDate ? new Date(b.endDate).getTime() : 0;
      return dateB - dateA;
    });

    const latest = matchingSubs[0];
    if (!latest.endDate) return toIso(today);

    const prevEnd = new Date(latest.endDate);
    prevEnd.setHours(0, 0, 0, 0);

    if (prevEnd >= today) {
      const nextDay = new Date(prevEnd);
      nextDay.setDate(nextDay.getDate() + 1);
      return toIso(nextDay);
    } else {
      return toIso(today);
    }
  }

  initPlanRows() {
    const { renewMemberModal, availablePlans } = HomeStore.getState();
    const { member, preselectedPlanId } = renewMemberModal;

    let targetPlanId = preselectedPlanId || '';
    const activePlans = (availablePlans || []).filter(p => p.isActive !== false);
    if (!targetPlanId) {
      if (member && member.subscriptions && member.subscriptions.length > 0) {
        const subPlan = member.subscriptions[0].planId;
        const isActiveSub = activePlans.some(p => p.id === subPlan);
        targetPlanId = isActiveSub ? subPlan : (activePlans[0]?.id || subPlan);
      } else if (activePlans.length > 0) {
        targetPlanId = activePlans[0].id;
      }
    }

    const initialPlan = (availablePlans || []).find(p => p.id === targetPlanId);
    const initialFee = initialPlan ? (initialPlan.price || 0) : 0;
    const initialDuration = initialPlan ? (initialPlan.durationDays || (initialPlan.durationMonths ? initialPlan.durationMonths * 30 : 30)) : 30;
    const startDate = this.getContiguousStartDate(targetPlanId, member);

    const pad = (n) => String(n).padStart(2, '0');
    const s = new Date(startDate);
    const e = new Date(s);
    e.setDate(e.getDate() + initialDuration);
    const endDate = `${e.getFullYear()}-${pad(e.getMonth() + 1)}-${pad(e.getDate())}`;

    this.planRows = [
      {
        id: `renew-row-${Date.now()}-0`,
        planId: targetPlanId,
        customName: 'Custom Plan',
        customDurationVal: 1,
        customDurationUnit: 'months',
        startDate,
        endDate,
        fee: initialFee,
        durationDays: initialDuration
      }
    ];

    this.resetPaymentRecords();
  }

  resetPaymentRecords() {
    const today = new Date();
    const pad = (n) => String(n).padStart(2, '0');
    const todayIso = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`;
    const totalFee = this.planRows.reduce((sum, r) => sum + (Number(r.fee) || 0), 0);

    this.paymentRecords = [
      {
        id: `renew-pay-${Date.now()}-0`,
        amount: totalFee,
        status: 'PAID',
        dueDate: todayIso,
        method: 'UPI',
        transactionId: '',
        notes: ''
      }
    ];
  }

  render() {
    const { renewMemberModal, availablePlans } = HomeStore.getState();
    const { isOpen, member } = renewMemberModal;

    if (!isOpen || !member) {
      this.innerHTML = '';
      return;
    }

    const totalFee = this.planRows.reduce((sum, r) => sum + (Number(r.fee) || 0), 0);
    const totalRecorded = this.paymentRecords.reduce((sum, p) => sum + (Number(p.amount) || 0), 0);
    const balanceDues = Math.max(0, totalFee - totalRecorded);

    const isProspect = (member.status || '').toLowerCase() === 'prospect' || (!member.subscriptions || member.subscriptions.length === 0);
    const isExpired = !isProspect && ((member.status || '').toLowerCase() === 'expired' || (member.statusLabel || '').toLowerCase() === 'expired');

    let badgeText = 'Active';
    let badgeBg = '#ecfdf5';
    let badgeColor = '#16a34a';
    let badgeBorder = '#a7f3d0';

    if (isProspect) {
      badgeText = 'Prospect';
      badgeBg = '#eff6ff';
      badgeColor = '#1d4ed8';
      badgeBorder = '#bfdbfe';
    } else if (isExpired) {
      badgeText = 'Expired';
      badgeBg = '#f1f5f9';
      badgeColor = '#475569';
      badgeBorder = '#cbd5e1';
    }

    const formattedPhone = member.phone
      ? (member.phone.startsWith('+') ? member.phone : `+${AppConfig.getCountryCode() || '91'} ${member.phone}`)
      : '';

    const today = new Date();
    const pad = (n) => String(n).padStart(2, '0');
    const todayIso = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`;

    const modalTitle = isProspect ? 'Enroll Member Plan' : 'Renew / Add Membership Plan';

    this.innerHTML = `
      <fl-modal open title="${modalTitle}" max-width="600px" z-index="1200">
        <form id="fl-renew-member-form" style="display: flex; flex-direction: column; gap: 16px;" novalidate>
          
          <!-- Target Member Profile Banner -->
          <div style="background: #f8fafc; border: 1px solid var(--fl-border); border-radius: 10px; padding: 14px 16px;">
            <div style="display: flex; align-items: center; gap: 10px;">
              <h3 style="margin: 0; font-size: 18px; font-weight: 800; color: #0f172a; line-height: 1.1;">
                ${member.name}
              </h3>
              <span style="
                background: ${badgeBg};
                color: ${badgeColor};
                border: 1px solid ${badgeBorder};
                border-radius: 9999px;
                padding: 2px 10px;
                font-size: 11.5px;
                font-weight: 600;
              ">
                ${badgeText}
              </span>
            </div>
            <div style="margin-top: 4px; font-size: 13px; color: #64748b; font-weight: 500;">
              ${formattedPhone}
            </div>
          </div>

          <!-- Plans to Renew / Add -->
          <div>
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
              <label style="font-size: 13.5px; font-weight: 700; color: var(--fl-text);">
                Plans <span style="color: var(--fl-danger);">*</span>
              </label>
              <button 
                type="button" 
                id="fl-add-renew-row-btn"
                class="fl-btn fl-btn-secondary"
                style="
                  padding: 6px 14px;
                  font-size: 12px;
                "
              >
                + Add Plan
              </button>
            </div>

            <div id="fl-renew-rows-container" style="display: flex; flex-direction: column; gap: 12px;">
              ${this.planRows.map((row, idx) => {
                const isCustom = row.planId === '__CUSTOM__';
                return `
                  <div class="fl-renew-row-card" data-row-id="${row.id}" style="
                    background: #f8fafc;
                    border: 1px solid var(--fl-border);
                    border-radius: 10px;
                    padding: 12px 14px;
                  ">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                      <span style="font-size: 12px; font-weight: 700; color: var(--fl-text-secondary); text-transform: uppercase;">
                        Plan #${idx + 1}
                      </span>
                      ${this.planRows.length > 1 ? `
                        <button type="button" class="fl-remove-renew-row-btn" data-row-id="${row.id}" style="
                          background: none;
                          border: none;
                          color: var(--fl-danger, #dc2626);
                          font-size: 12px;
                          font-weight: 600;
                          cursor: pointer;
                        ">
                          ✕ Remove
                        </button>
                      ` : ''}
                    </div>

                    <div style="display: grid; grid-template-columns: 2fr 1fr; gap: 10px;">
                      <div>
                        <label style="display: block; font-size: 11.5px; font-weight: 600; color: var(--fl-text); margin-bottom: 3px;">
                          Plan
                        </label>
                        <select 
                          class="fl-renew-plan-select" 
                          data-row-id="${row.id}"
                          style="width: 100%; height: 36px; padding: 0 8px; border: 1px solid var(--fl-border); border-radius: 6px; font-size: 13px; background: #ffffff;"
                        >
                          <option value="">Select a plan...</option>
                          ${(availablePlans || [])
                            .filter(p => p.isActive !== false || p.id === row.planId)
                            .map(p => `
                            <option value="${p.id}" ${row.planId === p.id ? 'selected' : ''}>
                              ${p.name}${p.isActive === false ? ' (Deactivated)' : ''} — ${AppConfig.formatCurrency(p.price || 0)} (${p.durationMonths || 1} mo)
                            </option>
                          `).join('')}
                          <option value="__CUSTOM__" ${isCustom ? 'selected' : ''}>⚡ Custom Plan...</option>
                        </select>
                      </div>

                      <div>
                        <label style="display: block; font-size: 11.5px; font-weight: 600; color: var(--fl-text); margin-bottom: 3px;">
                          Fee (${AppConfig.getCurrencySymbol() || '₹'})
                        </label>
                        <input 
                          type="number" 
                          class="fl-renew-fee-input" 
                          data-row-id="${row.id}"
                          value="${row.fee !== undefined ? row.fee : ''}"
                          placeholder="0"
                          min="0"
                          step="any"
                          style="width: 100%; height: 36px; padding: 0 8px; border: 1px solid var(--fl-border); border-radius: 6px; font-size: 13px; background: #ffffff;"
                        />
                      </div>
                    </div>

                    <!-- Custom Plan Inputs if selected -->
                    ${isCustom ? `
                      <div style="margin-top: 10px; padding: 10px; background: #ffffff; border: 1px dashed var(--fl-border); border-radius: 6px;">
                        <div style="display: grid; grid-template-columns: 2fr 1fr 1fr; gap: 8px;">
                          <div>
                            <label style="display: block; font-size: 11px; font-weight: 600; color: var(--fl-text); margin-bottom: 2px;">Custom Name</label>
                            <input 
                              type="text" 
                              class="fl-renew-custom-name" 
                              data-row-id="${row.id}" 
                              value="${row.customName || 'Custom Plan'}" 
                              style="width: 100%; height: 32px; padding: 0 8px; border: 1px solid var(--fl-border); border-radius: 4px; font-size: 12px;"
                            />
                          </div>
                          <div>
                            <label style="display: block; font-size: 11px; font-weight: 600; color: var(--fl-text); margin-bottom: 2px;">Duration</label>
                            <input 
                              type="number" 
                              class="fl-renew-custom-dur-val" 
                              data-row-id="${row.id}" 
                              value="${row.customDurationVal || 1}" 
                              min="1" 
                              style="width: 100%; height: 32px; padding: 0 8px; border: 1px solid var(--fl-border); border-radius: 4px; font-size: 12px;"
                            />
                          </div>
                          <div>
                            <label style="display: block; font-size: 11px; font-weight: 600; color: var(--fl-text); margin-bottom: 2px;">Unit</label>
                            <select 
                              class="fl-renew-custom-dur-unit" 
                              data-row-id="${row.id}" 
                              style="width: 100%; height: 32px; padding: 0 6px; border: 1px solid var(--fl-border); border-radius: 4px; font-size: 12px; background: #fff;"
                            >
                              <option value="months" ${row.customDurationUnit === 'months' ? 'selected' : ''}>Months</option>
                              <option value="days" ${row.customDurationUnit === 'days' ? 'selected' : ''}>Days</option>
                            </select>
                          </div>
                        </div>
                      </div>
                    ` : ''}

                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-top: 8px;">
                      <div>
                        <label style="display: block; font-size: 11.5px; font-weight: 600; color: var(--fl-text); margin-bottom: 3px;">
                          Start Date
                        </label>
                        <input 
                          type="date" 
                          class="fl-renew-start-input" 
                          data-row-id="${row.id}"
                          value="${row.startDate}"
                          style="width: 100%; height: 36px; padding: 0 8px; border: 1px solid var(--fl-border); border-radius: 6px; font-size: 12.5px; background: #ffffff;"
                        />
                      </div>
                      <div>
                        <label style="display: block; font-size: 11.5px; font-weight: 600; color: var(--fl-text); margin-bottom: 3px;">
                          Valid Until
                        </label>
                        <input 
                          type="date" 
                          class="fl-renew-end-input" 
                          data-row-id="${row.id}"
                          value="${row.endDate}"
                          style="width: 100%; height: 36px; padding: 0 8px; border: 1px solid var(--fl-border); border-radius: 6px; font-size: 12.5px; background: #ffffff;"
                        />
                      </div>
                    </div>
                  </div>
                `;
              }).join('')}
            </div>
          </div>

          <!-- Total Summary Bar -->
          <div style="background: #f8fafc; border: 1px solid var(--fl-border); border-radius: 8px; padding: 12px 16px; display: flex; justify-content: space-between; align-items: center;">
            <div>
              <div style="font-size: 12px; color: var(--fl-text-secondary); font-weight: 600;">
                ${isProspect ? 'Enrollment Total Fee' : 'Renewal Total Fee'}
              </div>
              <div style="font-size: 11px; color: var(--fl-text-muted);">
                ${this.planRows.length} ${this.planRows.length === 1 ? 'plan' : 'plans'} ${isProspect ? 'enrolling' : 'renewing'}
              </div>
            </div>
            <div style="font-size: 20px; font-weight: 800; color: #0f172a;" id="fl-renew-total-display">
              ${AppConfig.formatCurrency(totalFee)}
            </div>
          </div>

          <!-- Flexible Payment Schedule & Installments -->
          <div style="border-top: 1px solid var(--fl-border); padding-top: 14px;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
              <div>
                <label style="font-size: 13px; font-weight: 700; color: var(--fl-text);">
                  Payment Schedule & Installments
                </label>
                <div style="font-size: 11.5px; color: var(--fl-text-secondary); margin-top: 1px;">
                  Set flexible installments, record payments now, or schedule upcoming dues.
                </div>
              </div>
              <button 
                type="button" 
                id="fl-add-renew-pay-btn"
                style="
                  display: inline-flex;
                  align-items: center;
                  gap: 5px;
                  background: #f1f5f9;
                  color: #0f172a;
                  border: 1px solid var(--fl-border);
                  border-radius: 6px;
                  padding: 5px 10px;
                  font-size: 12px;
                  font-weight: 700;
                  cursor: pointer;
                "
              >
                + Add Installment
              </button>
            </div>

            <div id="fl-renew-pay-error" style="display: none; padding: 8px 12px; background: #fef2f2; border: 1px solid #fecaca; border-radius: 6px; color: #dc2626; font-size: 12px; font-weight: 600; margin-bottom: 8px;"></div>

            <div id="fl-renew-payments-container" style="display: flex; flex-direction: column; gap: 10px; max-height: 260px; overflow-y: auto; padding-right: 4px;">
              ${this.paymentRecords.map((pay, idx) => {
                const isPaid = pay.status === 'PAID';
                return `
                  <div class="fl-renew-pay-card" data-pay-id="${pay.id}" style="
                    background: ${isPaid ? '#f0fdf4' : '#fffbeb'};
                    border: 1px solid ${isPaid ? '#bbf7d0' : '#fef3c7'};
                    border-radius: 8px;
                    padding: 10px 12px;
                  ">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                      <span style="font-size: 11.5px; font-weight: 700; color: ${isPaid ? '#15803d' : '#b45309'}; text-transform: uppercase;">
                        Payment #${idx + 1} • ${isPaid ? 'Paid' : 'Unpaid (Due)'}
                      </span>
                      ${this.paymentRecords.length > 1 ? `
                        <button type="button" class="fl-remove-renew-pay-btn" data-pay-id="${pay.id}" style="background: none; border: none; color: #dc2626; font-size: 11.5px; font-weight: 600; cursor: pointer;">
                          ✕ Remove
                        </button>
                      ` : ''}
                    </div>

                    <div style="display: grid; grid-template-columns: 1.2fr 1.2fr 1.4fr; gap: 8px;">
                      <div>
                        <label style="display: block; font-size: 11px; font-weight: 600; color: var(--fl-text); margin-bottom: 2px;">
                          Amount (${AppConfig.getCurrencySymbol() || '₹'})
                        </label>
                        <input 
                          type="number" 
                          class="fl-renew-pay-amt-input" 
                          data-pay-id="${pay.id}" 
                          value="${pay.amount !== undefined ? pay.amount : ''}" 
                          min="0"
                          step="any"
                          placeholder="0"
                          style="width: 100%; height: 34px; padding: 0 8px; border: 1px solid var(--fl-border); border-radius: 6px; font-size: 12.5px; background: #ffffff;"
                        />
                      </div>

                      <div>
                        <label style="display: block; font-size: 11px; font-weight: 600; color: var(--fl-text); margin-bottom: 2px;">
                          Status
                        </label>
                        <select 
                          class="fl-renew-pay-status-select" 
                          data-pay-id="${pay.id}"
                          style="width: 100%; height: 34px; padding: 0 6px; border: 1px solid var(--fl-border); border-radius: 6px; font-size: 12.5px; background: #ffffff; font-weight: 600;"
                        >
                          <option value="PAID" ${isPaid ? 'selected' : ''}>✅ Paid</option>
                          <option value="UNPAID" ${!isPaid ? 'selected' : ''}>⏳ Unpaid (Due)</option>
                        </select>
                      </div>

                      <div>
                        <label style="display: block; font-size: 11px; font-weight: 600; color: var(--fl-text); margin-bottom: 2px;">
                          ${isPaid ? 'Paid Date' : 'Due Date'}
                        </label>
                        <input 
                          type="date" 
                          class="fl-renew-pay-date-input" 
                          data-pay-id="${pay.id}" 
                          value="${pay.dueDate || todayIso}"
                          style="width: 100%; height: 34px; padding: 0 8px; border: 1px solid var(--fl-border); border-radius: 6px; font-size: 12.5px; background: #ffffff;"
                        />
                      </div>
                    </div>

                    <div style="display: grid; grid-template-columns: ${isPaid ? '1fr 1.2fr 1.2fr' : '1fr'}; gap: 8px; margin-top: 6px;">
                      ${isPaid ? `
                      <div>
                        <select 
                          class="fl-renew-pay-method-select" 
                          data-pay-id="${pay.id}"
                          style="width: 100%; height: 30px; padding: 0 6px; border: 1px solid var(--fl-border); border-radius: 6px; font-size: 11.5px; background: #ffffff;"
                        >
                          <option value="UPI" ${pay.method === 'UPI' ? 'selected' : ''}>UPI / GPay</option>
                          <option value="CASH" ${pay.method === 'CASH' ? 'selected' : ''}>Cash</option>
                          <option value="CARD" ${pay.method === 'CARD' ? 'selected' : ''}>Card</option>
                          <option value="BANK_TRANSFER" ${pay.method === 'BANK_TRANSFER' ? 'selected' : ''}>Bank Transfer</option>
                        </select>
                      </div>

                      <div>
                        <input 
                          type="text" 
                          class="fl-renew-pay-txnid-input" 
                          data-pay-id="${pay.id}" 
                          value="${pay.transactionId || ''}"
                          placeholder="Txn ID / Ref (Optional)"
                          style="width: 100%; height: 30px; padding: 0 8px; border: 1px solid var(--fl-border); border-radius: 6px; font-size: 11.5px; background: #ffffff;"
                        />
                      </div>
                      ` : ''}

                      <div>
                        <input 
                          type="text" 
                          class="fl-renew-pay-notes-input" 
                          data-pay-id="${pay.id}" 
                          value="${pay.notes || ''}"
                          placeholder="${isPaid ? 'Note (Optional)' : 'Installment note (e.g. 2nd Installment due)'}"
                          style="width: 100%; height: 30px; padding: 0 8px; border: 1px solid var(--fl-border); border-radius: 6px; font-size: 11.5px; background: #ffffff;"
                        />
                      </div>
                    </div>
                  </div>
                `;
              }).join('')}
            </div>

            <!-- Payment Totals Bar -->
            <div style="margin-top: 10px; padding: 10px 14px; background: #f8fafc; border: 1px solid var(--fl-border); border-radius: 8px; display: flex; justify-content: space-between; align-items: center; font-size: 12.5px;">
              <div>
                <span style="color: var(--fl-text-secondary);">Total Recorded:</span> 
                <strong style="color: var(--fl-text);" id="fl-renew-recorded-display">${AppConfig.formatCurrency(totalRecorded)}</strong>
              </div>
              <div>
                <span style="color: var(--fl-text-secondary);">Balance:</span> 
                <strong style="color: ${balanceDues > 0 ? '#b45309' : '#15803d'};" id="fl-renew-balance-display">
                  ${balanceDues > 0 ? `${AppConfig.formatCurrency(balanceDues)} Pending` : 'Fully Settled ✓'}
                </strong>
              </div>
            </div>
          </div>

          <!-- Action Buttons -->
          <div style="display: flex; justify-content: flex-end; gap: 10px; margin-top: 8px; border-top: 1px solid var(--fl-border); padding-top: 14px;">
            <button 
              type="button" 
              id="fl-cancel-renew-btn" 
              class="fl-btn fl-btn-secondary"
              style="padding: 9px 18px; font-size: 13.5px;"
            >
              Cancel
            </button>
            <button 
              type="submit" 
              id="fl-submit-renew-btn" 
              class="fl-btn fl-btn-primary"
              style="padding: 9px 20px; font-size: 13.5px;"
            >
              ${isProspect ? 'Confirm Enrollment' : 'Confirm Renewal'}
            </button>
          </div>
        </form>
      </fl-modal>
    `;

    this.bindEvents();
  }

  bindEvents() {
    const pad = (n) => String(n).padStart(2, '0');
    const toIso = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

    const modal = this.querySelector('fl-modal');
    if (modal) {
      const handleClose = () => {
        this.planRows = [];
        this.paymentRecords = [];
        this.activeMemberId = null;
        HomeStore.closeRenewMemberModal();
      };
      modal.addEventListener('close', handleClose);
      modal.addEventListener('fl-modal-close', handleClose);
    }

    const cancelBtn = this.querySelector('#fl-cancel-renew-btn');
    if (cancelBtn) {
      cancelBtn.addEventListener('click', () => {
        this.planRows = [];
        this.paymentRecords = [];
        this.activeMemberId = null;
        HomeStore.closeRenewMemberModal();
      });
    }

    // Add Plan Row
    const addRowBtn = this.querySelector('#fl-add-renew-row-btn');
    if (addRowBtn) {
      addRowBtn.addEventListener('click', () => {
        const { renewMemberModal, availablePlans } = HomeStore.getState();
        const activePlans = (availablePlans || []).filter(p => p.isActive !== false);
        const defaultPlan = activePlans[0] || (availablePlans || [])[0];
        const defaultFee = defaultPlan ? (defaultPlan.price || 0) : 0;
        const defaultDur = defaultPlan ? (defaultPlan.durationDays || (defaultPlan.durationMonths ? defaultPlan.durationMonths * 30 : 30)) : 30;
        const startDate = this.getContiguousStartDate(defaultPlan?.id, renewMemberModal.member);

        const s = new Date(startDate);
        const e = new Date(s);
        e.setDate(e.getDate() + defaultDur);
        const endDate = toIso(e);

        this.planRows.push({
          id: `renew-row-${Date.now()}-${this.planRows.length}`,
          planId: defaultPlan?.id || '',
          customName: 'Custom Plan',
          customDurationVal: 1,
          customDurationUnit: 'months',
          startDate,
          endDate,
          fee: defaultFee,
          durationDays: defaultDur
        });

        // If only 1 payment record, sync its amount to new total
        const totalFee = this.planRows.reduce((sum, r) => sum + (Number(r.fee) || 0), 0);
        if (this.paymentRecords.length === 1) {
          this.paymentRecords[0].amount = totalFee;
        }

        this.render();
      });
    }

    // Remove Plan Row
    this.querySelectorAll('.fl-remove-renew-row-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const rowId = btn.getAttribute('data-row-id');
        this.planRows = this.planRows.filter(r => r.id !== rowId);
        if (this.planRows.length === 0) this.initPlanRows();

        const totalFee = this.planRows.reduce((sum, r) => sum + (Number(r.fee) || 0), 0);
        if (this.paymentRecords.length === 1) {
          this.paymentRecords[0].amount = totalFee;
        }

        this.render();
      });
    });

    // Plan Select change
    const { availablePlans, renewMemberModal } = HomeStore.getState();
    this.querySelectorAll('.fl-renew-plan-select').forEach(sel => {
      sel.addEventListener('change', () => {
        const rowId = sel.getAttribute('data-row-id');
        const row = this.planRows.find(r => r.id === rowId);
        if (!row) return;

        row.planId = sel.value;
        if (sel.value === '__CUSTOM__') {
          row.durationDays = row.customDurationUnit === 'months' ? row.customDurationVal * 30 : row.customDurationVal;
        } else {
          const found = (availablePlans || []).find(p => p.id === sel.value);
          if (found) {
            row.fee = found.price || 0;
            row.durationDays = found.durationDays || (found.durationMonths ? found.durationMonths * 30 : 30);
            row.startDate = this.getContiguousStartDate(found.id, renewMemberModal.member);
          }
        }

        const s = new Date(row.startDate || new Date());
        const e = new Date(s);
        e.setDate(e.getDate() + (row.durationDays || 30));
        row.endDate = toIso(e);

        const totalFee = this.planRows.reduce((sum, r) => sum + (Number(r.fee) || 0), 0);
        if (this.paymentRecords.length === 1) {
          this.paymentRecords[0].amount = totalFee;
        }

        this.render();
      });
    });

    // Fee change
    this.querySelectorAll('.fl-renew-fee-input').forEach(input => {
      input.addEventListener('input', () => {
        const rowId = input.getAttribute('data-row-id');
        const row = this.planRows.find(r => r.id === rowId);
        if (row) {
          row.fee = Number(input.value) || 0;
          const totalFee = this.planRows.reduce((sum, r) => sum + (Number(r.fee) || 0), 0);
          const totalDisplay = this.querySelector('#fl-renew-total-display');
          if (totalDisplay) totalDisplay.textContent = AppConfig.formatCurrency(totalFee);

          if (this.paymentRecords.length === 1) {
            this.paymentRecords[0].amount = totalFee;
            const recDisplay = this.querySelector('#fl-renew-recorded-display');
            if (recDisplay) recDisplay.textContent = AppConfig.formatCurrency(totalFee);
            const payInput = this.querySelector('.fl-renew-pay-amt-input');
            if (payInput) payInput.value = totalFee;
            const balDisplay = this.querySelector('#fl-renew-balance-display');
            if (balDisplay) {
              balDisplay.textContent = 'Fully Settled ✓';
              balDisplay.style.color = '#15803d';
            }
          } else {
            const recorded = this.paymentRecords.reduce((sum, p) => sum + (Number(p.amount) || 0), 0);
            const balance = Math.max(0, totalFee - recorded);
            const balDisplay = this.querySelector('#fl-renew-balance-display');
            if (balDisplay) {
              balDisplay.textContent = balance > 0 ? `${AppConfig.formatCurrency(balance)} Pending` : 'Fully Settled ✓';
              balDisplay.style.color = balance > 0 ? '#b45309' : '#15803d';
            }
          }
        }
      });
    });

    // Start date change
    this.querySelectorAll('.fl-renew-start-input').forEach(input => {
      input.addEventListener('input', () => {
        const rowId = input.getAttribute('data-row-id');
        const row = this.planRows.find(r => r.id === rowId);
        if (row) {
          row.startDate = input.value;
          const s = new Date(row.startDate);
          const e = new Date(s);
          e.setDate(e.getDate() + (row.durationDays || 30));
          row.endDate = toIso(e);
          this.render();
        }
      });
    });

    // Valid until (custom end date) change -> recalculates durationDays!
    this.querySelectorAll('.fl-renew-end-input').forEach(input => {
      input.addEventListener('input', () => {
        const rowId = input.getAttribute('data-row-id');
        const row = this.planRows.find(r => r.id === rowId);
        if (row && input.value) {
          row.endDate = input.value;
          const s = new Date(row.startDate);
          const e = new Date(row.endDate);
          const diffDays = Math.round((e.getTime() - s.getTime()) / (1000 * 60 * 60 * 24));
          if (diffDays > 0) {
            row.durationDays = diffDays;
          }
        }
      });
    });

    // Custom duration inputs
    this.querySelectorAll('.fl-renew-custom-name').forEach(input => {
      input.addEventListener('input', () => {
        const rowId = input.getAttribute('data-row-id');
        const row = this.planRows.find(r => r.id === rowId);
        if (row) row.customName = input.value;
      });
    });

    this.querySelectorAll('.fl-renew-custom-dur-val').forEach(input => {
      input.addEventListener('input', () => {
        const rowId = input.getAttribute('data-row-id');
        const row = this.planRows.find(r => r.id === rowId);
        if (row) {
          row.customDurationVal = Number(input.value) || 1;
          row.durationDays = row.customDurationUnit === 'months' ? row.customDurationVal * 30 : row.customDurationVal;
          const s = new Date(row.startDate);
          const e = new Date(s);
          e.setDate(e.getDate() + row.durationDays);
          row.endDate = toIso(e);
          this.render();
        }
      });
    });

    this.querySelectorAll('.fl-renew-custom-dur-unit').forEach(sel => {
      sel.addEventListener('change', () => {
        const rowId = sel.getAttribute('data-row-id');
        const row = this.planRows.find(r => r.id === rowId);
        if (row) {
          row.customDurationUnit = sel.value;
          row.durationDays = row.customDurationUnit === 'months' ? row.customDurationVal * 30 : row.customDurationVal;
          const s = new Date(row.startDate);
          const e = new Date(s);
          e.setDate(e.getDate() + row.durationDays);
          row.endDate = toIso(e);
          this.render();
        }
      });
    });

    // Add Installment / Payment Record
    const addPayBtn = this.querySelector('#fl-add-renew-pay-btn');
    if (addPayBtn) {
      addPayBtn.addEventListener('click', () => {
        const totalFee = this.planRows.reduce((sum, r) => sum + (Number(r.fee) || 0), 0);
        const recorded = this.paymentRecords.reduce((sum, p) => sum + (Number(p.amount) || 0), 0);
        const remaining = Math.max(0, totalFee - recorded);

        this.paymentRecords.push({
          id: `renew-pay-${Date.now()}-${this.paymentRecords.length}`,
          amount: remaining,
          status: 'UNPAID',
          dueDate: toIso(new Date()),
          method: 'UPI',
          transactionId: '',
          notes: `Installment #${this.paymentRecords.length + 1}`
        });
        this.render();
      });
    }

    // Remove Payment Record
    this.querySelectorAll('.fl-remove-renew-pay-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const payId = btn.getAttribute('data-pay-id');
        this.paymentRecords = this.paymentRecords.filter(p => p.id !== payId);
        if (this.paymentRecords.length === 0) this.resetPaymentRecords();
        this.render();
      });
    });

    // Payment Record Amount Input
    this.querySelectorAll('.fl-renew-pay-amt-input').forEach(input => {
      input.addEventListener('input', () => {
        const payId = input.getAttribute('data-pay-id');
        const pay = this.paymentRecords.find(p => p.id === payId);
        if (pay) {
          pay.amount = Number(input.value) || 0;
          const totalFee = this.planRows.reduce((sum, r) => sum + (Number(r.fee) || 0), 0);
          const totalRecorded = this.paymentRecords.reduce((sum, p) => sum + (Number(p.amount) || 0), 0);
          const balance = Math.max(0, totalFee - totalRecorded);

          const recDisplay = this.querySelector('#fl-renew-recorded-display');
          if (recDisplay) recDisplay.textContent = AppConfig.formatCurrency(totalRecorded);

          const balDisplay = this.querySelector('#fl-renew-balance-display');
          if (balDisplay) {
            balDisplay.textContent = balance > 0 ? `${AppConfig.formatCurrency(balance)} Pending` : 'Fully Settled ✓';
            balDisplay.style.color = balance > 0 ? '#b45309' : '#15803d';
          }
        }
      });
    });

    // Payment Record Status Select
    this.querySelectorAll('.fl-renew-pay-status-select').forEach(sel => {
      sel.addEventListener('change', () => {
        const payId = sel.getAttribute('data-pay-id');
        const pay = this.paymentRecords.find(p => p.id === payId);
        if (pay) {
          pay.status = sel.value;
          this.render();
        }
      });
    });

    // Payment Record Date Input
    this.querySelectorAll('.fl-renew-pay-date-input').forEach(input => {
      input.addEventListener('input', () => {
        const payId = input.getAttribute('data-pay-id');
        const pay = this.paymentRecords.find(p => p.id === payId);
        if (pay) pay.dueDate = input.value;
      });
    });

    // Payment Record Method Select
    this.querySelectorAll('.fl-renew-pay-method-select').forEach(sel => {
      sel.addEventListener('change', () => {
        const payId = sel.getAttribute('data-pay-id');
        const pay = this.paymentRecords.find(p => p.id === payId);
        if (pay) pay.method = sel.value;
      });
    });

    // Payment Record Txn ID Input
    this.querySelectorAll('.fl-renew-pay-txnid-input').forEach(input => {
      input.addEventListener('input', () => {
        const payId = input.getAttribute('data-pay-id');
        const pay = this.paymentRecords.find(p => p.id === payId);
        if (pay) pay.transactionId = input.value;
      });
    });

    // Payment Record Notes Input
    this.querySelectorAll('.fl-renew-pay-notes-input').forEach(input => {
      input.addEventListener('input', () => {
        const payId = input.getAttribute('data-pay-id');
        const pay = this.paymentRecords.find(p => p.id === payId);
        if (pay) pay.notes = input.value;
      });
    });

    // Submit form
    const form = this.querySelector('#fl-renew-member-form');
    if (form) {
      form.addEventListener('submit', async (e) => {
        e.preventDefault();
        FormBinder.clearFormErrors(form);

        const { member } = HomeStore.getState().renewMemberModal;
        if (!member) return;

        const errBox = this.querySelector('#fl-renew-pay-error');
        if (errBox) errBox.style.display = 'none';

        // Validate plan rows
        for (let i = 0; i < this.planRows.length; i++) {
          const r = this.planRows[i];
          if (!r.planId) {
            if (errBox) {
              errBox.textContent = `Please select a plan for row #${i + 1}`;
              errBox.style.display = 'block';
            }
            return;
          }
          if (r.planId === '__CUSTOM__' && !r.customName.trim()) {
            if (errBox) {
              errBox.textContent = `Custom plan name required for row #${i + 1}`;
              errBox.style.display = 'block';
            }
            return;
          }
        }

        // Validate payment records
        if (this.paymentRecords.length === 0) {
          if (errBox) {
            errBox.textContent = 'At least one payment record is required.';
            errBox.style.display = 'block';
          }
          return;
        }

        for (let i = 0; i < this.paymentRecords.length; i++) {
          const p = this.paymentRecords[i];
          if (p.amount === undefined || p.amount === null || isNaN(p.amount) || p.amount < 0) {
            if (errBox) {
              errBox.textContent = `Please enter a valid amount for Payment #${i + 1}`;
              errBox.style.display = 'block';
            }
            return;
          }
          if (!p.dueDate) {
            if (errBox) {
              errBox.textContent = `Please specify a due/paid date for Payment #${i + 1}`;
              errBox.style.display = 'block';
            }
            return;
          }
        }

        const statusNormalized = (member.status || '').toLowerCase();
        const isProspect = statusNormalized === 'prospect' || (!member.subscriptions || member.subscriptions.length === 0);

        const submitBtn = form.querySelector('#fl-submit-renew-btn');
        submitBtn.disabled = true;
        submitBtn.textContent = isProspect ? 'Enrolling...' : 'Renewing...';

        try {
          const paymentsToSubmit = this.paymentRecords.map((p, idx) => {
            const isPaid = p.status === 'PAID';
            const amt = Number(p.amount) || 0;
            const txnId = (p.transactionId && p.transactionId.trim()) ? p.transactionId.trim() : null;
            return {
              amount: amt,
              paidAmount: isPaid ? amt : 0,
              dueDate: p.dueDate,
              paidDate: isPaid ? p.dueDate : null,
              paymentMethod: isPaid ? (p.method || 'UPI') : 'NONE',
              transactionId: isPaid ? txnId : null,
              notes: p.notes || (isPaid ? `Payment #${idx + 1} (Paid)` : `Installment #${idx + 1} (Due)`)
            };
          });

          await HomeStore.renewMember({
            userId: member.id || member.userId,
            plans: this.planRows.map(r => ({
              planId: r.planId,
              startDate: r.startDate,
              endDate: r.endDate,
              durationDays: r.durationDays,
              fee: Number(r.fee) || 0,
              customPlanData: r.planId === '__CUSTOM__' ? {
                name: r.customName.trim(),
                price: Number(r.fee) || 0,
                value: r.customDurationVal || 1,
                unit: r.customDurationUnit || 'months'
              } : null
            })),
            payments: paymentsToSubmit
          });
          this.planRows = [];
          this.paymentRecords = [];
        } catch (err) {
          ErrorMapper.handle(err, form);
        } finally {
          submitBtn.disabled = false;
          submitBtn.textContent = isProspect ? 'Confirm Enrollment' : 'Confirm Renewal';
        }
      });
    }
  }
}

customElements.define('fl-renew-member-modal', FlRenewMemberModal);
