/**
 * <fl-members-section> Native Web Component
 * Members management section: tabular view, filters, search, multi-selection, and pagination.
 */

import { HomeStore } from '../home-store.js';
import { Icons } from '../../../assets/icons/icons.js';
import { AppConfig } from '../../../core/config/app-config.js';
import '../../../core/components/table/fl-badge.js';

export class FlMembersSection extends HTMLElement {
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
    const headers = ['Name', 'Phone', 'Plan', 'Status', 'Expiry Date'];
    const rows = data.map(m => [
      `"${m.name || ''}"`,
      `"${m.phone || ''}"`,
      `"${m.plans || 'None'}"`,
      m.statusLabel || m.status || '',
      m.expiryDate || ''
    ]);

    const csvContent = 'data:text/csv;charset=utf-8,' + [headers.join(','), ...rows.map(e => e.join(','))].join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `members_export_${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    link.remove();
  }

  render() {
    const state = HomeStore.getState();
    const { members, memberStatusTab, selectedMemberIds, loading } = state;
    const filteredMembers = HomeStore.getFilteredMembers();

    // Counts for tabs
    const allCount = members.length;
    const activeCount = members.filter(m => m.status === 'active').length;
    const renewalsCount = members.filter(m => m.status === 'renewals').length;
    const expiredCount = members.filter(m => m.status === 'expired').length;

    // Pagination calculations
    const totalCount = filteredMembers.length;
    const totalPages = Math.max(1, Math.ceil(totalCount / this.pageSize));
    if (this.page > totalPages) this.page = totalPages;
    const startIndex = (this.page - 1) * this.pageSize;
    const pageData = filteredMembers.slice(startIndex, startIndex + this.pageSize);

    const isAllSelected = pageData.length > 0 && pageData.every(m => selectedMemberIds.has(m.id));

    this.innerHTML = `
      <div style="display: flex; flex-direction: column; gap: 16px;">
        
        <!-- Controls Bar: Filter Tabs & Actions -->
        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px;">
          
          <!-- Status Tabs -->
          <div style="display: flex; gap: 8px; flex-wrap: wrap;">
            <button type="button" class="fl-tab-pill ${memberStatusTab === 'all' ? 'active' : ''}" data-tab="all">
              All Members <span class="fl-pill-count">${allCount}</span>
            </button>
            <button type="button" class="fl-tab-pill ${memberStatusTab === 'active' ? 'active' : ''}" data-tab="active">
              Active <span class="fl-pill-count">${activeCount}</span>
            </button>
            <button type="button" class="fl-tab-pill ${memberStatusTab === 'renewals' ? 'active' : ''}" data-tab="renewals">
              Expiring Soon <span class="fl-pill-count">${renewalsCount}</span>
            </button>
            <button type="button" class="fl-tab-pill ${memberStatusTab === 'expired' ? 'active' : ''}" data-tab="expired">
              Expired <span class="fl-pill-count">${expiredCount}</span>
            </button>
          </div>

          <!-- Search & Add Member -->
          <div style="display: flex; gap: 10px; align-items: center;">
            <div style="position: relative; width: 240px;">
              <input 
                type="text" 
                id="fl-member-search-input" 
                placeholder="Search member, phone..." 
                value="${state.searchQuery || ''}"
                style="width: 100%; height: 36px; padding: 0 12px 0 32px; border: 1px solid var(--fl-border); border-radius: 8px; font-size: 13px;"
              />
              <span style="position: absolute; left: 10px; top: 9px; color: var(--fl-text-muted);">
                ${Icons.search || '🔍'}
              </span>
            </div>

            <button type="button" id="fl-export-csv-btn" style="
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

