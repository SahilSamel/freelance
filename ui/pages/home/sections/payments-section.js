/**
 * <fl-payments-section> Native Web Component
 * Payments & Installments section: tracking dues, upcoming installments, and overdue balances.
 */

import { HomeStore } from '../home-store.js';
import { Icons } from '../../../assets/icons/icons.js';
import '../../../core/components/table/fl-badge.js';

export class FlPaymentsSection extends HTMLElement {
  constructor() {
    super();
    this.page = 1;
    this.pageSize = 10;
  }

  connectedCallback() {
    this.unsubscribe = HomeStore.subscribe(() => this.render());
    this.render();
  }

  disconnectedCallback() {
    if (this.unsubscribe) this.unsubscribe();
  }

  exportToCsv(data) {
    if (!data || data.length === 0) return;
    const headers = ['Member', 'Phone', 'Amount', 'Due Date', 'Paid Date', 'Status', 'Method'];
    const rows = data.map(p => [
      `"${p.memberName || ''}"`,
      `"${p.phone || ''}"`,
      p.amount || 0,
      p.dueDate || '',
      p.paidDate || '',
      p.statusLabel || p.status || '',
      p.paymentMethod || ''
    ]);

    const csvContent = 'data:text/csv;charset=utf-8,' + [headers.join(','), ...rows.map(e => e.join(','))].join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `payments_export_${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    link.remove();
  }

  render() {
    const state = HomeStore.getState();
    const { payments, paymentStatusTab, loading } = state;
    const filteredPayments = HomeStore.getFilteredPayments();

    // Counts for tabs
    const allCount = payments.length;
    const paidCount = payments.filter(p => p.status === 'paid').length;
    const upcomingCount = payments.filter(p => p.status === 'upcoming').length;
    const overdueCount = payments.filter(p => p.status === 'overdue').length;

    // Pagination calculations
    const totalCount = filteredPayments.length;
    const totalPages = Math.max(1, Math.ceil(totalCount / this.pageSize));
    if (this.page > totalPages) this.page = totalPages;
    const startIndex = (this.page - 1) * this.pageSize;
    const pageData = filteredPayments.slice(startIndex, startIndex + this.pageSize);

    this.innerHTML = `
      <div style="display: flex; flex-direction: column; gap: 16px;">
        
        <!-- Controls Bar -->
        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px;">
          
          <!-- Status Tabs -->
          <div style="display: flex; gap: 8px; flex-wrap: wrap;">
            <button type="button" class="fl-pay-pill ${paymentStatusTab === 'all' ? 'active' : ''}" data-tab="all">
              All Payments <span class="fl-pill-count">${allCount}</span>
            </button>
            <button type="button" class="fl-pay-pill ${paymentStatusTab === 'paid' ? 'active' : ''}" data-tab="paid">
              Paid <span class="fl-pill-count">${paidCount}</span>
            </button>
            <button type="button" class="fl-pay-pill ${paymentStatusTab === 'upcoming' ? 'active' : ''}" data-tab="upcoming">
              Upcoming <span class="fl-pill-count">${upcomingCount}</span>
            </button>
            <button type="button" class="fl-pay-pill ${paymentStatusTab === 'overdue' ? 'active' : ''}" data-tab="overdue">
              Overdue <span class="fl-pill-count">${overdueCount}</span>
            </button>
          </div>

          <!-- Search & Export -->
          <div style="display: flex; gap: 10px; align-items: center;">
            <div style="position: relative; width: 260px;">
              <input 
                type="text" 
                id="fl-payment-search-input" 
                placeholder="Search member, txn, note..." 
                value="${state.searchQuery || ''}"
                style="width: 100%; height: 36px; padding: 0 12px 0 32px; border: 1px solid var(--fl-border); border-radius: 8px; font-size: 13px;"
              />
              <span style="position: absolute; left: 10px; top: 9px; color: var(--fl-text-muted);">
                ${Icons.search || '🔍'}
              </span>
            </div>

            <button type="button" id="fl-export-pay-csv-btn" style="
              height: 36px;
              padding: 0 14px;
              border: 1px solid var(--fl-border);
              border-radius: 8px;
              background: #ffffff;
              font-size: 13px;
              font-weight: 600;
              color: var(--fl-text);
              cursor: pointer;
              display: flex;
              align-items: center;
              gap: 6px;
            ">
              Export CSV
            </button>
          </div>

        </div>

        <!-- Payments Table -->
        <div style="background: #ffffff; border: 1px solid var(--fl-border); border-radius: 12px; overflow: hidden; box-shadow: var(--fl-shadow-sm);">
          <div style="overflow-x: auto;">
            <table style="width: 100%; border-collapse: collapse; text-align: left; font-size: 13.5px;">
              <thead>
                <tr style="background: #f8fafc; border-bottom: 1px solid var(--fl-border); color: var(--fl-text-secondary); font-size: 12px; text-transform: uppercase; font-weight: 700; letter-spacing: 0.5px;">
                  <th style="padding: 12px 16px;">Member</th>
                  <th style="padding: 12px 16px;">Amount</th>
                  <th style="padding: 12px 16px;">Due Date</th>
                  <th style="padding: 12px 16px;">Paid Date</th>
                  <th style="padding: 12px 16px;">Status</th>
                  <th style="padding: 12px 16px;">Method</th>
                  <th style="padding: 12px 16px;">Notes / Item</th>
                  <th style="padding: 12px 16px; text-align: right;">Action</th>
                </tr>
              </thead>
              <tbody>
                ${loading.members ? `
                  <tr>
                    <td colspan="8" style="padding: 40px; text-align: center; color: var(--fl-text-muted);">
                      Loading payments ledger...
                    </td>
                  </tr>
                ` : pageData.length === 0 ? `
                  <tr>
                    <td colspan="8" style="padding: 48px; text-align: center; color: var(--fl-text-muted);">
                      <div style="font-size: 24px; margin-bottom: 8px;">💳</div>
                      <div style="font-weight: 600; color: var(--fl-text);">No payment records found</div>
                      <div style="font-size: 12.5px; margin-top: 4px;">No records match your selected status tab.</div>
                    </td>
                  </tr>
                ` : pageData.map(p => {
                  const isPaid = p.status === 'paid';
                  const isOverdue = p.status === 'overdue';
                  let badgeVariant = 'success';
                  if (isOverdue) badgeVariant = 'danger';
                  else if (p.status === 'upcoming') badgeVariant = 'warning';

                  return `
                    <tr style="border-bottom: 1px solid var(--fl-border); transition: background 0.15s;">
                      <td style="padding: 12px 16px;">
                        <div style="font-weight: 600; color: var(--fl-text);">${p.memberName}</div>
                        <div style="font-size: 12px; color: var(--fl-text-muted);">${p.phone}</div>
                      </td>
                      <td style="padding: 12px 16px; font-weight: 700; color: ${isPaid ? '#16a34a' : (isOverdue ? '#dc2626' : '#d97706')};">
                        ${p.amountFormatted}
                      </td>
                      <td style="padding: 12px 16px; color: var(--fl-text-secondary); font-size: 12.5px;">
                        ${p.dueDate}
                      </td>
                      <td style="padding: 12px 16px; color: var(--fl-text-secondary); font-size: 12.5px;">
                        ${p.paidDate}
                      </td>
                      <td style="padding: 12px 16px;">
                        <fl-badge variant="${badgeVariant}" text="${p.statusLabel || p.status}"></fl-badge>
                      </td>
                      <td style="padding: 12px 16px; font-size: 12.5px; color: var(--fl-text-secondary);">
                        ${p.paymentMethod || 'UPI'}
                      </td>
                      <td style="padding: 12px 16px; font-size: 12.5px; color: var(--fl-text-secondary); max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                        ${p.notes || 'Installment'}
                      </td>
                      <td style="padding: 12px 16px; text-align: right;">
                        ${!isPaid ? `
                          <button type="button" class="fl-payment-remind-btn" data-id="${p.memberId}" data-amount="${p.amount}" title="Send Overdue WhatsApp Reminder" style="
                            padding: 6px 12px;
                            background: #25d366;
                            color: #ffffff;
                            border: none;
                            border-radius: 6px;
                            font-size: 12px;
                            font-weight: 600;
                            cursor: pointer;
                          ">
                            💬 Remind
                          </button>
                        ` : `
                          <span style="font-size: 12px; color: #16a34a; font-weight: 600;">✓ Settled</span>
                        `}
                      </td>
                    </tr>
                  `;
                }).join('')}
              </tbody>
            </table>
          </div>

          <!-- Pagination Bar -->
          <div style="padding: 12px 16px; border-top: 1px solid var(--fl-border); display: flex; justify-content: space-between; align-items: center; font-size: 13px; color: var(--fl-text-secondary);">
            <span>
              Showing ${totalCount === 0 ? 0 : startIndex + 1} to ${Math.min(startIndex + this.pageSize, totalCount)} of ${totalCount} records
            </span>
            <div style="display: flex; gap: 6px;">
              <button type="button" id="fl-pay-prev-btn" ${this.page <= 1 ? 'disabled' : ''} style="padding: 6px 12px; border: 1px solid var(--fl-border); border-radius: 6px; background: #ffffff; cursor: ${this.page <= 1 ? 'not-allowed' : 'pointer'}; opacity: ${this.page <= 1 ? '0.5' : '1'};">
                Previous
              </button>
              <span style="display: flex; align-items: center; padding: 0 8px; font-weight: 600;">Page ${this.page} of ${totalPages}</span>
              <button type="button" id="fl-pay-next-btn" ${this.page >= totalPages ? 'disabled' : ''} style="padding: 6px 12px; border: 1px solid var(--fl-border); border-radius: 6px; background: #ffffff; cursor: ${this.page >= totalPages ? 'not-allowed' : 'pointer'}; opacity: ${this.page >= totalPages ? '0.5' : '1'};">
                Next
              </button>
            </div>
          </div>
        </div>

      </div>

      <style>
        .fl-pay-pill {
          padding: 8px 14px;
          border-radius: 8px;
          border: 1px solid var(--fl-border, #e2e8f0);
          background: #ffffff;
          font-size: 13px;
          font-weight: 600;
          color: var(--fl-text-secondary, #475569);
          cursor: pointer;
          display: inline-flex;
          align-items: center;
          gap: 6px;
          transition: all 0.15s ease;
        }
        .fl-pay-pill.active {
          background: var(--fl-primary, #dc2626);
          color: #ffffff;
          border-color: var(--fl-primary, #dc2626);
        }
        .fl-pay-pill .fl-pill-count {
          background: rgba(0,0,0,0.06);
          padding: 1px 6px;
          border-radius: 9999px;
          font-size: 11px;
        }
        .fl-pay-pill.active .fl-pill-count {
          background: rgba(255,255,255,0.25);
          color: #ffffff;
        }
      </style>
    `;

    this.bindEvents();
  }

  bindEvents() {
    // Tab pill clicks
    this.querySelectorAll('.fl-pay-pill').forEach(btn => {
      btn.addEventListener('click', () => {
        HomeStore.setPaymentStatusTab(btn.getAttribute('data-tab'));
      });
    });

    // Search input
    const searchInput = this.querySelector('#fl-payment-search-input');
    if (searchInput) {
      searchInput.addEventListener('input', (e) => {
        HomeStore.setSearchQuery(e.target.value);
      });
    }

    // Export CSV
    const exportBtn = this.querySelector('#fl-export-pay-csv-btn');
    if (exportBtn) {
      exportBtn.addEventListener('click', () => {
        this.exportToCsv(HomeStore.getFilteredPayments());
      });
    }

    // Remind buttons
    this.querySelectorAll('.fl-payment-remind-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const memberId = btn.getAttribute('data-id');
        const amount = btn.getAttribute('data-amount');
        const member = HomeStore.getState().members.find(m => m.id === memberId || m.userId === memberId);
        if (member) {
          HomeStore.openWhatsAppModal(member, 'PAYMENT_OVERDUE', { amount });
        }
      });
    });

    // Pagination buttons
    const prevBtn = this.querySelector('#fl-pay-prev-btn');
    if (prevBtn) {
      prevBtn.addEventListener('click', () => {
        if (this.page > 1) {
          this.page--;
          this.render();
        }
      });
    }

    const nextBtn = this.querySelector('#fl-pay-next-btn');
    if (nextBtn) {
      nextBtn.addEventListener('click', () => {
        this.page++;
        this.render();
      });
    }
  }
}

customElements.define('fl-payments-section', FlPaymentsSection);
