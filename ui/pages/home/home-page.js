/**
 * <fl-home-page> Native Web Component
 * Central Home Console Shell
 * Orchestrates the 4 modular sections (Members, Payments, Plans, Chats) mapped via section-registry.js.
 */

import { HomeStore } from './home-store.js';
import { getAllSections, getSection } from './section-registry.js';
import { AppConfig } from '../../core/config/app-config.js';
import '../../core/components/shell/fl-navbar.js';
import '../../core/components/stats/fl-stat-card.js';
import './modals/add-member-modal.js';
import './modals/renew-member-modal.js';
import './modals/collect-payment-modal.js';
import './modals/member-drawer.js';
import './modals/whatsapp-modal.js';
import './modals/plan-modal.js';
import { ChatStreamClient } from '../../core/chat/chat-stream-client.js';

export class FlHomePage extends HTMLElement {
  connectedCallback() {
    this.unsubscribe = HomeStore.subscribe(() => {
      const state = HomeStore.getState();
      if (state.addMemberModal?.isOpen || state.renewMemberModal?.isOpen || state.planModal?.isOpen || state.whatsAppModal?.isOpen || state.collectPaymentModal?.isOpen) {
        return;
      }
      const hash = `${state.activeSection}_${state.members.length}_${state.payments.length}_${state.availablePlans.length}_${state.loading.members}_${state.loading.payments}_${state.loading.plans}`;
      if (this.lastStateHash !== hash) {
        this.lastStateHash = hash;
        this.render();
      }
    });

    // Listen for hash changes to sync active section
    this.hashListener = () => this.syncFromHash();
    window.addEventListener('hashchange', this.hashListener);

    // Initial sync and data load (only if memory is empty)
    this.syncFromHash();
    const state = HomeStore.getState();
    if (!state.members || state.members.length === 0) {
      HomeStore.loadInitialData();
    }

    // Start global real-time chat SSE stream
    ChatStreamClient.init();

    this.render();
  }

  disconnectedCallback() {
    if (this.unsubscribe) this.unsubscribe();
    if (this.hashListener) window.removeEventListener('hashchange', this.hashListener);
    ChatStreamClient.destroy();
  }

