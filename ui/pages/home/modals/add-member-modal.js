/**
 * <fl-add-member-modal> Native Web Component
 * Multi-Step Onboarding Modal:
 * Step 1: Contact Details (First Name, Last Name, Phone Number)
 *         - Can save directly as Prospect from here without taking a plan!
 * Step 2: Plans & Services (Multi-plan builder with customizable duration & prices)
 * Step 3: Payment (Itemized overview of all plans + customizable payment records)
 *
 * All contact info is shared continuously between prospect and member flows.
 * Completed steps show a green numbered check badge.
 */

import { HomeStore } from '../home-store.js';
import { Icons } from '../../../assets/icons/icons.js';
import { FormBinder } from '../../../core/error/form-binder.js';
import { ErrorMapper } from '../../../core/error/error-mapper.js';
import { AppConfig } from '../../../core/config/app-config.js';
import '../../../core/components/feedback/fl-modal.js';

export class FlAddMemberModal extends HTMLElement {
  constructor() {
    super();
    this.currentStep = 1;
    this.planRows = [];
    this.paymentRecords = [];
    this.isProspect = false;
    this.createdUserId = null;
    this.lastSavedContact = null;
    this.contactData = {
      firstName: '',
      lastName: '',
      phone: ''
    };
  }

  connectedCallback() {
    this.unsubscribe = HomeStore.subscribe(() => {
      const { addMemberModal } = HomeStore.getState();
      if (addMemberModal.isOpen && !this.wasOpen) {
        this.wasOpen = true;
        this.resetState();
      } else if (!addMemberModal.isOpen) {
        this.wasOpen = false;
      }
      this.render();
    });
    this.resetState();
    this.render();
  }

  disconnectedCallback() {
    if (this.unsubscribe) this.unsubscribe();
  }

  resetState() {
    this.currentStep = 1;
    this.isProspect = false;
    this.createdUserId = null;
    this.lastSavedContact = null;
    this.contactData = {
      firstName: '',
      lastName: '',
      phone: ''
    };
    this.resetPlanRows();
    this.resetPaymentRecords();
  }

  resetPlanRows() {
    const today = new Date();
    const pad = (n) => String(n).padStart(2, '0');
    const todayIso = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`;
    const nextMonth = new Date(today);
    nextMonth.setDate(nextMonth.getDate() + 30);
    const nextMonthIso = `${nextMonth.getFullYear()}-${pad(nextMonth.getMonth() + 1)}-${pad(nextMonth.getDate())}`;

    this.planRows = [
      {
        id: `row-${Date.now()}-0`,
        planId: '',
        customName: 'Custom Plan',
        customDurationVal: 1,
        customDurationUnit: 'months',
        startDate: todayIso,
        endDate: nextMonthIso,
        fee: 0,
        durationDays: 30
      }
    ];
  }

  resetPaymentRecords() {
    const today = new Date();
    const pad = (n) => String(n).padStart(2, '0');
    const todayIso = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`;

    const totalFee = this.planRows.reduce((sum, r) => sum + (Number(r.fee) || 0), 0);

    this.paymentRecords = [
      {
        id: `pay-${Date.now()}-0`,
        amount: totalFee,
        status: 'PAID', // 'PAID' or 'UNPAID'
        dueDate: todayIso,
        method: 'UPI',
        notes: ''
      }
    ];
  }

  saveInputValuesFromDom() {
    const fName = this.querySelector('input[name="firstName"]');
    if (fName) this.contactData.firstName = fName.value.trim();

    const lName = this.querySelector('input[name="lastName"]');
    if (lName) this.contactData.lastName = lName.value.trim();

    const ph = this.querySelector('input[name="phone"]');
    if (ph) this.contactData.phone = ph.value.trim();
  }

  validateContactStep() {
    this.saveInputValuesFromDom();
    const errors = [];
    const form = this.querySelector('#fl-add-member-form');
    if (form) FormBinder.clearFormErrors(form);

    if (!this.contactData.firstName) {
      errors.push({ field: 'firstName', message: 'First name is required' });
    }
    if (!this.contactData.lastName) {
      errors.push({ field: 'lastName', message: 'Last name is required' });
    }
    if (!this.contactData.phone) {
      errors.push({ field: 'phone', message: 'Phone number is required' });
    } else if (!/^\d{10}$/.test(this.contactData.phone.replace(/\s+/g, ''))) {
      errors.push({ field: 'phone', message: 'Enter a valid 10-digit mobile number' });
    }

    if (errors.length > 0 && form) {
      FormBinder.applyFieldErrors(form, errors);
      return false;
    }
    return true;
  }

  validatePlansStep() {
    const errors = [];
    const form = this.querySelector('#fl-add-member-form');
    if (form) FormBinder.clearFormErrors(form);

    if (this.planRows.length === 0) {
      errors.push({ field: 'planSelection', message: 'At least one membership plan is required' });
    }

    for (let i = 0; i < this.planRows.length; i++) {
      const r = this.planRows[i];
      if (!r.planId) {
        errors.push({ field: 'planSelection', message: `Please select a plan for row #${i + 1}` });
        break;
      }
      if (r.planId === '__CUSTOM__' && !r.customName.trim()) {
        errors.push({ field: 'planSelection', message: `Custom plan name required for row #${i + 1}` });
        break;
      }
    }

    if (errors.length > 0) {
      const errBox = this.querySelector('#fl-plan-step-error');
      if (errBox) {
        errBox.textContent = errors[0].message;
        errBox.style.display = 'block';
      }
      return false;
    }
    return true;
  }

