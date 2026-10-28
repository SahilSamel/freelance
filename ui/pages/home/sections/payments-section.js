/**
 * <fl-payments-section> Native Web Component
 * Payments & Installments section: tracking dues, upcoming installments, and overdue balances.
 * Exact 100% pixel-faithful match to user's Image 2.
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
      const curYear = String(new Date().getFullYear());
      return year === curYear ? `${day} ${months[mIdx]}` : `${day} ${months[mIdx]} ${year}`;
    }
    const d = new Date(dStr);
    return isNaN(d.getTime()) ? dStr : d.toLocaleDateString('en-GB', { day: 'numeric', month: 'short' });
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

  const minus30 = new Date(now);
  minus30.setDate(now.getDate() - 30);
  const minus30Str = toIso(minus30);

  const firstDayThisMonth = new Date(now.getFullYear(), now.getMonth(), 1);
  const firstDayThisMonthStr = toIso(firstDayThisMonth);

  const lastDayThisMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0);
  const lastDayThisMonthStr = toIso(lastDayThisMonth);

  const firstDayLastMonth = new Date(now.getFullYear(), now.getMonth() - 1, 1);
  const firstDayLastMonthStr = toIso(firstDayLastMonth);

  const lastDayLastMonth = new Date(now.getFullYear(), now.getMonth(), 0);
  const lastDayLastMonthStr = toIso(lastDayLastMonth);

  return {
    todayStr,
    yesterdayStr,
    plus7Str,
    plus30Str,
    minus30Str,
    firstDayThisMonthStr,
    lastDayThisMonthStr,
    firstDayLastMonthStr,
    lastDayLastMonthStr
  };
}

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

  getMemberStatusDot(memberStatus) {
    if (memberStatus === 'prospect') {
      return `<span style="display: inline-block; width: 7px; height: 7px; border-radius: 50%; background: #2563eb; margin-left: 6px;" title="Prospect"></span>`;
    }
    if (memberStatus === 'expired') {
      return `<span style="display: inline-block; width: 7px; height: 7px; border-radius: 50%; background: #64748b; margin-left: 6px;" title="Expired member"></span>`;
    }
    return `<span style="display: inline-block; width: 7px; height: 7px; border-radius: 50%; background: #22c55e; margin-left: 6px;" title="Active member"></span>`;
  }

  getPaymentStatusBadge(p) {
    if (p.isPaid || p.status === 'paid') {
      return `<span style="background: #ecfdf5; color: #16a34a; border: 1px solid #a7f3d0; border-radius: 9999px; padding: 2px 10px; font-size: 11.5px; font-weight: 600; display: inline-block; white-space: nowrap;">Paid</span>`;
    }

    if (p.status === 'overdue' || p.isOverdue) {
      const label = p.statusLabel || 'Overdue';
      return `<span style="background: #fee2e2; color: #dc2626; border: 1px solid #fca5a5; border-radius: 9999px; padding: 2px 10px; font-size: 11.5px; font-weight: 600; display: inline-block; white-space: nowrap;">${label}</span>`;
    }

    if (p.status === 'upcoming' || p.isUpcoming) {
      const label = p.statusLabel || 'Upcoming';
      return `<span style="background: #fef3c7; color: #b45309; border: 1px solid #fde68a; border-radius: 9999px; padding: 2px 10px; font-size: 11.5px; font-weight: 600; display: inline-block; white-space: nowrap;">${label}</span>`;
    }

    if (p.status === 'future' || p.isFuture) {
      const label = p.statusLabel || 'Future';
      return `<span style="background: #f1f5f9; color: #64748b; border: 1px solid #cbd5e1; border-radius: 9999px; padding: 2px 10px; font-size: 11.5px; font-weight: 600; display: inline-block; white-space: nowrap;">${label}</span>`;
    }

    return `<span style="background: #fee2e2; color: #dc2626; border: 1px solid #fca5a5; border-radius: 9999px; padding: 2px 10px; font-size: 11.5px; font-weight: 600; display: inline-block; white-space: nowrap;">${p.statusLabel || 'Unpaid'}</span>`;
  }

  exportToCsv(data) {
    if (!data || data.length === 0) return;
    const headers = ['Member', 'Phone', 'Amount', 'Due Date', 'Paid Date', 'Status', 'Method', 'Txn ID', 'Notes'];
    const rows = data.map(p => [
      `"${p.memberName || ''}"`,
      `"${p.phone || ''}"`,
      p.amount || 0,
      p.dueDate || '',
      p.paidDate || '',
      p.statusLabel || p.status || '',
      p.paymentMethod || '',
      p.txnId || '',
      `"${p.notes || ''}"`
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
    const activeEl = document.activeElement;
    const isSearchFocused = !!(activeEl && (activeEl.id === 'fl-payment-search-input' || activeEl === this.querySelector('#fl-payment-search-input')));
    const selStart = isSearchFocused ? activeEl.selectionStart : null;
    const selEnd = isSearchFocused ? activeEl.selectionEnd : null;

    const state = HomeStore.getState();
    const { loading } = state;
    const filteredPayments = HomeStore.getFilteredPayments();

    // Pagination calculations
    const totalCount = filteredPayments.length;
    const totalPages = Math.max(1, Math.ceil(totalCount / this.pageSize));
    if (this.page > totalPages) this.page = totalPages;
    const startIndex = (this.page - 1) * this.pageSize;
    const pageData = filteredPayments.slice(startIndex, startIndex + this.pageSize);

    const selectedPaymentIds = state.selectedPaymentIds || new Set();
    const isAllSelected = pageData.length > 0 && pageData.every(p => selectedPaymentIds.has(p.id));

    const paymentFilters = state.paymentFilters || {
      amount: 'all',
      amountMin: null,
      amountMax: null,
      statuses: new Set(['unpaid']),
      dueDate: 'oct-2026',
      paidDate: 'all'
    };

    const paymentSort = state.paymentSort || { field: null, direction: 'asc' };
    const getSortIndicator = (f) => {
      if (paymentSort.field !== f) {
        return `<span style="color: #cbd5e1; font-size: 11px; margin-left: 4px; user-select: none;">⇅</span>`;
      }
      return paymentSort.direction === 'asc'
        ? `<span style="color: #0f172a; font-weight: 700; font-size: 12px; margin-left: 4px; user-select: none;">↑</span>`
        : `<span style="color: #0f172a; font-weight: 700; font-size: 12px; margin-left: 4px; user-select: none;">↓</span>`;
    };

    const maxPaymentAmount = (state.payments && state.payments.length)
      ? Math.max(...state.payments.map(p => Number(p.amount) || 0), 20000)
      : 20000;
    const maxLimit = Math.max(20000, Math.ceil(maxPaymentAmount / 5000) * 5000);

    const currMin = (paymentFilters.amountMin !== null && paymentFilters.amountMin !== undefined) ? Number(paymentFilters.amountMin) : 0;
    const currMax = (paymentFilters.amountMax !== null && paymentFilters.amountMax !== undefined) ? Number(paymentFilters.amountMax) : maxLimit;
    const hasAmountFilter = (paymentFilters.amountMin !== null && paymentFilters.amountMin !== undefined && paymentFilters.amountMin > 0) ||
                            (paymentFilters.amountMax !== null && paymentFilters.amountMax !== undefined && paymentFilters.amountMax < maxLimit);

    let amountBtnLabel = 'Amount';
    if (hasAmountFilter) {
      const formatVal = (v) => v >= 1000 ? `${(v / 1000).toFixed(v % 1000 === 0 ? 0 : 1)}k` : `${v}`;
      if (currMin > 0 && currMax < maxLimit) {
        amountBtnLabel = `Amount (₹${formatVal(currMin)}–₹${formatVal(currMax)})`;
      } else if (currMin > 0) {
        amountBtnLabel = `Amount (≥ ₹${formatVal(currMin)})`;
      } else if (currMax < maxLimit) {
        amountBtnLabel = `Amount (≤ ₹${formatVal(currMax)})`;
      }
    } else if (paymentFilters.amount && paymentFilters.amount !== 'all') {
      const amtLabelMap = { 'under-2k': 'Amount (< ₹2k)', '2k-5k': 'Amount (₹2k-5k)', 'above-5k': 'Amount (> ₹5k)' };
      amountBtnLabel = amtLabelMap[paymentFilters.amount] || 'Amount';
    }

    const hasMethodFilter = paymentFilters.method && paymentFilters.method !== 'all';
    const methodLabels = {
      'upi': 'UPI',
      'cash': 'Cash',
      'card': 'Card',
      'netbanking': 'Net Banking'
    };
    const methodBtnLabel = hasMethodFilter
      ? `Method (${methodLabels[paymentFilters.method.toLowerCase()] || paymentFilters.method.toUpperCase()})`
      : 'Method';

    const hasDueFilter = !!(paymentFilters.dueDateFrom || paymentFilters.dueDateTo || (paymentFilters.dueDate && paymentFilters.dueDate !== 'all'));
    const dueBtnLabel = (paymentFilters.dueDateFrom || paymentFilters.dueDateTo)
      ? formatRangeLabel(paymentFilters.dueDateFrom, paymentFilters.dueDateTo, 'Due Date')
      : (paymentFilters.dueDate && paymentFilters.dueDate !== 'all' ? (paymentFilters.dueDate === 'oct-2026' || paymentFilters.dueDate === 'this-month' ? 'Due Date (≤ Month End)' : paymentFilters.dueDate) : 'Due Date');

    const hasPaidFilter = !!(paymentFilters.paidDateFrom || paymentFilters.paidDateTo || (paymentFilters.paidDate && paymentFilters.paidDate !== 'all'));
    let paidBtnLabel = 'Paid Date';
    if (paymentFilters.paidDateFrom || paymentFilters.paidDateTo) {
      paidBtnLabel = formatRangeLabel(paymentFilters.paidDateFrom, paymentFilters.paidDateTo, 'Paid Date');
    } else if (paymentFilters.paidDate && paymentFilters.paidDate !== 'all') {
      const pBtnMap = {
        'unpaid': 'Paid Date (Not Paid)',
        'this-month': 'Paid Date (This Mo)',
        'last-month': 'Paid Date (Last Mo)',
        'today': 'Paid Date (Today)',
        'earlier': 'Paid Date (Earlier)'
      };
      paidBtnLabel = pBtnMap[paymentFilters.paidDate] || `Paid Date (${paymentFilters.paidDate})`;
    }

    // Applied chips calculation
    const appliedChips = [];
    if (paymentFilters.unpaidOnly) {
      appliedChips.push({ key: 'unpaidOnly', label: 'Unpaid Collections', type: 'unpaidOnly' });
    }
    if (hasMethodFilter) {
      const displayMethod = methodLabels[paymentFilters.method.toLowerCase()] || paymentFilters.method.toUpperCase();
      appliedChips.push({
        key: 'method',
        label: `Method: ${displayMethod}`,
        type: 'method',
        val: paymentFilters.method
      });
    }
    if (hasAmountFilter) {
      let chipLabel = 'Amount: ';
      if (currMin > 0 && currMax < maxLimit) {
        chipLabel += `₹${currMin.toLocaleString('en-IN')} – ₹${currMax.toLocaleString('en-IN')}`;
      } else if (currMin > 0) {
        chipLabel += `≥ ₹${currMin.toLocaleString('en-IN')}`;
      } else {
        chipLabel += `≤ ₹${currMax.toLocaleString('en-IN')}`;
      }
      appliedChips.push({ key: 'amount', label: chipLabel, type: 'amount' });
    } else if (paymentFilters.amount && paymentFilters.amount !== 'all') {
      const aLabels = { 'under-2k': 'Amount: < ₹2,000', '2k-5k': 'Amount: ₹2,000 – ₹5,000', 'above-5k': 'Amount: > ₹5,000' };
      appliedChips.push({ key: 'amount', label: aLabels[paymentFilters.amount], type: 'amount' });
    }
    if (paymentFilters.dueDateFrom || paymentFilters.dueDateTo) {
      appliedChips.push({
        key: 'duedate',
        label: formatChipLabel('Due Date', paymentFilters.dueDateFrom, paymentFilters.dueDateTo),
        type: 'duedate'
      });
    } else if (paymentFilters.dueDate && paymentFilters.dueDate !== 'all') {
      const dLabels = { 'this-month': 'Due Date: ≤ Month End', 'oct-2026': 'Due Date: ≤ Month End' };
      appliedChips.push({ key: 'duedate', label: dLabels[paymentFilters.dueDate] || 'Due Date', type: 'duedate' });
    }
    if (paymentFilters.paidDateFrom || paymentFilters.paidDateTo) {
      appliedChips.push({
        key: 'paiddate',
        label: formatChipLabel('Paid Date', paymentFilters.paidDateFrom, paymentFilters.paidDateTo),
        type: 'paiddate'
      });
    } else if (paymentFilters.paidDate && paymentFilters.paidDate !== 'all') {
      const pLabels = {
        'unpaid': 'Paid: Not Paid',
        'this-month': 'Paid: This Month',
        'last-month': 'Paid: Last Month',
        'today': 'Paid: Today',
        'earlier': 'Paid: Earlier'
      };
      appliedChips.push({ key: 'paiddate', label: pLabels[paymentFilters.paidDate] || `Paid: ${paymentFilters.paidDate}`, type: 'paiddate' });
    }

    this.innerHTML = `
      <div style="background: #ffffff; border: 1px solid var(--fl-border, #e2e8f0); border-radius: 12px; box-shadow: var(--fl-shadow-xs); overflow: visible;">
        
        <!-- Filter Toolbar -->
        <div style="padding: 14px 20px; border-bottom: 1px solid var(--fl-border, #e2e8f0); display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px;">
          <!-- Left Filters -->
          <div style="display: flex; align-items: center; gap: 10px; flex-wrap: wrap; flex: 1;">
            <!-- Search Input -->
            <div style="position: relative; width: 250px;">
              <input 
                type="text" 
                id="fl-payment-search-input" 
                placeholder="Search name, contact, TXN ID..." 
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

            <!-- Amount Dropdown with Dual Min & Max Sliders -->
            <div style="position: relative;" class="fl-dropdown-container">
              <button type="button" class="fl-pay-filter-trigger" data-dropdown="amount" style="height: 34px; padding: 0 12px; border: 1px solid ${hasAmountFilter || paymentFilters.amount !== 'all' ? '#0f172a' : '#e2e8f0'}; border-radius: 6px; background: #ffffff; font-size: 13px; color: ${hasAmountFilter || paymentFilters.amount !== 'all' ? '#0f172a' : '#475569'}; font-weight: ${hasAmountFilter || paymentFilters.amount !== 'all' ? '600' : '500'}; cursor: pointer; display: flex; align-items: center; gap: 6px;">
                <span>${amountBtnLabel}</span>
                <svg class="fl-filter-arrow" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#64748b" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" style="flex-shrink: 0; transition: transform 0.2s ease; transform: rotate(0deg);">
                  <polyline points="6 9 12 15 18 9"></polyline>
                </svg>
              </button>
              <div class="fl-dropdown-panel" id="fl-dropdown-amount" style="display: none; position: absolute; top: calc(100% + 4px); left: 0; width: 290px; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 8px; box-shadow: 0 10px 15px -3px rgba(0,0,0,0.1), 0 4px 6px -4px rgba(0,0,0,0.05); z-index: 100; padding: 14px 16px; box-sizing: border-box;">
                <div style="font-size: 12px; font-weight: 700; color: #1e293b; margin-bottom: 8px; display: flex; justify-content: space-between; align-items: center;">
                  <span>Amount Range</span>
                  <span id="fl-amount-range-display" style="font-size: 11.5px; font-weight: 700; color: #0f172a;">
                    ₹${currMin.toLocaleString('en-IN')} – ${currMax >= maxLimit ? `₹${maxLimit.toLocaleString('en-IN')}+` : `₹${currMax.toLocaleString('en-IN')}`}
                  </span>
                </div>

                <!-- Presets -->
                <div style="margin-bottom: 12px; display: flex; flex-wrap: wrap; gap: 6px;">
                  <button type="button" class="fl-pay-amount-preset" data-min="0" data-max="${maxLimit}" style="padding: 3px 8px; border: 1px solid #e2e8f0; background: #f8fafc; border-radius: 4px; font-size: 11px; font-weight: 500; color: #334155; cursor: pointer;">All</button>
                  <button type="button" class="fl-pay-amount-preset" data-min="0" data-max="2000" style="padding: 3px 8px; border: 1px solid #e2e8f0; background: #f8fafc; border-radius: 4px; font-size: 11px; font-weight: 500; color: #334155; cursor: pointer;">< ₹2k</button>
                  <button type="button" class="fl-pay-amount-preset" data-min="2000" data-max="5000" style="padding: 3px 8px; border: 1px solid #e2e8f0; background: #f8fafc; border-radius: 4px; font-size: 11px; font-weight: 500; color: #334155; cursor: pointer;">₹2k – ₹5k</button>
                  <button type="button" class="fl-pay-amount-preset" data-min="5000" data-max="${maxLimit}" style="padding: 3px 8px; border: 1px solid #e2e8f0; background: #f8fafc; border-radius: 4px; font-size: 11px; font-weight: 500; color: #334155; cursor: pointer;">> ₹5k</button>
                  <button type="button" class="fl-pay-amount-preset" data-min="10000" data-max="${maxLimit}" style="padding: 3px 8px; border: 1px solid #e2e8f0; background: #f8fafc; border-radius: 4px; font-size: 11px; font-weight: 500; color: #334155; cursor: pointer;">> ₹10k</button>
                </div>

                <!-- Dual Range Slider Track & Handles -->
                <div class="fl-amount-slider-wrap" style="position: relative; height: 32px; display: flex; align-items: center; margin-bottom: 12px;">
                  <div style="position: absolute; left: 0; right: 0; height: 6px; background: #e2e8f0; border-radius: 9999px;"></div>
                  <div id="fl-amount-slider-track" style="position: absolute; height: 6px; background: #0f172a; border-radius: 9999px; left: ${Math.round((currMin / maxLimit) * 100)}%; right: ${Math.round(100 - (currMax / maxLimit) * 100)}%;"></div>
                  <input type="range" id="fl-amount-min-slider" min="0" max="${maxLimit}" step="250" value="${currMin}" class="fl-dual-range-slider fl-range-min" style="position: absolute; left: 0; width: 100%; -webkit-appearance: none; appearance: none; background: transparent; pointer-events: none; margin: 0; outline: none; z-index: 3;" />
                  <input type="range" id="fl-amount-max-slider" min="0" max="${maxLimit}" step="250" value="${currMax}" class="fl-dual-range-slider fl-range-max" style="position: absolute; left: 0; width: 100%; -webkit-appearance: none; appearance: none; background: transparent; pointer-events: none; margin: 0; outline: none; z-index: 4;" />
                </div>

                <!-- Inputs: Min & Max Amount -->
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 14px;">
                  <div>
                    <label style="display: block; font-size: 11px; font-weight: 600; color: #475569; margin-bottom: 4px;">Min Amount</label>
                    <div style="position: relative; display: flex; align-items: center;">
                      <span style="position: absolute; left: 8px; font-size: 11.5px; color: #94a3b8; font-weight: 600;">₹</span>
                      <input type="number" id="fl-amount-min-input" value="${currMin}" min="0" max="${maxLimit}" step="250" style="width: 100%; height: 32px; padding: 2px 6px 2px 20px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 12px; color: #0f172a; box-sizing: border-box; outline: none; background: #ffffff; font-weight: 600;" />
                    </div>
                  </div>
                  <div>
                    <label style="display: block; font-size: 11px; font-weight: 600; color: #475569; margin-bottom: 4px;">Max Amount</label>
                    <div style="position: relative; display: flex; align-items: center;">
                      <span style="position: absolute; left: 8px; font-size: 11.5px; color: #94a3b8; font-weight: 600;">₹</span>
                      <input type="number" id="fl-amount-max-input" value="${currMax}" min="0" max="${maxLimit}" step="250" style="width: 100%; height: 32px; padding: 2px 6px 2px 20px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 12px; color: #0f172a; box-sizing: border-box; outline: none; background: #ffffff; font-weight: 600;" />
                    </div>
                  </div>
                </div>

                <!-- Action Footer -->
                <div style="display: flex; justify-content: flex-end; align-items: center; gap: 8px; border-top: 1px solid #f1f5f9; padding-top: 10px;">
                  <button type="button" id="fl-amount-clear" class="fl-btn fl-btn-secondary" style="padding: 5px 12px; font-size: 12px;">Clear</button>
                  <button type="button" id="fl-amount-apply" class="fl-btn fl-btn-primary" style="padding: 5px 14px; font-size: 12px;">Apply</button>
                </div>
              </div>
            </div>

            <!-- Method Dropdown -->
            <div style="position: relative;" class="fl-dropdown-container">
              <button type="button" class="fl-pay-filter-trigger" data-dropdown="method" style="height: 34px; padding: 0 12px; border: 1px solid ${hasMethodFilter ? '#0f172a' : '#e2e8f0'}; border-radius: 6px; background: #ffffff; font-size: 13px; color: ${hasMethodFilter ? '#0f172a' : '#475569'}; font-weight: ${hasMethodFilter ? '600' : '500'}; cursor: pointer; display: flex; align-items: center; gap: 6px;">
                <span>${methodBtnLabel}</span>
                <svg class="fl-filter-arrow" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#64748b" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" style="flex-shrink: 0; transition: transform 0.2s ease; transform: rotate(0deg);">
                  <polyline points="6 9 12 15 18 9"></polyline>
                </svg>
              </button>
              <div class="fl-dropdown-panel" id="fl-dropdown-method" style="display: none; position: absolute; top: calc(100% + 4px); left: 0; min-width: 175px; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 8px; box-shadow: 0 10px 15px -3px rgba(0,0,0,0.1), 0 4px 6px -4px rgba(0,0,0,0.05); z-index: 100; padding: 4px 0;">
                <div class="fl-drop-item" data-filter="method" data-val="all" style="padding: 7px 12px; font-size: 13px; color: #334155; cursor: pointer; display: flex; justify-content: space-between; align-items: center; ${!hasMethodFilter ? 'font-weight: 700; background: #f8fafc;' : ''}">
                  <span>All Methods</span>
                  ${!hasMethodFilter ? '<span style="color: #0f172a;">✓</span>' : ''}
                </div>
                <div class="fl-drop-item" data-filter="method" data-val="upi" style="padding: 7px 12px; font-size: 13px; color: #334155; cursor: pointer; display: flex; justify-content: space-between; align-items: center; ${paymentFilters.method === 'upi' ? 'font-weight: 700; background: #f8fafc;' : ''}">
                  <span>UPI</span>
                  ${paymentFilters.method === 'upi' ? '<span style="color: #0f172a;">✓</span>' : ''}
                </div>
                <div class="fl-drop-item" data-filter="method" data-val="cash" style="padding: 7px 12px; font-size: 13px; color: #334155; cursor: pointer; display: flex; justify-content: space-between; align-items: center; ${paymentFilters.method === 'cash' ? 'font-weight: 700; background: #f8fafc;' : ''}">
                  <span>Cash</span>
                  ${paymentFilters.method === 'cash' ? '<span style="color: #0f172a;">✓</span>' : ''}
                </div>
                <div class="fl-drop-item" data-filter="method" data-val="card" style="padding: 7px 12px; font-size: 13px; color: #334155; cursor: pointer; display: flex; justify-content: space-between; align-items: center; ${paymentFilters.method === 'card' ? 'font-weight: 700; background: #f8fafc;' : ''}">
                  <span>Debit / Credit Card</span>
                  ${paymentFilters.method === 'card' ? '<span style="color: #0f172a;">✓</span>' : ''}
                </div>
                <div class="fl-drop-item" data-filter="method" data-val="netbanking" style="padding: 7px 12px; font-size: 13px; color: #334155; cursor: pointer; display: flex; justify-content: space-between; align-items: center; ${paymentFilters.method === 'netbanking' ? 'font-weight: 700; background: #f8fafc;' : ''}">
                  <span>Net Banking / NEFT</span>
                  ${paymentFilters.method === 'netbanking' ? '<span style="color: #0f172a;">✓</span>' : ''}
                </div>
              </div>
            </div>

            <!-- Due Date Dropdown -->
            <div style="position: relative;" class="fl-dropdown-container">
              <button type="button" class="fl-pay-filter-trigger" data-dropdown="duedate" style="height: 34px; padding: 0 12px; border: 1px solid ${hasDueFilter ? '#0f172a' : '#e2e8f0'}; border-radius: 6px; background: #ffffff; font-size: 13px; color: ${hasDueFilter ? '#0f172a' : '#475569'}; font-weight: ${hasDueFilter ? '600' : '500'}; cursor: pointer; display: flex; align-items: center; gap: 6px;">
                <span>${dueBtnLabel}</span>
                <svg class="fl-filter-arrow" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#64748b" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" style="flex-shrink: 0; transition: transform 0.2s ease; transform: rotate(0deg);">
                  <polyline points="6 9 12 15 18 9"></polyline>
                </svg>
              </button>
              <div class="fl-dropdown-panel" id="fl-dropdown-duedate" style="display: none; position: absolute; top: calc(100% + 4px); left: 0; width: 285px; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 8px; box-shadow: 0 10px 15px -3px rgba(0,0,0,0.1), 0 4px 6px -4px rgba(0,0,0,0.05); z-index: 100; padding: 14px 16px; box-sizing: border-box;">
                <div style="font-size: 12px; font-weight: 700; color: #1e293b; margin-bottom: 8px; display: flex; justify-content: space-between; align-items: center;">
                  <span>Due Date Range</span>
                  <span style="font-size: 11px; font-weight: 500; color: #64748b;">Presets</span>
                </div>
                <div style="margin-bottom: 12px; display: flex; flex-wrap: wrap; gap: 6px;">
                  <button type="button" class="fl-pay-date-preset" data-target="due" data-preset="month-end" style="padding: 3px 8px; border: 1px solid #e2e8f0; background: #f8fafc; border-radius: 4px; font-size: 11px; font-weight: 500; color: #334155; cursor: pointer;">≤ Month End</button>
                  <button type="button" class="fl-pay-date-preset" data-target="due" data-preset="this-month" style="padding: 3px 8px; border: 1px solid #e2e8f0; background: #f8fafc; border-radius: 4px; font-size: 11px; font-weight: 500; color: #334155; cursor: pointer;">This Month</button>
                  <button type="button" class="fl-pay-date-preset" data-target="due" data-preset="next-7d" style="padding: 3px 8px; border: 1px solid #e2e8f0; background: #f8fafc; border-radius: 4px; font-size: 11px; font-weight: 500; color: #334155; cursor: pointer;">Next 7d</button>
                  <button type="button" class="fl-pay-date-preset" data-target="due" data-preset="next-30d" style="padding: 3px 8px; border: 1px solid #e2e8f0; background: #f8fafc; border-radius: 4px; font-size: 11px; font-weight: 500; color: #334155; cursor: pointer;">Next 30d</button>
                  <button type="button" class="fl-pay-date-preset" data-target="due" data-preset="overdue" style="padding: 3px 8px; border: 1px solid #e2e8f0; background: #f8fafc; border-radius: 4px; font-size: 11px; font-weight: 500; color: #334155; cursor: pointer;">Overdue</button>
                </div>
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 14px;">
                  <div>
                    <label style="display: block; font-size: 11px; font-weight: 600; color: #475569; margin-bottom: 4px;">From</label>
                    <input type="date" id="fl-due-date-from" value="${paymentFilters.dueDateFrom || ''}" style="width: 100%; height: 32px; padding: 2px 6px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 12px; color: #0f172a; box-sizing: border-box; outline: none; background: #ffffff;" />
                  </div>
                  <div>
                    <label style="display: block; font-size: 11px; font-weight: 600; color: #475569; margin-bottom: 4px;">To</label>
                    <input type="date" id="fl-due-date-to" value="${paymentFilters.dueDateTo || ''}" style="width: 100%; height: 32px; padding: 2px 6px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 12px; color: #0f172a; box-sizing: border-box; outline: none; background: #ffffff;" />
                  </div>
                </div>
                <div style="display: flex; justify-content: flex-end; align-items: center; gap: 8px; border-top: 1px solid #f1f5f9; padding-top: 10px;">
                  <button type="button" id="fl-due-date-clear" class="fl-btn fl-btn-secondary" style="padding: 5px 12px; font-size: 12px;">Clear</button>
                  <button type="button" id="fl-due-date-apply" class="fl-btn fl-btn-primary" style="padding: 5px 14px; font-size: 12px;">Apply</button>
                </div>
              </div>
            </div>

            <!-- Paid Date Dropdown -->
            <div style="position: relative;" class="fl-dropdown-container">
              <button type="button" class="fl-pay-filter-trigger" data-dropdown="paiddate" style="height: 34px; padding: 0 12px; border: 1px solid ${hasPaidFilter ? '#0f172a' : '#e2e8f0'}; border-radius: 6px; background: #ffffff; font-size: 13px; color: ${hasPaidFilter ? '#0f172a' : '#475569'}; font-weight: ${hasPaidFilter ? '600' : '500'}; cursor: pointer; display: flex; align-items: center; gap: 6px;">
                <span>${paidBtnLabel}</span>
                <svg class="fl-filter-arrow" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#64748b" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" style="flex-shrink: 0; transition: transform 0.2s ease; transform: rotate(0deg);">
                  <polyline points="6 9 12 15 18 9"></polyline>
                </svg>
              </button>
              <div class="fl-dropdown-panel" id="fl-dropdown-paiddate" style="display: none; position: absolute; top: calc(100% + 4px); left: 0; width: 285px; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 8px; box-shadow: 0 10px 15px -3px rgba(0,0,0,0.1), 0 4px 6px -4px rgba(0,0,0,0.05); z-index: 100; padding: 14px 16px; box-sizing: border-box;">
                <div style="font-size: 12px; font-weight: 700; color: #1e293b; margin-bottom: 8px; display: flex; justify-content: space-between; align-items: center;">
                  <span>Paid Date Range</span>
                  <span style="font-size: 11px; font-weight: 500; color: #64748b;">Presets</span>
                </div>
                <div style="margin-bottom: 12px; display: flex; flex-wrap: wrap; gap: 6px;">
                  <button type="button" class="fl-pay-date-preset" data-target="paid" data-preset="unpaid" style="padding: 3px 8px; border: 1px solid ${paymentFilters.paidDate === 'unpaid' ? '#0f172a' : '#e2e8f0'}; background: ${paymentFilters.paidDate === 'unpaid' ? '#f1f5f9' : '#f8fafc'}; font-weight: ${paymentFilters.paidDate === 'unpaid' ? '700' : '500'}; border-radius: 4px; font-size: 11px; color: #0f172a; cursor: pointer;">Not Paid</button>
                  <button type="button" class="fl-pay-date-preset" data-target="paid" data-preset="today" style="padding: 3px 8px; border: 1px solid #e2e8f0; background: #f8fafc; border-radius: 4px; font-size: 11px; font-weight: 500; color: #334155; cursor: pointer;">Today</button>
                  <button type="button" class="fl-pay-date-preset" data-target="paid" data-preset="this-month" style="padding: 3px 8px; border: 1px solid #e2e8f0; background: #f8fafc; border-radius: 4px; font-size: 11px; font-weight: 500; color: #334155; cursor: pointer;">This Month</button>
                  <button type="button" class="fl-pay-date-preset" data-target="paid" data-preset="last-month" style="padding: 3px 8px; border: 1px solid #e2e8f0; background: #f8fafc; border-radius: 4px; font-size: 11px; font-weight: 500; color: #334155; cursor: pointer;">Last Month</button>
                  <button type="button" class="fl-pay-date-preset" data-target="paid" data-preset="past-30d" style="padding: 3px 8px; border: 1px solid #e2e8f0; background: #f8fafc; border-radius: 4px; font-size: 11px; font-weight: 500; color: #334155; cursor: pointer;">Past 30d</button>
                </div>
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 14px;">
                  <div>
                    <label style="display: block; font-size: 11px; font-weight: 600; color: #475569; margin-bottom: 4px;">From</label>
                    <input type="date" id="fl-paid-date-from" value="${paymentFilters.paidDateFrom || ''}" style="width: 100%; height: 32px; padding: 2px 6px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 12px; color: #0f172a; box-sizing: border-box; outline: none; background: #ffffff;" />
                  </div>
                  <div>
                    <label style="display: block; font-size: 11px; font-weight: 600; color: #475569; margin-bottom: 4px;">To</label>
                    <input type="date" id="fl-paid-date-to" value="${paymentFilters.paidDateTo || ''}" style="width: 100%; height: 32px; padding: 2px 6px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 12px; color: #0f172a; box-sizing: border-box; outline: none; background: #ffffff;" />
                  </div>
                </div>
                <div style="display: flex; justify-content: flex-end; align-items: center; gap: 8px; border-top: 1px solid #f1f5f9; padding-top: 10px;">
                  <button type="button" id="fl-paid-date-clear" class="fl-btn fl-btn-secondary" style="padding: 5px 12px; font-size: 12px;">Clear</button>
                  <button type="button" id="fl-paid-date-apply" class="fl-btn fl-btn-primary" style="padding: 5px 14px; font-size: 12px;">Apply</button>
                </div>
              </div>
            </div>
          </div>

          <!-- Right: Refresh Payments -->
          <div style="display: flex; align-items: center; gap: 10px;">
            <button 
              type="button" 
              id="fl-refresh-payments-btn" 
              class="fl-btn fl-btn-secondary" 
              title="Refresh Payments from backend"
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
          </div>
        </div>

        <!-- Applied Filter Chips Row (Image 2) -->
        ${appliedChips.length > 0 ? `
          <div style="padding: 10px 20px; border-bottom: 1px solid #f1f5f9; display: flex; align-items: center; gap: 10px; flex-wrap: wrap;">
            <span style="font-size: 11px; font-weight: 700; color: #475569; letter-spacing: 0.5px;">
              APPLIED FILTERS:
            </span>
            ${appliedChips.map(c => `
              <span style="display: inline-flex; align-items: center; padding: 3px 6px 3px 12px; background: #f0f7ff; border: 1px solid #bfdbfe; border-radius: 9999px; font-size: 12px; color: #1e40af; font-weight: 500;">
                ${c.label} <span class="fl-filter-remove-badge" data-key="${c.key}" data-type="${c.type}" data-val="${c.val || ''}" style="display: inline-flex; align-items: center; justify-content: center; width: 14px; height: 14px; border-radius: 50%; background: #f07176; color: #ffffff; margin-left: 6px; cursor: pointer; flex-shrink: 0;"><svg width="7" height="7" viewBox="0 0 10 10" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="display: block; pointer-events: none;"><path d="M2 2L8 8M8 2L2 8"/></svg></span>
              </span>
            `).join('')}
            <button type="button" id="fl-clear-filters-link" style="border: none; background: transparent; color: #0f172a; font-size: 12px; font-weight: 600; text-decoration: underline; cursor: pointer; margin-left: 6px;">
              Clear All
            </button>
          </div>
        ` : ''}

        <!-- Selected Actions Toolbar (Appears when items are checked) -->
        ${selectedPaymentIds.size > 0 ? `
          <div style="background: #eff6ff; border-bottom: 1px solid #bfdbfe; padding: 10px 20px; display: flex; justify-content: space-between; align-items: center;">
            <span style="font-size: 13px; font-weight: 600; color: #1e40af;">
              ${selectedPaymentIds.size} payment${selectedPaymentIds.size > 1 ? 's' : ''} selected
            </span>
            <div style="display: flex; gap: 8px;">
              <button 
                type="button" 
                id="fl-bulk-pay-wa-btn" 
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
              <button 
                type="button" 
                id="fl-bulk-pay-clear-btn" 
                style="
                  padding: 5px 10px; 
                  border: 1px solid #cbd5e1; 
                  background: #ffffff; 
                  color: #64748b; 
                  border-radius: 6px; 
                  font-size: 12px; 
                  font-weight: 500; 
                  cursor: pointer;
                  transition: all 0.15s ease;
                "
                onmouseover="this.style.background='#f8fafc';"
                onmouseout="this.style.background='#ffffff';"
              >
                Deselect
              </button>
            </div>
          </div>
        ` : ''}

        <!-- Payments Table (Image 2) -->
        <div style="overflow-x: auto; overflow-y: visible; min-height: 120px;">
          <table style="width: 100%; border-collapse: collapse; text-align: left; font-size: 13.5px;">
            <thead>
              <tr style="background: #ffffff; border-bottom: 1px solid #e2e8f0; color: #64748b; font-size: 11px; text-transform: uppercase; font-weight: 700; letter-spacing: 0.5px;">
                <th style="padding: 14px 16px; width: 36px; text-align: center;">
                  <input type="checkbox" id="fl-select-all-pay-checkbox" ${isAllSelected ? 'checked' : ''} style="cursor: pointer;" />
                </th>
                <th style="padding: 14px 16px; cursor: pointer; user-select: none;" class="fl-pay-sort-th" data-sort="member">
                  <div style="display: inline-flex; align-items: center;">
                    <span>MEMBER</span>
                    ${getSortIndicator('member')}
                  </div>
                </th>
                <th style="padding: 14px 16px; cursor: pointer; user-select: none;" class="fl-pay-sort-th" data-sort="amount">
                  <div style="display: inline-flex; align-items: center;">
                    <span>AMOUNT</span>
                    ${getSortIndicator('amount')}
                  </div>
                </th>
                <th style="padding: 14px 16px; cursor: pointer; user-select: none;" class="fl-pay-sort-th" data-sort="method">
                  <div style="display: inline-flex; align-items: center;">
                    <span>METHOD</span>
                    ${getSortIndicator('method')}
                  </div>
                </th>
                <th style="padding: 14px 16px; cursor: pointer; user-select: none;" class="fl-pay-sort-th" data-sort="dueDate">
                  <div style="display: inline-flex; align-items: center;">
                    <span>DUE DATE</span>
                    ${getSortIndicator('dueDate')}
                  </div>
                </th>
                <th style="padding: 14px 16px; cursor: pointer; user-select: none;" class="fl-pay-sort-th" data-sort="paidDate">
                  <div style="display: inline-flex; align-items: center;">
                    <span>PAID DATE</span>
                    ${getSortIndicator('paidDate')}
                  </div>
                </th>
                <th style="padding: 14px 16px;">TXN ID</th>
                <th style="padding: 14px 16px; cursor: pointer; user-select: none;" class="fl-pay-sort-th" data-sort="notes">
                  <div style="display: inline-flex; align-items: center;">
                    <span>NOTES</span>
                    ${getSortIndicator('notes')}
                  </div>
                </th>
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
                    <div style="font-size: 12.5px; margin-top: 4px;">No records match your selected filters.</div>
                  </td>
                </tr>
              ` : pageData.map(p => {
                const isPaid = p.isPaid || p.status === 'paid';
                const isChecked = selectedPaymentIds.has(p.id);

                return `
                  <tr class="fl-payment-row" data-id="${p.id}" style="border-bottom: 1px solid #f1f5f9; transition: background 0.15s; position: relative; ${isChecked ? 'background: #f8fafc;' : ''}">
                    <td style="padding: 14px 16px; text-align: center;" onclick="event.stopPropagation();">
                      <input type="checkbox" class="fl-pay-row-checkbox" data-id="${p.id}" ${isChecked ? 'checked' : ''} style="cursor: pointer;" />
                    </td>
                    <td style="padding: 14px 16px; position: relative;">
                      <div style="display: inline-flex; align-items: center; gap: 6px;">
                        <span class="fl-pay-member-link" data-member-id="${p.memberId}" style="font-weight: 700; color: #0f172a; text-decoration: none; cursor: pointer; display: inline-flex; align-items: center;">
                          ${p.memberName}
                          ${this.getMemberStatusDot(p.memberStatus)}
                        </span>
                        <div class="fl-pay-row-actions-wrap" style="position: relative; display: inline-flex; align-items: center;">
                          <button 
                            type="button" 
                            class="fl-pay-row-dots-btn" 
                            data-id="${p.id}"
                            title="Payment actions"
                            style="
                              display: inline-flex;
                              align-items: center;
                              justify-content: center;
                              width: 22px;
                              height: 22px;
                              padding: 0;
                              border: 1px solid transparent;
                              background: transparent;
                              border-radius: 4px;
                              cursor: pointer;
                              color: #64748b;
                              transition: all 0.15s ease;
                            "
                          >
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                              <circle cx="12" cy="4" r="2"></circle>
                              <circle cx="12" cy="12" r="2"></circle>
                              <circle cx="12" cy="20" r="2"></circle>
                            </svg>
                          </button>
                          <div 
                            class="fl-pay-row-menu" 
                            id="fl-pay-row-menu-${p.id}" 
                            style="
                              display: none;
                              position: fixed;
                              background: #ffffff;
                              border: 1px solid #cbd5e1;
                              border-radius: 8px;
                              box-shadow: 0 10px 25px -3px rgba(0,0,0,0.18), 0 4px 6px -4px rgba(0,0,0,0.1);
                              z-index: 999999;
                              min-width: 135px;
                              padding: 4px 0;
                              white-space: nowrap;
                            "
                          >
                            ${isPaid ? `
                              <div 
                                style="
                                  padding: 8px 12px;
                                  font-size: 12.5px;
                                  font-weight: 600;
                                  color: #94a3b8;
                                  cursor: default;
                                  display: flex;
                                  align-items: center;
                                  gap: 8px;
                                  user-select: none;
                                "
                              >
                                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                                  <polyline points="20 6 9 17 4 12"></polyline>
                                </svg>
                                <span>Already Paid</span>
                              </div>
                            ` : `
                              <div 
                                class="fl-pay-collect-btn" 
                                data-id="${p.id}" 
                                style="
                                  padding: 8px 12px;
                                  font-size: 12.5px;
                                  font-weight: 600;
                                  color: #0f172a;
                                  cursor: pointer;
                                  display: flex;
                                  align-items: center;
                                  gap: 8px;
                                "
                              >
                                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="#16a34a" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                                  <polyline points="20 6 9 17 4 12"></polyline>
                                </svg>
                                <span>Mark Paid</span>
                              </div>
                              <div 
                                class="fl-pay-remind-menu-btn" 
                                data-payment-id="${p.id}" 
                                data-member-id="${p.memberId}"
                                style="
                                  padding: 8px 12px;
                                  font-size: 12.5px;
                                  font-weight: 600;
                                  color: #0f172a;
                                  cursor: pointer;
                                  display: flex;
                                  align-items: center;
                                  gap: 8px;
                                  border-top: 1px solid #f1f5f9;
                                "
                              >
                                <svg width="13" height="13" viewBox="0 0 24 24" style="flex-shrink: 0;">
                                  <path fill="#25D366" d="M12.04 2C6.58 2 2.13 6.45 2.13 11.91C2.13 13.66 2.59 15.36 3.45 16.86L2.05 22L7.3 20.62C8.75 21.41 10.38 21.83 12.04 21.83C17.5 21.83 21.95 17.38 21.95 11.92C21.95 9.27 20.92 6.78 19.05 4.91C17.18 3.03 14.69 2 12.04 2Z"/>
                                  <path fill="#FFFFFF" d="M17.52 14.33C17.22 14.18 15.75 13.45 15.48 13.35C15.2 13.25 15 13.2 14.8 13.5C14.6 13.8 14.03 14.48 13.85 14.68C13.68 14.88 13.5 14.9 13.2 14.75C12.9 14.6 11.94 14.29 10.8 13.27C9.91 12.48 9.31 11.51 9.14 11.21C8.96 10.91 9.12 10.75 9.27 10.6C9.4 10.47 9.56 10.26 9.71 10.08C9.86 9.9 9.91 9.78 10.01 9.58C10.11 9.38 10.06 9.2 9.99 9.05C9.91 8.9 9.31 7.43 9.06 6.83C8.82 6.25 8.57 6.33 8.39 6.32C8.21 6.31 8.01 6.31 7.81 6.31C7.61 6.31 7.29 6.39 7.01 6.69C6.74 6.99 5.96 7.71 5.96 9.19C5.96 10.66 7.04 12.09 7.19 12.29C7.34 12.49 9.31 15.52 12.33 16.82C13.05 17.13 13.61 17.32 14.05 17.46C14.77 17.69 15.42 17.65 15.94 17.58C16.52 17.49 17.72 16.85 17.97 16.15C18.22 15.45 18.22 14.85 18.15 14.72C18.07 14.6 17.82 14.48 17.52 14.33Z"/>
                                </svg>
                                <span>Remind</span>
                              </div>
                            `}
                          </div>
                        </div>
                      </div>
                    </td>
                    <td style="padding: 14px 16px; font-weight: 700; font-size: 13.5px; color: #0f172a;">
                      ${p.amountFormatted}
                    </td>
                    <td style="padding: 14px 16px; font-size: 13px; color: #475569;">
                      ${p.paymentMethod || '—'}
                    </td>
                    <td style="padding: 14px 16px; color: #0f172a; font-size: 13px;">
                      ${p.dueDate}
                    </td>
                    <td style="padding: 14px 16px; color: #64748b; font-size: 13px;">
                      ${p.paidDate || '—'}
                    </td>
                    <td style="padding: 14px 16px; color: #64748b; font-size: 12.5px;">
                      ${p.txnId || '—'}
                    </td>
                    <td style="padding: 14px 16px; font-size: 13px; color: #334155;">
                      ${p.notes || 'Installment'}
                    </td>
                  </tr>
                `;
              }).join('')}
            </tbody>
          </table>
        </div>

        <!-- Pagination Bar (Image 2) -->
        <div style="padding: 14px 20px; border-top: 1px solid #e2e8f0; display: flex; justify-content: space-between; align-items: center; font-size: 12.5px; color: #64748b;">
          <span>
            Showing <strong>${totalCount === 0 ? 0 : startIndex + 1}–${Math.min(startIndex + this.pageSize, totalCount)}</strong> of <strong>${totalCount}</strong> records
          </span>
          <div style="display: flex; align-items: center; gap: 12px;">
            <button type="button" id="fl-prev-pay-btn" ${this.page <= 1 ? 'disabled' : ''} style="padding: 4px 10px; border: 1px solid #e2e8f0; border-radius: 6px; background: #ffffff; font-size: 12px; font-weight: 500; color: #475569; cursor: ${this.page <= 1 ? 'not-allowed' : 'pointer'}; opacity: ${this.page <= 1 ? '0.5' : '1'};">
              ← Prev
            </button>
            <span style="font-weight: 600; color: #0f172a;">Page ${this.page} of ${totalPages}</span>
            <button type="button" id="fl-next-pay-btn" ${this.page >= totalPages ? 'disabled' : ''} style="padding: 4px 10px; border: 1px solid #e2e8f0; border-radius: 6px; background: #ffffff; font-size: 12px; font-weight: 500; color: #475569; cursor: ${this.page >= totalPages ? 'not-allowed' : 'pointer'}; opacity: ${this.page >= totalPages ? '0.5' : '1'};">
              Next →
            </button>
          </div>
        </div>

      </div>

      <style>
        .fl-payment-row:hover {
          background: #f8fafc;
        }
        .fl-payment-row:has(.fl-menu-open) {
          z-index: 50 !important;
        }
        .fl-pay-row-dots-btn {
          opacity: 0;
          pointer-events: none;
        }
        .fl-payment-row:hover .fl-pay-row-dots-btn,
        .fl-pay-row-dots-btn.fl-menu-open {
          opacity: 1;
          pointer-events: auto;
        }
        .fl-pay-row-dots-btn:hover {
          background: #e2e8f0 !important;
          color: #0f172a !important;
        }
        .fl-pay-collect-btn:hover,
        .fl-pay-remind-menu-btn:hover {
          background: #f8fafc;
          color: #16a34a !important;
        }
        .fl-pay-member-link:hover {
          color: #2563eb !important;
        }
        .fl-drop-item:hover, .fl-drop-checkbox:hover {
          background: #f1f5f9;
        }
        .fl-pay-filter-trigger:hover {
          border-color: #cbd5e1 !important;
        }
        .fl-pay-filter-trigger.fl-dropdown-open {
          border-color: #0f172a !important;
          color: #0f172a !important;
          font-weight: 600 !important;
        }
        .fl-pay-filter-trigger.fl-dropdown-open .fl-filter-arrow {
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
      const nextInput = this.querySelector('#fl-payment-search-input');
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

  bindEvents(pageData) {
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

    // Refresh payments button
    const refreshBtn = this.querySelector('#fl-refresh-payments-btn');
    if (refreshBtn) {
      refreshBtn.addEventListener('click', () => {
        HomeStore.refreshGymData(true);
      });
    }

    // Dropdown toggles
    this.querySelectorAll('.fl-pay-filter-trigger').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const dropId = btn.getAttribute('data-dropdown');
        const panel = this.querySelector(`#fl-dropdown-${dropId}`);
        const isShown = panel && panel.style.display === 'block';
        // Close all dropdowns, remove open class, and reset arrows
        this.querySelectorAll('.fl-dropdown-panel').forEach(p => p.style.display = 'none');
        this.querySelectorAll('.fl-pay-filter-trigger').forEach(b => b.classList.remove('fl-dropdown-open'));
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
        this.querySelectorAll('.fl-pay-filter-trigger').forEach(b => b.classList.remove('fl-dropdown-open'));
        this.querySelectorAll('.fl-filter-arrow').forEach(a => a.style.transform = 'rotate(0deg)');
      });
    }

    // Stop click bubbling on dropdown panels so date picker interactions do not close the panel
    this.querySelectorAll('.fl-dropdown-panel').forEach(p => {
      p.addEventListener('click', (e) => {
        e.stopPropagation();
      });
    });

    // Date Presets
    const presets = getPresetDates();
    this.querySelectorAll('.fl-pay-date-preset').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const target = btn.getAttribute('data-target');
        const preset = btn.getAttribute('data-preset');
        let from = '';
        let to = '';

        if (target === 'due') {
          if (preset === 'month-end' || preset === 'this-month') {
            from = '';
            to = presets.lastDayThisMonthStr;
          } else if (preset === 'next-7d') {
            from = presets.todayStr;
            to = presets.plus7Str;
          } else if (preset === 'next-30d') {
            from = presets.todayStr;
            to = presets.plus30Str;
          } else if (preset === 'overdue') {
            from = '';
            to = presets.yesterdayStr;
          }
          const fromInput = this.querySelector('#fl-due-date-from');
          const toInput = this.querySelector('#fl-due-date-to');
          if (fromInput) fromInput.value = from;
          if (toInput) toInput.value = to;
          HomeStore.setPaymentDateFilter('due', from, to);
        } else if (target === 'paid') {
          if (preset === 'unpaid') {
            const fromInput = this.querySelector('#fl-paid-date-from');
            const toInput = this.querySelector('#fl-paid-date-to');
            if (fromInput) fromInput.value = '';
            if (toInput) toInput.value = '';
            this.querySelectorAll('.fl-dropdown-panel').forEach(p => p.style.display = 'none');
            HomeStore.setPaymentFilter('paidDate', 'unpaid');
            HomeStore.setPaymentFilter('paidDateFrom', '');
            HomeStore.setPaymentFilter('paidDateTo', '');
            return;
          } else if (preset === 'today') {
            from = presets.todayStr;
            to = presets.todayStr;
          } else if (preset === 'this-month') {
            from = presets.firstDayThisMonthStr;
            to = presets.lastDayThisMonthStr;
          } else if (preset === 'last-month') {
            from = presets.firstDayLastMonthStr;
            to = presets.lastDayLastMonthStr;
          } else if (preset === 'past-30d') {
            from = presets.minus30Str;
            to = presets.todayStr;
          }
          const fromInput = this.querySelector('#fl-paid-date-from');
          const toInput = this.querySelector('#fl-paid-date-to');
          if (fromInput) fromInput.value = from;
          if (toInput) toInput.value = to;
          HomeStore.setPaymentDateFilter('paid', from, to);
        }
      });
    });

    // Due Date Apply & Clear
    const dueApply = this.querySelector('#fl-due-date-apply');
    if (dueApply) {
      dueApply.addEventListener('click', (e) => {
        e.stopPropagation();
        const from = this.querySelector('#fl-due-date-from')?.value || '';
        const to = this.querySelector('#fl-due-date-to')?.value || '';
        this.querySelectorAll('.fl-dropdown-panel').forEach(p => p.style.display = 'none');
        HomeStore.setPaymentDateFilter('due', from, to);
      });
    }

    const dueClear = this.querySelector('#fl-due-date-clear');
    if (dueClear) {
      dueClear.addEventListener('click', (e) => {
        e.stopPropagation();
        this.querySelectorAll('.fl-dropdown-panel').forEach(p => p.style.display = 'none');
        HomeStore.setPaymentDateFilter('due', '', '');
      });
    }

    // Paid Date Apply & Clear
    const paidApply = this.querySelector('#fl-paid-date-apply');
    if (paidApply) {
      paidApply.addEventListener('click', (e) => {
        e.stopPropagation();
        const from = this.querySelector('#fl-paid-date-from')?.value || '';
        const to = this.querySelector('#fl-paid-date-to')?.value || '';
        this.querySelectorAll('.fl-dropdown-panel').forEach(p => p.style.display = 'none');
        HomeStore.setPaymentDateFilter('paid', from, to);
      });
    }

    const paidClear = this.querySelector('#fl-paid-date-clear');
    if (paidClear) {
      paidClear.addEventListener('click', (e) => {
        e.stopPropagation();
        this.querySelectorAll('.fl-dropdown-panel').forEach(p => p.style.display = 'none');
        HomeStore.setPaymentDateFilter('paid', '', '');
      });
    }

    // Amount Dual Slider & Inputs
    const minSlider = this.querySelector('#fl-amount-min-slider');
    const maxSlider = this.querySelector('#fl-amount-max-slider');
    const minInput = this.querySelector('#fl-amount-min-input');
    const maxInput = this.querySelector('#fl-amount-max-input');
    const amountTrack = this.querySelector('#fl-amount-slider-track');
    const amountDisplay = this.querySelector('#fl-amount-range-display');
    const sliderMaxLimit = maxSlider ? Number(maxSlider.getAttribute('max')) || 20000 : 20000;

    const updateAmountUI = (minVal, maxVal) => {
      const min = Math.max(0, Math.min(Number(minVal), Number(maxVal)));
      const max = Math.min(sliderMaxLimit, Math.max(Number(minVal), Number(maxVal)));

      if (minSlider) minSlider.value = min;
      if (maxSlider) maxSlider.value = max;
      if (minInput) minInput.value = min;
      if (maxInput) maxInput.value = max;

      if (amountTrack) {
        const leftPct = Math.round((min / sliderMaxLimit) * 100);
        const rightPct = Math.round(100 - (max / sliderMaxLimit) * 100);
        amountTrack.style.left = `${leftPct}%`;
        amountTrack.style.right = `${rightPct}%`;
      }

      if (amountDisplay) {
        amountDisplay.textContent = `₹${min.toLocaleString('en-IN')} – ${max >= sliderMaxLimit ? `₹${sliderMaxLimit.toLocaleString('en-IN')}+` : `₹${max.toLocaleString('en-IN')}`}`;
      }
    };

    if (minSlider && maxSlider) {
      minSlider.addEventListener('input', (e) => {
        e.stopPropagation();
        minSlider.style.zIndex = '5';
        maxSlider.style.zIndex = '4';
        let min = Number(minSlider.value);
        let max = Number(maxSlider.value);
        if (min > max) {
          min = max;
          minSlider.value = min;
        }
        updateAmountUI(min, max);
      });

      maxSlider.addEventListener('input', (e) => {
        e.stopPropagation();
        maxSlider.style.zIndex = '5';
        minSlider.style.zIndex = '4';
        let min = Number(minSlider.value);
        let max = Number(maxSlider.value);
        if (max < min) {
          max = min;
          maxSlider.value = max;
        }
        updateAmountUI(min, max);
      });
    }

    if (minInput) {
      minInput.addEventListener('change', (e) => {
        e.stopPropagation();
        let min = Math.max(0, Math.min(Number(minInput.value) || 0, sliderMaxLimit));
        let max = Number(maxSlider?.value || sliderMaxLimit);
        if (min > max) min = max;
        updateAmountUI(min, max);
      });
    }

    if (maxInput) {
      maxInput.addEventListener('change', (e) => {
        e.stopPropagation();
        let max = Math.max(0, Math.min(Number(maxInput.value) || sliderMaxLimit, sliderMaxLimit));
        let min = Number(minSlider?.value || 0);
        if (max < min) max = min;
        updateAmountUI(min, max);
      });
    }

    // Amount Presets
    this.querySelectorAll('.fl-pay-amount-preset').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const pMin = Number(btn.getAttribute('data-min')) || 0;
        const pMax = Number(btn.getAttribute('data-max')) || sliderMaxLimit;
        updateAmountUI(pMin, pMax);
        this.querySelectorAll('.fl-dropdown-panel').forEach(p => p.style.display = 'none');
        HomeStore.setPaymentAmountRange(pMin, pMax, sliderMaxLimit);
      });
    });

    // Amount Apply
    const amountApply = this.querySelector('#fl-amount-apply');
    if (amountApply) {
      amountApply.addEventListener('click', (e) => {
        e.stopPropagation();
        const min = Number(minSlider?.value || 0);
        const max = Number(maxSlider?.value || sliderMaxLimit);
        this.querySelectorAll('.fl-dropdown-panel').forEach(p => p.style.display = 'none');
        HomeStore.setPaymentAmountRange(min, max, sliderMaxLimit);
      });
    }

    // Amount Clear
    const amountClear = this.querySelector('#fl-amount-clear');
    if (amountClear) {
      amountClear.addEventListener('click', (e) => {
        e.stopPropagation();
        this.querySelectorAll('.fl-dropdown-panel').forEach(p => p.style.display = 'none');
        HomeStore.setPaymentAmountRange(0, sliderMaxLimit, sliderMaxLimit);
      });
    }

    // Option items click (single select)
    this.querySelectorAll('.fl-drop-item').forEach(item => {
      item.addEventListener('click', (e) => {
        e.stopPropagation();
        const fKey = item.getAttribute('data-filter');
        const fVal = item.getAttribute('data-val');
        this.querySelectorAll('.fl-dropdown-panel').forEach(p => p.style.display = 'none');
        HomeStore.setPaymentFilter(fKey, fVal);
      });
    });

    // Applied chip remove
    this.querySelectorAll('.fl-filter-remove-badge').forEach(badge => {
      badge.addEventListener('click', (e) => {
        e.stopPropagation();
        const type = badge.getAttribute('data-type');
        if (type === 'unpaidOnly') {
          HomeStore.setPaymentFilter('unpaidOnly', false);
        } else if (type === 'method') {
          HomeStore.setPaymentFilter('method', 'all');
        } else if (type === 'amount') {
          HomeStore.setPaymentAmountRange(0, sliderMaxLimit, sliderMaxLimit);
        } else if (type === 'duedate') {
          HomeStore.setPaymentDateFilter('due', '', '');
        } else if (type === 'paiddate') {
          HomeStore.setPaymentFilter('paidDate', 'all');
          HomeStore.setPaymentDateFilter('paid', '', '');
        }
      });
    });

    // Clear all
    const clearBtn = this.querySelector('#fl-clear-filters-link');
    if (clearBtn) {
      clearBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        HomeStore.clearPaymentFilters();
      });
    }

    // Sorting table headers click
    this.querySelectorAll('.fl-pay-sort-th').forEach(th => {
      th.addEventListener('click', () => {
        const field = th.getAttribute('data-sort');
        if (field) {
          HomeStore.togglePaymentSort(field);
        }
      });
    });

    // Member link click -> open drawer
    this.querySelectorAll('.fl-pay-member-link').forEach(link => {
      link.addEventListener('click', (e) => {
        e.stopPropagation();
        const memberId = link.getAttribute('data-member-id');
        const member = HomeStore.getState().members.find(m => m.id === memberId);
        if (member) HomeStore.openMemberDrawer(member);
      });
    });

    // Payment row three-dots toggle
    this.querySelectorAll('.fl-pay-row-dots-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const payId = btn.getAttribute('data-id');
        const menu = this.querySelector(`#fl-pay-row-menu-${payId}`);
        const isShown = menu && menu.style.display === 'block';

        // Close all other row menus and dropdowns
        this.querySelectorAll('.fl-pay-row-menu').forEach(m => m.style.display = 'none');
        this.querySelectorAll('.fl-pay-row-dots-btn').forEach(b => b.classList.remove('fl-menu-open'));

        if (menu && !isShown) {
          const rect = btn.getBoundingClientRect();
          menu.style.top = `${rect.bottom + 4}px`;
          menu.style.left = `${rect.left}px`;
          menu.style.display = 'block';
          btn.classList.add('fl-menu-open');
        }
      });
    });

    // Close row menu on document click or scroll
    if (!this.hasRowMenuCloseListener) {
      this.hasRowMenuCloseListener = true;
      const closeAllRowMenus = () => {
        this.querySelectorAll('.fl-pay-row-menu').forEach(m => m.style.display = 'none');
        this.querySelectorAll('.fl-pay-row-dots-btn').forEach(b => b.classList.remove('fl-menu-open'));
      };
      document.addEventListener('click', closeAllRowMenus);
      window.addEventListener('scroll', closeAllRowMenus, true);
    }

    // Collect Payment buttons (Mark Paid)
    this.querySelectorAll('.fl-pay-collect-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        this.querySelectorAll('.fl-pay-row-menu').forEach(m => m.style.display = 'none');
        this.querySelectorAll('.fl-pay-row-dots-btn').forEach(b => b.classList.remove('fl-menu-open'));
        const paymentId = btn.getAttribute('data-id');
        const payment = HomeStore.getState().payments.find(p => p.id === paymentId);
        if (payment) {
          HomeStore.openCollectPaymentModal(payment);
        }
      });
    });

    // Remind buttons in row 3-dots menu
    this.querySelectorAll('.fl-pay-remind-menu-btn').forEach(btn => {
      btn.addEventListener('click', async (e) => {
        e.stopPropagation();
        this.querySelectorAll('.fl-pay-row-menu').forEach(m => m.style.display = 'none');
        this.querySelectorAll('.fl-pay-row-dots-btn').forEach(b => b.classList.remove('fl-menu-open'));

        const paymentId = btn.getAttribute('data-payment-id');
        const memberId = btn.getAttribute('data-member-id');
        const member = HomeStore.getState().members.find(m => m.id === memberId);
        const payment = HomeStore.getState().payments.find(p => p.id === paymentId);

        if (payment && member) {
          try {
            await HomeStore.sendPaymentReminder(payment, member);
          } catch (err) {
            console.error('Failed to send payment reminder:', err);
          }
        }
      });
    });

    // Remind buttons
    this.querySelectorAll('.fl-pay-remind-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const memberId = btn.getAttribute('data-id');
        const paymentId = btn.getAttribute('data-payment-id');
        const member = HomeStore.getState().members.find(m => m.id === memberId);
        const payment = HomeStore.getState().payments.find(p => p.id === paymentId);
        if (member) {
          const tplKey = (payment && (payment.isOverdue || payment.status === 'overdue'))
            ? 'payment_overdue_reminder'
            : 'upcoming_payment_reminder';
          HomeStore.openWhatsAppModal(member, tplKey, payment);
        }
      });
    });

    // Bulk WhatsApp Broadcast from Payments
    const bulkPayWaBtn = this.querySelector('#fl-bulk-pay-wa-btn');
    if (bulkPayWaBtn) {
      bulkPayWaBtn.addEventListener('click', () => {
        const state = HomeStore.getState();
        const selectedPayments = state.payments.filter(p => state.selectedPaymentIds.has(p.id));
        if (selectedPayments.length === 0) return;

        // Resolve unique members for selected payments
        const memberMap = new Map();
        selectedPayments.forEach(p => {
          const m = state.members.find(mem => mem.id === p.memberId);
          if (m && !memberMap.has(m.id)) {
            memberMap.set(m.id, m);
          }
        });
        const recipients = Array.from(memberMap.values());
        if (recipients.length === 0) return;

        // Pick appropriate default template: overdue reminder if any is overdue, else upcoming reminder
        const hasOverdue = selectedPayments.some(p => p.isOverdue || p.status === 'overdue');
        const defaultTpl = hasOverdue ? 'payment_overdue_reminder' : 'upcoming_payment_reminder';

        HomeStore.openBulkWhatsAppModal(recipients, defaultTpl);
      });
    }

    // Bulk clear payment selection
    const bulkPayClearBtn = this.querySelector('#fl-bulk-pay-clear-btn');
    if (bulkPayClearBtn) {
      bulkPayClearBtn.addEventListener('click', () => {
        HomeStore.clearPaymentSelection();
      });
    }

    // Select all payment checkbox
    const selectAllPay = this.querySelector('#fl-select-all-pay-checkbox');
    if (selectAllPay) {
      selectAllPay.addEventListener('change', (e) => {
        if (e.target.checked) {
          HomeStore.selectAllPayments(pageData.map(p => p.id));
        } else {
          HomeStore.clearPaymentSelection();
        }
      });
    }

    // Row payment checkboxes
    this.querySelectorAll('.fl-pay-row-checkbox').forEach(cb => {
      cb.addEventListener('change', () => {
        HomeStore.togglePaymentSelection(cb.getAttribute('data-id'));
      });
    });

    // Pagination buttons
    const prevBtn = this.querySelector('#fl-prev-pay-btn');
    if (prevBtn) {
      prevBtn.addEventListener('click', () => {
        if (this.page > 1) {
          this.page--;
          this.render();
        }
      });
    }

    const nextBtn = this.querySelector('#fl-next-pay-btn');
    if (nextBtn) {
      nextBtn.addEventListener('click', () => {
        this.page++;
        this.render();
      });
    }
  }
}

customElements.define('fl-payments-section', FlPaymentsSection);
