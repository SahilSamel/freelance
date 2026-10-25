/**
 * <fl-plan-modal> Native Web Component
 * Modal dialog for Creating and Updating Membership Plans.
 * Handles both active and deactivated plans with duplicate name detection and reactivation.
 */

import { HomeStore } from '../home-store.js';
import { ErrorMapper } from '../../../core/error/error-mapper.js';
import { Icons } from '../../../assets/icons/icons.js';
import '../../../core/components/feedback/fl-modal.js';

export class FlPlanModal extends HTMLElement {
  connectedCallback() {
    this.unsubscribe = HomeStore.subscribe(() => this.render());
    this.render();
  }

  disconnectedCallback() {
    if (this.unsubscribe) this.unsubscribe();
  }

  render() {
    const state = HomeStore.getState();
    const { planModal } = state;

    if (!planModal || !planModal.isOpen) {
      this.innerHTML = '';
      return;
    }

    const isEdit = planModal.mode === 'edit';
    const plan = planModal.plan || {};
    const isInactive = plan.isActive === false;

    // Calculate initial duration values
    let durationVal = 1;
    let durationUnit = 'months';
    if (plan.durationMonths) {
      durationVal = plan.durationMonths;
      durationUnit = 'months';
    } else if (plan.durationDays) {
      if (plan.durationDays % 30 === 0) {
        durationVal = Math.round(plan.durationDays / 30);
        durationUnit = 'months';
      } else {
        durationVal = plan.durationDays;
        durationUnit = 'days';
      }
    }

    const price = plan.price !== undefined ? plan.price : (plan.defaultPrice !== undefined ? plan.defaultPrice : '');

    let titleText = 'Create New Membership Plan';
    if (isEdit) {
      titleText = isInactive ? 'Edit Deactivated Plan' : 'Edit Membership Plan';
    }

    this.innerHTML = `
      <fl-modal open title="${titleText}" max-width="520px">
        <form id="fl-plan-form" style="display: flex; flex-direction: column; gap: 16px;" novalidate>
          
          <!-- Inactive Notice Banner if editing deactivated plan -->
          ${isInactive ? `
            <div style="background: #fef2f2; border: 1px solid #fee2e2; border-radius: 8px; padding: 10px 14px; display: flex; align-items: center; justify-content: space-between; gap: 10px; font-size: 13px; color: #991b1b;">
              <div>
                <b>This plan is currently deactivated.</b><br/>
                <span style="font-size: 12px; color: #b91c1c;">It is hidden from new member enrollments and renewals.</span>
              </div>
              <button 
                type="button" 
                id="fl-reactivate-plan-modal-btn"
                style="padding: 6px 12px; background: #0f172a; color: #ffffff; border: none; border-radius: 6px; font-size: 12px; font-weight: 600; cursor: pointer; display: inline-flex; align-items: center; gap: 5px; white-space: nowrap; transition: background 0.15s ease;"
              >
                ${Icons.refresh}
                <span>Reactivate Now</span>
              </button>
            </div>
          ` : ''}

          <!-- Plan Name -->
          <div class="fl-form-group">
            <label style="display: block; font-size: 13px; font-weight: 600; color: #0f172a; margin-bottom: 5px;">
              Plan Name <span style="color: #dc2626;">*</span>
            </label>
            <input 
              type="text" 
              name="name" 
              value="${plan.name || ''}" 
              placeholder="e.g. 1 Month General Fitness, 3 Months Strength..."
              required
              autocomplete="off"
              style="width: 100%; height: 38px; padding: 0 12px; border: 1px solid #cbd5e1; border-radius: 8px; font-size: 13.5px; box-sizing: border-box; outline: none;"
            />
            <div id="fl-plan-name-feedback" style="display: none; margin-top: 6px;"></div>
          </div>

          <!-- Price & Duration Row -->
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
            <!-- Price -->
            <div class="fl-form-group">
              <label style="display: block; font-size: 13px; font-weight: 600; color: #0f172a; margin-bottom: 5px;">
                Fee / Price (₹) <span style="color: #dc2626;">*</span>
              </label>
              <div style="position: relative; display: flex; align-items: center;">
                <span style="position: absolute; left: 10px; font-size: 13.5px; color: #94a3b8; font-weight: 600;">₹</span>
                <input 
                  type="number" 
                  name="price" 
                  value="${price}" 
                  placeholder="2000"
                  min="0"
                  step="50"
                  required
                  style="width: 100%; height: 38px; padding: 0 12px 0 26px; border: 1px solid #cbd5e1; border-radius: 8px; font-size: 13.5px; box-sizing: border-box; outline: none;"
                />
              </div>
            </div>

            <!-- Duration -->
            <div class="fl-form-group">
              <label style="display: block; font-size: 13px; font-weight: 600; color: #0f172a; margin-bottom: 5px;">
                Validity Duration <span style="color: #dc2626;">*</span>
              </label>
              <div style="display: flex; gap: 6px;">
                <input 
                  type="number" 
                  name="durationVal" 
                  value="${durationVal}" 
                  min="1" 
                  step="1"
                  required
                  style="width: 70px; height: 38px; padding: 0 10px; border: 1px solid #cbd5e1; border-radius: 8px; font-size: 13.5px; box-sizing: border-box; outline: none;"
                />
                <select 
                  name="durationUnit" 
                  style="flex: 1; height: 38px; padding: 0 8px; border: 1px solid #cbd5e1; border-radius: 8px; font-size: 13px; box-sizing: border-box; background: #ffffff; outline: none;"
                >
                  <option value="months" ${durationUnit === 'months' ? 'selected' : ''}>Month(s)</option>
                  <option value="days" ${durationUnit === 'days' ? 'selected' : ''}>Day(s)</option>
                </select>
              </div>
            </div>
          </div>

          <!-- Description -->
          <div class="fl-form-group">
            <label style="display: block; font-size: 13px; font-weight: 600; color: #0f172a; margin-bottom: 5px;">
              Description & Highlights
            </label>
            <textarea 
              name="description" 
              rows="3"
              placeholder="e.g. Full access gym workout membership with standard equipment and facility usage."
              style="width: 100%; padding: 8px 12px; border: 1px solid #cbd5e1; border-radius: 8px; font-size: 13px; box-sizing: border-box; resize: vertical; outline: none; font-family: inherit;"
            >${plan.description || ''}</textarea>
          </div>

          <!-- Action Buttons -->
          <div style="display: flex; justify-content: ${isEdit ? 'space-between' : 'flex-end'}; align-items: center; gap: 10px; margin-top: 10px; border-top: 1px solid #e2e8f0; padding-top: 14px;">
            ${isEdit ? `
              ${isInactive ? `
                <button 
                  type="button" 
                  id="fl-reactivate-plan-footer-btn"
                  class="fl-btn fl-btn-primary"
                  style="height: 38px; padding: 0 14px; font-size: 12.5px;"
                >
                  ${Icons.refresh}
                  <span>Reactivate Plan</span>
                </button>
              ` : `
                <button 
                  type="button" 
                  id="fl-delete-plan-modal-btn"
                  class="fl-btn fl-btn-secondary"
                  style="height: 38px; padding: 0 14px; font-size: 12.5px; border-color: #fecdd3 !important; color: #dc2626 !important;"
                >
                  ${Icons.trash}
                  <span>Deactivate Plan</span>
                </button>
              `}
            ` : ''}

            <div style="display: flex; align-items: center; gap: 10px;">
              <button 
                type="button" 
                id="fl-cancel-plan-btn"
                class="fl-btn fl-btn-secondary"
                style="height: 38px; padding: 0 16px; font-size: 13px;"
              >
                Cancel
              </button>
              <button 
                type="submit" 
                id="fl-save-plan-btn"
                class="fl-btn fl-btn-primary"
                style="height: 38px; padding: 0 20px; font-size: 13px;"
              >
                ${isEdit ? 'Save Changes' : 'Create Plan'}
              </button>
            </div>
          </div>

        </form>
      </fl-modal>
    `;

    this.bindEvents(isEdit, plan);
  }

