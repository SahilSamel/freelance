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
import './modals/member-drawer.js';
import './modals/whatsapp-modal.js';

export class FlHomePage extends HTMLElement {
  connectedCallback() {
    this.unsubscribe = HomeStore.subscribe(() => this.render());

    // Listen for hash changes to sync active section
    this.hashListener = () => this.syncFromHash();
    window.addEventListener('hashchange', this.hashListener);

    // Initial sync and data load
    this.syncFromHash();
    HomeStore.loadInitialData();

    this.render();
  }

  disconnectedCallback() {
    if (this.unsubscribe) this.unsubscribe();
    if (this.hashListener) window.removeEventListener('hashchange', this.hashListener);
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

    this.innerHTML = `
      <div style="min-height: 100vh; background: var(--fl-bg, #f8fafc); display: flex; flex-direction: column;">
        <!-- Top Sticky App Navbar -->
        <fl-navbar active-tab="${activeSection}"></fl-navbar>

        <!-- Main Content Area -->
        <main style="max-width: 1440px; margin: 0 auto; padding: 24px; width: 100%; box-sizing: border-box; flex: 1;">
          
          <!-- Top Hero KPI Stat Cards -->
          <div style="
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
            gap: 16px;
            margin-bottom: 24px;
          ">
            <!-- Card 1: Active Members -->
            <div id="fl-kpi-active" style="
              background: #ffffff;
              border: 1px solid var(--fl-border);
              border-radius: 12px;
              padding: 16px 20px;
              cursor: pointer;
              transition: transform 0.15s ease, box-shadow 0.15s ease;
              box-shadow: var(--fl-shadow-xs);
            " class="fl-kpi-card">
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                <span style="font-size: 13px; font-weight: 600; color: var(--fl-text-secondary);">Active Members</span>
                <span style="font-size: 12px; font-weight: 700; color: #16a34a; background: #dcfce7; padding: 2px 8px; border-radius: 9999px;">
                  Active
                </span>
              </div>
              <div style="font-size: 28px; font-weight: 800; color: var(--fl-text);">
                ${kpis.activeMembersCount}
              </div>
              <div style="font-size: 12px; color: var(--fl-text-muted); margin-top: 4px;">
                Out of ${kpis.totalMembers} total registered
              </div>
            </div>

            <!-- Card 2: Renewals This Month -->
            <div id="fl-kpi-renewals" style="
              background: #ffffff;
              border: 1px solid var(--fl-border);
              border-radius: 12px;
              padding: 16px 20px;
              cursor: pointer;
              transition: transform 0.15s ease, box-shadow 0.15s ease;
              box-shadow: var(--fl-shadow-xs);
            " class="fl-kpi-card">
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                <span style="font-size: 13px; font-weight: 600; color: var(--fl-text-secondary);">Expiring Soon</span>
                <span style="font-size: 12px; font-weight: 700; color: #d97706; background: #fef3c7; padding: 2px 8px; border-radius: 9999px;">
                  Action Required
                </span>
              </div>
              <div style="font-size: 28px; font-weight: 800; color: #d97706;">
                ${kpis.renewalsThisMonthCount}
              </div>
              <div style="font-size: 12px; color: var(--fl-text-muted); margin-top: 4px;">
                Plans up for renewal this month
              </div>
            </div>

            <!-- Card 3: Pending Dues (Money Story) -->
            <div id="fl-kpi-pending" style="
              background: #ffffff;
              border: 1px solid var(--fl-border);
              border-radius: 12px;
              padding: 16px 20px;
              cursor: pointer;
              transition: transform 0.15s ease, box-shadow 0.15s ease;
              box-shadow: var(--fl-shadow-xs);
            " class="fl-kpi-card">
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                <span style="font-size: 13px; font-weight: 600; color: var(--fl-text-secondary);">Pending Revenue</span>
                <span style="font-size: 12px; font-weight: 700; color: #dc2626; background: #fee2e2; padding: 2px 8px; border-radius: 9999px;">
                  ${AppConfig.getCurrencySymbol()} Dues
                </span>
              </div>
              <div style="font-size: 28px; font-weight: 800; color: #dc2626;">
                ${AppConfig.formatCurrency(kpis.totalPendingThisMonth)}
              </div>
              <div style="font-size: 12px; color: var(--fl-text-muted); margin-top: 4px;">
                ${AppConfig.formatCurrency(kpis.overdueTotal)} overdue • ${AppConfig.formatCurrency(kpis.dueThisMonthTotal)} upcoming
              </div>
            </div>

            <!-- Card 4: Membership Plans -->
            <div id="fl-kpi-plans" style="
              background: #ffffff;
              border: 1px solid var(--fl-border);
              border-radius: 12px;
              padding: 16px 20px;
              cursor: pointer;
              transition: transform 0.15s ease, box-shadow 0.15s ease;
              box-shadow: var(--fl-shadow-xs);
            " class="fl-kpi-card">
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                <span style="font-size: 13px; font-weight: 600; color: var(--fl-text-secondary);">Active Packages</span>
                <span style="font-size: 12px; font-weight: 700; color: #2563eb; background: #dbeafe; padding: 2px 8px; border-radius: 9999px;">
                  Catalog
                </span>
              </div>
              <div style="font-size: 28px; font-weight: 800; color: var(--fl-text);">
                ${kpis.totalPlansCount} Plans
              </div>
              <div style="font-size: 12px; color: var(--fl-text-muted); margin-top: 4px;">
                ${kpis.enrolledMembersCount} members currently enrolled
              </div>
            </div>
          </div>

          <!-- Section Switcher Bar -->
          <div style="
            display: flex;
            align-items: center;
            gap: 6px;
            background: #ffffff;
            border: 1px solid var(--fl-border);
            border-radius: 12px;
            padding: 6px;
            margin-bottom: 20px;
            box-shadow: var(--fl-shadow-xs);
            overflow-x: auto;
          ">
            ${sections.map(s => {
              const isActive = s.id === activeSection;
              return `
                <button 
                  type="button" 
                  class="fl-section-nav-btn ${isActive ? 'active' : ''}" 
                  data-section="${s.id}"
                  style="
                    display: inline-flex;
                    align-items: center;
                    gap: 8px;
                    padding: 10px 20px;
                    border: none;
                    border-radius: 8px;
                    font-size: 14px;
                    font-weight: 600;
                    cursor: pointer;
                    transition: all 0.15s ease;
                    background: ${isActive ? 'var(--fl-primary, #dc2626)' : 'transparent'};
                    color: ${isActive ? '#ffffff' : 'var(--fl-text-secondary, #475569)'};
                  "
                >
                  ${s.label}
                </button>
              `;
            }).join('')}
          </div>

          <!-- Dynamic Active Section Container -->
          <div id="fl-active-section-container">
            <${sectionConfig.componentTag}></${sectionConfig.componentTag}>
          </div>

        </main>

        <!-- Global Modals & Drawers Host -->
        <fl-add-member-modal></fl-add-member-modal>
        <fl-member-drawer></fl-member-drawer>
        <fl-whatsapp-modal></fl-whatsapp-modal>
      </div>

      <style>
        .fl-kpi-card:hover {
          transform: translateY(-2px);
          box-shadow: var(--fl-shadow-md);
        }
        .fl-section-nav-btn:not(.active):hover {
          background: #f1f5f9;
          color: var(--fl-text);
        }
      </style>
    `;

    this.bindEvents();
  }

  bindEvents() {
    // KPI Cards clicks
    this.querySelector('#fl-kpi-active')?.addEventListener('click', () => {
      HomeStore.setMemberStatusTab('active');
      this.navigateToSection('members');
    });

    this.querySelector('#fl-kpi-renewals')?.addEventListener('click', () => {
      HomeStore.setMemberStatusTab('renewals');
      this.navigateToSection('members');
    });

    this.querySelector('#fl-kpi-pending')?.addEventListener('click', () => {
      HomeStore.setPaymentStatusTab('overdue');
      this.navigateToSection('payments');
    });

    this.querySelector('#fl-kpi-plans')?.addEventListener('click', () => {
      this.navigateToSection('plans');
    });

    // Section Switcher clicks
    this.querySelectorAll('.fl-section-nav-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const targetSection = btn.getAttribute('data-section');
        this.navigateToSection(targetSection);
      });
    });
  }
}

customElements.define('fl-home-page', FlHomePage);
