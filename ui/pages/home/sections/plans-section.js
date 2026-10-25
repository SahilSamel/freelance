/**
 * <fl-plans-section> Native Web Component
 * Membership Packages & Plans Management:
 * Full CRUD capabilities: Create, Read, Update, and Reactivate/Deactivate gym plans.
 * Filter toolbar with Search and Status dropdown filter matching Members & Payments sections.
 */

import { HomeStore } from '../home-store.js';
import { AppConfig } from '../../../core/config/app-config.js';
import { Icons } from '../../../assets/icons/icons.js';
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
    const activeEl = document.activeElement;
    const isSearchFocused = !!(activeEl && (activeEl.id === 'fl-plan-search-input' || activeEl === this.querySelector('#fl-plan-search-input')));
    const selStart = isSearchFocused ? activeEl.selectionStart : null;
    const selEnd = isSearchFocused ? activeEl.selectionEnd : null;

    const state = HomeStore.getState();
    const { availablePlans, members, loading, planFilters } = state;
    const allPlans = availablePlans || [];
    const filteredPlans = HomeStore.getFilteredPlans();

    // Calculate status counts
    const activePlansCount = allPlans.filter(p => p.isActive !== false).length;
    const deactivatedPlansCount = allPlans.filter(p => p.isActive === false).length;
    const totalPlansCount = allPlans.length;

    // Compute unique user enrollment count per plan
    const planUserSets = {};
    members.forEach(m => {
      const userPlanIds = new Set();
      (m.subscriptions || []).forEach(s => {
        if (s.planId) userPlanIds.add(s.planId);
      });
      (m.planBatches || []).forEach(b => {
        if (b.planId) userPlanIds.add(b.planId);
      });

      userPlanIds.forEach(pId => {
        if (!planUserSets[pId]) planUserSets[pId] = new Set();
        planUserSets[pId].add(m.id || m.userId);
      });
    });

    const planEnrollments = {};
    Object.keys(planUserSets).forEach(pId => {
      planEnrollments[pId] = planUserSets[pId].size;
    });

    const currentStatus = (planFilters && planFilters.status) || 'all';

    // Status Filter Button Label (Matching Members status dropdown)
    const statusLabelMap = {
      'all': 'Status',
      'active': 'Status (Active)',
      'deactivated': 'Status (Deactivated)'
    };
    const statusBtnLabel = statusLabelMap[currentStatus] || 'Status';

    // Applied Chips Calculation (Only status)
    const appliedPlanChips = [];
    if (currentStatus && currentStatus !== 'all') {
      appliedPlanChips.push({
        key: 'status',
        label: `Status: ${currentStatus === 'active' ? 'Active' : 'Deactivated'}`,
        type: 'status'
      });
    }

    this.innerHTML = `
      <div style="background: #ffffff; border: 1px solid var(--fl-border, #e2e8f0); border-radius: 12px; box-shadow: var(--fl-shadow-xs); overflow: visible;">
        
        <!-- Filter / Actions Toolbar (Matching Members & Payments) -->
        <div style="padding: 14px 20px; border-bottom: 1px solid var(--fl-border, #e2e8f0); display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px;">
          
          <!-- Left: Search Input + Status Filter -->
          <div style="display: flex; align-items: center; gap: 10px; flex-wrap: wrap; flex: 1;">
            
            <!-- Search Input -->
            <div style="position: relative; width: 220px;">
              <input 
                type="text" 
                id="fl-plan-search-input" 
                placeholder="Search plans..." 
                value="${state.searchQuery || ''}"
                style="width: 100%; height: 34px; padding: 0 10px 0 30px; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 13px; color: #0f172a; box-sizing: border-box; outline: none; background: #ffffff;"
              />
              <span style="position: absolute; left: 9px; top: 50%; transform: translateY(-50%); color: #94a3b8; display: flex; pointer-events: none;">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="11" cy="11" r="8"></circle>
                  <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                </svg>
              </span>
            </div>

            <!-- Status Dropdown (Same style as Members / Payments) -->
            <div style="position: relative;" class="fl-dropdown-container">
              <button 
                type="button" 
                class="fl-plan-filter-trigger" 
                data-dropdown="status" 
                style="height: 34px; padding: 0 12px; border: 1px solid ${currentStatus !== 'all' ? '#0f172a' : '#e2e8f0'}; border-radius: 6px; background: #ffffff; font-size: 13px; color: ${currentStatus !== 'all' ? '#0f172a' : '#475569'}; font-weight: ${currentStatus !== 'all' ? '600' : '500'}; cursor: pointer; display: flex; align-items: center; gap: 6px;"
              >
                <span>${statusBtnLabel}</span>
                <svg class="fl-filter-arrow" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#64748b" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" style="flex-shrink: 0; transition: transform 0.2s ease; transform: rotate(0deg);">
                  <polyline points="6 9 12 15 18 9"></polyline>
                </svg>
              </button>
              <div 
                class="fl-dropdown-panel" 
                id="fl-plan-dropdown-status" 
                style="display: none; position: absolute; top: calc(100% + 4px); left: 0; min-width: 180px; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 8px; box-shadow: 0 10px 15px -3px rgba(0,0,0,0.1), 0 4px 6px -4px rgba(0,0,0,0.05); z-index: 100; padding: 4px 0;"
              >
                <div class="fl-plan-drop-item" data-filter="status" data-val="all" style="padding: 7px 12px; font-size: 13px; color: #334155; cursor: pointer; display: flex; justify-content: space-between; align-items: center; ${currentStatus === 'all' ? 'font-weight: 700; background: #f8fafc;' : ''}">
                  <span>All Statuses (${totalPlansCount})</span>
                  ${currentStatus === 'all' ? '<span style="color: #0f172a;">✓</span>' : ''}
                </div>
                <div class="fl-plan-drop-item" data-filter="status" data-val="active" style="padding: 7px 12px; font-size: 13px; color: #334155; cursor: pointer; display: flex; justify-content: space-between; align-items: center; ${currentStatus === 'active' ? 'font-weight: 700; background: #f8fafc;' : ''}">
                  <span>Active (${activePlansCount})</span>
                  ${currentStatus === 'active' ? '<span style="color: #0f172a;">✓</span>' : ''}
                </div>
                <div class="fl-plan-drop-item" data-filter="status" data-val="deactivated" style="padding: 7px 12px; font-size: 13px; color: #334155; cursor: pointer; display: flex; justify-content: space-between; align-items: center; ${currentStatus === 'deactivated' ? 'font-weight: 700; background: #f8fafc;' : ''}">
                  <span>Deactivated (${deactivatedPlansCount})</span>
                  ${currentStatus === 'deactivated' ? '<span style="color: #0f172a;">✓</span>' : ''}
                </div>
              </div>
            </div>

          </div>

          <!-- Right: Refresh & Action -->
          <div style="display: flex; align-items: center; gap: 10px;">
            <button 
              type="button" 
              id="fl-refresh-plans-btn" 
              class="fl-btn fl-btn-secondary" 
              title="Refresh Plans from backend"
              style="
                height: 34px;
                padding: 0 12px;
                font-size: 13px;
                display: flex;
                align-items: center;
                gap: 6px;
                background: #ffffff;
                border: 1px solid #cbd5e1;
                border-radius: 6px;
                cursor: ${state.syncingGymData ? 'not-allowed' : 'pointer'};
                color: #334155;
                font-weight: 600;
              "
              ${state.syncingGymData ? 'disabled' : ''}
            >
              <span class="${state.syncingGymData ? 'fl-sync-spin' : ''}" style="display: inline-flex;">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.2"/>
                </svg>
              </span>
              <span>Refresh</span>
            </button>

            <button 
              type="button" 
              id="fl-create-plan-btn"
              class="fl-btn fl-btn-primary"
              style="
                height: 34px;
                padding: 0 14px;
                font-size: 13px;
              "
            >
              + Create Plan
            </button>
          </div>
        </div>

        <!-- Applied Filter Chips Row -->
        ${appliedPlanChips.length > 0 ? `
          <div style="padding: 10px 20px; border-bottom: 1px solid #f1f5f9; display: flex; align-items: center; gap: 10px; flex-wrap: wrap;">
            <span style="font-size: 11px; font-weight: 700; color: #475569; letter-spacing: 0.5px;">
              APPLIED FILTERS:
            </span>
            ${appliedPlanChips.map(c => `
              <span style="display: inline-flex; align-items: center; padding: 3px 6px 3px 12px; background: #f0f7ff; border: 1px solid #bfdbfe; border-radius: 9999px; font-size: 12px; color: #1e40af; font-weight: 500;">
                ${c.label} <span class="fl-plan-filter-remove-badge" data-type="${c.type}" style="display: inline-flex; align-items: center; justify-content: center; width: 14px; height: 14px; border-radius: 50%; background: #f07176; color: #ffffff; margin-left: 6px; cursor: pointer; flex-shrink: 0;"><svg width="7" height="7" viewBox="0 0 10 10" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="display: block; pointer-events: none;"><path d="M2 2L8 8M8 2L2 8"/></svg></span>
              </span>
            `).join('')}
            <button type="button" id="fl-clear-plan-filter-btn" style="border: none; background: transparent; color: #0f172a; font-size: 12px; font-weight: 600; text-decoration: underline; cursor: pointer; margin-left: 6px;">
              Clear All
            </button>
          </div>
        ` : ''}

        <!-- Plans Grid Body -->
        <div style="padding: 20px;">
          <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px;">
          ${loading.plans ? `
            <div style="grid-column: 1 / -1; padding: 48px; text-align: center; color: #64748b; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 12px;">
              Loading membership packages...
            </div>
          ` : filteredPlans.length === 0 ? `
            <div style="grid-column: 1 / -1; padding: 48px; text-align: center; color: #64748b; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 12px;">
              <div style="font-size: 32px; margin-bottom: 8px;">📦</div>
              <div style="font-size: 15px; font-weight: 700; color: #0f172a;">
                No membership plans found matching your criteria
              </div>
              <div style="font-size: 13px; color: #64748b; margin-top: 4px;">
                ${(state.searchQuery || appliedPlanChips.length > 0)
                  ? 'Try clearing or adjusting your search query or status filter.' 
                  : 'Create your first membership package to offer plans.'}
              </div>
              ${appliedPlanChips.length > 0 || state.searchQuery ? `
                <button type="button" id="fl-empty-reset-filters-btn" style="margin-top: 16px; height: 36px; padding: 0 16px; border: 1px solid #cbd5e1; border-radius: 8px; background: #ffffff; color: #0f172a; font-size: 13px; font-weight: 600; cursor: pointer;">
                  Reset Filters
                </button>
              ` : `
                <button type="button" id="fl-empty-create-plan-btn" style="margin-top: 16px; height: 36px; padding: 0 16px; border: none; border-radius: 8px; background: #0f172a; color: #ffffff; font-size: 13px; font-weight: 600; cursor: pointer;">
                  + Create Plan
                </button>
              `}
            </div>
          ` : filteredPlans.map(plan => {
            const isInactive = plan.isActive === false;
            const enrolled = planEnrollments[plan.id] || 0;
            const priceFormatted = AppConfig.formatCurrency(plan.price || plan.defaultPrice || 0);
            const durationText = (plan.durationDays === 1)
              ? '1 Day'
              : (plan.durationMonths && plan.durationMonths >= 1
                  ? `${plan.durationMonths} Month${plan.durationMonths > 1 ? 's' : ''}` 
                  : `${plan.durationDays || 30} Days`);

            return `
              <div style="
                background: #ffffff;
                border: 1px solid #e2e8f0;
                border-radius: 12px;
                padding: 20px;
                box-shadow: 0 1px 3px rgba(0,0,0,0.04);
                display: flex;
                flex-direction: column;
                justify-content: space-between;
                position: relative;
                transition: transform 0.15s ease, box-shadow 0.15s ease;
              " class="fl-plan-card">
                <div>
                  <!-- Card Header: Title & Status Badges -->
                  <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px; gap: 8px;">
                    <div style="flex: 1; min-width: 0;">
                      <h4 style="margin: 0; font-size: 16px; font-weight: 700; color: #0f172a; line-height: 1.3; word-break: break-word;">
                        ${plan.name}
                      </h4>
                    </div>
                    
                    <div style="display: flex; align-items: center; gap: 6px; flex-shrink: 0;">
                      ${isInactive ? `
                        <span style="background: #fee2e2; color: #dc2626; border: 1px solid #fecdd3; padding: 2px 8px; border-radius: 9999px; font-size: 10.5px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.3px;">
                          Deactivated
                        </span>
                      ` : `
                        <span style="background: #dcfce7; color: #166534; padding: 2px 8px; border-radius: 9999px; font-size: 10.5px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.3px;">
                          Active
                        </span>
                      `}
                      <span style="background: #f1f5f9; color: #475569; padding: 2px 8px; border-radius: 9999px; font-size: 11px; font-weight: 600; white-space: nowrap;">
                        ${durationText}
                      </span>
                    </div>
                  </div>
                  
                  <!-- Price Display -->
                  <div style="font-size: 24px; font-weight: 800; color: #0f172a; margin: 10px 0 6px 0;">
                    ${priceFormatted}
                  </div>

                  <!-- Description -->
                  <p style="font-size: 13px; color: #64748b; margin: 0 0 16px 0; line-height: 1.45; min-height: 38px;">
                    ${plan.description || 'Full access gym workout membership with standard equipment and facility usage.'}
                  </p>

                  <!-- Features / Highlights -->
                  <div style="border-top: 1px dashed #e2e8f0; padding-top: 12px; margin-bottom: 16px;">
                    <div style="font-size: 11px; font-weight: 700; text-transform: uppercase; color: #94a3b8; letter-spacing: 0.5px; margin-bottom: 6px;">
                      Plan Specifications
                    </div>
                    <div style="display: flex; flex-direction: column; gap: 4px; font-size: 12.5px; color: #475569;">
                      <span>⏱ <b>${plan.durationDays === 1 ? '1 Day' : `${plan.durationDays || 30} Days`}</b> validity</span>
                      <span>👥 <b>${enrolled}</b> enrolled subscriber${enrolled === 1 ? '' : 's'}</span>
                    </div>
                  </div>
                </div>

                <!-- Footer with Actions -->
                <div style="border-top: 1px solid #f1f5f9; padding-top: 14px; display: flex; justify-content: ${isInactive ? 'space-between' : 'flex-end'}; align-items: center; gap: 8px;">
                  <button 
                    type="button" 
                    class="fl-btn fl-btn-secondary fl-edit-plan-btn" 
                    data-plan-id="${plan.id}" 
                    title="Edit Plan"
                    style="
                      padding: 6px 14px;
                      font-size: 12.5px;
                    "
                  >
                    ${Icons.edit}
                    <span>Edit</span>
                  </button>

                  ${isInactive ? `
                    <button 
                      type="button" 
                      class="fl-btn fl-btn-primary fl-reactivate-plan-btn" 
                      data-plan-id="${plan.id}" 
                      title="Reactivate Plan"
                      style="
                        padding: 6px 14px;
                        font-size: 12.5px;
                      "
                    >
                      ${Icons.refresh}
                      <span>Reactivate</span>
                    </button>
                  ` : ''}
                </div>

              </div>
            `;
          }).join('')}
          </div>
        </div>

      </div>

      <style>
        .fl-plan-card:hover {
          transform: translateY(-2px);
          box-shadow: 0 4px 12px -2px rgba(0, 0, 0, 0.08);
        }
        .fl-edit-plan-btn:hover {
          background: #f8fafc !important;
          border-color: #94a3b8 !important;
        }
        .fl-reactivate-plan-btn:hover {
          background: #1e293b !important;
        }
        .fl-plan-drop-item:hover {
          background: #f1f5f9;
        }
        .fl-plan-filter-trigger:hover {
          border-color: #cbd5e1 !important;
        }
        .fl-plan-filter-trigger.fl-dropdown-open {
          border-color: #0f172a !important;
          color: #0f172a !important;
          font-weight: 600 !important;
        }
        .fl-plan-filter-trigger.fl-dropdown-open .fl-filter-arrow {
          transform: rotate(180deg) !important;
        }
      </style>
    `;

    this.bindEvents();

    if (isSearchFocused) {
      const nextInput = this.querySelector('#fl-plan-search-input');
      if (nextInput) {
        nextInput.focus();
        if (selStart !== null && selEnd !== null) {
          try {
            nextInput.setSelectionRange(selStart, selEnd);
          } catch (_) {}
        }
      }
    }
  }

  bindEvents() {
    // Search input
    const searchInput = this.querySelector('#fl-plan-search-input');
    if (searchInput) {
      searchInput.addEventListener('input', (e) => {
        HomeStore.setSearchQuery(e.target.value);
      });
    }

    // Dropdown toggles
    this.querySelectorAll('.fl-plan-filter-trigger').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const dropId = btn.getAttribute('data-dropdown');
        const panel = this.querySelector(`#fl-plan-dropdown-${dropId}`);
        const isShown = panel && panel.style.display === 'block';
        // Close all dropdowns, remove open class, and reset arrows
        this.querySelectorAll('.fl-dropdown-panel').forEach(p => p.style.display = 'none');
        this.querySelectorAll('.fl-plan-filter-trigger').forEach(b => b.classList.remove('fl-dropdown-open'));
        this.querySelectorAll('.fl-filter-arrow').forEach(a => a.style.transform = 'rotate(0deg)');
        if (panel && !isShown) {
          panel.style.display = 'block';
          btn.classList.add('fl-dropdown-open');
          const arrow = btn.querySelector('.fl-filter-arrow');
          if (arrow) arrow.style.transform = 'rotate(180deg)';
        }
      });
    });

    // Close on click outside
    if (!this.hasClickOutsideListener) {
      this.hasClickOutsideListener = true;
      document.addEventListener('click', () => {
        this.querySelectorAll('.fl-dropdown-panel').forEach(p => p.style.display = 'none');
        this.querySelectorAll('.fl-plan-filter-trigger').forEach(b => b.classList.remove('fl-dropdown-open'));
        this.querySelectorAll('.fl-filter-arrow').forEach(a => a.style.transform = 'rotate(0deg)');
      });
    }

    // Stop click bubbling on dropdown panels
    this.querySelectorAll('.fl-dropdown-panel').forEach(p => {
      p.addEventListener('click', (e) => {
        e.stopPropagation();
      });
    });

    // Dropdown item selection (Status)
    this.querySelectorAll('.fl-plan-drop-item').forEach(item => {
      item.addEventListener('click', (e) => {
        e.stopPropagation();
        const filterType = item.getAttribute('data-filter');
        const val = item.getAttribute('data-val');
        this.querySelectorAll('.fl-dropdown-panel').forEach(p => p.style.display = 'none');
        HomeStore.setPlanFilter(filterType, val);
      });
    });

    // Remove single applied filter chip
    this.querySelectorAll('.fl-plan-filter-remove-badge').forEach(badge => {
      badge.addEventListener('click', (e) => {
        e.stopPropagation();
        const type = badge.getAttribute('data-type');
        if (type === 'status') {
          HomeStore.setPlanFilter('status', 'all');
        }
      });
    });

    // Clear All filters button
    const clearAllBtn = this.querySelector('#fl-clear-plan-filter-btn');
    if (clearAllBtn) {
      clearAllBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        HomeStore.clearPlanFilters();
      });
    }

    // Reset filters in empty state
    const resetEmptyBtn = this.querySelector('#fl-empty-reset-filters-btn');
    if (resetEmptyBtn) {
      resetEmptyBtn.addEventListener('click', () => {
        HomeStore.clearPlanFilters();
        HomeStore.setSearchQuery('');
      });
    }

    // Create plan button
    const createBtn = this.querySelector('#fl-create-plan-btn');
    if (createBtn) {
      createBtn.addEventListener('click', () => {
        HomeStore.openPlanModal();
      });
    }

    // Refresh plans button
    const refreshBtn = this.querySelector('#fl-refresh-plans-btn');
    if (refreshBtn) {
      refreshBtn.addEventListener('click', () => {
        HomeStore.refreshGymData(true);
      });
    }

    const emptyCreateBtn = this.querySelector('#fl-empty-create-plan-btn');
    if (emptyCreateBtn) {
      emptyCreateBtn.addEventListener('click', () => {
        HomeStore.openPlanModal();
      });
    }

    // Edit plan buttons
    this.querySelectorAll('.fl-edit-plan-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const planId = btn.getAttribute('data-plan-id');
        const plan = HomeStore.getState().availablePlans.find(p => p.id === planId);
        if (plan) {
          HomeStore.openPlanModal(plan);
        }
      });
    });

    // Reactivate plan buttons
    this.querySelectorAll('.fl-reactivate-plan-btn').forEach(btn => {
      btn.addEventListener('click', async (e) => {
        e.stopPropagation();
        const planId = btn.getAttribute('data-plan-id');
        const plan = HomeStore.getState().availablePlans.find(p => p.id === planId);
        if (!plan) return;
        try {
          btn.disabled = true;
          btn.innerHTML = `<span>Reactivating...</span>`;
          await HomeStore.reactivatePlan(planId);
        } catch (err) {
          console.error('Reactivate plan error:', err);
          btn.disabled = false;
          btn.innerHTML = `${Icons.refresh} <span>Reactivate</span>`;
        }
      });
    });
  }
}

customElements.define('fl-plans-section', FlPlansSection);
