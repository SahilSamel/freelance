/**
 * <fl-members-page> Native Web Component
 * Complete FitPulse Members Console View.
 */

import { MockMembers, QuickTemplatesList } from '../../data/mock-data.js';
import { ErrorMapper } from '../../core/error/error-mapper.js';
import '../../core/components/shell/fl-navbar.js';
import '../../core/components/stats/fl-stat-card.js';
import '../../core/components/search/fl-search-bar.js';
import '../../core/components/table/fl-table.js';
import '../../core/components/floating/fl-bulk-action-bar.js';
import '../../core/components/drawer/fl-drawer.js';
import '../../core/components/feedback/fl-modal.js';
import '../../core/components/forms/fl-input.js';

export class FlMembersPage extends HTMLElement {
  constructor() {
    super();
    this.members = [...MockMembers];
    this.filteredMembers = [...MockMembers];
    this.activeFilter = { status: 'All', plan: 'Annual Gold', query: '' };
  }

  connectedCallback() {
    this.render();
    this.applyFilters();
  }

  applyFilters() {
    let result = [...this.members];

    if (this.activeFilter.query) {
      const q = this.activeFilter.query.toLowerCase();
      result = result.filter(m => 
        m.name.toLowerCase().includes(q) || 
        m.phone.toLowerCase().includes(q) || 
        m.email.toLowerCase().includes(q)
      );
    }

    if (this.activeFilter.status && this.activeFilter.status !== 'All') {
      result = result.filter(m => m.status.toLowerCase() === this.activeFilter.status.toLowerCase());
    }

    if (this.activeFilter.plan && this.activeFilter.plan !== 'All') {
      result = result.filter(m => m.plan.toLowerCase().includes(this.activeFilter.plan.toLowerCase()));
    }

    this.filteredMembers = result;
    const table = this.querySelector('fl-table');
    if (table) {
      table.setData(this.filteredMembers);
    }
  }