  validatePaymentStep() {
    const errBox = this.querySelector('#fl-payment-step-error');
    if (errBox) errBox.style.display = 'none';

    if (this.paymentRecords.length === 0) {
      if (errBox) {
        errBox.textContent = 'At least one payment record is required.';
        errBox.style.display = 'block';
      }
      return false;
    }

    for (let i = 0; i < this.paymentRecords.length; i++) {
      const p = this.paymentRecords[i];
      if (p.amount === undefined || p.amount === null || isNaN(p.amount) || p.amount < 0) {
        if (errBox) {
          errBox.textContent = `Please enter a valid amount for Payment #${i + 1}`;
          errBox.style.display = 'block';
        }
        return false;
      }
      if (!p.dueDate) {
        if (errBox) {
          errBox.textContent = `Please specify a due/paid date for Payment #${i + 1}`;
          errBox.style.display = 'block';
        }
        return false;
      }
    }

    return true;
  }

  getHumanDuration(row) {
    if (row.durationDays) {
      if (row.durationDays % 365 === 0) {
        const yrs = row.durationDays / 365;
        return `${yrs} Year${yrs > 1 ? 's' : ''}`;
      }
      if (row.durationDays % 30 === 0) {
        const mos = row.durationDays / 30;
        return `${mos} Month${mos > 1 ? 's' : ''}`;
      }
      return `${row.durationDays} Day${row.durationDays > 1 ? 's' : ''}`;
    }
    return '1 Month';
  }

  renderStepperHeader() {
    const steps = [
      { num: 1, label: 'Contact Details' },
      { num: 2, label: 'Plans & Services' },
      { num: 3, label: 'Payment' }
    ];

    return `
      <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 22px; padding: 0 4px;">
        ${steps.map((st, idx) => {
          const isDone = this.currentStep > st.num;
          const isCurrent = this.currentStep === st.num;
          
          let circleBg = '#e2e8f0';
          let circleColor = '#64748b';
          let circleContent = `${st.num}`;

          if (isDone) {
            circleBg = '#10b981';
            circleColor = '#ffffff';
            circleContent = `✓`;
          } else if (isCurrent) {
            circleBg = '#0f172a';
            circleColor = '#ffffff';
          }

          return `
            <div style="display: flex; align-items: center; gap: 8px; flex: ${idx < steps.length - 1 ? '1' : 'none'};">
              <div style="display: flex; align-items: center; gap: 8px; cursor: ${isDone ? 'pointer' : 'default'};" 
                   class="${isDone ? 'fl-stepper-jump' : ''}" 
                   data-jump-step="${st.num}">
                <div style="
                  width: 28px; 
                  height: 28px; 
                  border-radius: 50%; 
                  background: ${circleBg}; 
                  color: ${circleColor}; 
                  font-weight: 700; 
                  font-size: 13px; 
                  display: flex; 
                  align-items: center; 
                  justify-content: center;
                  box-shadow: ${isCurrent ? '0 0 0 3px rgba(15, 23, 42, 0.15)' : 'none'};
                  transition: all 0.2s ease;
                ">
                  ${circleContent}
                </div>
                <div style="font-size: 12.5px; font-weight: ${isCurrent ? '700' : '600'}; color: ${isCurrent ? '#0f172a' : (isDone ? '#10b981' : '#64748b')}; white-space: nowrap;">
                  ${st.label}
                </div>
              </div>
              ${idx < steps.length - 1 ? `
                <div style="flex: 1; height: 2px; margin: 0 10px; background: ${this.currentStep > st.num ? '#10b981' : '#e2e8f0'}; transition: background 0.2s ease;"></div>
              ` : ''}
            </div>
          `;
        }).join('')}
      </div>
    `;
  }

