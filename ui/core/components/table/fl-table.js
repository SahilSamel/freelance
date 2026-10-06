/**
 * <fl-table> Native Web Component
 * Interactive data table with row selection, custom cell renderers, and action triggers.
 */

import { Icons } from '../../../assets/icons/icons.js';
import { AppConfig } from '../../config/app-config.js';
import './fl-badge.js';
import './fl-dropdown-menu.js';
import '../shell/fl-avatar.js';

export class FlTable extends HTMLElement {
  constructor() {
    super();
    this.columns = [];
    this.data = [];
    this.selectedIds = new Set();
    this.isSelectable = true;
  }

  connectedCallback() {
    this.render();
  }

  setData(data = []) {
    this.data = data;
    this.render();
  }

  getSelected() {
    return this.data.filter(item => this.selectedIds.has(item.id));
  }

  clearSelection() {
    this.selectedIds.clear();
    this.render();
    this.dispatchSelection();
  }

  dispatchSelection() {
    this.dispatchEvent(new CustomEvent('fl-selection-change', {
      bubbles: true,
      detail: {
        selectedIds: Array.from(this.selectedIds),
        selectedRows: this.getSelected()
      }
    }));
  }

  render() {
    this.style.display = 'block';
    this.style.width = '100%';
    this.style.backgroundColor = '#ffffff';
    this.style.border = '1px solid var(--fl-border, #e2e8f0)';
    this.style.borderRadius = 'var(--fl-radius-lg, 12px)';
    this.style.overflow = 'hidden';
    this.style.boxShadow = 'var(--fl-shadow-xs)';

    const allSelected = this.data.length > 0 && this.data.every(d => this.selectedIds.has(d.id));

    this.innerHTML = `
      <div style="overflow-x: auto; width: 100%;">
        <table style="width: 100%; border-collapse: collapse; text-align: left; font-size: 13px;">
          <thead>
            <tr style="
              background-color: #fafbfc;
              border-bottom: 1px solid var(--fl-border, #e2e8f0);
              color: var(--fl-text-secondary, #475569);
              font-size: 11px;
              font-weight: 700;
              letter-spacing: 0.6px;
              text-transform: uppercase;
            ">
              <th style="padding: 14px 16px; width: 44px; text-align: center;">
                <input type="checkbox" id="fl-table-select-all" ${allSelected ? 'checked' : ''} style="
                  accent-color: var(--fl-primary, #dc2626);
                  width: 16px;
                  height: 16px;
                  cursor: pointer;
                " />
              </th>
              <th style="padding: 14px 16px;">Name</th>
              <th style="padding: 14px 16px;">Contact</th>
              <th style="padding: 14px 16px;">Plan</th>
              <th style="padding: 14px 16px;">Total Paid</th>
              <th style="padding: 14px 16px;">Status</th>
              <th style="padding: 14px 16px; text-align: right;">Actions</th>
            </tr>
          </thead>
          <tbody>
            ${this.data.length === 0 ? `
              <tr>
                <td colspan="7" style="padding: 48px; text-align: center; color: var(--fl-text-muted);">
                  No records found matching the current filter.
                </td>
              </tr>
            ` : this.data.map(row => {
              const isChecked = this.selectedIds.has(row.id);
              return `
                <tr data-id="${row.id}" class="fl-table-row" style="
                  border-bottom: 1px solid var(--fl-border-light, #f1f5f9);
                  background-color: ${isChecked ? 'var(--fl-status-active-bg, #eff6ff)' : 'transparent'};
                  transition: background-color var(--fl-transition-fast);
                  cursor: pointer;
                ">
                  <!-- Checkbox -->
                  <td style="padding: 14px 16px; width: 44px; text-align: center;" class="fl-td-checkbox">
                    <input type="checkbox" class="fl-row-checkbox" data-id="${row.id}" ${isChecked ? 'checked' : ''} style="
                      accent-color: var(--fl-primary, #dc2626);
                      width: 16px;
                      height: 16px;
                      cursor: pointer;
                    " />
                  </td>

                  <!-- Name -->
                  <td style="padding: 14px 16px;">
                    <div style="display: flex; align-items: center; gap: 12px;">
                      <fl-avatar size="34px" name="${row.name}" src="${row.avatar || ''}"></fl-avatar>
                      <span style="font-weight: 600; color: var(--fl-text, #0f172a);">
                        ${row.name}
                      </span>
                    </div>
                  </td>

                  <!-- Contact -->
                  <td style="padding: 14px 16px;">
                    <div style="display: flex; flex-direction: column;">
                      <span style="color: var(--fl-text, #0f172a); font-weight: 500;">
                        ${row.phone || '-'}
                      </span>
                      <span style="font-size: 12px; color: var(--fl-text-muted, #64748b);">
                        ${row.email || ''}
                      </span>
                    </div>
                  </td>

                  <!-- Plan -->
                  <td style="padding: 14px 16px; color: var(--fl-text, #0f172a); font-weight: 500;">
                    ${row.plan || 'Standard'}
                  </td>

                  <!-- Total Paid -->
                  <td style="padding: 14px 16px; font-weight: 600; color: var(--fl-text, #0f172a);">
                    ${row.totalPaidFormatted || (row.totalPaid !== undefined ? AppConfig.formatCurrency(row.totalPaid) : AppConfig.formatCurrency(0))}
                  </td>

                  <!-- Status -->
                  <td style="padding: 14px 16px;">
                    <fl-badge variant="${row.status}"></fl-badge>
                  </td>

                  <!-- Actions -->
                  <td style="padding: 14px 16px; text-align: right;" class="fl-td-actions">
                    <div style="display: inline-flex; align-items: center; gap: 4px;">
                      <button type="button" class="fl-quick-chat-btn" data-id="${row.id}" title="Send WhatsApp Message" style="
                        width: 32px;
                        height: 32px;
                        border-radius: var(--fl-radius-md, 8px);
                        border: none;
                        background: #f1f5f9;
                        color: #475569;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        cursor: pointer;
                        transition: all var(--fl-transition-fast);
                      " onmouseover="this.style.background='var(--fl-primary-light)'; this.style.color='var(--fl-primary)'" onmouseout="this.style.background='#f1f5f9'; this.style.color='#475569'">
                        ${Icons.tag}
                      </button>

                      <fl-dropdown-menu>
                        <div class="fl-kebab-item" data-action="view" data-id="${row.id}" style="padding: 8px 12px; font-size: 13px; color: var(--fl-text); cursor: pointer; border-radius: 4px;" onmouseover="this.style.background='#f1f5f9'" onmouseout="this.style.background='transparent'">
                          View Profile
                        </div>
                        <div class="fl-kebab-item" data-action="chat" data-id="${row.id}" style="padding: 8px 12px; font-size: 13px; color: var(--fl-text); cursor: pointer; border-radius: 4px;" onmouseover="this.style.background='#f1f5f9'" onmouseout="this.style.background='transparent'">
                          Open Chat
                        </div>
                        <div class="fl-kebab-item" data-action="renew" data-id="${row.id}" style="padding: 8px 12px; font-size: 13px; color: var(--fl-primary); font-weight: 500; cursor: pointer; border-radius: 4px;" onmouseover="this.style.background='#ecfdf5'" onmouseout="this.style.background='transparent'">
                          Renew Plan
                        </div>
                      </fl-dropdown-menu>
                    </div>
                  </td>
                </tr>
              `;
            }).join('')}
          </tbody>
        </table>
      </div>
    `;

    // Row hover & Click Listeners
    this.querySelectorAll('.fl-table-row').forEach(tr => {
      const id = tr.getAttribute('data-id');
      const item = this.data.find(d => d.id === id);

      tr.addEventListener('mouseenter', () => {
        if (!this.selectedIds.has(id)) {
          tr.style.backgroundColor = '#f8fafc';
        }
      });
      tr.addEventListener('mouseleave', () => {
        if (!this.selectedIds.has(id)) {
          tr.style.backgroundColor = 'transparent';
        }
      });

      tr.addEventListener('click', (e) => {
        // Prevent opening drawer if clicking checkbox, action button or dropdown
        if (e.target.closest('.fl-td-checkbox') || e.target.closest('.fl-td-actions') || e.target.closest('fl-dropdown-menu')) {
          return;
        }
        this.dispatchEvent(new CustomEvent('fl-row-click', { bubbles: true, detail: { item } }));
      });
    });

    // Select All Checkbox
    const selectAllBox = this.querySelector('#fl-table-select-all');
    if (selectAllBox) {
      selectAllBox.addEventListener('change', (e) => {
        if (e.target.checked) {
          this.data.forEach(d => this.selectedIds.add(d.id));
        } else {
          this.selectedIds.clear();
        }
        this.render();
        this.dispatchSelection();
      });
    }

    // Individual Row Checkboxes
    this.querySelectorAll('.fl-row-checkbox').forEach(cb => {
      cb.addEventListener('change', (e) => {
        e.stopPropagation();
        const id = cb.getAttribute('data-id');
        if (cb.checked) {
          this.selectedIds.add(id);
        } else {
          this.selectedIds.delete(id);
        }
        this.render();
        this.dispatchSelection();
      });
    });

    // Quick Action Buttons
    this.querySelectorAll('.fl-quick-chat-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const id = btn.getAttribute('data-id');
        const item = this.data.find(d => d.id === id);
        this.dispatchEvent(new CustomEvent('fl-quick-action', { bubbles: true, detail: { action: 'chat', item } }));
      });
    });

    // Kebab Menu Action Items
    this.querySelectorAll('.fl-kebab-item').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const action = btn.getAttribute('data-action');
        const id = btn.getAttribute('data-id');
        const item = this.data.find(d => d.id === id);
        this.dispatchEvent(new CustomEvent('fl-row-action', { bubbles: true, detail: { action, item } }));
      });
    });
  }
}

customElements.define('fl-table', FlTable);
