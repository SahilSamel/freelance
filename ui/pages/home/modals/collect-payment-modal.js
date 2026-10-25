/**
 * <fl-collect-payment-modal> Native Web Component
 * Collect Pending Installment / Invoice dues and record payment method, date, and reference.
 */

import { HomeStore } from '../home-store.js';
import { FormBinder } from '../../../core/error/form-binder.js';
import { ErrorMapper } from '../../../core/error/error-mapper.js';
import { AppConfig } from '../../../core/config/app-config.js';
import '../../../core/components/feedback/fl-modal.js';

export class FlCollectPaymentModal extends HTMLElement {
  connectedCallback() {
    this.unsubscribe = HomeStore.subscribe(() => this.render());
    this.render();
  }

  disconnectedCallback() {
    if (this.unsubscribe) this.unsubscribe();
  }

  render() {
    const { collectPaymentModal } = HomeStore.getState();
    const { isOpen, payment, member } = collectPaymentModal;

    if (!isOpen || !payment) {
      this.innerHTML = '';
      return;
    }

    const today = new Date();
    const pad = (n) => String(n).padStart(2, '0');
    const todayIso = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`;

    const memberName = member ? (member.name || member.username) : (payment.memberName || 'Member');
    const memberPhone = member ? member.phone : (payment.phone || '');
    const pendingDue = (payment.pendingAmount !== undefined && payment.pendingAmount !== null)
      ? payment.pendingAmount
      : payment.amount;

    this.innerHTML = `
      <fl-modal open title="Record Payment Collection" max-width="480px" z-index="1200">
        <form id="fl-collect-payment-form" style="display: flex; flex-direction: column; gap: 14px;" novalidate>
          
          <!-- Payment Due Overview Card -->
          <div style="background: #f8fafc; border: 1px solid var(--fl-border); border-radius: 10px; padding: 14px 16px;">
            <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px;">
              <div>
                <div style="font-weight: 700; font-size: 14px; color: var(--fl-text);">${memberName}</div>
                ${memberPhone ? `<div style="font-size: 12px; color: var(--fl-text-secondary); margin-top: 1px;">📞 ${memberPhone}</div>` : ''}
              </div>
              <div style="text-align: right;">
                <span style="font-size: 11px; text-transform: uppercase; font-weight: 600; color: var(--fl-text-muted);">Amount Due</span>
                <div style="font-size: 18px; font-weight: 800; color: var(--fl-danger, #dc2626); margin-top: 1px;">
                  ${AppConfig.formatCurrency(pendingDue)}
                </div>
              </div>
            </div>

            <div style="display: flex; gap: 12px; font-size: 12px; color: var(--fl-text-secondary); border-top: 1px solid #e2e8f0; padding-top: 8px; margin-top: 8px;">
              <span><strong>Due Date:</strong> ${payment.dueDate || 'Immediate'}</span>
              ${payment.coveredPlans && payment.coveredPlans.length > 0 ? `
                <span><strong>Services:</strong> ${payment.coveredPlans.join(', ')}</span>
              ` : ''}
            </div>
          </div>

          <!-- Paid Amount & Payment Method -->
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
            <div class="fl-form-group">
              <label style="display: block; font-size: 12.5px; font-weight: 600; color: var(--fl-text); margin-bottom: 4px;">
                Collected Amount (${AppConfig.getCurrencySymbol() || '₹'}) <span style="color: var(--fl-danger);">*</span>
              </label>
              <input 
                type="number" 
                name="paidAmount" 
                value="${pendingDue}" 
                min="1"
                step="any"
                class="fl-input-field" 
                style="width: 100%; height: 38px; padding: 0 10px; border: 1px solid var(--fl-border); border-radius: 8px; font-size: 13.5px;"
              />
            </div>

            <div class="fl-form-group">
              <label style="display: block; font-size: 12.5px; font-weight: 600; color: var(--fl-text); margin-bottom: 4px;">
                Payment Mode <span style="color: var(--fl-danger);">*</span>
              </label>
              <select 
                name="paymentMethod" 
                class="fl-input-field" 
                style="width: 100%; height: 38px; padding: 0 10px; border: 1px solid var(--fl-border); border-radius: 8px; font-size: 13.5px; background: #ffffff;"
              >
                <option value="UPI" selected>UPI (GPay / PhonePe / Paytm)</option>
                <option value="CASH">Cash</option>
                <option value="CARD">Debit / Credit Card</option>
                <option value="NETBANKING">Net Banking / NEFT</option>
              </select>
            </div>
          </div>

          <!-- Paid Date & Txn ID -->
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
            <div class="fl-form-group">
              <label style="display: block; font-size: 12.5px; font-weight: 600; color: var(--fl-text); margin-bottom: 4px;">
                Payment Date <span style="color: var(--fl-danger);">*</span>
              </label>
              <input 
                type="date" 
                name="paidDate" 
                value="${todayIso}" 
                class="fl-input-field" 
                style="width: 100%; height: 38px; padding: 0 10px; border: 1px solid var(--fl-border); border-radius: 8px; font-size: 13px;"
              />
            </div>

            <div class="fl-form-group">
              <label style="display: block; font-size: 12.5px; font-weight: 600; color: var(--fl-text); margin-bottom: 4px;">
                Transaction ID / Ref
              </label>
              <input 
                type="text" 
                name="transactionId" 
                placeholder="e.g. UTR / Receipt #" 
                class="fl-input-field" 
                style="width: 100%; height: 38px; padding: 0 10px; border: 1px solid var(--fl-border); border-radius: 8px; font-size: 13px;"
              />
            </div>
          </div>

          <!-- Notes -->
          <div class="fl-form-group">
            <label style="display: block; font-size: 12.5px; font-weight: 600; color: var(--fl-text); margin-bottom: 4px;">
              Notes / Remarks
            </label>
            <input 
              type="text" 
              name="notes" 
              placeholder="e.g. Settled remaining installment balance" 
              value="${payment.notes || ''}"
              class="fl-input-field" 
              style="width: 100%; height: 36px; padding: 0 10px; border: 1px solid var(--fl-border); border-radius: 8px; font-size: 13px;"
            />
          </div>

          <!-- Action Buttons -->
          <div style="display: flex; justify-content: flex-end; gap: 10px; margin-top: 8px; border-top: 1px solid var(--fl-border); padding-top: 14px;">
            <button 
              type="button" 
              id="fl-cancel-collect-btn" 
              class="fl-btn fl-btn-secondary"
              style="padding: 9px 18px; font-size: 13.5px;"
            >
              Cancel
            </button>
            <button 
              type="submit" 
              id="fl-submit-collect-btn" 
              class="fl-btn fl-btn-primary"
              style="padding: 9px 20px; font-size: 13.5px;"
            >
              Confirm & Mark as Paid
            </button>
          </div>
        </form>
      </fl-modal>
    `;

    this.bindEvents();
  }

  bindEvents() {
    const modal = this.querySelector('fl-modal');
    if (modal) {
      const handleClose = () => HomeStore.closeCollectPaymentModal();
      modal.addEventListener('close', handleClose);
      modal.addEventListener('fl-modal-close', handleClose);
    }

    const cancelBtn = this.querySelector('#fl-cancel-collect-btn');
    if (cancelBtn) {
      cancelBtn.addEventListener('click', () => HomeStore.closeCollectPaymentModal());
    }

    const form = this.querySelector('#fl-collect-payment-form');
    if (form) {
      form.addEventListener('submit', async (e) => {
        e.preventDefault();
        FormBinder.clearFormErrors(form);

        const { payment } = HomeStore.getState().collectPaymentModal;
        if (!payment) return;

        const formData = new FormData(form);
        const paidAmount = formData.get('paidAmount');
        const paymentMethod = formData.get('paymentMethod');
        const paidDate = formData.get('paidDate');
        const transactionId = (formData.get('transactionId') || '').trim();
        const notes = (formData.get('notes') || '').trim();

        const errors = [];
        if (!paidAmount || Number(paidAmount) <= 0) {
          errors.push({ field: 'paidAmount', message: 'Enter a valid amount > 0' });
        }
        if (!paidDate) {
          errors.push({ field: 'paidDate', message: 'Paid date is required' });
        }

        if (errors.length > 0) {
          FormBinder.applyFieldErrors(form, errors);
          return;
        }

        const submitBtn = form.querySelector('#fl-submit-collect-btn');
        submitBtn.disabled = true;
        submitBtn.textContent = 'Recording...';

        try {
          await HomeStore.collectPayment({
            paymentId: payment.id,
            paidAmount: Number(paidAmount),
            paidDate,
            paymentMethod,
            transactionId: transactionId || null,
            notes: notes || payment.notes || 'Collected Installment'
          });
        } catch (err) {
          ErrorMapper.handle(err, form);
        } finally {
          submitBtn.disabled = false;
          submitBtn.textContent = '✓ Mark as Paid';
        }
      });
    }
  }
}

customElements.define('fl-collect-payment-modal', FlCollectPaymentModal);