  syncFromHash() {
    const hash = window.location.hash.replace(/^#\/?/, '').split('?')[0];
    const validSections = getAllSections().map(s => s.id);
    if (validSections.includes(hash)) {
      HomeStore.setActiveSection(hash);
    } else if (!hash || hash === 'home') {
      HomeStore.setActiveSection('members');
    }
  }

  navigateToSection(sectionId) {
    HomeStore.setActiveSection(sectionId);
    window.location.hash = `#/${sectionId}`;
  }

  async render() {
    const state = HomeStore.getState();
    const { activeSection } = state;
    const kpis = HomeStore.getKPIs();
    const sections = getAllSections();

    // Ensure active section component is loaded
    const sectionConfig = getSection(activeSection) || sections[0];
    if (sectionConfig && typeof sectionConfig.load === 'function') {
      try {
        await sectionConfig.load();
      } catch (err) {
        console.error('Failed to load section:', activeSection, err);
      }
    }

    const overdueTotal = kpis.overdueTotal || 0;
    const upcomingTotal = kpis.upcomingTotal || 0;
    const totalPending = kpis.totalPendingThisMonth ?? (kpis.totalPending || (overdueTotal + upcomingTotal));
    const sumTotal = (overdueTotal + upcomingTotal);
    const overduePct = sumTotal > 0 ? Math.round((overdueTotal / sumTotal) * 100) : 0;
    const upcomingPct = sumTotal > 0 ? (100 - overduePct) : 0;

    this.innerHTML = `
      <div style="height: 100vh; background: var(--fl-bg, #f8fafc); display: flex; flex-direction: column; overflow: ${activeSection === 'chats' ? 'hidden' : 'auto'};">
        <!-- Top Sticky App Navbar -->
        <fl-navbar active-tab="${activeSection}"></fl-navbar>

        <!-- Main Content Area (Bound to 1440px max-width and 24px padding to align with navbar logo and profile) -->
        <main style="max-width: 1440px; margin: 0 auto; padding: ${activeSection === 'chats' ? '12px 24px 16px 24px' : '24px 24px 120px 24px'}; width: 100%; box-sizing: border-box; flex: 1; display: flex; flex-direction: column; min-height: 0; overflow: ${activeSection === 'chats' ? 'hidden' : 'visible'};">
          
          <!-- Top Hero 5 KPI Pastel Stat Cards (Red, Green, Yellow, Grey, Blue) -->
          <div style="
            display: ${activeSection === 'chats' ? 'none' : 'grid'};
            grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
            gap: 16px;
            margin-bottom: 24px;
          ">
            <!-- Card 1: Red - Collections -->
            <div id="fl-kpi-pending" style="
              background: linear-gradient(135deg, #fff1f1 0%, #fde7e7 45%, #f9dbde 100%);
              border: 1px solid #fecdd3;
              border-radius: 12px;
              padding: 18px 20px;
              cursor: pointer;
              transition: transform 0.15s ease, box-shadow 0.15s ease;
              box-shadow: var(--fl-shadow-xs);
              display: flex;
              flex-direction: column;
              justify-content: space-between;
            " class="fl-kpi-card" title="Click to view all unpaid collections due this month">
              <div>
                <div style="display: flex; justify-content: space-between; align-items: flex-start;">
                  <span style="font-size: 26px; font-weight: 800; color: #0f172a; line-height: 1.1;">
                    ${AppConfig.formatCurrency(totalPending)}
                  </span>
                  <span style="font-size: 12px; font-weight: 600; color: #dc2626;">
                    Collections
                  </span>
                </div>
                <div style="font-size: 14px; font-weight: 700; color: #0f172a; margin-top: 6px;">
                  To be Collected
                </div>
                <div style="font-size: 12px; color: #64748b; margin-top: 2px;">
                  by the end of this month
                </div>
              </div>

              <!-- Split Progress Bar (Overdue vs Upcoming) -->
              <div style="margin-top: 14px; height: 5px; width: 100%; background: #fed7aa; border-radius: 9999px; overflow: hidden; display: flex;">
                <div style="width: ${overduePct}%; background: #dc2626; height: 100%; transition: width 0.3s ease;"></div>
                <div style="width: ${upcomingPct}%; background: #ea580c; height: 100%; transition: width 0.3s ease;"></div>
              </div>

              <div style="margin-top: 10px; display: flex; gap: 10px; flex-wrap: wrap; font-size: 11px; font-weight: 600;">
                <span style="color: #dc2626;">• Overdue: ${AppConfig.formatCurrency(overdueTotal)}</span>
                <span style="color: #ea580c;">• Upcoming: ${AppConfig.formatCurrency(upcomingTotal)}</span>
              </div>
            </div>

            <!-- Card 2: Green - Active Members -->
            <div id="fl-kpi-active" style="
              background: linear-gradient(135deg, #eefcf2 0%, #e2fceb 45%, #d4f8e4 100%);
              border: 1px solid #bbf7d0;
              border-radius: 12px;
              padding: 18px 20px;
              cursor: pointer;
              transition: transform 0.15s ease, box-shadow 0.15s ease;
              box-shadow: var(--fl-shadow-xs);
              display: flex;
              flex-direction: column;
              justify-content: space-between;
            " class="fl-kpi-card" title="Click to filter active members valid beyond this month">
              <div>
                <div style="display: flex; justify-content: space-between; align-items: flex-start;">
                  <span style="font-size: 26px; font-weight: 800; color: #0f172a; line-height: 1.1;">
                    ${kpis.activeMembersCount}
                  </span>
                  <span style="font-size: 12px; font-weight: 600; color: #15803d;">
                    Active
                  </span>
                </div>
                <div style="font-size: 14px; font-weight: 700; color: #0f172a; margin-top: 6px;">
                  Active Members
                </div>
                <div style="font-size: 12px; color: #64748b; margin-top: 2px;">
                  valid beyond this month
                </div>
              </div>
              <div style="margin-top: 16px;">
                <span style="font-size: 12px; font-weight: 600; color: #16a34a; cursor: pointer;">
                  Filter active members →
                </span>
              </div>
            </div>

            <!-- Card 3: Yellow - Members to Renew -->
            <div id="fl-kpi-renewals" style="
              background: linear-gradient(135deg, #fefbe8 0%, #fff6d7 45%, #faeeba 100%);
              border: 1px solid #fde68a;
              border-radius: 12px;
              padding: 18px 20px;
              cursor: pointer;
              transition: transform 0.15s ease, box-shadow 0.15s ease;
              box-shadow: var(--fl-shadow-xs);
              display: flex;
              flex-direction: column;
              justify-content: space-between;
            " class="fl-kpi-card" title="Click to filter members expiring this month">
              <div>
                <div style="display: flex; justify-content: space-between; align-items: flex-start;">
                  <span style="font-size: 26px; font-weight: 800; color: #0f172a; line-height: 1.1;">
                    ${kpis.renewalsThisMonthCount}
                  </span>
                  <span style="font-size: 12px; font-weight: 600; color: #b45309;">
                    Renewals
                  </span>
                </div>
                <div style="font-size: 14px; font-weight: 700; color: #0f172a; margin-top: 6px;">
                  Plans to Renew
                </div>
                <div style="font-size: 12px; color: #64748b; margin-top: 2px;">
                  by end of this month
                </div>
              </div>
              <div style="margin-top: 16px;">
                <span style="font-size: 12px; font-weight: 600; color: #d97706; cursor: pointer;">
                  View Expiring Plans →
                </span>
              </div>
            </div>

            <!-- Card 4: Grey - Expired Members -->
            <div id="fl-kpi-expired" style="
              background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 45%, #e2e8f0 100%);
              border: 1px solid #cbd5e1;
              border-radius: 12px;
              padding: 18px 20px;
              cursor: pointer;
              transition: transform 0.15s ease, box-shadow 0.15s ease;
              box-shadow: var(--fl-shadow-xs);
              display: flex;
              flex-direction: column;
              justify-content: space-between;
            " class="fl-kpi-card" title="Click to filter expired members">
              <div>
                <div style="display: flex; justify-content: space-between; align-items: flex-start;">
                  <span style="font-size: 26px; font-weight: 800; color: #0f172a; line-height: 1.1;">
                    ${kpis.expiredMembersCount ?? (kpis.expiredCount ?? 0)}
                  </span>
                  <span style="font-size: 12px; font-weight: 600; color: #475569;">
                    Expired
                  </span>
                </div>
                <div style="font-size: 14px; font-weight: 700; color: #0f172a; margin-top: 6px;">
                  Expired Members
                </div>
                <div style="font-size: 12px; color: #64748b; margin-top: 2px;">
                  to winback
                </div>
              </div>
              <div style="margin-top: 16px;">
                <span style="font-size: 12px; font-weight: 600; color: #475569; cursor: pointer;">
                  Show past members →
                </span>
              </div>
            </div>

            <!-- Card 5: Blue - Potential Members -->
            <div id="fl-kpi-potential" style="
              background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 45%, #bfdbfe 100%);
              border: 1px solid #93c5fd;
              border-radius: 12px;
              padding: 18px 20px;
              cursor: pointer;
              transition: transform 0.15s ease, box-shadow 0.15s ease;
              box-shadow: var(--fl-shadow-xs);
              display: flex;
              flex-direction: column;
              justify-content: space-between;
            " class="fl-kpi-card" title="Click to filter potential members who have never had a plan">
              <div>
                <div style="display: flex; justify-content: space-between; align-items: flex-start;">
                  <span style="font-size: 26px; font-weight: 800; color: #0f172a; line-height: 1.1;">
                    ${kpis.potentialMembersCount ?? 0}
                  </span>
                  <span style="font-size: 12px; font-weight: 600; color: #1d4ed8;">
                    Prospects
                  </span>
                </div>
                <div style="font-size: 14px; font-weight: 700; color: #0f172a; margin-top: 6px;">
                  Potential Members
                </div>
                <div style="font-size: 12px; color: #64748b; margin-top: 2px;">
                  to convert
                </div>
              </div>
              <div style="margin-top: 16px;">
                <span style="font-size: 12px; font-weight: 600; color: #2563eb; cursor: pointer;">
                  Show Potential Leads →
                </span>
              </div>
            </div>
          </div>

          <!-- Centered Section Underline Tabs (Matching Screenshot) -->
          <div style="
            position: relative;
            margin: ${activeSection === 'chats' ? '0 0 16px 0' : '10px 0 26px 0'};
            width: 100%;
          ">
            <!-- Tabs Row -->
            <div style="
              display: flex;
              justify-content: center;
              align-items: center;
              gap: 36px;
              position: relative;
              z-index: 2;
            ">
              <button 
                type="button" 
                class="fl-center-tab-btn ${activeSection === 'members' ? 'active' : ''}" 
                data-section="members"
                style="
                  background: transparent;
                  border: none;
                  border-bottom: ${activeSection === 'members' ? '2.5px solid #0f172a' : '2.5px solid transparent'};
                  padding: 8px 12px;
                  margin-bottom: -1px;
                  font-size: 15.5px;
                  font-weight: ${activeSection === 'members' ? '700' : '500'};
                  color: ${activeSection === 'members' ? '#0f172a' : '#64748b'};
                  cursor: pointer;
                  transition: all 0.15s ease;
                "
              >
                Members
              </button>
              <button 
                type="button" 
                class="fl-center-tab-btn ${activeSection === 'payments' ? 'active' : ''}" 
                data-section="payments"
                style="
                  background: transparent;
                  border: none;
                  border-bottom: ${activeSection === 'payments' ? '2.5px solid #0f172a' : '2.5px solid transparent'};
                  padding: 8px 12px;
                  margin-bottom: -1px;
                  font-size: 15.5px;
                  font-weight: ${activeSection === 'payments' ? '700' : '500'};
                  color: ${activeSection === 'payments' ? '#0f172a' : '#64748b'};
                  cursor: pointer;
                  transition: all 0.15s ease;
                "
              >
                Payments
              </button>
              <button 
                type="button" 
                class="fl-center-tab-btn ${activeSection === 'plans' ? 'active' : ''}" 
                data-section="plans"
                style="
                  background: transparent;
                  border: none;
                  border-bottom: ${activeSection === 'plans' ? '2.5px solid #0f172a' : '2.5px solid transparent'};
                  padding: 8px 12px;
                  margin-bottom: -1px;
                  font-size: 15.5px;
                  font-weight: ${activeSection === 'plans' ? '700' : '500'};
                  color: ${activeSection === 'plans' ? '#0f172a' : '#64748b'};
                  cursor: pointer;
                  transition: all 0.15s ease;
                "
              >
                Plans
              </button>
              <button 
                type="button" 
                class="fl-center-tab-btn ${activeSection === 'chats' ? 'active' : ''}" 
                data-section="chats"
                style="
                  background: transparent;
                  border: none;
                  border-bottom: ${activeSection === 'chats' ? '2.5px solid #0f172a' : '2.5px solid transparent'};
                  padding: 8px 12px;
                  margin-bottom: -1px;
                  font-size: 15.5px;
                  font-weight: ${activeSection === 'chats' ? '700' : '500'};
                  color: ${activeSection === 'chats' ? '#0f172a' : '#64748b'};
                  cursor: pointer;
                  transition: all 0.15s ease;
                "
              >
                Chats
              </button>
            </div>

            <!-- Grey baseline with gradient fading out from center to sides -->
            <div style="
              position: absolute;
              bottom: 0;
              left: 0;
              right: 0;
              height: 1px;
              background: linear-gradient(90deg, rgba(203, 213, 225, 0) 0%, rgba(203, 213, 225, 0.35) 15%, rgba(148, 163, 184, 0.75) 40%, rgba(148, 163, 184, 0.75) 60%, rgba(203, 213, 225, 0.35) 85%, rgba(203, 213, 225, 0) 100%);
              z-index: 1;
              pointer-events: none;
            "></div>
          </div>

          <!-- Dynamic Active Section Container -->
          <div id="fl-active-section-container" style="${activeSection === 'chats' ? 'flex: 1; display: flex; flex-direction: column; min-height: 0;' : ''}">
            <${sectionConfig.componentTag} style="${activeSection === 'chats' ? 'flex: 1; display: flex; flex-direction: column; min-height: 0;' : ''}"></${sectionConfig.componentTag}>
          </div>

        </main>

        <!-- Global Modals & Drawers Host -->
        <fl-member-drawer></fl-member-drawer>
        <fl-add-member-modal></fl-add-member-modal>
        <fl-renew-member-modal></fl-renew-member-modal>
        <fl-collect-payment-modal></fl-collect-payment-modal>
        <fl-whatsapp-modal></fl-whatsapp-modal>
        <fl-plan-modal></fl-plan-modal>
      </div>

      <style>
        .fl-kpi-card:hover {
          transform: translateY(-2px);
          box-shadow: var(--fl-shadow-md);
        }
        .fl-center-tab-btn:hover {
          color: #0f172a !important;
        }
      </style>
    `;

    this.bindEvents();
  }

  bindEvents() {
    // KPI Cards clicks
    this.querySelector('#fl-kpi-pending')?.addEventListener('click', () => {
      HomeStore.filterPaymentsToCollectThisMonth();
      this.navigateToSection('payments');
    });

    this.querySelector('#fl-kpi-active')?.addEventListener('click', () => {
      HomeStore.filterActiveMembersValidBeyondThisMonth();
      this.navigateToSection('members');
    });

    this.querySelector('#fl-kpi-renewals')?.addEventListener('click', () => {
      HomeStore.filterRenewalMembersExpiringThisMonth();
      this.navigateToSection('members');
    });

    this.querySelector('#fl-kpi-expired')?.addEventListener('click', () => {
      HomeStore.filterExpiredMembers();
      this.navigateToSection('members');
    });

    this.querySelector('#fl-kpi-potential')?.addEventListener('click', () => {
      HomeStore.filterPotentialMembers();
      this.navigateToSection('members');
    });

    // Center Underline Tabs clicks
    this.querySelectorAll('.fl-center-tab-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const targetSection = btn.getAttribute('data-section');
        this.navigateToSection(targetSection);
      });
    });
  }
}

customElements.define('fl-home-page', FlHomePage);
