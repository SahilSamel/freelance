/**
 * <fl-members-section> Native Web Component
 * Members management section: tabular view, filters, search, multi-selection, and pagination.
 * Exact 100% pixel-faithful match to the user's theme.
 */

import { HomeStore } from '../home-store.js';
import { Icons } from '../../../assets/icons/icons.js';
import { AppConfig } from '../../../core/config/app-config.js';
function formatShortDate(dStr) {
  if (!dStr) return '';
  try {
    const parts = dStr.split('-');
    if (parts.length === 3) {
      const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
      const mIdx = parseInt(parts[1], 10) - 1;
      const day = parseInt(parts[2], 10);
      const year = parts[0];
      return `${day} ${months[mIdx]} ${year}`;
    }
    const d = new Date(dStr);
    return isNaN(d.getTime()) ? dStr : d.toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' });
  } catch {
    return dStr;
  }
}

function formatRangeLabel(from, to, defaultName) {
  if (from && to) {
    if (from === to) return `${defaultName} (${formatShortDate(from)})`;
    const fParts = from.split('-');
    const tParts = to.split('-');
    const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    if (fParts[0] === tParts[0] && fParts[1] === tParts[1]) {
      const mName = months[parseInt(fParts[1], 10) - 1];
      return `${defaultName} (${parseInt(fParts[2], 10)} – ${parseInt(tParts[2], 10)} ${mName})`;
    }
    return `${defaultName} (${formatShortDate(from)} – ${formatShortDate(to)})`;
  }
  if (to) {
    return `${defaultName} (≤ ${formatShortDate(to)})`;
  }
  if (from) {
    return `${defaultName} (≥ ${formatShortDate(from)})`;
  }
  return defaultName;
}

function formatChipLabel(fieldLabel, from, to) {
  if (from && to) {
    if (from === to) return `${fieldLabel}: ${formatShortDate(from)}`;
    return `${fieldLabel}: ${formatShortDate(from)} – ${formatShortDate(to)}`;
  }
  if (to) {
    return `${fieldLabel}: ≤ ${formatShortDate(to)}`;
  }
  if (from) {
    return `${fieldLabel}: ≥ ${formatShortDate(from)}`;
  }
  return '';
}

function getPresetDates() {
  const now = new Date();
  now.setHours(0, 0, 0, 0);
  const pad = (n) => String(n).padStart(2, '0');
  const toIso = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

  const todayStr = toIso(now);
  
  const yesterday = new Date(now);
  yesterday.setDate(now.getDate() - 1);
  const yesterdayStr = toIso(yesterday);

  const plus7 = new Date(now);
  plus7.setDate(now.getDate() + 7);
  const plus7Str = toIso(plus7);

  const plus30 = new Date(now);
  plus30.setDate(now.getDate() + 30);
  const plus30Str = toIso(plus30);

  const firstDayThisMonth = new Date(now.getFullYear(), now.getMonth(), 1);
  const firstDayThisMonthStr = toIso(firstDayThisMonth);

  const lastDayThisMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0);
  const lastDayThisMonthStr = toIso(lastDayThisMonth);

  return {
    todayStr,
    yesterdayStr,
    plus7Str,
    plus30Str,
    firstDayThisMonthStr,
    lastDayThisMonthStr
  };
}

export class FlMembersSection extends HTMLElement {
  constructor() {
    super();
    this.page = 1;
    this.pageSize = 13;
  }

  connectedCallback() {
    this.unsubscribe = HomeStore.subscribe(() => this.render());
    this.render();
  }

