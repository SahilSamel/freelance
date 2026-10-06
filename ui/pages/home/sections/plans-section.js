/**
 * <fl-plans-section> Native Web Component
 * Membership Plans management section: cards grid with pricing, duration, features, and subscriber stats.
 */

import { HomeStore } from '../home-store.js';
import { AppConfig } from '../../../core/config/app-config.js';
import '../../../core/components/table/fl-badge.js';

export class FlPlansSection extends HTMLElement {
  connectedCallback() {
    this.unsubscribe = HomeStore.subscribe(() => this.render());
    this.render();
  }

  disconnectedCallback() {
    if (this.unsubscribe) this.unsubscribe();
  }

  render() {
    const { availablePlans, members, loading } = HomeStore.getState();

    // Compute enrollment per plan
    const planEnrollments = {};
    members.forEach(m => {
      (m.planBatches || []).forEach(b => {
        if (b.planId) {
          planEnrollments[b.planId] = (planEnrollments[b.planId] || 0) + 1;
        }
      });
    });

    this.innerHTML = `
      <div style="display: flex; flex-direction: column; gap: 20px;">
        
        <!-- Plans Header & Stats -->
        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px;">
          <div>
            <h3 style="margin: 0; font-size: 18px; font-weight: 700; color: var(--fl-text);">
              Membership Packages & Plans
            </h3>
            <p style="margin: 4px 0 0 0; font-size: 13px; color: var(--fl-text-secondary);">
              Configure recurring plans, durations, and pricing offered across the gym.
            </p>
          </div>

          <button type="button" id="fl-add-new-member-from-plans" style="
            height: 36px;
            padding: 0 16px;
            border: none;
            border-radius: 8px;
            background: var(--fl-primary, #dc2626);
            color: #ffffff;
            font-size: 13px;
            font-weight: 600;
            cursor: pointer;
          ">
            + Onboard Member
          </button>
        </div>

        <!-- Plans Grid -->
        <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px;">
          ${loading.plans ? `
            <div style="grid-column: 1 / -1; padding: 40px; text-align: center; color: var(--fl-text-muted);">
              Loading available membership plans...
            </div>
          ` : availablePlans.length === 0 ? `
            <div style="grid-column: 1 / -1; padding: 48px; text-align: center; color: var(--fl-text-muted);">
              No membership plans created yet.
            </div>
          ` : availablePlans.map(plan => {
            const enrolled = planEnrollments[plan.id] || 0;
            const priceFormatted = AppConfig.formatCurrency(plan.price || 0);
            const durationText = plan.durationMonths 
              ? `${plan.durationMonths} Month${plan.durationMonths > 1 ? 's' : ''}` 
              : (plan.durationDays ? `${plan.durationDays} Days` : '1 Month');

            return `
              <div style="
                background: #ffffff;
                border: 1px solid var(--fl-border);
                border-radius: 12px;
                padding: 20px;
                box-shadow: var(--fl-shadow-sm);
                display: flex;
                flex-direction: column;
                justify-content: space-between;
                transition: transform 0.15s ease, box-shadow 0.15s ease;
              " class="fl-plan-card">
                <div>
                  <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px;">
                    <h4 style="margin: 0; font-size: 16px; font-weight: 700; color: var(--fl-text);">
                      ${plan.name}
                    </h4>
                    <fl-badge variant="neutral" text="${durationText}"></fl-badge>
                  </div>
                  
                  <div style="font-size: 24px; font-weight: 800; color: var(--fl-primary, #dc2626); margin-bottom: 8px;">
                    ${priceFormatted}
                  </div>

                  <p style="font-size: 13px; color: var(--fl-text-secondary); margin: 0 0 16px 0; line-height: 1.4;">
                    ${plan.description || 'Full access gym workout membership with standard equipment and facility usage.'}
                  </p>

                  <!-- Features List -->
                  <div style="border-top: 1px dashed var(--fl-border); padding-top: 12px; margin-bottom: 16px;">
                    <div style="font-size: 11.5px; font-weight: 700; text-transform: uppercase; color: var(--fl-text-muted); margin-bottom: 6px;">
                      Plan Highlights
                    </div>
                    <ul style="margin: 0; padding-left: 18px; font-size: 12.5px; color: var(--fl-text-secondary); line-height: 1.6;">
                      <li>Cardio & Strength floor access</li>
                      <li>Locker room & shower access</li>
                      ${plan.durationMonths >= 3 ? '<li>Free 1-on-1 Fitness Assessment</li>' : ''}
                      ${plan.durationMonths >= 6 ? '<li>Diet & Nutrition starter guide</li>' : ''}
                    </ul>
                  </div>
                </div>

                <!-- Footer with Enrolled Count & Action -->
                <div style="border-top: 1px solid var(--fl-border); padding-top: 12px; display: flex; justify-content: space-between; align-items: center;">
                  <span style="font-size: 12.5px; color: var(--fl-text-muted); font-weight: 500;">
                    👥 <b>${enrolled}</b> Active Member${enrolled === 1 ? '' : 's'}
                  </span>
                  <button type="button" class="fl-assign-plan-btn" data-plan-id="${plan.id}" style="
                    background: #f1f5f9;
                    border: 1px solid var(--fl-border);
                    color: var(--fl-text);
                    padding: 6px 12px;
                    border-radius: 6px;
                    font-size: 12px;
                    font-weight: 600;
                    cursor: pointer;
                  ">
                    Assign Plan
                  </button>
                </div>
              </div>
            `;
          }).join('')}
        </div>

      </div>
    `;

    const onboardBtn = this.querySelector('#fl-add-new-member-from-plans');
    if (onboardBtn) {
      onboardBtn.addEventListener('click', () => {
        HomeStore.openAddMemberModal();
      });
    }

    this.querySelectorAll('.fl-assign-plan-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        HomeStore.openAddMemberModal();
      });
    });
  }
}

customElements.define('fl-plans-section', FlPlansSection);