  render() {
    this.style.display = 'block';
    this.style.minHeight = '100vh';
    this.style.backgroundColor = 'var(--fl-bg, #f8fafc)';

    this.innerHTML = `
      <!-- App Shell Topbar -->
      <fl-navbar title="FitPulse" subtitle="GYM OPERATOR CONSOLE" active-tab="members"></fl-navbar>

      <!-- Main Page Container -->
      <main class="fl-page-container">
        <!-- KPI Stat Ribbon -->
        <fl-stat-ribbon>
          <fl-stat-card label="Active Members" value="184" variant="success" icon="users"></fl-stat-card>
          <fl-stat-card label="Expiring Soon" value="12" variant="warning" icon="warning"></fl-stat-card>
          <fl-stat-card label="Overdue Fees" value="5" variant="attention" icon="clock"></fl-stat-card>
          <fl-stat-card label="Expired" value="24" variant="danger" icon="userX"></fl-stat-card>
        </fl-stat-ribbon>

        <!-- Search & Filter Bar -->
        <div style="
          display: flex;
          align-items: center;
          justify-content: space-between;
          flex-wrap: wrap;
          gap: 12px;
          margin-bottom: 16px;
        ">
          <!-- Left: Search & Filter Pills -->
          <div style="display: flex; align-items: center; flex-wrap: wrap; gap: 10px; flex: 1;">
            <fl-search-bar placeholder="Search members by name or phone..."></fl-search-bar>
            <fl-filter-pill label="Status" value="All" options="All, Active, Expiring, Overdue, Expired"></fl-filter-pill>
            <fl-filter-pill label="Plan" value="Annual Gold" active options="All, Annual Gold, Quarterly Pro, Monthly Basic, Half-Yearly Elite"></fl-filter-pill>
            <fl-filter-pill label="Date" value="This Month" options="This Month, Last Month, This Year, All Time"></fl-filter-pill>
          </div>

          <!-- Right: Clear Filters Link -->
          <div>
            <button type="button" id="fl-clear-filters-btn" style="
              background: transparent;
              border: none;
              color: var(--fl-primary, #10b981);
              font-size: 13px;
              font-weight: 600;
              cursor: pointer;
            " onmouseover="this.style.textDecoration='underline'" onmouseout="this.style.textDecoration='none'">
              Clear All
            </button>
          </div>
        </div>

        <!-- Interactive Members Data Table -->
        <fl-table></fl-table>

        <!-- Floating Bottom Bulk Action Bar -->
        <fl-bulk-action-bar count="0"></fl-bulk-action-bar>

        <!-- Floating Action Button (+ Add Member) -->
        <fl-fab label="+ Add Member" fl-permission="USER_CREATE"></fl-fab>

        <!-- Slide-Over Member Profile Detail Drawer -->
        <fl-drawer title="Member Profile"></fl-drawer>

        <!-- Add Member Modal -->
        <fl-modal id="fl-add-member-modal" title="Add New Gym Member">
          <form id="fl-add-member-form">
            <fl-input label="Full Name" name="name" placeholder="e.g. Rahul Sen" required></fl-input>
            <fl-input label="Phone Number" name="phone" placeholder="+91 98765 00000" required></fl-input>
            <fl-input label="Email Address" name="email" type="email" placeholder="rahul.sen@example.com"></fl-input>
            <fl-input label="Membership Plan" name="plan" placeholder="e.g. Annual Gold" value="Annual Gold" required></fl-input>
            <fl-input label="Total Amount Paid (₹)" name="amount" type="number" placeholder="18500" value="18500" required></fl-input>
            <div style="display: flex; gap: 10px; margin-top: 20px;">
              <button type="button" id="fl-cancel-modal-btn" style="
                flex: 1;
                height: 38px;
                border: 1px solid var(--fl-border);
                background: #fff;
                color: var(--fl-text);
                border-radius: var(--fl-radius-md);
                font-weight: 600;
                cursor: pointer;
              ">Cancel</button>
              <button type="submit" style="
                flex: 1;
                height: 38px;
                border: none;
                background: var(--fl-primary);
                color: #fff;
                border-radius: var(--fl-radius-md);
                font-weight: 600;
                cursor: pointer;
              ">Save Member</button>
            </div>
          </form>
        </fl-modal>
      </main>
    `;

    // Hook up Event Listeners
    const table = this.querySelector('fl-table');
    const bulkBar = this.querySelector('fl-bulk-action-bar');
    const drawer = this.querySelector('fl-drawer');
    const fab = this.querySelector('fl-fab');
    const addModal = this.querySelector('#fl-add-member-modal');
    const addForm = this.querySelector('#fl-add-member-form');
    const cancelModalBtn = this.querySelector('#fl-cancel-modal-btn');
    const clearBtn = this.querySelector('#fl-clear-filters-btn');

    if (table) {
      table.setData(this.filteredMembers);
    }

    // Row selection for bulk actions
    this.addEventListener('fl-selection-change', (e) => {
      const count = e.detail.selectedIds.length;
      if (bulkBar) {
        bulkBar.setAttribute('count', count.toString());
      }
    });

    // Row Click: Open Slide-over Drawer
    this.addEventListener('fl-row-click', (e) => {
      if (drawer && e.detail.item) {
        drawer.setMember(e.detail.item);
      }
    });

    // Bulk WhatsApp Send Action
    this.addEventListener('fl-bulk-submit', (e) => {
      const { template, count } = e.detail;
      ErrorMapper.notifySuccess(
        `Sent '${template}' WhatsApp broadcast message to ${count} members!`,
        'WhatsApp Broadcast Sent'
      );
      if (table) table.clearSelection();
    });

    // Quick Chat from Row
    this.addEventListener('fl-quick-action', (e) => {
      if (e.detail.action === 'chat') {
        if (window.fl && window.fl.router) {
          window.fl.router.navigate('/chat');
        }
      }
    });

    // Drawer Actions
    this.addEventListener('fl-member-chat', (e) => {
      if (window.fl && window.fl.router) {
        window.fl.router.navigate('/chat');
      }
    });

    this.addEventListener('fl-member-renew', (e) => {
      ErrorMapper.notifySuccess(`Renewal payment link generated and sent to ${e.detail.member.name}!`, 'Plan Renewed');
    });

    // Search Input
    this.addEventListener('fl-search', (e) => {
      this.activeFilter.query = e.detail.query;
      this.applyFilters();
    });

    // Filter Change
    this.addEventListener('fl-filter-change', (e) => {
      const { label, value } = e.detail;
      if (label === 'Status') this.activeFilter.status = value;
      if (label === 'Plan') this.activeFilter.plan = value;
      this.applyFilters();
    });

    // Clear Filters
    if (clearBtn) {
      clearBtn.addEventListener('click', () => {
        this.activeFilter = { status: 'All', plan: 'All', query: '' };
        this.querySelectorAll('fl-filter-pill').forEach(pill => {
          pill.setAttribute('value', 'All');
          pill.removeAttribute('active');
        });
        const searchBar = this.querySelector('fl-search-bar');
        if (searchBar) searchBar.setValue('');
        this.applyFilters();
        ErrorMapper.notifyInfo('Filters have been reset.', 'Filters Reset');
      });
    }

    // Add Member Modal
    if (fab && addModal) {
      fab.addEventListener('click', () => addModal.open());
    }

    if (cancelModalBtn && addModal) {
      cancelModalBtn.addEventListener('click', () => addModal.close());
    }

    if (addForm && addModal) {
      addForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const nameInput = addForm.querySelector('[name="name"]');
        const phoneInput = addForm.querySelector('[name="phone"]');
        const emailInput = addForm.querySelector('[name="email"]');
        const planInput = addForm.querySelector('[name="plan"]');
        const amountInput = addForm.querySelector('[name="amount"]');

        const name = nameInput ? nameInput.getValue() : '';
        const phone = phoneInput ? phoneInput.getValue() : '';
        const email = emailInput ? emailInput.getValue() : '';
        const plan = planInput ? planInput.getValue() : 'Annual Gold';
        const amount = amountInput ? parseInt(amountInput.getValue(), 10) : 18500;

        const newMember = {
          id: `MEM-2024-${Math.floor(100 + Math.random() * 900)}`,
          name,
          phone,
          email,
          plan,
          totalPaid: amount,
          totalPaidFormatted: `₹${amount.toLocaleString('en-IN')}`,
          status: 'active',
          avatar: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=120',
          joinDate: 'Today',
          expiryDate: 'Next Year',
          paidAmount: `₹${amount.toLocaleString('en-IN')} Paid`,
          paymentMethod: 'via Cash / Desk',
          paymentDate: 'Today'
        };

        this.members.unshift(newMember);
        this.applyFilters();
        addModal.close();
        addForm.reset();
        ErrorMapper.notifySuccess(`Member ${name} added successfully!`, 'Member Enrolled');
      });
    }
  }
}

customElements.define('fl-members-page', FlMembersPage);