  bindEvents(isEdit, plan) {
    const modalEl = this.querySelector('fl-modal');
    if (modalEl) {
      const handleClose = () => HomeStore.closePlanModal();
      modalEl.addEventListener('fl-modal-close', handleClose);
      modalEl.addEventListener('fl-modal:close', handleClose);
      modalEl.addEventListener('close', handleClose);
    }

    const cancelBtn = this.querySelector('#fl-cancel-plan-btn');
    if (cancelBtn) {
      cancelBtn.addEventListener('click', () => {
        HomeStore.closePlanModal();
      });
    }

    // Deactivate plan button
    const deleteBtn = this.querySelector('#fl-delete-plan-modal-btn');
    if (deleteBtn) {
      deleteBtn.addEventListener('click', async () => {
        if (!plan.id) return;
        const confirmed = window.confirm(`Are you sure you want to deactivate "${plan.name || 'this plan'}"?`);
        if (!confirmed) return;
        try {
          deleteBtn.disabled = true;
          deleteBtn.textContent = 'Deactivating...';
          await HomeStore.deletePlan(plan.id);
          HomeStore.closePlanModal();
        } catch (err) {
          console.error('Deactivate plan error:', err);
          ErrorMapper.showErrorWindow(err.message || 'Failed to deactivate plan', 'Plan Error', err.status || '500');
          deleteBtn.disabled = false;
          deleteBtn.innerHTML = `${Icons.trash} <span>Deactivate Plan</span>`;
        }
      });
    }

    // Reactivate button handlers (banner & footer)
    const handleReactivate = async (btn) => {
      if (!plan.id) return;
      try {
        if (btn) {
          btn.disabled = true;
          btn.textContent = 'Reactivating...';
        }
        await HomeStore.reactivatePlan(plan.id);
        HomeStore.closePlanModal();
      } catch (err) {
        console.error('Reactivate plan error:', err);
        ErrorMapper.showErrorWindow(err.message || 'Failed to reactivate plan', 'Plan Error', err.status || '500');
        if (btn) {
          btn.disabled = false;
          btn.innerHTML = `${Icons.refresh} <span>Reactivate Plan</span>`;
        }
      }
    };

    const reactivateBannerBtn = this.querySelector('#fl-reactivate-plan-modal-btn');
    if (reactivateBannerBtn) {
      reactivateBannerBtn.addEventListener('click', () => handleReactivate(reactivateBannerBtn));
    }

    const reactivateFooterBtn = this.querySelector('#fl-reactivate-plan-footer-btn');
    if (reactivateFooterBtn) {
      reactivateFooterBtn.addEventListener('click', () => handleReactivate(reactivateFooterBtn));
    }

    const form = this.querySelector('#fl-plan-form');
    if (form) {
      const nameInput = form.querySelector('[name="name"]');
      const feedbackEl = form.querySelector('#fl-plan-name-feedback');
      const submitBtn = form.querySelector('#fl-save-plan-btn');

      // Live duplicate check on name input
      const checkNameDuplicate = (typedName) => {
        if (!feedbackEl) return;
        const trimmed = (typedName || '').trim().toLowerCase();
        if (!trimmed) {
          feedbackEl.style.display = 'none';
          feedbackEl.innerHTML = '';
          if (submitBtn) submitBtn.disabled = false;
          return;
        }

        const { availablePlans } = HomeStore.getState();
        const match = (availablePlans || []).find(p => 
          p.name && p.name.trim().toLowerCase() === trimmed && (!isEdit || p.id !== plan.id)
        );

        if (!match) {
          feedbackEl.style.display = 'none';
          feedbackEl.innerHTML = '';
          if (submitBtn) submitBtn.disabled = false;
          return;
        }

        if (match.isActive === false) {
          feedbackEl.style.display = 'block';
          feedbackEl.innerHTML = `
            <div style="padding: 8px 12px; background: #fff7ed; border: 1px solid #fed7aa; border-radius: 6px; font-size: 12px; color: #9a3412; display: flex; align-items: center; justify-content: space-between; gap: 8px;">
              <div>
                ⚠️ <b>A deactivated plan with this name exists.</b><br/>
                <span style="font-size: 11.5px; color: #c2410c;">Saving will reactivate & update "${match.name}".</span>
              </div>
              <button 
                type="button" 
                id="fl-quick-reactivate-btn" 
                data-plan-id="${match.id}"
                style="padding: 4px 10px; background: #ea580c; color: #ffffff; border: none; border-radius: 4px; font-size: 11.5px; font-weight: 600; cursor: pointer; white-space: nowrap;"
              >
                Reactivate Now
              </button>
            </div>
          `;
          const quickBtn = feedbackEl.querySelector('#fl-quick-reactivate-btn');
          if (quickBtn) {
            quickBtn.addEventListener('click', async () => {
              try {
                quickBtn.disabled = true;
                quickBtn.textContent = 'Reactivating...';
                await HomeStore.reactivatePlan(match.id);
                HomeStore.closePlanModal();
              } catch (err) {
                console.error('Quick reactivate error:', err);
                ErrorMapper.showErrorWindow(err.message || 'Failed to reactivate plan', 'Plan Error', err.status || '500');
              }
            });
          }
          if (submitBtn) submitBtn.disabled = false;
        } else {
          feedbackEl.style.display = 'block';
          feedbackEl.innerHTML = `
            <div style="padding: 8px 12px; background: #fef2f2; border: 1px solid #fee2e2; border-radius: 6px; font-size: 12px; color: #991b1b;">
              ⚠️ An active plan named "<b>${match.name}</b>" already exists. Please choose a unique name.
            </div>
          `;
          if (submitBtn) submitBtn.disabled = true;
        }
      };

      if (nameInput) {
        nameInput.addEventListener('input', (e) => {
          checkNameDuplicate(e.target.value);
        });
      }

      form.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (submitBtn) {
          submitBtn.disabled = true;
          submitBtn.textContent = isEdit ? 'Saving...' : 'Creating...';
        }

        try {
          const name = form.querySelector('[name="name"]').value.trim();
          if (!name) {
            ErrorMapper.showErrorWindow('Plan name is required', 'Validation Error', '400');
            if (submitBtn) {
              submitBtn.disabled = false;
              submitBtn.textContent = isEdit ? 'Save Changes' : 'Create Plan';
            }
            return;
          }

          const price = Number(form.querySelector('[name="price"]').value) || 0;
          const durationVal = Number(form.querySelector('[name="durationVal"]').value) || 1;
          const durationUnit = form.querySelector('[name="durationUnit"]').value || 'months';
          const durationDays = durationUnit === 'months' ? durationVal * 30 : durationVal;
          const description = form.querySelector('[name="description"]').value.trim();

          if (isEdit) {
            await HomeStore.updatePlan({
              id: plan.id,
              name,
              price,
              durationDays,
              durationMonths: durationUnit === 'months' ? durationVal : Math.max(1, Math.round(durationDays / 30)),
              description,
              isActive: plan.isActive !== false
            });
          } else {
            await HomeStore.createPlan({
              name,
              price,
              durationDays,
              durationMonths: durationUnit === 'months' ? durationVal : Math.max(1, Math.round(durationDays / 30)),
              description
            });
          }
        } catch (err) {
          console.error('Plan save error:', err);
          ErrorMapper.showErrorWindow(err.message || 'Failed to save plan', 'Plan Error', err.status || '500');
          if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.textContent = isEdit ? 'Save Changes' : 'Create Plan';
          }
        }
      });
    }
  }
}

customElements.define('fl-plan-modal', FlPlanModal);