            <button type="button" id="fl-open-add-member-btn" style="
              height: 36px;
              padding: 0 16px;
              border: none;
              border-radius: 8px;
              background: var(--fl-primary, #dc2626);
              color: #ffffff;
              font-size: 13px;
              font-weight: 600;
              cursor: pointer;
              display: flex;
              align-items: center;
              gap: 6px;
            ">
              + Add Member
            </button>
          </div>

        </div>

        <!-- Selected Actions Toolbar (Appears when items are checked) -->
        ${selectedMemberIds.size > 0 ? `
          <div style="background: #eff6ff; border: 1px solid #bfdbfe; border-radius: 8px; padding: 10px 16px; display: flex; justify-content: space-between; align-items: center;">
            <span style="font-size: 13px; font-weight: 600; color: #1e40af;">
              ${selectedMemberIds.size} member${selectedMemberIds.size > 1 ? 's' : ''} selected
            </span>
            <div style="display: flex; gap: 8px;">
              <button type="button" id="fl-bulk-export-btn" style="padding: 6px 12px; border: 1px solid #93c5fd; background: #ffffff; border-radius: 6px; font-size: 12px; font-weight: 600; color: #1e40af; cursor: pointer;">
                Export Selected
              </button>
              <button type="button" id="fl-bulk-clear-btn" style="padding: 6px 12px; border: none; background: transparent; font-size: 12px; font-weight: 600; color: #64748b; cursor: pointer;">
                Clear Selection
              </button>
            </div>
          </div>
        ` : ''}

        <!-- Members Data Table -->
        <div style="background: #ffffff; border: 1px solid var(--fl-border); border-radius: 12px; overflow: hidden; box-shadow: var(--fl-shadow-sm);">
          <div style="overflow-x: auto;">
            <table style="width: 100%; border-collapse: collapse; text-align: left; font-size: 13.5px;">
              <thead>
                <tr style="background: #f8fafc; border-bottom: 1px solid var(--fl-border); color: var(--fl-text-secondary); font-size: 12px; text-transform: uppercase; font-weight: 700; letter-spacing: 0.5px;">
                  <th style="padding: 12px 16px; width: 40px; text-align: center;">
                    <input type="checkbox" id="fl-select-all-checkbox" ${isAllSelected ? 'checked' : ''} style="cursor: pointer;" />
                  </th>
                  <th style="padding: 12px 16px;">Member</th>
                  <th style="padding: 12px 16px;">Status</th>
                  <th style="padding: 12px 16px;">Assigned Plan</th>
                  <th style="padding: 12px 16px;">Pending Dues</th>
                  <th style="padding: 12px 16px;">Total Paid</th>
                  <th style="padding: 12px 16px;">Expiry Date</th>
                  <th style="padding: 12px 16px; text-align: right;">Actions</th>
                </tr>
              </thead>
              <tbody>
                ${loading.members ? `
                  <tr>
                    <td colspan="8" style="padding: 40px; text-align: center; color: var(--fl-text-muted);">
                      Loading members database...
                    </td>
                  </tr>
                ` : pageData.length === 0 ? `
                  <tr>
                    <td colspan="8" style="padding: 48px; text-align: center; color: var(--fl-text-muted);">
                      <div style="font-size: 24px; margin-bottom: 8px;">👥</div>
                      <div style="font-weight: 600; color: var(--fl-text);">No members found</div>
                      <div style="font-size: 12.5px; margin-top: 4px;">Try adjusting your search query or status filter.</div>
                    </td>
                  </tr>
                ` : pageData.map(m => {
                  const isChecked = selectedMemberIds.has(m.id);
                  const isExpired = m.status === 'expired';
                  const isRenewals = m.status === 'renewals';
                  let badgeVariant = 'success';
                  if (isExpired) badgeVariant = 'danger';
                  else if (isRenewals) badgeVariant = 'warning';

                  return `
                    <tr class="fl-table-row" data-id="${m.id}" style="border-bottom: 1px solid var(--fl-border); transition: background 0.15s; cursor: pointer; ${isChecked ? 'background: #f8fafc;' : ''}">
                      <td style="padding: 12px 16px; text-align: center;" onclick="event.stopPropagation();">
                        <input type="checkbox" class="fl-row-checkbox" data-id="${m.id}" ${isChecked ? 'checked' : ''} style="cursor: pointer;" />
                      </td>
                      <td style="padding: 12px 16px;">
                        <div style="display: flex; align-items: center; gap: 12px;">
                          <div style="width: 36px; height: 36px; border-radius: 50%; background: #fee2e2; color: #dc2626; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 13px;">
                            ${(m.name || 'M').charAt(0).toUpperCase()}
                          </div>
                          <div>
                            <div style="font-weight: 600; color: var(--fl-text);">${m.name}</div>
                            <div style="font-size: 12px; color: var(--fl-text-muted);">${m.phone}</div>
                          </div>
                        </div>
                      </td>
                      <td style="padding: 12px 16px;">
                        <fl-badge variant="${badgeVariant}" text="${m.statusLabel || m.status}"></fl-badge>
                      </td>
                      <td style="padding: 12px 16px; color: var(--fl-text-secondary); font-size: 13px;">
                        ${m.plans || 'None'}
                      </td>
                      <td style="padding: 12px 16px;">
                        <span style="font-weight: 700; color: ${m.pendingAmount > 0 ? 'var(--fl-danger, #dc2626)' : '#16a34a'};">
                          ${AppConfig.formatCurrency(m.pendingAmount || 0)}
                        </span>
                      </td>
                      <td style="padding: 12px 16px; font-weight: 600; color: var(--fl-text);">
                        ${AppConfig.formatCurrency(m.totalSpent || 0)}
                      </td>
                      <td style="padding: 12px 16px; color: var(--fl-text-secondary); font-size: 12.5px;">
                        ${HomeStore.formatDateDisplay(m.expiryDate)}
                      </td>
                      <td style="padding: 12px 16px; text-align: right;" onclick="event.stopPropagation();">
                        <div style="display: flex; justify-content: flex-end; gap: 6px;">
                          <button type="button" class="fl-action-wa-btn" data-id="${m.id}" title="Send WhatsApp Message" style="
                            padding: 6px 10px;
                            background: #25d366;
                            color: #ffffff;
                            border: none;
                            border-radius: 6px;
                            font-size: 12px;
                            font-weight: 600;
                            cursor: pointer;
                          ">
                            💬 Chat
                          </button>
                          <button type="button" class="fl-action-view-btn" data-id="${m.id}" title="View Details" style="
                            padding: 6px 10px;
                            background: #f1f5f9;
                            color: var(--fl-text);
                            border: 1px solid var(--fl-border);
                            border-radius: 6px;
                            font-size: 12px;
                            font-weight: 600;
                            cursor: pointer;
                          ">
                            View
                          </button>
                        </div>
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
              Showing ${totalCount === 0 ? 0 : startIndex + 1} to ${Math.min(startIndex + this.pageSize, totalCount)} of ${totalCount} members
            </span>
            <div style="display: flex; gap: 6px;">
              <button type="button" id="fl-prev-page-btn" ${this.page <= 1 ? 'disabled' : ''} style="padding: 6px 12px; border: 1px solid var(--fl-border); border-radius: 6px; background: #ffffff; cursor: ${this.page <= 1 ? 'not-allowed' : 'pointer'}; opacity: ${this.page <= 1 ? '0.5' : '1'};">
                Previous
              </button>
              <span style="display: flex; align-items: center; padding: 0 8px; font-weight: 600;">Page ${this.page} of ${totalPages}</span>
              <button type="button" id="fl-next-page-btn" ${this.page >= totalPages ? 'disabled' : ''} style="padding: 6px 12px; border: 1px solid var(--fl-border); border-radius: 6px; background: #ffffff; cursor: ${this.page >= totalPages ? 'not-allowed' : 'pointer'}; opacity: ${this.page >= totalPages ? '0.5' : '1'};">
                Next
              </button>
            </div>
          </div>
        </div>

      </div>

      <style>
        .fl-tab-pill {
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
        .fl-tab-pill.active {
          background: var(--fl-primary, #dc2626);
          color: #ffffff;
          border-color: var(--fl-primary, #dc2626);
        }
        .fl-tab-pill .fl-pill-count {
          background: rgba(0,0,0,0.06);
          padding: 1px 6px;
          border-radius: 9999px;
          font-size: 11px;
        }
        .fl-tab-pill.active .fl-pill-count {
          background: rgba(255,255,255,0.25);
          color: #ffffff;
        }
        .fl-table-row:hover {
          background: #f8fafc;
        }
      </style>
    `;

    this.bindEvents(pageData);
  }

  bindEvents(pageData) {
    // Tab pill clicks
    this.querySelectorAll('.fl-tab-pill').forEach(btn => {
      btn.addEventListener('click', () => {
        HomeStore.setMemberStatusTab(btn.getAttribute('data-tab'));
      });
    });

    // Search input
    const searchInput = this.querySelector('#fl-member-search-input');
    if (searchInput) {
      searchInput.addEventListener('input', (e) => {
        HomeStore.setSearchQuery(e.target.value);
      });
    }

    // Add member button
    const addBtn = this.querySelector('#fl-open-add-member-btn');
    if (addBtn) {
      addBtn.addEventListener('click', () => {
        HomeStore.openAddMemberModal();
      });
    }

    // Export CSV
    const exportBtn = this.querySelector('#fl-export-csv-btn');
    if (exportBtn) {
      exportBtn.addEventListener('click', () => {
        this.exportToCsv(HomeStore.getFilteredMembers());
      });
    }

    // Bulk export
    const bulkExportBtn = this.querySelector('#fl-bulk-export-btn');
    if (bulkExportBtn) {
      bulkExportBtn.addEventListener('click', () => {
        const state = HomeStore.getState();
        const selected = state.members.filter(m => state.selectedMemberIds.has(m.id));
        this.exportToCsv(selected);
      });
    }

    // Bulk clear
    const bulkClearBtn = this.querySelector('#fl-bulk-clear-btn');
    if (bulkClearBtn) {
      bulkClearBtn.addEventListener('click', () => {
        HomeStore.clearMemberSelection();
      });
    }

    // Select all checkbox
    const selectAll = this.querySelector('#fl-select-all-checkbox');
    if (selectAll) {
      selectAll.addEventListener('change', (e) => {
        if (e.target.checked) {
          HomeStore.selectAllMembers(pageData.map(m => m.id));
        } else {
          HomeStore.clearMemberSelection();
        }
      });
    }

    // Row checkboxes
    this.querySelectorAll('.fl-row-checkbox').forEach(cb => {
      cb.addEventListener('change', () => {
        HomeStore.toggleMemberSelection(cb.getAttribute('data-id'));
      });
    });

    // Row clicks -> open drawer
    this.querySelectorAll('.fl-table-row').forEach(row => {
      row.addEventListener('click', () => {
        const id = row.getAttribute('data-id');
        const member = HomeStore.getState().members.find(m => m.id === id);
        if (member) HomeStore.openMemberDrawer(member);
      });
    });

    // Action buttons
    this.querySelectorAll('.fl-action-view-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const id = btn.getAttribute('data-id');
        const member = HomeStore.getState().members.find(m => m.id === id);
        if (member) HomeStore.openMemberDrawer(member);
      });
    });

    this.querySelectorAll('.fl-action-wa-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const id = btn.getAttribute('data-id');
        const member = HomeStore.getState().members.find(m => m.id === id);
        if (member) HomeStore.openWhatsAppModal(member);
      });
    });

    // Pagination buttons
    const prevBtn = this.querySelector('#fl-prev-page-btn');
    if (prevBtn) {
      prevBtn.addEventListener('click', () => {
        if (this.page > 1) {
          this.page--;
          this.render();
        }
      });
    }

    const nextBtn = this.querySelector('#fl-next-page-btn');
    if (nextBtn) {
      nextBtn.addEventListener('click', () => {
        this.page++;
        this.render();
      });
    }
  }
}

customElements.define('fl-members-section', FlMembersSection);