  render() {
    const { addMemberModal, availablePlans } = HomeStore.getState();
    const { isOpen } = addMemberModal;

    if (!isOpen) {
      this.innerHTML = '';
      return;
    }

    const today = new Date();
    const pad = (n) => String(n).padStart(2, '0');
    const todayIso = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`;

    // Preserve dom values before re-rendering
    this.saveInputValuesFromDom();

    const totalFee = this.planRows.reduce((sum, r) => sum + (Number(r.fee) || 0), 0);
    const totalRecorded = this.paymentRecords.reduce((sum, p) => sum + (Number(p.amount) || 0), 0);
    const balanceDues = Math.max(0, totalFee - totalRecorded);

    let modalTitle = 'Register Member / Prospect';
    if (this.currentStep === 1) modalTitle = 'Step 1: Contact Details';
    if (this.currentStep === 2) modalTitle = 'Step 2: Assign Plans & Services';
    if (this.currentStep === 3) modalTitle = 'Step 3: Payment Settlement';

    this.innerHTML = `
      <fl-modal open title="${modalTitle}" max-width="640px">
        <form id="fl-add-member-form" style="display: flex; flex-direction: column;" novalidate>
          
          <!-- Stepper Header -->
          ${this.renderStepperHeader()}

          <!-- ============================================== -->
          <!-- STEP 1: CONTACT DETAILS                        -->
          <!-- ============================================== -->
          <div id="fl-step-1-container" style="display: ${this.currentStep === 1 ? 'flex' : 'none'}; flex-direction: column; gap: 16px;">
            <div style="background: #f8fafc; border: 1px solid var(--fl-border); border-radius: 8px; padding: 12px 14px; font-size: 12.5px; color: var(--fl-text-secondary); line-height: 1.45;">
              Enter the client's information. You can either continue to select membership plans or directly save them as a <strong>Prospect</strong> if they haven't decided yet.
            </div>

            <!-- Name Row -->
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
              <div class="fl-form-group">
                <label style="display: block; font-size: 13px; font-weight: 600; color: var(--fl-text); margin-bottom: 4px;">
                  First Name <span style="color: var(--fl-danger);">*</span>
                </label>
                <input 
                  type="text" 
                  name="firstName" 
                  value="${this.contactData.firstName || ''}"
                  placeholder="e.g. Rahul" 
                  class="fl-input-field" 
                  style="width: 100%; height: 38px; padding: 0 12px; border: 1px solid var(--fl-border); border-radius: 8px; font-size: 13.5px;"
                />
              </div>

              <div class="fl-form-group">
                <label style="display: block; font-size: 13px; font-weight: 600; color: var(--fl-text); margin-bottom: 4px;">
                  Last Name <span style="color: var(--fl-danger);">*</span>
                </label>
                <input 
                  type="text" 
                  name="lastName" 
                  value="${this.contactData.lastName || ''}"
                  placeholder="e.g. Sharma" 
                  class="fl-input-field" 
                  style="width: 100%; height: 38px; padding: 0 12px; border: 1px solid var(--fl-border); border-radius: 8px; font-size: 13.5px;"
                />
              </div>
            </div>

            <!-- Contact Row -->
            <div class="fl-form-group">
              <label style="display: block; font-size: 13px; font-weight: 600; color: var(--fl-text); margin-bottom: 4px;">
                Phone Number <span style="color: var(--fl-danger);">*</span>
              </label>
              <div style="display: flex; gap: 6px;">
                ${AppConfig.getCountryCode() ? `<span style="display: flex; align-items: center; justify-content: center; background: #f1f5f9; padding: 0 10px; border: 1px solid var(--fl-border); border-radius: 8px; font-size: 13px; color: var(--fl-text-secondary);">+${AppConfig.getCountryCode()}</span>` : ''}
                <input 
                  type="tel" 
                  name="phone" 
                  value="${this.contactData.phone || ''}"
                  placeholder="Enter 10-digit number" 
                  class="fl-input-field" 
                  style="flex: 1; height: 38px; padding: 0 12px; border: 1px solid var(--fl-border); border-radius: 8px; font-size: 13.5px;"
                />
              </div>
            </div>

            <!-- Step 1 Actions -->
            <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 10px; border-top: 1px solid var(--fl-border); padding-top: 14px;">
              <button 
                type="button" 
                id="fl-cancel-btn-step1" 
                class="fl-btn fl-btn-secondary"
                style="padding: 9px 18px; font-size: 13px;"
              >
                Cancel
              </button>
              
              <button 
                type="button" 
                id="fl-next-to-plans-btn" 
                class="fl-btn fl-btn-primary"
                style="padding: 9px 20px; font-size: 13px;"
              >
                <span>Choose Plans</span> <span>→</span>
              </button>
            </div>
          </div>

          <!-- ============================================== -->
          <!-- STEP 2: PLANS & SERVICES                       -->
          <!-- ============================================== -->
          <div id="fl-step-2-container" style="display: ${this.currentStep === 2 ? 'flex' : 'none'}; flex-direction: column; gap: 14px;">
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <div>
                <label style="font-size: 13.5px; font-weight: 700; color: var(--fl-text);">
                  Select Membership Plans
                </label>
                <div style="font-size: 11.5px; color: var(--fl-text-secondary); margin-top: 2px;">
                  Client: <strong>${this.contactData.firstName} ${this.contactData.lastName}</strong> (${this.contactData.phone})
                </div>
              </div>
              <button 
                type="button" 
                id="fl-add-plan-row-btn"
                class="fl-btn fl-btn-secondary"
                style="
                  padding: 6px 14px;
                  font-size: 12px;
                "
              >
                + Add Another Plan
              </button>
            </div>

            <div id="fl-plan-step-error" style="display: none; padding: 8px 12px; background: #fef2f2; border: 1px solid #fecaca; border-radius: 6px; color: #dc2626; font-size: 12px; font-weight: 600;"></div>

            <div id="fl-plan-rows-container" style="display: flex; flex-direction: column; gap: 12px; max-height: 380px; overflow-y: auto; padding-right: 4px;">
              ${this.planRows.map((row, idx) => {
                const isCustom = row.planId === '__CUSTOM__';
                const start = new Date(row.startDate || todayIso);
                let endIso = row.endDate;
                if (!endIso) {
                  const end = new Date(start);
                  end.setDate(end.getDate() + (row.durationDays || 30));
                  endIso = `${end.getFullYear()}-${pad(end.getMonth() + 1)}-${pad(end.getDate())}`;
                  row.endDate = endIso;
                }

                return `
                  <div class="fl-plan-row-card" data-row-id="${row.id}" style="
                    background: #f8fafc;
                    border: 1px solid var(--fl-border);
                    border-radius: 10px;
                    padding: 12px 14px;
                    position: relative;
                  ">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                      <span style="font-size: 12px; font-weight: 700; color: var(--fl-text-secondary); text-transform: uppercase;">
                        Plan #${idx + 1}
                      </span>
                      ${this.planRows.length > 1 ? `
                        <button type="button" class="fl-remove-plan-row-btn" data-row-id="${row.id}" style="
                          background: none;
                          border: none;
                          color: var(--fl-danger, #dc2626);
                          font-size: 12px;
                          font-weight: 600;
                          cursor: pointer;
                          padding: 2px 6px;
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
                          class="fl-plan-row-select" 
                          data-row-id="${row.id}"
                          style="width: 100%; height: 36px; padding: 0 8px; border: 1px solid var(--fl-border); border-radius: 6px; font-size: 13px; background: #ffffff;"
                        >
                          <option value="">Select a plan...</option>
                          ${(availablePlans || []).filter(p => p.isActive !== false).map(p => {
                            const durLabel = p.durationDays ? (p.durationDays === 1 ? '1 day' : `${p.durationDays} days`) : `${p.durationMonths || 1} mo`;
                            return `
                              <option value="${p.id}" ${row.planId === p.id ? 'selected' : ''}>
                                ${p.name} — ${AppConfig.formatCurrency(p.price || 0)} (${durLabel})
                              </option>
                            `;
                          }).join('')}
                          <option value="__CUSTOM__" ${isCustom ? 'selected' : ''}>⚡ Custom Plan...</option>
                        </select>
                      </div>

                      <div>
                        <label style="display: block; font-size: 11.5px; font-weight: 600; color: var(--fl-text); margin-bottom: 3px;">
                          Fee (${AppConfig.getCurrencySymbol() || '₹'})
                        </label>
                        <input 
                          type="number" 
                          class="fl-plan-row-fee" 
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
                              class="fl-custom-name" 
                              data-row-id="${row.id}" 
                              value="${row.customName || 'Custom Plan'}" 
                              style="width: 100%; height: 32px; padding: 0 8px; border: 1px solid var(--fl-border); border-radius: 4px; font-size: 12px;"
                            />
                          </div>
                          <div>
                            <label style="display: block; font-size: 11px; font-weight: 600; color: var(--fl-text); margin-bottom: 2px;">Duration</label>
                            <input 
                              type="number" 
                              class="fl-custom-dur-val" 
                              data-row-id="${row.id}" 
                              value="${row.customDurationVal || 1}" 
                              min="1" 
                              style="width: 100%; height: 32px; padding: 0 8px; border: 1px solid var(--fl-border); border-radius: 4px; font-size: 12px;"
                            />
                          </div>
                          <div>
                            <label style="display: block; font-size: 11px; font-weight: 600; color: var(--fl-text); margin-bottom: 2px;">Unit</label>
                            <select 
                              class="fl-custom-dur-unit" 
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
                          class="fl-plan-row-start" 
                          data-row-id="${row.id}"
                          value="${row.startDate || todayIso}"
                          style="width: 100%; height: 36px; padding: 0 8px; border: 1px solid var(--fl-border); border-radius: 6px; font-size: 12.5px; background: #ffffff;"
                        />
                      </div>
                      <div>
                        <label style="display: block; font-size: 11.5px; font-weight: 600; color: var(--fl-text); margin-bottom: 3px;">
                          Valid Until
                        </label>
                        <input 
                          type="date" 
                          class="fl-plan-row-end" 
                          data-row-id="${row.id}"
                          value="${row.endDate || endIso}"
                          style="width: 100%; height: 36px; padding: 0 8px; border: 1px solid var(--fl-border); border-radius: 6px; font-size: 12.5px; background: #ffffff;"
                        />
                      </div>
                    </div>
                  </div>
                `;
              }).join('')}
            </div>

            <!-- Total Preview Bar -->
            <div style="background: #f8fafc; border: 1px solid var(--fl-border); border-radius: 8px; padding: 10px 16px; display: flex; justify-content: space-between; align-items: center;">
              <div>
                <div style="font-size: 12px; color: var(--fl-text-secondary); font-weight: 600;">Total Plans Bill</div>
                <div style="font-size: 11px; color: var(--fl-text-muted);">${this.planRows.length} plan${this.planRows.length > 1 ? 's' : ''} bundled</div>
              </div>
              <div style="font-size: 18px; font-weight: 800; color: #0f172a;" id="fl-plans-total-preview">
                ${AppConfig.formatCurrency(totalFee)}
              </div>
            </div>

            <!-- Step 2 Actions -->
            <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 10px; border-top: 1px solid var(--fl-border); padding-top: 14px;">
              <button 
                type="button" 
                id="fl-back-to-contact-btn" 
                class="fl-btn fl-btn-secondary"
                style="padding: 9px 18px; font-size: 13px;"
              >
                ← Back
              </button>
              
              <button 
                type="button" 
                id="fl-next-to-payment-btn" 
                class="fl-btn fl-btn-primary"
                style="padding: 9px 20px; font-size: 13px;"
              >
                <span>Continue to Payment</span> <span>→</span>
              </button>
            </div>
          </div>

          <!-- ============================================== -->
          <!-- STEP 3: PAYMENT                                -->
          <!-- ============================================== -->
          <div id="fl-step-3-container" style="display: ${this.currentStep === 3 ? 'flex' : 'none'}; flex-direction: column; gap: 14px;">
            
            <!-- Itemized Enrollment Overview Card -->
            <div style="background: #f8fafc; border: 1px solid var(--fl-border); border-radius: 8px; padding: 14px 16px;">
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
                <span style="font-size: 12px; font-weight: 700; text-transform: uppercase; color: var(--fl-text-secondary);">
                  Enrollment Summary
                </span>
                <span style="font-size: 12.5px; font-weight: 600; color: var(--fl-text);">
                  ${this.contactData.firstName} ${this.contactData.lastName} (+${AppConfig.getCountryCode()} ${this.contactData.phone})
                </span>
              </div>

              <!-- Itemized Plans Table -->
              <div style="border-top: 1px solid var(--fl-border); border-bottom: 1px solid var(--fl-border); padding: 8px 0; margin-bottom: 10px;">
                <table style="width: 100%; border-collapse: collapse; font-size: 12.5px;">
                  <thead>
                    <tr style="color: var(--fl-text-secondary); text-align: left; font-size: 11px; text-transform: uppercase;">
                      <th style="padding: 4px 0; font-weight: 600;">Plan / Service</th>
                      <th style="padding: 4px 6px; font-weight: 600;">Duration</th>
                      <th style="padding: 4px 6px; font-weight: 600;">Dates</th>
                      <th style="padding: 4px 0; text-align: right; font-weight: 600;">Fee</th>
                    </tr>
                  </thead>
                  <tbody>
                    ${this.planRows.map((r, i) => {
                      let planName = 'Custom Plan';
                      if (r.planId === '__CUSTOM__') {
                        planName = r.customName || 'Custom Plan';
                      } else {
                        const found = (availablePlans || []).find(p => p.id === r.planId);
                        planName = found ? found.name : `Plan #${i + 1}`;
                      }
                      const humanDur = this.getHumanDuration(r);
                      const sStr = HomeStore.formatDateDisplay(r.startDate);
                      const eStr = HomeStore.formatDateDisplay(r.endDate);

                      return `
                        <tr style="border-top: ${i > 0 ? '1px dashed #e2e8f0' : 'none'};">
                          <td style="padding: 6px 0; font-weight: 600; color: var(--fl-text);">${planName}</td>
                          <td style="padding: 6px; color: var(--fl-text-secondary); font-size: 12px;">${humanDur}</td>
                          <td style="padding: 6px; color: var(--fl-text-secondary); font-size: 11.5px;">${sStr} → ${eStr}</td>
                          <td style="padding: 6px 0; text-align: right; font-weight: 700; color: var(--fl-text);">${AppConfig.formatCurrency(r.fee || 0)}</td>
                        </tr>
                      `;
                    }).join('')}
                  </tbody>
                </table>
              </div>

              <div style="display: flex; justify-content: space-between; align-items: center;">
                <span style="font-size: 13.5px; font-weight: 700; color: var(--fl-text);">Total Plan Bill:</span>
                <span style="font-size: 17px; font-weight: 800; color: #0f172a;">${AppConfig.formatCurrency(totalFee)}</span>
              </div>
            </div>

            <!-- Custom Payment Records Builder -->
            <div style="border-top: 1px solid var(--fl-border); padding-top: 12px;">
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                <div>
                  <label style="font-size: 13px; font-weight: 700; color: var(--fl-text);">
                    Record Payments & Installments
                  </label>
                  <div style="font-size: 11.5px; color: var(--fl-text-secondary);">
                    Set flexible installments, mark as paid or unpaid, and pick custom due dates.
                  </div>
                </div>
                <button 
                  type="button" 
                  id="fl-add-payment-record-btn"
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
                  + Add Payment / Due
                </button>
              </div>

              <div id="fl-payment-step-error" style="display: none; padding: 8px 12px; background: #fef2f2; border: 1px solid #fecaca; border-radius: 6px; color: #dc2626; font-size: 12px; font-weight: 600; margin-bottom: 8px;"></div>

              <div id="fl-payment-records-container" style="display: flex; flex-direction: column; gap: 10px; max-height: 260px; overflow-y: auto; padding-right: 4px;">
                ${this.paymentRecords.map((pay, idx) => {
                  const isPaid = pay.status === 'PAID';
                  return `
                    <div class="fl-payment-record-card" data-pay-id="${pay.id}" style="
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
                          <button type="button" class="fl-remove-payment-btn" data-pay-id="${pay.id}" style="background: none; border: none; color: #dc2626; font-size: 11.5px; font-weight: 600; cursor: pointer;">
                            ✕ Remove
                          </button>
                        ` : ''}
                      </div>

                      <div style="display: grid; grid-template-columns: 1.2fr 1.2fr 1.4fr; gap: 8px;">
                        <div>
                          <label style="display: block; font-size: 11px; font-weight: 600; color: var(--fl-text); margin-bottom: 2px;">
                            Amount (₹)
                          </label>
                          <input 
                            type="number" 
                            class="fl-pay-amount-input" 
                            data-pay-id="${pay.id}" 
                            value="${pay.amount !== undefined ? pay.amount : ''}" 
                            min="0"
                            placeholder="0"
                            style="width: 100%; height: 34px; padding: 0 8px; border: 1px solid var(--fl-border); border-radius: 6px; font-size: 12.5px; background: #ffffff;"
                          />
                        </div>

                        <div>
                          <label style="display: block; font-size: 11px; font-weight: 600; color: var(--fl-text); margin-bottom: 2px;">
                            Status
                          </label>
                          <select 
                            class="fl-pay-status-select" 
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
                            class="fl-pay-date-input" 
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
                            class="fl-pay-method-select" 
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
                            class="fl-pay-txnid-input" 
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
                            class="fl-pay-notes-input" 
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
                  <strong style="color: var(--fl-text);" id="fl-total-recorded-display">${AppConfig.formatCurrency(totalRecorded)}</strong>
                </div>
                <div>
                  <span style="color: var(--fl-text-secondary);">Balance:</span> 
                  <strong style="color: ${balanceDues > 0 ? '#b45309' : '#15803d'};" id="fl-balance-dues-display">
                    ${balanceDues > 0 ? `${AppConfig.formatCurrency(balanceDues)} Pending` : 'Fully Settled ✓'}
                  </strong>
                </div>
              </div>
            </div>

            <!-- Step 3 Actions -->
            <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 10px; border-top: 1px solid var(--fl-border); padding-top: 14px;">
              <button 
                type="button" 
                id="fl-back-to-plans-btn" 
                class="fl-btn fl-btn-secondary"
                style="padding: 9px 18px; font-size: 13px;"
              >
                ← Back
              </button>
              
              <div style="display: flex; gap: 10px; align-items: center;">
                <button 
                  type="button" 
                  id="fl-send-quote-wa-btn" 
                  class="fl-btn fl-btn-whatsapp"
                  title="Send quoted plans and pricing to the client on WhatsApp without activating ledger"
                  style="
                    padding: 9px 18px;
                    font-size: 13px;
                  "
                >
                  ${Icons.whatsapp || ''} <span>Send WhatsApp Quote</span>
                </button>

                <button 
                  type="submit" 
                  id="fl-submit-member-btn" 
                  class="fl-btn fl-btn-primary"
                  style="padding: 10px 22px; font-size: 13.5px;"
                >
                  Confirm & Enroll Member
                </button>
              </div>
            </div>
          </div>

        </form>
      </fl-modal>
    `;

    this.bindEvents();
  }

  bindEvents() {
    const modal = this.querySelector('fl-modal');
    if (modal) {
      const handleClose = () => {
        this.resetState();
        HomeStore.closeAddMemberModal();
      };
      modal.addEventListener('close', handleClose);
      modal.addEventListener('fl-modal-close', handleClose);
    }

    // Stepper jump clicks on completed steps
    this.querySelectorAll('.fl-stepper-jump').forEach(el => {
      el.addEventListener('click', () => {
        const targetStep = Number(el.getAttribute('data-jump-step'));
        if (targetStep < this.currentStep) {
          this.currentStep = targetStep;
          this.render();
        }
      });
    });

    // Step 1: Cancel
    const cancel1 = this.querySelector('#fl-cancel-btn-step1');
    if (cancel1) {
      cancel1.addEventListener('click', () => {
        this.resetState();
        HomeStore.closeAddMemberModal();
      });
    }

    // Step 1: Next to Plans (Auto-Saves Lead as Prospect or Updates on Phone/Name Change)
    const nextToPlansBtn = this.querySelector('#fl-next-to-plans-btn');
    if (nextToPlansBtn) {
      nextToPlansBtn.addEventListener('click', async () => {
        if (!this.validateContactStep()) return;

        nextToPlansBtn.disabled = true;
        nextToPlansBtn.innerHTML = `<span>Saving...</span>`;

        try {
          if (!this.createdUserId) {
            // First time advancing: Auto-save as Prospect to secure lead in DB
            const res = await HomeStore.autoSaveProspect({
              firstName: this.contactData.firstName,
              lastName: this.contactData.lastName,
              phone: this.contactData.phone
            });
            this.createdUserId = res?.userId || res?.id;
            this.lastSavedContact = { ...this.contactData };
          } else {
            // Returning to Step 1: Check if admin corrected the phone number or name
            const hasChanged = !this.lastSavedContact ||
              this.lastSavedContact.firstName !== this.contactData.firstName ||
              this.lastSavedContact.lastName !== this.contactData.lastName ||
              this.lastSavedContact.phone !== this.contactData.phone;

            if (hasChanged) {
              await HomeStore.updateMember({
                userId: this.createdUserId,
                firstName: this.contactData.firstName,
                lastName: this.contactData.lastName,
                phoneNumber: this.contactData.phone,
                plans: [],
                payments: [],
                silent: true
              });
              this.lastSavedContact = { ...this.contactData };
            }
          }

          this.currentStep = 2;
          this.render();
        } catch (err) {
          ErrorMapper.handle(err, this.querySelector('#fl-add-member-form'));
        } finally {
          nextToPlansBtn.disabled = false;
          nextToPlansBtn.innerHTML = `<span>Choose Plans</span> <span>→</span>`;
        }
      });
    }

    // Step 2: Back to Contact
    const backToContactBtn = this.querySelector('#fl-back-to-contact-btn');
    if (backToContactBtn) {
      backToContactBtn.addEventListener('click', () => {
        this.currentStep = 1;
        this.render();
      });
    }

    // Step 2: Add Plan Row
    const addRowBtn = this.querySelector('#fl-add-plan-row-btn');
    if (addRowBtn) {
      addRowBtn.addEventListener('click', () => {
        const today = new Date();
        const pad = (n) => String(n).padStart(2, '0');
        const todayIso = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`;
        const nextMonth = new Date(today);
        nextMonth.setDate(nextMonth.getDate() + 30);
        const nextMonthIso = `${nextMonth.getFullYear()}-${pad(nextMonth.getMonth() + 1)}-${pad(nextMonth.getDate())}`;

        this.planRows.push({
          id: `row-${Date.now()}-${this.planRows.length}`,
          planId: '',
          customName: 'Custom Service',
          customDurationVal: 1,
          customDurationUnit: 'months',
          startDate: todayIso,
          endDate: nextMonthIso,
          fee: 0,
          durationDays: 30
        });
        this.render();
      });
    }