  renderExpiryColumn(member, memberFilters) {
    const subs = member.subscriptions || [];
    if (!subs.length) {
      return '<span style="color: #64748b;">—</span>';
    }

    let matchingSubs = subs;

    // Check if an expiry date filter is active
    if (memberFilters) {
      const from = memberFilters.expiryFrom;
      const to = memberFilters.expiryTo;

      if (from || to) {
        const filtered = subs.filter(s => {
          if (!s.endDate) return false;
          const sDate = s.endDate.slice(0, 10);
          if (from && sDate < from) return false;
          if (to && sDate > to) return false;
          return true;
        });
        if (filtered.length > 0) {
          matchingSubs = filtered;
        }
      } else if (memberFilters.expiry && memberFilters.expiry !== 'all') {
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        const filtered = subs.filter(s => {
          if (!s.endDate) return false;
          const exp = new Date(s.endDate);
          exp.setHours(0, 0, 0, 0);
          const daysLeft = Math.ceil((exp.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
          if (memberFilters.expiry === '7d') return daysLeft >= 0 && daysLeft <= 7;
          if (memberFilters.expiry === '30d') return daysLeft >= 0 && daysLeft <= 30;
          if (memberFilters.expiry === 'expired') return daysLeft < 0;
          if (memberFilters.expiry === 'future') return daysLeft > 30;
          return true;
        });
        if (filtered.length > 0) {
          matchingSubs = filtered;
        }
      }
    }

    // Sort matching subscriptions by end date ascending (soonest first)
    const sorted = [...matchingSubs].sort((a, b) => {
      const dateA = a.endDate ? a.endDate.slice(0, 10) : '9999-99-99';
      const dateB = b.endDate ? b.endDate.slice(0, 10) : '9999-99-99';
      return dateA.localeCompare(dateB);
    });

    return sorted.map(s => {
      const d = s.endDate ? s.endDate.slice(0, 10) : '—';
      const p = s.planName || 'Plan';
      return `<div style="line-height: 1.45; white-space: nowrap;">${d} (${p})</div>`;
    }).join('');
  }

  exportToCsv(data) {
    if (!data || data.length === 0) return;
    const headers = ['Name', 'Phone', 'Plan', 'Status', 'Expiry Date', 'Pending Dues'];
    const rows = data.map(m => [
      `"${m.name || ''}"`,
      `"${m.phone || ''}"`,
      `"${m.plans || 'None'}"`,
      m.statusLabel || m.status || '',
      m.expiryDate || '',
      m.pendingAmount || 0
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
    try {
      const activeEl = document.activeElement;
    const isSearchFocused = !!(activeEl && (activeEl.id === 'fl-member-search-input' || activeEl === this.querySelector('#fl-member-search-input')));
    const selStart = isSearchFocused ? activeEl.selectionStart : null;
    const selEnd = isSearchFocused ? activeEl.selectionEnd : null;

    const state = HomeStore.getState();
    const { selectedMemberIds, loading } = state;
    const filteredMembers = HomeStore.getFilteredMembers();

    // Pagination calculations
    const totalCount = filteredMembers.length;
    const totalPages = Math.max(1, Math.ceil(totalCount / this.pageSize));
    if (this.page > totalPages) this.page = totalPages;
    const startIndex = (this.page - 1) * this.pageSize;
    const pageData = filteredMembers.slice(startIndex, startIndex + this.pageSize);

    const isAllSelected = pageData.length > 0 && pageData.every(m => selectedMemberIds.has(m.id));

    const memberFilters = state.memberFilters || {
      plan: 'all',
      pendingAmount: 'all',
      pendingAmountMin: null,
      pendingAmountMax: null,
      status: 'all',
      expiry: 'all'
    };

    const memberSort = state.memberSort || { field: null, direction: 'asc' };
    const getSortIndicator = (f) => {
      if (memberSort.field !== f) {
        return `<span style="color: #cbd5e1; font-size: 11px; margin-left: 4px; user-select: none;">⇅</span>`;
      }
      return memberSort.direction === 'asc'
        ? `<span style="color: #0f172a; font-weight: 700; font-size: 12px; margin-left: 4px; user-select: none;">↑</span>`
        : `<span style="color: #0f172a; font-weight: 700; font-size: 12px; margin-left: 4px; user-select: none;">↓</span>`;
    };

    const planBtnLabel = memberFilters.plan !== 'all' ? `Plan (${memberFilters.plan.length > 12 ? memberFilters.plan.slice(0, 10) + '...' : memberFilters.plan})` : 'Plans';
    
    const hasExpFilter = !!(memberFilters.expiryFrom || memberFilters.expiryTo);
    const expLabelMap = { 'all': 'Expiry Date', '7d': 'Expiry (≤ 7d)', '30d': 'Expiry (≤ 30d)', 'expired': 'Expiry (Expired)', 'future': 'Expiry (> 30d)' };
    const expBtnLabel = hasExpFilter
      ? formatRangeLabel(memberFilters.expiryFrom, memberFilters.expiryTo, 'Expiry Date')
      : (memberFilters.expiry && memberFilters.expiry !== 'all' ? (expLabelMap[memberFilters.expiry] || 'Expiry Date') : 'Expiry Date');

    // Applied chips calculation
    const appliedMemberChips = [];
    if (memberFilters.noPlanOnly) {
      appliedMemberChips.push({ key: 'noplan', label: 'Potential Members (No Plan)', type: 'noplan' });
    }
    if (memberFilters.plan && memberFilters.plan !== 'all') {
      appliedMemberChips.push({ key: 'plan', label: `Plan: ${memberFilters.plan}`, type: 'plan' });
    }
    if (hasExpFilter) {
      appliedMemberChips.push({
        key: 'expiry',
        label: formatChipLabel('Expiry', memberFilters.expiryFrom, memberFilters.expiryTo),
        type: 'expiry'
      });
    } else if (memberFilters.expiry && memberFilters.expiry !== 'all') {
      const expLabels = { '7d': 'Expiry: Within 7d', '30d': 'Expiry: This Month', 'expired': 'Expiry: Expired', 'future': 'Expiry: Beyond Month' };
      appliedMemberChips.push({ key: 'expiry', label: expLabels[memberFilters.expiry], type: 'expiry' });
    }

    const uniquePlans = [...new Set((state.availablePlans || []).map(p => p.name).filter(Boolean))];

    this.innerHTML = `
      <div style="background: #ffffff; border: 1px solid var(--fl-border, #e2e8f0); border-radius: 12px; box-shadow: var(--fl-shadow-xs); overflow: visible;">
        
        <!-- Filter Toolbar -->
        <div style="padding: 14px 20px; border-bottom: 1px solid var(--fl-border, #e2e8f0); display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px;">
          <!-- Left Filters -->
          <div style="display: flex; align-items: center; gap: 10px; flex-wrap: wrap; flex: 1;">
            <!-- Search Input -->
            <div style="position: relative; width: 240px;">
              <input 
                type="text" 
                id="fl-member-search-input" 
                placeholder="Search name or contact..." 
                value="${state.searchQuery || ''}"
                style="width: 100%; height: 34px; padding: 0 10px 0 30px; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 13px; color: #0f172a; box-sizing: border-box;"
              />
              <span style="position: absolute; left: 9px; top: 50%; transform: translateY(-50%); color: #94a3b8; display: flex; pointer-events: none;">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="11" cy="11" r="8"></circle>
                  <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                </svg>
              </span>
            </div>

            <!-- Plans Dropdown -->
            <div style="position: relative;" class="fl-dropdown-container">
              <button type="button" class="fl-member-filter-trigger" data-dropdown="plans" style="height: 34px; padding: 0 12px; border: 1px solid ${memberFilters.plan !== 'all' ? '#0f172a' : '#e2e8f0'}; border-radius: 6px; background: #ffffff; font-size: 13px; color: ${memberFilters.plan !== 'all' ? '#0f172a' : '#475569'}; font-weight: ${memberFilters.plan !== 'all' ? '600' : '500'}; cursor: pointer; display: flex; align-items: center; gap: 6px;">
                <span>${planBtnLabel}</span>
                <svg class="fl-filter-arrow" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#64748b" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" style="flex-shrink: 0; transition: transform 0.2s ease; transform: rotate(0deg);">
                  <polyline points="6 9 12 15 18 9"></polyline>
                </svg>
              </button>
              <div class="fl-dropdown-panel" id="fl-member-dropdown-plans" style="display: none; position: absolute; top: calc(100% + 4px); left: 0; min-width: 215px; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 8px; box-shadow: 0 10px 15px -3px rgba(0,0,0,0.1), 0 4px 6px -4px rgba(0,0,0,0.05); z-index: 100; padding: 4px 0; max-height: 250px; overflow-y: auto;">
                <div class="fl-member-drop-item" data-filter="plan" data-val="all" style="padding: 7px 12px; font-size: 13px; color: #334155; cursor: pointer; display: flex; justify-content: space-between; align-items: center; ${memberFilters.plan === 'all' ? 'font-weight: 700; background: #f8fafc;' : ''}">
                  <span>All Plans</span>
                  ${memberFilters.plan === 'all' ? '<span style="color: #0f172a;">✓</span>' : ''}
                </div>
                ${uniquePlans.map(pn => `
                  <div class="fl-member-drop-item" data-filter="plan" data-val="${pn}" style="padding: 7px 12px; font-size: 13px; color: #334155; cursor: pointer; display: flex; justify-content: space-between; align-items: center; ${memberFilters.plan === pn ? 'font-weight: 700; background: #f8fafc;' : ''}">
                    <span>${pn}</span>
                    ${memberFilters.plan === pn ? '<span style="color: #0f172a;">✓</span>' : ''}
                  </div>
                `).join('')}
              </div>
            </div>

            <!-- Expiry Date Dropdown -->
            <div style="position: relative;" class="fl-dropdown-container">
              <button type="button" class="fl-member-filter-trigger" data-dropdown="expiry" style="height: 34px; padding: 0 12px; border: 1px solid ${hasExpFilter ? '#0f172a' : '#e2e8f0'}; border-radius: 6px; background: #ffffff; font-size: 13px; color: ${hasExpFilter ? '#0f172a' : '#475569'}; font-weight: ${hasExpFilter ? '600' : '500'}; cursor: pointer; display: flex; align-items: center; gap: 6px;">
                <span>${expBtnLabel}</span>
                <svg class="fl-filter-arrow" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#64748b" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" style="flex-shrink: 0; transition: transform 0.2s ease; transform: rotate(0deg);">
                  <polyline points="6 9 12 15 18 9"></polyline>
                </svg>
              </button>
              <div class="fl-dropdown-panel" id="fl-member-dropdown-expiry" style="display: none; position: absolute; top: calc(100% + 4px); left: 0; width: 285px; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 8px; box-shadow: 0 10px 15px -3px rgba(0,0,0,0.1), 0 4px 6px -4px rgba(0,0,0,0.05); z-index: 100; padding: 14px 16px; box-sizing: border-box;">
                <div style="font-size: 12px; font-weight: 700; color: #1e293b; margin-bottom: 8px; display: flex; justify-content: space-between; align-items: center;">
                  <span>Expiry Date Range</span>
                  <span style="font-size: 11px; font-weight: 500; color: #64748b;">Presets</span>
                </div>
                <div style="margin-bottom: 12px; display: flex; flex-wrap: wrap; gap: 6px;">
                  <button type="button" class="fl-member-date-preset" data-preset="next-7d" style="padding: 3px 8px; border: 1px solid #e2e8f0; background: #f8fafc; border-radius: 4px; font-size: 11px; font-weight: 500; color: #334155; cursor: pointer;">Within 7d</button>
                  <button type="button" class="fl-member-date-preset" data-preset="this-month" style="padding: 3px 8px; border: 1px solid #e2e8f0; background: #f8fafc; border-radius: 4px; font-size: 11px; font-weight: 500; color: #334155; cursor: pointer;">This Month</button>
                  <button type="button" class="fl-member-date-preset" data-preset="next-30d" style="padding: 3px 8px; border: 1px solid #e2e8f0; background: #f8fafc; border-radius: 4px; font-size: 11px; font-weight: 500; color: #334155; cursor: pointer;">Next 30d</button>
                  <button type="button" class="fl-member-date-preset" data-preset="expired" style="padding: 3px 8px; border: 1px solid #e2e8f0; background: #f8fafc; border-radius: 4px; font-size: 11px; font-weight: 500; color: #334155; cursor: pointer;">Expired</button>
                </div>
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 14px;">
                  <div>
                    <label style="display: block; font-size: 11px; font-weight: 600; color: #475569; margin-bottom: 4px;">From</label>
                    <input type="date" id="fl-member-expiry-from" value="${memberFilters.expiryFrom || ''}" style="width: 100%; height: 32px; padding: 2px 6px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 12px; color: #0f172a; box-sizing: border-box; outline: none; background: #ffffff;" />
                  </div>
                  <div>
                    <label style="display: block; font-size: 11px; font-weight: 600; color: #475569; margin-bottom: 4px;">To</label>
                    <input type="date" id="fl-member-expiry-to" value="${memberFilters.expiryTo || ''}" style="width: 100%; height: 32px; padding: 2px 6px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 12px; color: #0f172a; box-sizing: border-box; outline: none; background: #ffffff;" />
                  </div>
                </div>
                <div style="display: flex; justify-content: flex-end; align-items: center; gap: 8px; border-top: 1px solid #f1f5f9; padding-top: 10px;">
                  <button type="button" id="fl-member-expiry-clear" class="fl-btn fl-btn-secondary" style="padding: 5px 12px; font-size: 12px;">Clear</button>
                  <button type="button" id="fl-member-expiry-apply" class="fl-btn fl-btn-primary" style="padding: 5px 14px; font-size: 12px;">Apply</button>
                </div>
              </div>
            </div>
          </div>

          <!-- Right: Refresh & + Add Member -->
          <div style="display: flex; align-items: center; gap: 10px;">
            <button 
              type="button" 
              id="fl-refresh-members-btn" 
              class="fl-btn fl-btn-secondary" 
              title="Refresh Members from backend"
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

            <button type="button" id="fl-open-add-member-btn" class="fl-btn fl-btn-primary" style="
              height: 34px;
              padding: 0 14px;
              font-size: 13px;
            ">
              + Add Member
            </button>
          </div>
        </div>

        <!-- Applied Filter Chips Row -->
        ${appliedMemberChips.length > 0 ? `
          <div style="padding: 10px 20px; border-bottom: 1px solid #f1f5f9; display: flex; align-items: center; gap: 10px; flex-wrap: wrap;">
            <span style="font-size: 11px; font-weight: 700; color: #475569; letter-spacing: 0.5px;">
              APPLIED FILTERS:
            </span>
            ${appliedMemberChips.map(c => `
              <span style="display: inline-flex; align-items: center; padding: 3px 6px 3px 12px; background: #f0f7ff; border: 1px solid #bfdbfe; border-radius: 9999px; font-size: 12px; color: #1e40af; font-weight: 500;">
                ${c.label} <span class="fl-member-filter-remove-badge" data-type="${c.type}" style="display: inline-flex; align-items: center; justify-content: center; width: 14px; height: 14px; border-radius: 50%; background: #f07176; color: #ffffff; margin-left: 6px; cursor: pointer; flex-shrink: 0;"><svg width="7" height="7" viewBox="0 0 10 10" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="display: block; pointer-events: none;"><path d="M2 2L8 8M8 2L2 8"/></svg></span>
              </span>
            `).join('')}
            <button type="button" id="fl-clear-member-filter-btn" style="border: none; background: transparent; color: #0f172a; font-size: 12px; font-weight: 600; text-decoration: underline; cursor: pointer; margin-left: 6px;">
              Clear All
            </button>
          </div>
        ` : ''}

        <!-- Selected Actions Toolbar (Appears when items are checked) -->
        ${selectedMemberIds.size > 0 ? `
          <div style="background: #eff6ff; border-bottom: 1px solid #bfdbfe; padding: 10px 20px; display: flex; justify-content: space-between; align-items: center;">
            <span style="font-size: 13px; font-weight: 600; color: #1e40af;">
              ${selectedMemberIds.size} member${selectedMemberIds.size > 1 ? 's' : ''} selected
            </span>
            <div style="display: flex; gap: 8px;">
              <button 
                type="button" 
                id="fl-bulk-wa-btn" 
                style="
                  padding: 5px 12px; 
                  border: 1px solid #cbd5e1; 
                  background: #ffffff; 
                  color: #0f172a; 
                  border-radius: 6px; 
                  font-size: 12px; 
                  font-weight: 600; 
                  cursor: pointer; 
                  display: flex; 
                  align-items: center; 
                  gap: 6px;
                  transition: all 0.15s ease;
                "
                onmouseover="this.style.background='#f8fafc'; this.style.borderColor='#94a3b8';"
                onmouseout="this.style.background='#ffffff'; this.style.borderColor='#cbd5e1';"
              >
                ${Icons.whatsapp || ''}
                <span>Broadcast WhatsApp</span>
              </button>
              <button type="button" id="fl-bulk-clear-btn" style="padding: 5px 12px; border: none; background: transparent; font-size: 12px; font-weight: 600; color: #64748b; cursor: pointer;">
                Clear Selection
              </button>
            </div>
          </div>
        ` : ''}

        <!-- Members Data Table -->
        <div style="overflow-x: auto;">
          <table style="width: 100%; border-collapse: collapse; text-align: left; font-size: 13.5px;">
            <thead>
              <tr style="background: #ffffff; border-bottom: 1px solid #e2e8f0; color: #64748b; font-size: 11px; text-transform: uppercase; font-weight: 700; letter-spacing: 0.5px;">
                <th style="padding: 14px 16px; width: 36px; text-align: center;">
                  <input type="checkbox" id="fl-select-all-checkbox" ${isAllSelected ? 'checked' : ''} style="cursor: pointer;" />
                </th>
                <th style="padding: 14px 16px; cursor: pointer; user-select: none;" class="fl-member-sort-th" data-sort="name">
                  <div style="display: inline-flex; align-items: center;">
                    <span>NAME</span>
                    ${getSortIndicator('name')}
                  </div>
                </th>
                <th style="padding: 14px 16px;">CONTACT</th>
                <th style="padding: 14px 16px; cursor: pointer; user-select: none;" class="fl-member-sort-th" data-sort="plans">
                  <div style="display: inline-flex; align-items: center;">
                    <span>PLANS</span>
                    ${getSortIndicator('plans')}
                  </div>
                </th>
                <th style="padding: 14px 16px; cursor: pointer; user-select: none;" class="fl-member-sort-th" data-sort="expiryDate">
                  <div style="display: inline-flex; align-items: center;">
                    <span>EXPIRY DATE</span>
                    ${getSortIndicator('expiryDate')}
                  </div>
                </th>
              </tr>
            </thead>
            <tbody>
              ${loading.members ? `
                <tr>
                  <td colspan="5" style="padding: 40px; text-align: center; color: var(--fl-text-muted);">
                    Loading members database...
                  </td>
                </tr>
              ` : pageData.length === 0 ? `
                <tr>
                  <td colspan="5" style="padding: 48px; text-align: center; color: var(--fl-text-muted);">
                    <div style="font-size: 24px; margin-bottom: 8px;">👥</div>
                    <div style="font-weight: 600; color: var(--fl-text);">No members found</div>
                    <div style="font-size: 12.5px; margin-top: 4px;">Try adjusting your search query or filters.</div>
                  </td>
                </tr>
              ` : pageData.map(m => {
                const isChecked = selectedMemberIds.has(m.id);

                return `
                  <tr class="fl-table-row" data-id="${m.id}" style="border-bottom: 1px solid #f1f5f9; transition: background 0.15s; ${isChecked ? 'background: #f8fafc;' : ''}">
                    <td style="padding: 14px 16px; text-align: center;" onclick="event.stopPropagation();">
                      <input type="checkbox" class="fl-row-checkbox" data-id="${m.id}" ${isChecked ? 'checked' : ''} style="cursor: pointer;" />
                    </td>
                    <td style="padding: 14px 16px;">
                      <span class="fl-member-name-link" data-id="${m.id}" style="font-weight: 700; color: #0f172a; text-decoration: none; cursor: pointer;">
                        ${m.name}
                      </span>
                    </td>
                    <td style="padding: 14px 16px; color: #475569; font-size: 13px;">
                      ${m.phone}
                    </td>
                    <td style="padding: 14px 16px; color: #334155; font-size: 13px; font-weight: 500;">
                      ${m.plans || 'None'}
                    </td>
                    <td style="padding: 14px 16px; color: #0f172a; font-size: 13px;">
                      ${this.renderExpiryColumn(m, memberFilters)}
                    </td>
                  </tr>
                `;
              }).join('')}
            </tbody>
          </table>
        </div>

        <!-- Pagination Bar (Image 1) -->
        <div style="padding: 14px 20px; border-top: 1px solid #e2e8f0; display: flex; justify-content: space-between; align-items: center; font-size: 12.5px; color: #64748b;">
          <span>
            Showing <strong>${totalCount === 0 ? 0 : startIndex + 1}–${Math.min(startIndex + this.pageSize, totalCount)}</strong> of <strong>${totalCount}</strong> records
          </span>
          <div style="display: flex; align-items: center; gap: 12px;">
            <button type="button" id="fl-prev-page-btn" ${this.page <= 1 ? 'disabled' : ''} style="padding: 4px 10px; border: 1px solid #e2e8f0; border-radius: 6px; background: #ffffff; font-size: 12px; font-weight: 500; color: #475569; cursor: ${this.page <= 1 ? 'not-allowed' : 'pointer'}; opacity: ${this.page <= 1 ? '0.5' : '1'};">
              ← Prev
            </button>
            <span style="font-weight: 600; color: #0f172a;">Page ${this.page} of ${totalPages}</span>
            <button type="button" id="fl-next-page-btn" ${this.page >= totalPages ? 'disabled' : ''} style="padding: 4px 10px; border: 1px solid #e2e8f0; border-radius: 6px; background: #ffffff; font-size: 12px; font-weight: 500; color: #475569; cursor: ${this.page >= totalPages ? 'not-allowed' : 'pointer'}; opacity: ${this.page >= totalPages ? '0.5' : '1'};">
              Next →
            </button>
          </div>
        </div>

      </div>

      <style>
        .fl-table-row:hover {
          background: #f8fafc;
        }
        .fl-member-name-link:hover {
          color: #2563eb !important;
        }
        .fl-member-drop-item:hover {
          background: #f1f5f9;
        }
        .fl-member-sort-th:hover {
          background: #f8fafc;
          color: #0f172a;
        }
        .fl-member-filter-trigger.fl-dropdown-open {
          border-color: #0f172a !important;
          color: #0f172a !important;
          font-weight: 600 !important;
        }
        .fl-member-filter-trigger.fl-dropdown-open .fl-filter-arrow {
          transform: rotate(180deg) !important;
        }
        .fl-dual-range-slider::-webkit-slider-thumb {
          -webkit-appearance: none;
          appearance: none;
          width: 16px;
          height: 16px;
          border-radius: 50%;
          background: #ffffff;
          border: 2.5px solid #0f172a;
          cursor: pointer;
          pointer-events: auto;
          box-shadow: 0 1px 3px rgba(0,0,0,0.18);
          transition: transform 0.1s ease, border-color 0.1s ease;
        }
        .fl-dual-range-slider::-webkit-slider-thumb:hover {
          transform: scale(1.2);
          border-color: #2563eb;
        }
        .fl-dual-range-slider::-moz-range-thumb {
          width: 16px;
          height: 16px;
          border-radius: 50%;
          background: #ffffff;
          border: 2.5px solid #0f172a;
          cursor: pointer;
          pointer-events: auto;
          box-shadow: 0 1px 3px rgba(0,0,0,0.18);
          transition: transform 0.1s ease, border-color 0.1s ease;
        }
        .fl-dual-range-slider::-moz-range-thumb:hover {
          transform: scale(1.2);
          border-color: #2563eb;
        }
      </style>
    `;

    this.bindEvents(pageData);

      if (isSearchFocused) {
        const nextInput = this.querySelector('#fl-member-search-input');
        if (nextInput) {
          nextInput.focus();
          if (selStart !== null && selEnd !== null) {
            try {
              nextInput.setSelectionRange(selStart, selEnd);
            } catch (_) {}
          }
        }
      }
    } catch (err) {
      console.error('Error rendering members section:', err);
      this.innerHTML = `
        <div style="padding: 32px; background: #ffffff; border: 1px solid #fee2e2; border-radius: 12px; text-align: center; color: #dc2626;">
          <h3 style="margin: 0 0 8px 0; font-size: 16px;">Failed to display Members list</h3>
          <p style="margin: 0; font-size: 13px; color: #64748b;">${err.message || 'An unexpected error occurred.'}</p>
        </div>
      `;
    }
  }

  bindEvents(pageData) {
    // Search input
    const searchInput = this.querySelector('#fl-member-search-input');
    if (searchInput) {
      searchInput.addEventListener('input', (e) => {
        HomeStore.setSearchQuery(e.target.value);
      });
    }

    // Dropdown toggles
    this.querySelectorAll('.fl-member-filter-trigger').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const dropId = btn.getAttribute('data-dropdown');
        const panel = this.querySelector(`#fl-member-dropdown-${dropId}`);
        const isShown = panel && panel.style.display === 'block';
        // Close all dropdowns, remove open class, and reset arrows
        this.querySelectorAll('.fl-dropdown-panel').forEach(p => p.style.display = 'none');
        this.querySelectorAll('.fl-member-filter-trigger').forEach(b => b.classList.remove('fl-dropdown-open'));
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
        this.querySelectorAll('.fl-member-filter-trigger').forEach(b => b.classList.remove('fl-dropdown-open'));
        this.querySelectorAll('.fl-filter-arrow').forEach(a => a.style.transform = 'rotate(0deg)');
      });
    }

    // Stop click bubbling on dropdown panels so date picker interactions do not close the panel
    this.querySelectorAll('.fl-dropdown-panel').forEach(p => {
      p.addEventListener('click', (e) => {
        e.stopPropagation();
      });
    });

    // Expiry Date Presets
    const presets = getPresetDates();
    this.querySelectorAll('.fl-member-date-preset').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const preset = btn.getAttribute('data-preset');
        let from = '';
        let to = '';

        if (preset === 'next-7d') {
          from = presets.todayStr;
          to = presets.plus7Str;
        } else if (preset === 'this-month') {
          from = presets.todayStr;
          to = presets.lastDayThisMonthStr;
        } else if (preset === 'next-30d') {
          from = presets.todayStr;
          to = presets.plus30Str;
        } else if (preset === 'expired') {
          from = '';
          to = presets.yesterdayStr;
        }

        const fromInput = this.querySelector('#fl-member-expiry-from');
        const toInput = this.querySelector('#fl-member-expiry-to');
        if (fromInput) fromInput.value = from;
        if (toInput) toInput.value = to;
        HomeStore.setMemberDateFilter('expiry', from, to);
      });
    });

    // Expiry Date Apply & Clear
    const expApply = this.querySelector('#fl-member-expiry-apply');
    if (expApply) {
      expApply.addEventListener('click', (e) => {
        e.stopPropagation();
        const from = this.querySelector('#fl-member-expiry-from')?.value || '';
        const to = this.querySelector('#fl-member-expiry-to')?.value || '';
        this.querySelectorAll('.fl-dropdown-panel').forEach(p => p.style.display = 'none');
        HomeStore.setMemberDateFilter('expiry', from, to);
      });
    }

    const expClear = this.querySelector('#fl-member-expiry-clear');
    if (expClear) {
      expClear.addEventListener('click', (e) => {
        e.stopPropagation();
        this.querySelectorAll('.fl-dropdown-panel').forEach(p => p.style.display = 'none');
        HomeStore.setMemberDateFilter('expiry', '', '');
      });
    }

    // Sorting table headers click
    this.querySelectorAll('.fl-member-sort-th').forEach(th => {
      th.addEventListener('click', () => {
        const field = th.getAttribute('data-sort');
        if (field) {
          HomeStore.toggleMemberSort(field);
        }
      });
    });

    // Option items click
    this.querySelectorAll('.fl-member-drop-item').forEach(item => {
      item.addEventListener('click', (e) => {
        e.stopPropagation();
        const fKey = item.getAttribute('data-filter');
        const fVal = item.getAttribute('data-val');
        this.querySelectorAll('.fl-dropdown-panel').forEach(p => p.style.display = 'none');
        HomeStore.setMemberFilter(fKey, fVal);
      });
    });

    // Applied chip remove
    this.querySelectorAll('.fl-member-filter-remove-badge').forEach(badge => {
      badge.addEventListener('click', (e) => {
        e.stopPropagation();
        const type = badge.getAttribute('data-type');
        if (type === 'expiry') {
          HomeStore.setMemberDateFilter('expiry', '', '');
        } else if (type === 'noplan') {
          HomeStore.setMemberFilter('noPlanOnly', false);
        } else {
          HomeStore.setMemberFilter(type, 'all');
        }
      });
    });

    // Clear all
    const clearBtn = this.querySelector('#fl-clear-member-filter-btn');
    if (clearBtn) {
      clearBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        HomeStore.clearMemberFilters();
      });
    }

    // Add member button
    const addBtn = this.querySelector('#fl-open-add-member-btn');
    if (addBtn) {
      addBtn.addEventListener('click', () => {
        HomeStore.openAddMemberModal();
      });
    }

    // Refresh members button
    const refreshBtn = this.querySelector('#fl-refresh-members-btn');
    if (refreshBtn) {
      refreshBtn.addEventListener('click', () => {
        HomeStore.refreshGymData(true);
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

    // Bulk WhatsApp Broadcast
    const bulkWaBtn = this.querySelector('#fl-bulk-wa-btn');
    if (bulkWaBtn) {
      bulkWaBtn.addEventListener('click', () => {
        const state = HomeStore.getState();
        const selected = state.members.filter(m => state.selectedMemberIds.has(m.id));
        if (selected.length > 0) {
          HomeStore.openBulkWhatsAppModal(selected);
        }
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

    // Name link click -> open drawer
    this.querySelectorAll('.fl-member-name-link').forEach(link => {
      link.addEventListener('click', (e) => {
        e.stopPropagation();
        const id = link.getAttribute('data-id');
        const member = HomeStore.getState().members.find(m => m.id === id);
        if (member) HomeStore.openMemberDrawer(member);
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

    // Renew quick action
    this.querySelectorAll('.fl-action-renew-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const id = btn.getAttribute('data-id');
        const member = HomeStore.getState().members.find(m => m.id === id);
        if (member) HomeStore.openRenewMemberModal(member);
      });
    });

    // WhatsApp quick action
    this.querySelectorAll('.fl-action-wa-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const id = btn.getAttribute('data-id');
        const member = HomeStore.getState().members.find(m => m.id === id);
        if (member) {
          const hasOverdue = (member.payments || []).some(p => !p.isPaid && (p.isOverdue || p.status === 'overdue'));
          const pendingTpl = hasOverdue ? 'payment_overdue_reminder' : 'upcoming_payment_reminder';
          const defaultTpl = member.status === 'renewals'
            ? 'plan_renewal_reminder'
            : member.status === 'expired'
            ? 'member_lapsed_winback'
            : (member.status === 'prospect' || !member.subscriptions || member.subscriptions.length === 0)
            ? 'prospect_walkin_recap'
            : (member.pendingAmount > 0 ? pendingTpl : 'google_review_request');
          HomeStore.openWhatsAppModal(member, defaultTpl);
        }
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
