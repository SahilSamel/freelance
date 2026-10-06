import { HomeStore } from '../home-store.js';
import { FormBinder } from '../../../core/error/form-binder.js';
import { ErrorMapper } from '../../../core/error/error-mapper.js';
import { AppConfig } from '../../../core/config/app-config.js';
import '../../../core/components/feedback/fl-modal.js';

export class FlAddMemberModal extends HTMLElement {
  connectedCallback() {
    this.unsubscribe = HomeStore.subscribe(() => this.render());
    this.render();
  }

  disconnectedCallback() {
    if (this.unsubscribe) this.unsubscribe();
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

    this.innerHTML = `
      <fl-modal title="➕ Register New Member" max-width="540px">
        <form id="fl-add-member-form" style="display: flex; flex-direction: column; gap: 14px;" novalidate>
          
          <!-- Name Row -->
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
            <div class="fl-form-group">
              <label style="display: block; font-size: 13px; font-weight: 600; color: var(--fl-text); margin-bottom: 4px;">
                First Name <span style="color: var(--fl-danger);">*</span>
              </label>
              <input 
                type="text" 
                name="firstName" 
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
                placeholder="Enter 10-digit number" 
                class="fl-input-field" 
                style="flex: 1; height: 38px; padding: 0 12px; border: 1px solid var(--fl-border); border-radius: 8px; font-size: 13.5px;"
              />
            </div>
          </div>

          <!-- Membership Plan Selection -->
          <div style="border-top: 1px solid var(--fl-border); padding-top: 14px;">
            <div class="fl-form-group">
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px;">
                <label style="font-size: 13px; font-weight: 600; color: var(--fl-text);">
                  Assign Membership Plan <span style="color: var(--fl-danger);">*</span>
                </label>
                <span style="font-size: 11.5px; color: var(--fl-text-muted);">Predefined or Custom</span>
              </div>
              <select 
                name="planId" 
                id="fl-plan-select"
                class="fl-input-field" 
                style="width: 100%; height: 38px; padding: 0 12px; border: 1px solid var(--fl-border); border-radius: 8px; font-size: 13.5px; background: #ffffff;"
              >
                <option value="">Select a Plan...</option>
                ${availablePlans.map(p => `
                  <option value="${p.id}" data-price="${p.price || 0}" data-duration="${p.durationDays || (p.durationMonths ? p.durationMonths * 30 : 30)}" data-months="${p.durationMonths || 1}">
                    ${p.name} — ${AppConfig.formatCurrency(p.price || 0)} (${p.durationMonths || 1} month${(p.durationMonths || 1) > 1 ? 's' : ''})
                  </option>
                `).join('')}
                <option value="__CUSTOM__">⚡ Custom Plan (Set Custom Duration & Fee)...</option>
              </select>

              <!-- Dynamic Custom Plan Configuration Panel -->
              <div id="fl-custom-plan-panel" style="display: none; background: #f8fafc; border: 1.5px dashed var(--fl-border, #cbd5e1); border-radius: 10px; padding: 14px; margin-top: 10px;">
                <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px;">
                  <span style="font-size: 12.5px; font-weight: 700; color: var(--fl-primary, #dc2626);">
                    🛠️ Configure Custom Plan
                  </span>
                  <span style="font-size: 11px; color: var(--fl-text-muted); background: #ffffff; padding: 2px 6px; border: 1px solid var(--fl-border); border-radius: 4px;">
                    Auto-saved to gym plans
                  </span>
                </div>

                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
                  <div class="fl-form-group">
                    <label style="display: block; font-size: 12px; font-weight: 600; color: var(--fl-text); margin-bottom: 3px;">
                      Plan Name
                    </label>
                    <input 
                      type="text" 
                      name="customPlanName" 
                      value="Custom Plan" 
                      placeholder="e.g. Special Trial or PT" 
                      class="fl-input-field" 
                      style="width: 100%; height: 36px; padding: 0 10px; border: 1px solid var(--fl-border); border-radius: 6px; font-size: 13px; background: #ffffff;"
                    />
                  </div>

                  <div class="fl-form-group">
                    <label style="display: block; font-size: 12px; font-weight: 600; color: var(--fl-text); margin-bottom: 3px;">
                      Total Fee (${AppConfig.getCurrencySymbol() || 'Amount'}) <span style="color: var(--fl-danger);">*</span>
                    </label>
                    <input 
                      type="number" 
                      name="customPlanPrice" 
                      placeholder="e.g. 3500" 
                      min="0"
                      step="any"
                      class="fl-input-field" 
                      style="width: 100%; height: 36px; padding: 0 10px; border: 1px solid var(--fl-border); border-radius: 6px; font-size: 13px; background: #ffffff;"
                    />
                  </div>
                </div>

                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-top: 10px;">
                  <div class="fl-form-group">
                    <label style="display: block; font-size: 12px; font-weight: 600; color: var(--fl-text); margin-bottom: 3px;">
                      Duration Value <span style="color: var(--fl-danger);">*</span>
                    </label>
                    <input 
                      type="number" 
                      name="customDurationValue" 
                      value="1" 
                      min="1" 
                      class="fl-input-field" 
                      style="width: 100%; height: 36px; padding: 0 10px; border: 1px solid var(--fl-border); border-radius: 6px; font-size: 13px; background: #ffffff;"
                    />
                  </div>

                  <div class="fl-form-group">
                    <label style="display: block; font-size: 12px; font-weight: 600; color: var(--fl-text); margin-bottom: 3px;">
                      Duration Unit
                    </label>
                    <select 
                      name="customDurationUnit" 
                      class="fl-input-field" 
                      style="width: 100%; height: 36px; padding: 0 10px; border: 1px solid var(--fl-border); border-radius: 6px; font-size: 13px; background: #ffffff;"
                    >
                      <option value="months">Month(s)</option>
                      <option value="days">Day(s)</option>
                    </select>
                  </div>
                </div>

                <div id="fl-custom-plan-summary" style="margin-top: 10px; padding: 8px 12px; background: #ffffff; border: 1px solid var(--fl-border); border-radius: 6px; font-size: 12px; color: var(--fl-text-secondary); display: flex; justify-content: space-between; align-items: center;">
                  <span>Calculated Duration: <strong id="fl-summary-duration">1 Month (30 Days)</strong></span>
                  <span>Expires: <strong id="fl-summary-expiry">—</strong></span>
                </div>
              </div>
            </div>
          </div>

          <!-- Dates & Payment Mode -->
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
            <div class="fl-form-group">
              <label style="display: block; font-size: 13px; font-weight: 600; color: var(--fl-text); margin-bottom: 4px;">
                Membership Start Date
              </label>
              <input 
                type="date" 
                name="startDate" 
                value="${todayIso}" 
                class="fl-input-field" 
                style="width: 100%; height: 38px; padding: 0 12px; border: 1px solid var(--fl-border); border-radius: 8px; font-size: 13.5px;"
              />
            </div>

            <div class="fl-form-group">
              <label style="display: block; font-size: 13px; font-weight: 600; color: var(--fl-text); margin-bottom: 4px;">
                Payment Schedule
              </label>
              <select 
                name="paymentSchedule" 
                class="fl-input-field" 
                style="width: 100%; height: 38px; padding: 0 12px; border: 1px solid var(--fl-border); border-radius: 8px; font-size: 13.5px; background: #ffffff;"
              >
                <option value="full_paid">Full Payment - Paid Now</option>
                <option value="full_pending">Full Payment - Pending</option>
                <option value="2_installments">2 Equal Installments</option>
                <option value="3_installments">3 Equal Installments</option>
              </select>
            </div>
          </div>

          <!-- Action Buttons -->
          <div style="display: flex; justify-content: flex-end; gap: 10px; margin-top: 10px; border-top: 1px solid var(--fl-border); padding-top: 14px;">
            <button 
              type="button" 
              id="fl-cancel-add-btn" 
              style="padding: 9px 18px; border: 1px solid var(--fl-border); border-radius: 8px; background: #ffffff; font-weight: 600; font-size: 13.5px; cursor: pointer;"
            >
              Cancel
            </button>
            <button 
              type="submit" 
              id="fl-submit-add-btn" 
              style="padding: 9px 20px; border: none; border-radius: 8px; background: var(--fl-primary, #dc2626); color: #ffffff; font-weight: 600; font-size: 13.5px; cursor: pointer;"
            >
              Save Member
            </button>
          </div>
        </form>
      </fl-modal>
    `;

    const modal = this.querySelector('fl-modal');
    if (modal) {
      modal.addEventListener('close', () => HomeStore.closeAddMemberModal());
    }

    const cancelBtn = this.querySelector('#fl-cancel-add-btn');
    if (cancelBtn) {
      cancelBtn.addEventListener('click', () => HomeStore.closeAddMemberModal());
    }

    const form = this.querySelector('#fl-add-member-form');
    if (form) {
      const planSelect = form.querySelector('#fl-plan-select');
      const customPanel = form.querySelector('#fl-custom-plan-panel');
      const startDateInput = form.querySelector('[name="startDate"]');
      const customValInput = form.querySelector('[name="customDurationValue"]');
      const customUnitSelect = form.querySelector('[name="customDurationUnit"]');
      const summaryDuration = form.querySelector('#fl-summary-duration');
      const summaryExpiry = form.querySelector('#fl-summary-expiry');

      const updateCalculations = () => {
        const isCustom = planSelect && planSelect.value === '__CUSTOM__';
        if (customPanel) {
          customPanel.style.display = isCustom ? 'block' : 'none';
        }

        const startVal = startDateInput && startDateInput.value ? new Date(startDateInput.value) : new Date();
        let daysToAdd = 30;

        if (isCustom) {
          const val = Number(customValInput?.value) || 1;
          const unit = customUnitSelect?.value || 'months';
          daysToAdd = unit === 'months' ? val * 30 : val;
          if (summaryDuration) {
            summaryDuration.textContent = unit === 'months' 
              ? `${val} Month${val > 1 ? 's' : ''} (${daysToAdd} Days)` 
              : `${val} Day${val > 1 ? 's' : ''}`;
          }
        } else if (planSelect && planSelect.selectedOptions && planSelect.selectedOptions[0]) {
          const opt = planSelect.selectedOptions[0];
          if (opt && opt.dataset.duration) {
            daysToAdd = Number(opt.dataset.duration) || 30;
          }
        }

        const expDate = new Date(startVal);
        expDate.setDate(expDate.getDate() + daysToAdd);
        if (summaryExpiry) {
          summaryExpiry.textContent = HomeStore.formatDateDisplay(expDate.toISOString().slice(0, 10));
        }
      };

      planSelect?.addEventListener('change', updateCalculations);
      startDateInput?.addEventListener('input', updateCalculations);
      customValInput?.addEventListener('input', updateCalculations);
      customUnitSelect?.addEventListener('change', updateCalculations);
      updateCalculations();

      // Clear errors on input
      form.querySelectorAll('input, select').forEach(input => {
        input.addEventListener('input', () => {
          input.classList.remove('fl-input--error');
          const errEl = form.querySelector(`.fl-field-error-msg[data-for="${input.name}"]`);
          if (errEl) errEl.remove();
        });
      });

      form.addEventListener('submit', async (e) => {
        e.preventDefault();
        FormBinder.clearFormErrors(form);

        const formData = new FormData(form);
        const firstName = (formData.get('firstName') || '').trim();
        const lastName = (formData.get('lastName') || '').trim();
        const phone = (formData.get('phone') || '').trim();
        const planId = formData.get('planId');
        const startDate = formData.get('startDate');
        const schedule = formData.get('paymentSchedule');

        // Client-side Validation - highlights fields with RED LINE on error
        const errors = [];
        if (!firstName) errors.push({ field: 'firstName', message: 'First name is required' });
        if (!lastName) errors.push({ field: 'lastName', message: 'Last name is required' });
        if (!phone) {
          errors.push({ field: 'phone', message: 'Phone number is required' });
        } else if (!/^\d{10}$/.test(phone.replace(/\s+/g, ''))) {
          errors.push({ field: 'phone', message: 'Enter a valid 10-digit mobile number' });
        }
        if (!planId) {
          errors.push({ field: 'planId', message: 'Please select a membership plan' });
        } else if (planId === '__CUSTOM__') {
          const customName = (formData.get('customPlanName') || '').trim();
          const customPriceRaw = formData.get('customPlanPrice');
          const customValRaw = formData.get('customDurationValue');

          if (!customName) {
            errors.push({ field: 'customPlanName', message: 'Plan name is required' });
          }
          if (customPriceRaw === '' || isNaN(Number(customPriceRaw)) || Number(customPriceRaw) < 0) {
            errors.push({ field: 'customPlanPrice', message: 'Enter a valid plan fee' });
          }
          if (!customValRaw || Number(customValRaw) <= 0) {
            errors.push({ field: 'customDurationValue', message: 'Enter duration > 0' });
          }
        }

        if (errors.length > 0) {
          FormBinder.applyFieldErrors(form, errors);
          return;
        }

        const submitBtn = form.querySelector('#fl-submit-add-btn');
        submitBtn.disabled = true;
        submitBtn.textContent = 'Saving...';

        try {
          await HomeStore.addMember({
            firstName,
            lastName,
            phone,
            planId,
            customPlanData: planId === '__CUSTOM__' ? {
              name: (formData.get('customPlanName') || 'Custom Plan').trim(),
              price: Number(formData.get('customPlanPrice')) || 0,
              value: Number(formData.get('customDurationValue')) || 1,
              unit: formData.get('customDurationUnit') || 'months'
            } : null,
            startDate,
            schedule
          });
        } catch (err) {
          ErrorMapper.handle(err, form);
        } finally {
          submitBtn.disabled = false;
          submitBtn.textContent = 'Save Member';
        }
      });
    }
  }
}

customElements.define('fl-add-member-modal', FlAddMemberModal);