    // Step 2: Remove Plan Row
    this.querySelectorAll('.fl-remove-plan-row-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const rowId = btn.getAttribute('data-row-id');
        this.planRows = this.planRows.filter(r => r.id !== rowId);
        if (this.planRows.length === 0) this.resetPlanRows();
        this.render();
      });
    });

    // Step 2: Plan Row Select
    const { availablePlans } = HomeStore.getState();
    const pad = (n) => String(n).padStart(2, '0');
    const toIso = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

    this.querySelectorAll('.fl-plan-row-select').forEach(sel => {
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
          }
        }
        // Auto-recalculate endDate based on new duration
        const s = new Date(row.startDate || new Date());
        const e = new Date(s);
        e.setDate(e.getDate() + (row.durationDays || 30));
        row.endDate = toIso(e);

        this.render();
      });
    });

    // Step 2: Plan Row Fee input
    this.querySelectorAll('.fl-plan-row-fee').forEach(input => {
      input.addEventListener('input', () => {
        const rowId = input.getAttribute('data-row-id');
        const row = this.planRows.find(r => r.id === rowId);
        if (row) {
          row.fee = Number(input.value) || 0;
          const totalFee = this.planRows.reduce((sum, r) => sum + (Number(r.fee) || 0), 0);
          const totalPreview = this.querySelector('#fl-plans-total-preview');
          if (totalPreview) totalPreview.textContent = AppConfig.formatCurrency(totalFee);
        }
      });
    });

    // Step 2: Start Date
    this.querySelectorAll('.fl-plan-row-start').forEach(input => {
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

    // Step 2: Valid Until (Custom End Date)
    this.querySelectorAll('.fl-plan-row-end').forEach(input => {
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

    // Step 2: Custom Name / Duration
    this.querySelectorAll('.fl-custom-name').forEach(input => {
      input.addEventListener('input', () => {
        const rowId = input.getAttribute('data-row-id');
        const row = this.planRows.find(r => r.id === rowId);
        if (row) row.customName = input.value;
      });
    });

    this.querySelectorAll('.fl-custom-dur-val').forEach(input => {
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

    this.querySelectorAll('.fl-custom-dur-unit').forEach(sel => {
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

    // Step 2: Next to Payment
    const nextToPaymentBtn = this.querySelector('#fl-next-to-payment-btn');
    if (nextToPaymentBtn) {
      nextToPaymentBtn.addEventListener('click', () => {
        if (!this.validatePlansStep()) return;
        // Re-initialize payments to default to full total fee if only 1 untouched record
        const totalFee = this.planRows.reduce((sum, r) => sum + (Number(r.fee) || 0), 0);
        if (this.paymentRecords.length <= 1) {
          this.resetPaymentRecords();
        }
        this.currentStep = 3;
        this.render();
      });
    }

    // Step 3: Back to Plans
    const backToPlansBtn = this.querySelector('#fl-back-to-plans-btn');
    if (backToPlansBtn) {
      backToPlansBtn.addEventListener('click', () => {
        this.currentStep = 2;
        this.render();
      });
    }

    // Step 3: Add Payment Record
    const addPaymentBtn = this.querySelector('#fl-add-payment-record-btn');
    if (addPaymentBtn) {
      addPaymentBtn.addEventListener('click', () => {
        const today = new Date();
        const pad = (n) => String(n).padStart(2, '0');
        const todayIso = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`;

        const totalFee = this.planRows.reduce((sum, r) => sum + (Number(r.fee) || 0), 0);
        const recorded = this.paymentRecords.reduce((sum, p) => sum + (Number(p.amount) || 0), 0);
        const remaining = Math.max(0, totalFee - recorded);

        this.paymentRecords.push({
          id: `pay-${Date.now()}-${this.paymentRecords.length}`,
          amount: remaining,
          status: 'UNPAID',
          dueDate: todayIso,
          method: 'UPI',
          notes: `Installment #${this.paymentRecords.length + 1}`
        });
        this.render();
      });
    }

    // Step 3: Remove Payment Record
    this.querySelectorAll('.fl-remove-payment-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const payId = btn.getAttribute('data-pay-id');
        this.paymentRecords = this.paymentRecords.filter(p => p.id !== payId);
        if (this.paymentRecords.length === 0) this.resetPaymentRecords();
        this.render();
      });
    });

    // Step 3: Payment Record Amount Input
    this.querySelectorAll('.fl-pay-amount-input').forEach(input => {
      input.addEventListener('input', () => {
        const payId = input.getAttribute('data-pay-id');
        const pay = this.paymentRecords.find(p => p.id === payId);
        if (pay) {
          pay.amount = Number(input.value) || 0;
          const totalFee = this.planRows.reduce((sum, r) => sum + (Number(r.fee) || 0), 0);
          const totalRecorded = this.paymentRecords.reduce((sum, p) => sum + (Number(p.amount) || 0), 0);
          const balance = Math.max(0, totalFee - totalRecorded);

          const recDisplay = this.querySelector('#fl-total-recorded-display');
          if (recDisplay) recDisplay.textContent = AppConfig.formatCurrency(totalRecorded);

          const balDisplay = this.querySelector('#fl-balance-dues-display');
          if (balDisplay) {
            balDisplay.textContent = balance > 0 ? `${AppConfig.formatCurrency(balance)} Pending` : 'Fully Settled ✓';
            balDisplay.style.color = balance > 0 ? '#b45309' : '#15803d';
          }
        }
      });
    });

    // Step 3: Payment Record Status Select
    this.querySelectorAll('.fl-pay-status-select').forEach(sel => {
      sel.addEventListener('change', () => {
        const payId = sel.getAttribute('data-pay-id');
        const pay = this.paymentRecords.find(p => p.id === payId);
        if (pay) {
          pay.status = sel.value;
          this.render();
        }
      });
    });

    // Step 3: Payment Record Date Input
    this.querySelectorAll('.fl-pay-date-input').forEach(input => {
      input.addEventListener('input', () => {
        const payId = input.getAttribute('data-pay-id');
        const pay = this.paymentRecords.find(p => p.id === payId);
        if (pay) pay.dueDate = input.value;
      });
    });

    // Step 3: Payment Record Method Select
    this.querySelectorAll('.fl-pay-method-select').forEach(sel => {
      sel.addEventListener('change', () => {
        const payId = sel.getAttribute('data-pay-id');
        const pay = this.paymentRecords.find(p => p.id === payId);
        if (pay) pay.method = sel.value;
      });
    });

    // Step 3: Payment Record Txn ID Input
    this.querySelectorAll('.fl-pay-txnid-input').forEach(input => {
      input.addEventListener('input', () => {
        const payId = input.getAttribute('data-pay-id');
        const pay = this.paymentRecords.find(p => p.id === payId);
        if (pay) pay.transactionId = input.value;
      });
    });

    // Step 3: Payment Record Notes Input
    this.querySelectorAll('.fl-pay-notes-input').forEach(input => {
      input.addEventListener('input', () => {
        const payId = input.getAttribute('data-pay-id');
        const pay = this.paymentRecords.find(p => p.id === payId);
        if (pay) pay.notes = input.value;
      });
    });

    // Step 3: Send WhatsApp Quote Button (Opens stacked WhatsApp modal with quote)
    const sendQuoteWaBtn = this.querySelector('#fl-send-quote-wa-btn');
    if (sendQuoteWaBtn) {
      sendQuoteWaBtn.addEventListener('click', () => {
        if (!this.validatePlansStep()) return;

        const existingMember = (HomeStore.getState().members || []).find(m => m.id === this.createdUserId);
        const memberObj = existingMember || {
          id: this.createdUserId,
          userId: this.createdUserId,
          name: `${this.contactData.firstName} ${this.contactData.lastName}`.trim(),
          firstName: this.contactData.firstName,
          lastName: this.contactData.lastName,
          phone: `+${AppConfig.getCountryCode() || '91'} ${this.contactData.phone}`,
          rawPhone: this.contactData.phone,
          status: 'prospect',
          statusLabel: 'Prospect'
        };

        HomeStore.openWhatsAppModal({
          member: memberObj,
          defaultTemplate: 'prospect_walkin_recap',
          quotedPlans: this.planRows.map(r => ({ ...r })),
          stackedFromEnrollment: true
        });
      });
    }

    // Step 3: Final Form Submit (Enroll Member)
    const form = this.querySelector('#fl-add-member-form');
    if (form) {
      // Clear errors on typing in inputs
      form.querySelectorAll('input').forEach(input => {
        input.addEventListener('input', () => {
          input.classList.remove('fl-input--error');
          const errEl = form.querySelector(`.fl-field-error-msg[data-for="${input.name}"]`);
          if (errEl) errEl.remove();
        });
      });

      form.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (this.currentStep !== 3) return;
        if (!this.validatePaymentStep()) return;

        const submitBtn = this.querySelector('#fl-submit-member-btn');
        if (submitBtn) {
          submitBtn.disabled = true;
          submitBtn.textContent = 'Enrolling Member...';
        }

        try {
          const plansToSubmit = this.planRows.map(r => ({
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
          }));

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

          if (this.createdUserId) {
            await HomeStore.updateMember({
              userId: this.createdUserId,
              firstName: this.contactData.firstName,
              lastName: this.contactData.lastName,
              phoneNumber: this.contactData.phone,
              plans: plansToSubmit,
              payments: paymentsToSubmit,
              successMessage: `Member "${this.contactData.firstName} ${this.contactData.lastName}" enrolled successfully!`
            });
            this.resetState();
            HomeStore.closeAddMemberModal();
          } else {
            await HomeStore.addMember({
              firstName: this.contactData.firstName,
              lastName: this.contactData.lastName,
              phone: this.contactData.phone,
              plans: plansToSubmit,
              payments: paymentsToSubmit
            });
            this.resetState();
          }
        } catch (err) {
          ErrorMapper.handle(err, form);
        } finally {
          if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.textContent = 'Confirm & Enroll Member';
          }
        }
      });
    }
  }
}

customElements.define('fl-add-member-modal', FlAddMemberModal);
