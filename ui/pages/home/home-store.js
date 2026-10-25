/**
 * HomeStore - Centralized Reactive State Store for Home and its Sections
 * Strict Live Backend Integration without hardcoded data fallbacks.
 * Any API failures trigger the global top-center red error window.
 */

import { api } from '../../core/api/client.js';
import { AppConfig } from '../../core/config/app-config.js';
import { ErrorMapper } from '../../core/error/error-mapper.js';
import { SessionManager } from '../../core/auth/session.js';
import { GymCache } from '../../core/storage/gym-cache.js';

class HomeStoreClass {
  constructor() {
    this.state = {
      activeSection: 'members', // 'members' | 'payments' | 'plans' | 'chats'
      members: [],
      rawMembers: [],
      payments: [],
      availablePlans: [],
      gyms: [],
      currentGymId: null,
      conversations: [],
      activeChatId: null,
      chatFilterTab: 'all', // 'all' | 'needs_attention' | 'active'
      chatSearchQuery: '',
      isTemplateModalOpen: false,
      searchQuery: '',
      memberStatusTab: 'all', // 'all' | 'active' | 'renewals' | 'expired' | 'prospect'
      memberSort: {
        field: null, // 'name' | 'plans' | 'status' | null
        direction: 'asc' // 'asc' | 'desc'
      },
      paymentSort: {
        field: null, // 'member' | 'amount' | 'method' | 'status' | 'dueDate' | 'paidDate' | 'notes' | null
        direction: 'asc' // 'asc' | 'desc'
      },
      paymentStatusTab: 'all', // 'all' | 'paid' | 'upcoming' | 'overdue'
      memberFilters: {
        plan: 'all',
        pendingAmount: 'all',
        pendingAmountMin: null,
        pendingAmountMax: null,
        status: 'all',
        expiry: 'all',
        expiryFrom: '',
        expiryTo: ''
      },
      paymentFilters: {
        amount: 'all',
        amountMin: null,
        amountMax: null,
        statuses: new Set(['unpaid']),
        method: 'all',
        dueDate: 'oct-2026',
        dueDateFrom: '',
        dueDateTo: '2026-10-31',
        paidDate: 'all',
        paidDateFrom: '',
        paidDateTo: ''
      },
      planFilters: {
        status: 'all'
      },
      selectedMemberIds: new Set(),
      selectedPaymentIds: new Set(),
      loadError: null,
      syncingGymData: false,
      lastSyncedAt: null,
      loading: {
        members: false,
        plans: false,
        gyms: false
      },
      // Modals and Drawers
      memberDrawer: {
        isOpen: false,
        member: null
      },
      addMemberModal: {
        isOpen: false
      },
      renewMemberModal: {
        isOpen: false,
        member: null,
        preselectedPlanId: null
      },
      collectPaymentModal: {
        isOpen: false,
        payment: null,
        member: null
      },
      whatsAppModal: {
        isOpen: false,
        member: null,
        defaultTemplate: 'EXPIRY_REMINDER',
        paymentInfo: null
      },
      planModal: {
        isOpen: false,
        mode: 'create',
        plan: null
      }
    };

    this.listeners = new Set();
  }

  getState() {
    return this.state;
  }

  subscribe(listener) {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  notify() {
    for (const listener of this.listeners) {
      try {
        listener(this.state);
      } catch (err) {
        console.error('HomeStore listener error:', err);
      }
    }
  }

  setActiveSection(section) {
    if (this.state.activeSection !== section) {
      this.state.activeSection = section;
      this.state.searchQuery = '';
      this.state.addMemberModal = { isOpen: false };
      this.state.renewMemberModal = { isOpen: false, member: null, preselectedPlanId: null };
      this.state.collectPaymentModal = { isOpen: false, payment: null, member: null };
      this.state.whatsAppModal = { isOpen: false, member: null, defaultTemplate: 'EXPIRY_REMINDER', paymentInfo: null };
      this.state.planModal = { isOpen: false, mode: 'create', plan: null };
      this.state.memberDrawer = { isOpen: false, member: null };
      this.notify();
    }
  }

  setSearchQuery(q) {
    this.state.searchQuery = q || '';
    if (this.state.memberPagination) this.state.memberPagination.page = 1;
    if (this.state.paymentPagination) this.state.paymentPagination.page = 1;
    this.notify();
  }

  setMemberStatusTab(tab) {
    this.state.memberStatusTab = tab;
    if (this.state.memberFilters) {
      this.state.memberFilters.status = tab;
    }
    this.notify();
  }

  setMemberFilter(key, value) {
    if (this.state.memberFilters) {
      this.state.memberFilters[key] = value;
      if (key === 'status') {
        this.state.memberStatusTab = value;
      }
      this.notify();
    }
  }

  toggleMemberSort(field) {
    if (!this.state.memberSort) {
      this.state.memberSort = { field: null, direction: 'asc' };
    }
    if (this.state.memberSort.field === field) {
      if (this.state.memberSort.direction === 'asc') {
        this.state.memberSort.direction = 'desc';
      } else {
        // cycle back to no sorting
        this.state.memberSort.field = null;
        this.state.memberSort.direction = 'asc';
      }
    } else {
      this.state.memberSort.field = field;
      this.state.memberSort.direction = 'asc';
    }
    this.notify();
  }

  togglePaymentSort(field) {
    if (!this.state.paymentSort) {
      this.state.paymentSort = { field: null, direction: 'asc' };
    }
    if (this.state.paymentSort.field === field) {
      if (this.state.paymentSort.direction === 'asc') {
        this.state.paymentSort.direction = 'desc';
      } else {
        // cycle back to no sorting
        this.state.paymentSort.field = null;
        this.state.paymentSort.direction = 'asc';
      }
    } else {
      this.state.paymentSort.field = field;
      this.state.paymentSort.direction = 'asc';
    }
    this.notify();
  }

  clearMemberFilters() {
    this.state.memberFilters = {
      plan: 'all',
      pendingAmount: 'all',
      pendingAmountMin: null,
      pendingAmountMax: null,
      status: 'all',
      expiry: 'all',
      expiryFrom: '',
      expiryTo: ''
    };
    this.state.memberStatusTab = 'all';
    this.notify();
  }

  setMemberPendingAmountRange(min, max, maxLimit = 20000) {
    if (this.state.memberFilters) {
      const minNum = (min !== null && min !== undefined && min !== '') ? Math.max(0, Number(min)) : 0;
      const maxNum = (max !== null && max !== undefined && max !== '') ? Number(max) : maxLimit;
      this.state.memberFilters.pendingAmountMin = minNum;
      this.state.memberFilters.pendingAmountMax = maxNum;
      this.state.memberFilters.pendingAmount = (minNum > 0 || maxNum < maxLimit) ? 'custom' : 'all';
      if (this.state.memberPagination) this.state.memberPagination.page = 1;
      this.notify();
    }
  }

  setMemberDateFilter(field, from, to) {
    if (this.state.memberFilters) {
      if (field === 'expiry') {
        this.state.memberFilters.expiryFrom = from || '';
        this.state.memberFilters.expiryTo = to || '';
        this.state.memberFilters.expiry = (from || to) ? 'custom' : 'all';
      }
      if (this.state.memberPagination) this.state.memberPagination.page = 1;
      this.notify();
    }
  }

  setPaymentStatusTab(tab) {
    this.state.paymentStatusTab = tab;
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const pad = (n) => String(n).padStart(2, '0');
    const toIso = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
    const todayStr = toIso(today);

    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);
    const yesterdayStr = toIso(yesterday);

    if (tab === 'all') {
      this.state.paymentFilters.statuses.clear();
      this.state.paymentFilters.dueDate = 'all';
      this.state.paymentFilters.dueDateFrom = '';
      this.state.paymentFilters.dueDateTo = '';
    } else if (tab === 'pending') {
      this.state.paymentFilters.statuses = new Set(['unpaid']);
      this.state.paymentFilters.dueDate = 'all';
      this.state.paymentFilters.dueDateFrom = '';
      this.state.paymentFilters.dueDateTo = '';
      this.state.paymentFilters.amount = 'all';
      this.state.paymentFilters.paidDate = 'all';
      this.state.paymentFilters.paidDateFrom = '';
      this.state.paymentFilters.paidDateTo = '';
    } else if (tab === 'paid') {
      this.state.paymentFilters.statuses = new Set(['paid']);
      this.state.paymentFilters.dueDate = 'all';
      this.state.paymentFilters.dueDateFrom = '';
      this.state.paymentFilters.dueDateTo = '';
    } else if (tab === 'unpaid') {
      this.state.paymentFilters.statuses = new Set(['unpaid']);
      this.state.paymentFilters.dueDate = 'all';
      this.state.paymentFilters.dueDateFrom = '';
      this.state.paymentFilters.dueDateTo = '';
    } else if (tab === 'overdue') {
      this.state.paymentFilters.statuses = new Set(['unpaid']);
      this.state.paymentFilters.dueDate = 'overdue';
      this.state.paymentFilters.dueDateFrom = '';
      this.state.paymentFilters.dueDateTo = yesterdayStr;
    } else if (tab === 'upcoming') {
      this.state.paymentFilters.statuses = new Set(['unpaid']);
      this.state.paymentFilters.dueDate = 'upcoming';
      this.state.paymentFilters.dueDateFrom = todayStr;
      this.state.paymentFilters.dueDateTo = '';
    } else {
      this.state.paymentFilters.statuses = new Set([tab]);
    }
    if (this.state.paymentPagination) this.state.paymentPagination.page = 1;
    this.notify();
  }

  filterActiveMembersValidBeyondThisMonth() {
    const now = new Date();
    now.setHours(0, 0, 0, 0);
    const pad = (n) => String(n).padStart(2, '0');
    // Day after last day of this month = first day of next month
    const firstDayNextMonth = new Date(now.getFullYear(), now.getMonth() + 1, 1);
    const firstDayNextMonthStr = `${firstDayNextMonth.getFullYear()}-${pad(firstDayNextMonth.getMonth() + 1)}-${pad(firstDayNextMonth.getDate())}`;

    this.state.memberFilters = {
      plan: 'all',
      pendingAmount: 'all',
      pendingAmountMin: null,
      pendingAmountMax: null,
      noPlanOnly: false,
      expiry: 'custom',
      expiryFrom: firstDayNextMonthStr,
      expiryTo: ''
    };
    if (this.state.memberPagination) {
      this.state.memberPagination.page = 1;
    }
    this.notify();
  }

  filterRenewalMembersExpiringThisMonth() {
    const now = new Date();
    now.setHours(0, 0, 0, 0);
    const pad = (n) => String(n).padStart(2, '0');
    const todayStr = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
    const lastDayThisMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0);
    const lastDayThisMonthStr = `${lastDayThisMonth.getFullYear()}-${pad(lastDayThisMonth.getMonth() + 1)}-${pad(lastDayThisMonth.getDate())}`;

    this.state.memberFilters = {
      plan: 'all',
      pendingAmount: 'all',
      pendingAmountMin: null,
      pendingAmountMax: null,
      noPlanOnly: false,
      expiry: 'custom',
      expiryFrom: todayStr,
      expiryTo: lastDayThisMonthStr
    };
    if (this.state.memberPagination) {
      this.state.memberPagination.page = 1;
    }
    this.notify();
  }

  filterExpiredMembers() {
    const now = new Date();
    now.setHours(0, 0, 0, 0);
    const pad = (n) => String(n).padStart(2, '0');
    const yesterday = new Date(now);
    yesterday.setDate(yesterday.getDate() - 1);
    const yesterdayStr = `${yesterday.getFullYear()}-${pad(yesterday.getMonth() + 1)}-${pad(yesterday.getDate())}`;

    this.state.memberFilters = {
      plan: 'all',
      pendingAmount: 'all',
      pendingAmountMin: null,
      pendingAmountMax: null,
      noPlanOnly: false,
      expiry: 'custom',
      expiryFrom: '',
      expiryTo: yesterdayStr
    };
    if (this.state.memberPagination) {
      this.state.memberPagination.page = 1;
    }
    this.notify();
  }

  filterPotentialMembers() {
    this.state.memberFilters = {
      plan: 'all',
      pendingAmount: 'all',
      pendingAmountMin: null,
      pendingAmountMax: null,
      noPlanOnly: true,
      expiry: 'all',
      expiryFrom: '',
      expiryTo: ''
    };
    if (this.state.memberPagination) {
      this.state.memberPagination.page = 1;
    }
    this.notify();
  }

  filterPaymentsToCollectThisMonth() {
    const now = new Date();
    now.setHours(0, 0, 0, 0);
    const pad = (n) => String(n).padStart(2, '0');
    const lastDayThisMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0);
    const lastDayThisMonthStr = `${lastDayThisMonth.getFullYear()}-${pad(lastDayThisMonth.getMonth() + 1)}-${pad(lastDayThisMonth.getDate())}`;

    this.state.paymentFilters = {
      ...this.state.paymentFilters,
      dueDate: 'this-month',
      dueDateFrom: '',
      dueDateTo: lastDayThisMonthStr,
      unpaidOnly: true,
      amount: 'all',
      amountMin: null,
      amountMax: null,
      paidDate: 'all',
      paidDateFrom: '',
      paidDateTo: ''
    };
    if (this.state.paymentPagination) {
      this.state.paymentPagination.page = 1;
    }
    this.notify();
  }

  setPaymentFilter(key, value) {
    if (this.state.paymentFilters) {
      this.state.paymentFilters[key] = value;
      this.notify();
    }
  }

  setPaymentAmountRange(min, max, maxLimit = 20000) {
    if (this.state.paymentFilters) {
      const minNum = (min !== null && min !== undefined && min !== '') ? Math.max(0, Number(min)) : 0;
      const maxNum = (max !== null && max !== undefined && max !== '') ? Number(max) : maxLimit;
      this.state.paymentFilters.amountMin = minNum;
      this.state.paymentFilters.amountMax = maxNum;
      this.state.paymentFilters.amount = (minNum > 0 || maxNum < maxLimit) ? 'custom' : 'all';
      if (this.state.paymentPagination) this.state.paymentPagination.page = 1;
      this.notify();
    }
  }

  setPaymentDateFilter(field, from, to) {
    if (this.state.paymentFilters) {
      if (field === 'due') {
        this.state.paymentFilters.dueDateFrom = from || '';
        this.state.paymentFilters.dueDateTo = to || '';
        this.state.paymentFilters.dueDate = (from || to) ? 'custom' : 'all';
      } else if (field === 'paid') {
        this.state.paymentFilters.paidDateFrom = from || '';
        this.state.paymentFilters.paidDateTo = to || '';
        this.state.paymentFilters.paidDate = (from || to) ? 'custom' : 'all';
      }
      if (this.state.paymentPagination) this.state.paymentPagination.page = 1;
      this.notify();
    }
  }

  togglePaymentStatus(status) {
    if (!this.state.paymentFilters.statuses) {
      this.state.paymentFilters.statuses = new Set();
    }

    if (this.state.paymentFilters.statuses.has(status)) {
      this.state.paymentFilters.statuses.delete(status);
    } else {
      this.state.paymentFilters.statuses.add(status);
    }
    if (this.state.paymentPagination) this.state.paymentPagination.page = 1;
    this.notify();
  }

  clearPaymentFilters() {
    this.state.paymentFilters = {
      amount: 'all',
      amountMin: null,
      amountMax: null,
      statuses: new Set(),
      method: 'all',
      dueDate: 'all',
      dueDateFrom: '',
      dueDateTo: '',
      paidDate: 'all',
      paidDateFrom: '',
      paidDateTo: ''
    };
    this.state.paymentStatusTab = 'all';
    this.notify();
  }

  setPlanFilter(key, value) {
    if (this.state.planFilters) {
      this.state.planFilters[key] = value;
      this.notify();
    }
  }

  clearPlanFilters() {
    this.state.planFilters = {
      status: 'all'
    };
    this.notify();
  }

  toggleMemberSelection(id) {
    if (this.state.selectedMemberIds.has(id)) {
      this.state.selectedMemberIds.delete(id);
    } else {
      this.state.selectedMemberIds.add(id);
    }
    this.notify();
  }

  selectAllMembers(ids) {
    ids.forEach(id => this.state.selectedMemberIds.add(id));
    this.notify();
  }

  clearMemberSelection() {
    this.state.selectedMemberIds.clear();
    this.notify();
  }

  togglePaymentSelection(id) {
    if (!this.state.selectedPaymentIds) {
      this.state.selectedPaymentIds = new Set();
    }
    if (this.state.selectedPaymentIds.has(id)) {
      this.state.selectedPaymentIds.delete(id);
    } else {
      this.state.selectedPaymentIds.add(id);
    }
    this.notify();
  }

  selectAllPayments(ids) {
    if (!this.state.selectedPaymentIds) {
      this.state.selectedPaymentIds = new Set();
    }
    ids.forEach(id => this.state.selectedPaymentIds.add(id));
    this.notify();
  }

  clearPaymentSelection() {
    if (this.state.selectedPaymentIds) {
      this.state.selectedPaymentIds.clear();
    }
    this.notify();
  }

  openMemberDrawer(member) {
    this.state.memberDrawer = { isOpen: true, member };
    this.notify();
  }

  closeMemberDrawer() {
    this.state.memberDrawer = { isOpen: false, member: null };
    this.notify();
  }

  openAddMemberModal() {
    this.state.addMemberModal = { isOpen: true };
    this.notify();
  }

  closeAddMemberModal() {
    this.state.addMemberModal = { isOpen: false };
    this.notify();
    this.loadInitialData();
  }

  openRenewMemberModal(member, preselectedPlanId = null) {
    this.state.renewMemberModal = {
      isOpen: true,
      member,
      preselectedPlanId
    };
    this.notify();
  }

  closeRenewMemberModal() {
    this.state.renewMemberModal = {
      isOpen: false,
      member: null,
      preselectedPlanId: null
    };
    this.notify();
  }

  openCollectPaymentModal(payment, member = null) {
    const resolvedMember = member || (payment ? this.state.members.find(m => m.id === payment.memberId) : null);
    this.state.collectPaymentModal = {
      isOpen: true,
      payment,
      member: resolvedMember
    };
    this.notify();
  }

  closeCollectPaymentModal() {
    this.state.collectPaymentModal = {
      isOpen: false,
      payment: null,
      member: null
    };
    this.notify();
  }

  openWhatsAppModal(memberOrOptions, defaultTemplate = 'EXPIRY_REMINDER', paymentInfo = null) {
    let member = memberOrOptions;
    let tpl = defaultTemplate;
    let pay = paymentInfo;
    let quotedPlans = [];
    let stackedFromEnrollment = false;

    if (memberOrOptions && typeof memberOrOptions === 'object' && ('member' in memberOrOptions || 'quotedPlans' in memberOrOptions || 'stackedFromEnrollment' in memberOrOptions)) {
      member = memberOrOptions.member || null;
      tpl = memberOrOptions.defaultTemplate || defaultTemplate;
      pay = memberOrOptions.paymentInfo || paymentInfo;
      quotedPlans = memberOrOptions.quotedPlans || [];
      stackedFromEnrollment = Boolean(memberOrOptions.stackedFromEnrollment);
    }

    this.state.whatsAppModal = {
      isOpen: true,
      isBulk: false,
      recipients: member ? [member] : [],
      member,
      defaultTemplate: tpl,
      paymentInfo: pay,
      quotedPlans,
      stackedFromEnrollment
    };
    this.notify();
  }

  openBulkWhatsAppModal(members, defaultTemplate = 'campaign_festive_offer') {
    this.state.whatsAppModal = {
      isOpen: true,
      isBulk: true,
      recipients: members || [],
      member: members && members.length > 0 ? members[0] : null,
      defaultTemplate,
      paymentInfo: null,
      quotedPlans: [],
      stackedFromEnrollment: false
    };
    this.notify();
  }

  closeWhatsAppModal(cancelled = false) {
    const wasStacked = Boolean(this.state.whatsAppModal?.stackedFromEnrollment);
    this.state.whatsAppModal = {
      isOpen: false,
      isBulk: false,
      recipients: [],
      member: null,
      defaultTemplate: 'EXPIRY_REMINDER',
      paymentInfo: null,
      quotedPlans: [],
      stackedFromEnrollment: false
    };
    if (wasStacked && !cancelled) {
      this.closeAddMemberModal();
    }
    this.notify();
  }

  openPlanModal(plan = null) {
    this.state.planModal = {
      isOpen: true,
      mode: plan ? 'edit' : 'create',
      plan: plan || null
    };
    this.notify();
  }

  closePlanModal() {
    this.state.planModal = {
      isOpen: false,
      mode: 'create',
      plan: null
    };
    this.notify();
  }

  formatDateDisplay(dStr) {
    if (!dStr) return '—';
    try {
      const d = new Date(dStr);
      if (isNaN(d.getTime())) return dStr;
      return d.toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' });
    } catch {
      return dStr;
    }
  }

  computeMemberStatus(subscriptions, memberFilters = null, today = new Date()) {
    today = new Date(today);
    today.setHours(0, 0, 0, 0);

    // Rule 1: Prospect if member has no plans
    if (!subscriptions || subscriptions.length === 0) {
      return {
        status: 'prospect',
        statusLabel: 'Prospect',
        statusVariant: 'expired',
        expiringSubs: [],
        validBeyondSubs: [],
        activeSubs: []
      };
    }

    // Determine unexpired and expired subscriptions
    const validSubs = [];
    let hasAnyValid = false;

    for (const s of subscriptions) {
      if (!s.endDate) {
        validSubs.push(s);
        hasAnyValid = true;
      } else {
        const endDt = new Date(s.endDate);
        endDt.setHours(23, 59, 59, 999);
        if (endDt >= today) {
          validSubs.push(s);
          hasAnyValid = true;
        }
      }
    }

    // Rule 2: Expired if all plans expired
    if (!hasAnyValid) {
      return {
        status: 'expired',
        statusLabel: 'Expired',
        statusVariant: 'expired',
        expiringSubs: [],
        validBeyondSubs: [],
        activeSubs: []
      };
    }

    // Determine threshold date
    // Default threshold: end of current month
    let thresholdDate = new Date(today.getFullYear(), today.getMonth() + 1, 0, 23, 59, 59, 999);

    if (memberFilters) {
      if (memberFilters.expiryTo) {
        thresholdDate = new Date(memberFilters.expiryTo);
        thresholdDate.setHours(23, 59, 59, 999);
      } else if (memberFilters.expiry && memberFilters.expiry !== 'all' && memberFilters.expiry !== 'custom') {
        if (memberFilters.expiry === '7d' || memberFilters.expiry === 'next-7d') {
          thresholdDate = new Date(today.getTime() + 7 * 24 * 60 * 60 * 1000);
          thresholdDate.setHours(23, 59, 59, 999);
        } else if (memberFilters.expiry === '30d' || memberFilters.expiry === 'next-30d') {
          thresholdDate = new Date(today.getTime() + 30 * 24 * 60 * 60 * 1000);
          thresholdDate.setHours(23, 59, 59, 999);
        } else if (memberFilters.expiry === 'this-month') {
          thresholdDate = new Date(today.getFullYear(), today.getMonth() + 1, 0, 23, 59, 59, 999);
        }
      }
    }

    // Partition valid subscriptions:
    // Expiring within threshold vs valid beyond threshold
    const expiringSubs = [];
    const validBeyondSubs = [];

    for (const s of validSubs) {
      if (!s.endDate) {
        // Indefinite/no end date is considered active beyond threshold
        validBeyondSubs.push(s);
      } else {
        const endDt = new Date(s.endDate);
        endDt.setHours(23, 59, 59, 999);
        if (endDt <= thresholdDate) {
          expiringSubs.push(s);
        } else {
          validBeyondSubs.push(s);
        }
      }
    }

    expiringSubs.sort((a, b) => (a.daysLeft ?? 0) - (b.daysLeft ?? 0));
    const activeSubs = [...expiringSubs, ...validBeyondSubs];

    // Rule 3: Active if at least one plan is active
    return {
      status: 'active',
      statusLabel: 'Active',
      statusVariant: 'active',
      expiringSubs,
      validBeyondSubs,
      activeSubs
    };
  }

  computePaymentStatus(payment, paymentFilters = null, today = new Date()) {
    today = new Date(today);
    today.setHours(0, 0, 0, 0);

    const amt = Number(payment.amount) || 0;
    const paidAmt = (payment.paidAmount !== undefined && payment.paidAmount !== null && payment.paidAmount !== '')
      ? (Number(payment.paidAmount) || 0)
      : (payment.paidDate ? amt : 0);
    const isPaid = (payment.isPaid !== undefined && payment.isPaid !== null)
      ? Boolean(payment.isPaid)
      : (paidAmt >= amt && amt > 0);

    if (isPaid) {
      return {
        status: 'paid',
        statusLabel: 'Paid',
        statusVariant: 'paid',
        isPaid: true,
        isOverdue: false,
        isUpcoming: false,
        isFuture: false,
        daysDiff: null
      };
    }

    if (!payment.dueDate || payment.dueDate === '—') {
      return {
        status: 'upcoming',
        statusLabel: 'Unpaid',
        statusVariant: 'warning',
        isPaid: false,
        isOverdue: false,
        isUpcoming: true,
        isFuture: false,
        daysDiff: null
      };
    }

    const dueDt = new Date(payment.dueDate);
    dueDt.setHours(0, 0, 0, 0);

    const daysDiff = Math.ceil((dueDt.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));

    // Overdue: due date is before today (dueDt < today)
    if (dueDt < today) {
      const daysOverdue = Math.abs(daysDiff);
      const statusLabel = daysOverdue <= 1 ? 'Overdue by 1 day' : `Overdue by ${daysOverdue} days`;
      return {
        status: 'overdue',
        statusLabel,
        statusVariant: 'danger',
        isPaid: false,
        isOverdue: true,
        isUpcoming: false,
        isFuture: false,
        daysDiff
      };
    }

    // Determine threshold date:
    // Default threshold: end of current month
    let thresholdDate = new Date(today.getFullYear(), today.getMonth() + 1, 0, 23, 59, 59, 999);

    if (paymentFilters) {
      if (paymentFilters.dueDateTo) {
        thresholdDate = new Date(paymentFilters.dueDateTo);
        thresholdDate.setHours(23, 59, 59, 999);
      } else if (paymentFilters.dueDate && paymentFilters.dueDate !== 'all' && paymentFilters.dueDate !== 'custom') {
        if (paymentFilters.dueDate === 'next-7d') {
          thresholdDate = new Date(today.getTime() + 7 * 24 * 60 * 60 * 1000);
          thresholdDate.setHours(23, 59, 59, 999);
        } else if (paymentFilters.dueDate === 'next-30d') {
          thresholdDate = new Date(today.getTime() + 30 * 24 * 60 * 60 * 1000);
          thresholdDate.setHours(23, 59, 59, 999);
        } else if (paymentFilters.dueDate === 'this-month' || paymentFilters.dueDate === 'month-end' || paymentFilters.dueDate === 'oct-2026') {
          thresholdDate = new Date(today.getFullYear(), today.getMonth() + 1, 0, 23, 59, 59, 999);
        }
      }
    }

    const dueEnd = new Date(payment.dueDate);
    dueEnd.setHours(23, 59, 59, 999);

    if (dueEnd <= thresholdDate) {
      // Upcoming: within end of month or due date filter
      const statusLabel = daysDiff === 0 ? 'Due today' : (daysDiff === 1 ? 'Due in 1 day' : `Due in ${daysDiff} days`);
      return {
        status: 'upcoming',
        statusLabel,
        statusVariant: 'warning',
        isPaid: false,
        isOverdue: false,
        isUpcoming: true,
        isFuture: false,
        daysDiff
      };
    }

    // Future: later than end of month or due date filter
    const statusLabel = daysDiff === 1 ? 'Due in 1 day' : `Due in ${daysDiff} days`;
    return {
      status: 'future',
      statusLabel,
      statusVariant: 'secondary',
      isPaid: false,
      isOverdue: false,
      isUpcoming: false,
      isFuture: true,
      daysDiff
    };
  }

  processRawMembers(rawMembers) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const lastDayOfMonth = new Date(today.getFullYear(), today.getMonth() + 1, 0);
    const pad = (n) => String(n).padStart(2, '0');
    const toIso = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
    const endOfMonthStr = toIso(lastDayOfMonth);

    const processedMembers = [];
    const processedPayments = [];

    for (const m of (rawMembers || [])) {
      const rawPayments = m.payments || [];
      let rawPlans = (m.plans && Array.isArray(m.plans) && m.plans.length > 0) ? m.plans : [];
      if (!rawPlans.length && rawPayments.length) {
        const planMap = new Map();
        for (const p of rawPayments) {
          for (const pl of (p.plans || [])) {
            if (pl && pl.id && !planMap.has(pl.id)) {
              planMap.set(pl.id, pl);
            }
          }
        }
        rawPlans = Array.from(planMap.values());
      }

      let memberTotalPending = 0;
      let memberTotalAmount = 0;
      let memberTotalPaid = 0;
      let earliestDate = null;

      // 1. Process Member Subscriptions (Service Validity Periods)
      const subscriptions = [];
      for (const pl of rawPlans) {
        if (pl.startDate) {
          const dt = new Date(pl.startDate);
          if (!earliestDate || dt < earliestDate) earliestDate = dt;
        }

        let subStatus = 'expired';
        let subStatusLabel = 'Expired';
        let subStatusVariant = 'danger';
        let daysLeft = null;

        if (pl.endDate) {
          const endDt = new Date(pl.endDate);
          endDt.setHours(0, 0, 0, 0);
          daysLeft = Math.ceil((endDt.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
          const expIso = pl.endDate.slice(0, 10);

          if (endDt < today) {
            subStatus = 'expired';
            subStatusVariant = 'danger';
            const daysAgo = Math.abs(daysLeft);
            subStatusLabel = daysAgo <= 1 ? 'Expired yesterday' : `Expired ${daysAgo}d ago`;
          } else if (expIso <= endOfMonthStr || daysLeft <= 30) {
            subStatus = 'renewals';
            subStatusVariant = 'warning';
            if (daysLeft <= 0) {
              subStatusLabel = 'Expires Today';
            } else if (daysLeft === 1) {
              subStatusLabel = 'Expires tomorrow';
            } else if (daysLeft < 7) {
              subStatusLabel = `Expires in ${daysLeft} days`;
            } else {
              const w = Math.round(daysLeft / 7);
              subStatusLabel = w <= 1 ? 'Expires in 1 week' : `Expires in ${w} weeks`;
            }
          } else {
            subStatus = 'active';
            subStatusVariant = 'success';
            subStatusLabel = 'Active';
          }
        } else {
          subStatus = 'active';
          subStatusVariant = 'success';
          subStatusLabel = 'Active';
        }

        const periodStr = (pl.startDate && pl.endDate)
          ? `${this.formatDateDisplay(pl.startDate)} – ${this.formatDateDisplay(pl.endDate)}`
          : (pl.startDate ? `Started ${this.formatDateDisplay(pl.startDate)}` : 'Active Service');

        const matchingPlan = (this.state.availablePlans || []).find(p => p.id === pl.planId);
        const resolvedPrice = matchingPlan ? (Number(matchingPlan.price || matchingPlan.defaultPrice) || 0) : 0;

        subscriptions.push({
          id: pl.id,
          planId: pl.planId,
          planName: pl.planName || matchingPlan?.name || 'Gym Service',
          startDate: pl.startDate,
          endDate: pl.endDate,
          period: periodStr,
          status: subStatus,
          statusLabel: subStatusLabel,
          statusVariant: subStatusVariant,
          daysLeft,
          fee: resolvedPrice
        });
      }

      // Sort subscriptions: active/renewals first, furthest end date first
      subscriptions.sort((a, b) => {
        const dateA = a.endDate ? new Date(a.endDate).getTime() : 0;
        const dateB = b.endDate ? new Date(b.endDate).getTime() : 0;
        return dateB - dateA;
      });

      // 2. Process Member Payments & Invoices (Ledger)
      const memberPayments = [];
      for (const p of rawPayments) {
        const amt = Number(p.amount) || 0;
        const paidAmt = (p.paidAmount !== undefined && p.paidAmount !== null && p.paidAmount !== '')
          ? (Number(p.paidAmount) || 0)
          : (p.paidDate ? amt : 0);
        const pending = Math.max(0, amt - paidAmt);
        memberTotalPending += pending;
        memberTotalAmount += amt;
        memberTotalPaid += paidAmt;

        const isPaid = paidAmt >= amt;
        const dueDt = p.dueDate ? new Date(p.dueDate) : null;
        if (dueDt) dueDt.setHours(0, 0, 0, 0);

        if (p.dueDate) {
          const dt = new Date(p.dueDate);
          if (!earliestDate || dt < earliestDate) earliestDate = dt;
        }
        if (p.paidDate) {
          const dt = new Date(p.paidDate);
          if (!earliestDate || dt < earliestDate) earliestDate = dt;
        }

        const payStatusInfo = this.computePaymentStatus({
          amount: amt,
          paidAmount: paidAmt,
          paidDate: p.paidDate,
          dueDate: p.dueDate,
          isPaid
        }, this.state.paymentFilters, today);

        const coveredPlans = (p.plans && Array.isArray(p.plans))
          ? p.plans.map(pl => pl.planName).filter(Boolean)
          : [];
        const coveredPlanNames = coveredPlans.length > 0 ? coveredPlans.join(', ') : 'All Services';

        const cleanNote = p.notes 
          ? p.notes.replace(/\s*\((Overdue|Paid|Upcoming|Pending)\)/gi, '').trim()
          : (coveredPlans.length > 0 ? coveredPlans[0] : 'Installment');

        const method = p.paymentMethod || (p.transactionId && p.transactionId.toLowerCase().includes('cash') ? 'CASH' : 'UPI');

        const userPlanIds = (p.plans && Array.isArray(p.plans))
          ? p.plans.map(pl => pl.id).filter(Boolean)
          : [];
        const planDetails = (p.plans && Array.isArray(p.plans)) ? p.plans : [];

        const payObj = {
          id: p.id,
          memberId: m.userId,
          memberName: `${m.firstName || ''} ${m.lastName || ''}`.trim() || m.username,
          phone: m.phoneNumber ? `+${m.phoneNumber.countryCode || AppConfig.getCountryCode()} ${m.phoneNumber.number}` : '—',
          rawPhone: m.phoneNumber ? m.phoneNumber.number : '',
          amount: amt,
          amountFormatted: AppConfig.formatCurrency(amt),
          paidAmount: paidAmt,
          paidAmountFormatted: AppConfig.formatCurrency(paidAmt),
          pendingAmount: pending,
          pendingAmountFormatted: AppConfig.formatCurrency(pending),
          dueDate: p.dueDate || '—',
          paidDate: p.paidDate || '—',
          date: p.paidDate || p.dueDate || '—',
          txnId: p.transactionId || null,
          notes: cleanNote,
          note: cleanNote,
          status: payStatusInfo.status,
          statusLabel: payStatusInfo.statusLabel,
          statusVariant: payStatusInfo.statusVariant,
          isPaid: payStatusInfo.isPaid,
          isOverdue: payStatusInfo.isOverdue,
          isUpcoming: payStatusInfo.isUpcoming,
          isFuture: payStatusInfo.isFuture,
          paymentMethod: method,
          coveredPlans,
          coveredPlanNames,
          userPlanIds,
          plans: planDetails,
          memberStatus: 'active'
        };

        memberPayments.push(payObj);
        processedPayments.push(payObj);
      }

      // 3. Determine Overall Member Status & Plan Names
      const statusInfo = this.computeMemberStatus(subscriptions, this.state.memberFilters, today);
      const memberStatus = statusInfo.status;
      const memberStatusLabel = statusInfo.statusLabel;
      const memberStatusVariant = statusInfo.statusVariant;
      const latestExpiryDate = subscriptions.length > 0 ? subscriptions[0].endDate : null;

      const activeSubs = statusInfo.activeSubs;
      const activePlanNames = activeSubs.map(s => s.planName);
      const allPlanNames = [...new Set(subscriptions.map(s => s.planName))];
      const displayPlanNames = (activePlanNames.length > 0 ? activePlanNames : allPlanNames).join(', ') || 'No Plan';

      // Update payment memberStatus
      memberPayments.forEach(p => { p.memberStatus = memberStatus; });

      // Plan batches backward-compatibility
      const planBatches = subscriptions.map(s => ({
        id: `sub-${s.id}`,
        userPlanId: s.id,
        planId: s.planId,
        period: s.period,
        startDate: s.startDate,
        endDate: s.endDate,
        plans: s.planName,
        totalAmount: 0,
        totalAmountFormatted: '',
        pendingAmount: 0,
        pendingAmountFormatted: '',
        installments: memberPayments.filter(p => !p.coveredPlans.length || p.coveredPlans.includes(s.planName))
      }));

      processedMembers.push({
        id: m.userId,
        userId: m.userId,
        name: `${m.firstName || ''} ${m.lastName || ''}`.trim() || m.username,
        phone: m.phoneNumber ? `+${m.phoneNumber.countryCode || AppConfig.getCountryCode()} ${m.phoneNumber.number}` : '—',
        rawPhone: m.phoneNumber ? m.phoneNumber.number : '',
        plans: displayPlanNames,
        activePlansList: activePlanNames.length > 0 ? activePlanNames : allPlanNames,
        subscriptions,
        payments: memberPayments,
        pendingAmount: memberTotalPending,
        totalSpent: memberTotalPaid,
        status: memberStatus,
        statusLabel: memberStatusLabel,
        statusVariant: memberStatusVariant,
        expiryDate: latestExpiryDate,
        memberSince: earliestDate ? earliestDate.toISOString().slice(0, 10) : null,
        planBatches,
        rawRecord: m
      });
    }

    return { processedMembers, processedPayments };
  }

  syncConversationsFromMembers() {
    const existingMap = new Map((this.state.conversations || []).map(c => [c.id, c]));

    this.state.conversations = this.state.members.map(m => {
      const existing = existingMap.get(m.id);
      if (existing) {
        return {
          ...existing,
          name: m.name,
          phone: m.phone,
          status: m.status,
          statusLabel: m.statusLabel || m.status,
          plans: m.plans,
          pendingAmount: m.pendingAmount,
          expiryDate: m.expiryDate
        };
      }

      let isHumanRequired = false;
      let messages = [];
      let lastMessage = 'No messages yet';
      let lastTime = '';

      return {
        id: m.id,
        memberId: m.id,
        name: m.name,
        phone: m.phone,
        status: m.status,
        statusLabel: m.statusLabel || (m.status === 'active' ? 'Active' : m.status === 'expired' ? 'Expired' : 'Prospect'),
        plans: m.plans,
        pendingAmount: m.pendingAmount,
        expiryDate: m.expiryDate,
        isHumanRequired,
        unreadCount: isHumanRequired ? 1 : 0,
        lastMessage,
        lastTime,
        messages
      };
    });
  }

  async refreshGymData(showToast = true) {
    await this.loadInitialData({ forceRefresh: true });
    if (showToast) {
      if (window.fl && window.fl.errors && typeof window.fl.errors.toast === 'function') {
        window.fl.errors.toast('Members, Plans & Payments updated successfully!', 'success');
      } else {
        ErrorMapper.notifySuccess('Gym data synchronized successfully!');
      }
    }
  }

  async loadInitialData(options = {}) {
    const isJustLoggedIn = sessionStorage.getItem('fl_just_logged_in') === 'true';
    if (isJustLoggedIn) {
      sessionStorage.removeItem('fl_just_logged_in');
    }
    const forceRefresh = options?.forceRefresh === true || isJustLoggedIn;
    const currentTenant = SessionManager.getTenantId();

    if (forceRefresh) {
      this._conversationsLoaded = false;
    }

    // 1. Resolve Gym and ensure branding config is loaded from backend
    if (currentTenant) {
      try {
        await AppConfig.loadFromBackend(currentTenant, forceRefresh);
      } catch (_) {}
    }
    this.state.gyms = [
      { id: currentTenant || 'default', gymId: currentTenant || 'default', name: AppConfig.getBrandName() || 'Fitness Center' }
    ];
    this.state.currentGymId = currentTenant || 'default';

    // 2. Try IndexedDB cache first if not explicitly forceRefreshing (instant 0ms load)
    if (!forceRefresh && currentTenant) {
      try {
        const cached = await GymCache.getGymCache(currentTenant);
        if (cached && Array.isArray(cached.rawMembers) && Array.isArray(cached.plans) && (cached.rawMembers.length > 0 || cached.plans.length > 0)) {
          this.state.availablePlans = cached.plans.map(p => ({
            id: p.id,
            name: p.name,
            price: Number(p.defaultPrice || p.price) || 0,
            defaultPrice: Number(p.defaultPrice || p.price) || 0,
            durationDays: p.defaultDuration || p.durationDays || 30,
            durationMonths: (p.durationMonths !== undefined) ? p.durationMonths : ((p.defaultDuration && p.defaultDuration >= 30) ? Math.round(p.defaultDuration / 30) : 0),
            description: p.description || '',
            isActive: p.isActive !== false
          }));
          this.state.rawMembers = cached.rawMembers;
          const { processedMembers, processedPayments } = this.processRawMembers(cached.rawMembers);
          this.state.members = processedMembers;
          this.state.payments = processedPayments;
          this.state.lastSyncedAt = cached.cachedAt || Date.now();
          this.state.loading.members = false;
          this.state.loading.plans = false;
          this.state.loading.gyms = false;
          this.state.syncingGymData = false;

          // Keep open member drawer updated with fresh relational state
          if (this.state.memberDrawer.isOpen && this.state.memberDrawer.member) {
            const freshMember = processedMembers.find(m => m.id === this.state.memberDrawer.member.id);
            if (freshMember) {
              this.state.memberDrawer.member = freshMember;
            }
          }

          // Derive conversations from cached members
          this.syncConversationsFromMembers();
          this.notify();

          // Non-blocking background load for chat conversations
          this.loadAllConversations().catch(() => {});
          return;
        }
      } catch (cacheErr) {
        console.warn('[HomeStore] Failed to read IndexedDB cache, falling back to backend:', cacheErr);
      }
    }

    this.state.syncingGymData = true;
    this.state.loading.members = true;
    this.state.loading.plans = true;
    this.state.loading.gyms = true;
    this.state.loadError = null;
    this.notify();

    let hasError = false;

    let rawPlans = [];
    let rawList = [];

    try {
      const [plansRes, membersRes] = await Promise.all([
        api.get('/plan/list'),
        api.get('/members/list')
      ]);

      if (Array.isArray(plansRes)) {
        rawPlans = plansRes;
      } else if (plansRes && Array.isArray(plansRes.plans)) {
        rawPlans = plansRes.plans;
      }

      if (Array.isArray(membersRes)) {
        rawList = membersRes;
      } else if (membersRes && Array.isArray(membersRes.members)) {
        rawList = membersRes.members;
      }

      this.state.availablePlans = rawPlans.map(p => ({
        id: p.id,
        name: p.name,
        price: Number(p.defaultPrice) || 0,
        defaultPrice: Number(p.defaultPrice) || 0,
        durationDays: p.defaultDuration || 30,
        durationMonths: (p.defaultDuration && p.defaultDuration >= 30) ? Math.round(p.defaultDuration / 30) : 0,
        description: p.description || '',
        isActive: p.isActive !== false
      }));

      this.state.rawMembers = rawList;
      const { processedMembers, processedPayments } = this.processRawMembers(rawList);
      this.state.members = processedMembers;
      this.state.payments = processedPayments;

      // Keep open member drawer updated with fresh relational state
      if (this.state.memberDrawer.isOpen && this.state.memberDrawer.member) {
        const freshMember = processedMembers.find(m => m.id === this.state.memberDrawer.member.id);
        if (freshMember) {
          this.state.memberDrawer.member = freshMember;
        }
      }

      this.state.lastSyncedAt = Date.now();

      // Persist fresh dataset to IndexedDB cache
      if (currentTenant) {
        GymCache.setGymCache(currentTenant, {
          plans: rawPlans,
          rawMembers: rawList,
          cachedAt: this.state.lastSyncedAt
        }).catch(() => {});
      }
    } catch (err) {
      if (!hasError) {
        hasError = true;
        this.state.loadError = err.message || 'Failed to sync gym data';
        ErrorMapper.showErrorWindow(err.message || 'Failed to sync gym data', 'API Error', err.status || '500');
      }
    } finally {
      this.state.loading.plans = false;
      this.state.loading.members = false;
      this.state.loading.gyms = false;
      this.state.syncingGymData = false;
    }

    // 4. Derive Conversations from loaded members
    this.syncConversationsFromMembers();

    // 5. Load active conversation timelines from backend
    await this.loadAllConversations();

    // Check if current activeChatId still exists or pick the first conversation with messages / attention
    if (this.state.activeChatId) {
      const activeExists = this.state.conversations.some(c => c.id === this.state.activeChatId);
      if (!activeExists) this.state.activeChatId = null;
    }

    if (!this.state.activeChatId) {
      const withMessages = this.state.conversations.find(c => (c.messages && c.messages.length > 0) || c.isHumanRequired);
      if (withMessages) {
        this.state.activeChatId = withMessages.id;
      }
    }

    if (this.state.activeChatId) {
      const conv = (this.state.conversations || []).find(c => String(c.id) === String(this.state.activeChatId));
      if (!conv || !Array.isArray(conv.messages) || conv.messages.length === 0) {
        this.loadConversationTimeline(this.state.activeChatId);
      }
    }

    this.notify();
  }

  getFilteredMembers() {
    const { searchQuery, memberFilters } = this.state;
    let result = [...(this.state.members || [])];

    if (memberFilters) {
      // 1. Potential members (never had a plan)
      if (memberFilters.noPlanOnly) {
        result = result.filter(m => !m.subscriptions || m.subscriptions.length === 0);
      }

      // 2. Plan filter
      if (memberFilters.plan && memberFilters.plan !== 'all') {
        const pLower = memberFilters.plan.toLowerCase();
        result = result.filter(m => m.plans && m.plans.toLowerCase().includes(pLower));
      }

      // 3. Pending amount filter
      if ((memberFilters.pendingAmountMin !== null && memberFilters.pendingAmountMin !== undefined) ||
          (memberFilters.pendingAmountMax !== null && memberFilters.pendingAmountMax !== undefined)) {
        const min = (memberFilters.pendingAmountMin !== null && memberFilters.pendingAmountMin !== undefined) ? Number(memberFilters.pendingAmountMin) : 0;
        const max = (memberFilters.pendingAmountMax !== null && memberFilters.pendingAmountMax !== undefined) ? Number(memberFilters.pendingAmountMax) : Infinity;
        result = result.filter(m => {
          const amt = Number(m.pendingAmount) || 0;
          return amt >= min && amt <= max;
        });
      } else if (memberFilters.pendingAmount === 'with-dues') {
        result = result.filter(m => Number(m.pendingAmount) > 0);
      } else if (memberFilters.pendingAmount === 'zero-dues') {
        result = result.filter(m => !m.pendingAmount || Number(m.pendingAmount) === 0);
      }

      // 4. Expiry date filter (checked against member plans)
      if (memberFilters.expiryFrom || memberFilters.expiryTo) {
        const from = memberFilters.expiryFrom;
        const to = memberFilters.expiryTo;
        result = result.filter(m => {
          const subs = m.subscriptions || [];
          if (!subs.length) return false;
          return subs.some(s => {
            if (!s.endDate) return false;
            const sDate = s.endDate.slice(0, 10);
            if (from && sDate < from) return false;
            if (to && sDate > to) return false;
            return true;
          });
        });
      } else if (memberFilters.expiry && memberFilters.expiry !== 'all') {
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        result = result.filter(m => {
          const subs = m.subscriptions || [];
          if (!subs.length) return false;
          return subs.some(s => {
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
        });
      }
    }

    if (searchQuery && searchQuery.trim()) {
      const q = searchQuery.trim().toLowerCase();
      result = result.filter(m => 
        (m.name && m.name.toLowerCase().includes(q)) ||
        (m.phone && m.phone.toLowerCase().includes(q)) ||
        (m.plans && m.plans.toLowerCase().includes(q))
      );
    }

    // Sorting by Name, Plans, Expiry Date
    const { memberSort } = this.state;
    if (memberSort && memberSort.field) {
      const dir = memberSort.direction === 'desc' ? -1 : 1;
      const field = memberSort.field;

      result.sort((a, b) => {
        if (field === 'name') {
          const nameA = (a.name || '').toLowerCase();
          const nameB = (b.name || '').toLowerCase();
          return nameA.localeCompare(nameB) * dir;
        } else if (field === 'plans') {
          const plansA = (a.plans || '').toLowerCase();
          const plansB = (b.plans || '').toLowerCase();
          return plansA.localeCompare(plansB) * dir;
        } else if (field === 'expiryDate') {
          const hasDateA = a.expiryDate && a.expiryDate !== '—';
          const hasDateB = b.expiryDate && b.expiryDate !== '—';
          if (!hasDateA && !hasDateB) return 0;
          if (!hasDateA) return 1;
          if (!hasDateB) return -1;
          const timeA = new Date(a.expiryDate).getTime();
          const timeB = new Date(b.expiryDate).getTime();
          return (timeA - timeB) * dir;
        }
        return 0;
      });
    }

    return result;
  }

  getFilteredPayments() {
    const { searchQuery, paymentFilters } = this.state;
    let result = [...(this.state.payments || [])];

    if (paymentFilters) {
      // Unpaid only filter (used when clicking pending collections card)
      if (paymentFilters.unpaidOnly) {
        result = result.filter(p => !p.isPaid);
      }

      // Method Filter
      if (paymentFilters.method && paymentFilters.method !== 'all') {
        const mLower = paymentFilters.method.toLowerCase();
        result = result.filter(p => {
          const pm = (p.paymentMethod || '').toLowerCase();
          if (mLower === 'netbanking' || mLower === 'bank_transfer') {
            return pm === 'netbanking' || pm === 'bank_transfer';
          }
          return pm === mLower;
        });
      }

      // Amount Filter (Range slider min/max + presets)
      const hasMin = paymentFilters.amountMin !== null && paymentFilters.amountMin !== undefined && paymentFilters.amountMin > 0;
      const hasMax = paymentFilters.amountMax !== null && paymentFilters.amountMax !== undefined && paymentFilters.amountMax < (paymentFilters.maxLimit || 20000);

      if (hasMin || hasMax) {
        if (hasMin) {
          result = result.filter(p => Number(p.amount) >= paymentFilters.amountMin);
        }
        if (hasMax) {
          result = result.filter(p => Number(p.amount) <= paymentFilters.amountMax);
        }
      } else if (paymentFilters.amount === 'under-2k') {
        result = result.filter(p => p.amount < 2000);
      } else if (paymentFilters.amount === '2k-5k') {
        result = result.filter(p => p.amount >= 2000 && p.amount <= 5000);
      } else if (paymentFilters.amount === 'above-5k') {
        result = result.filter(p => p.amount > 5000);
      }

      // Due Date Filter
      if (paymentFilters.dueDateFrom || paymentFilters.dueDateTo) {
        const from = paymentFilters.dueDateFrom;
        const to = paymentFilters.dueDateTo;
        result = result.filter(p => {
          if (!p.dueDate || p.dueDate === '—') return false;
          const pDate = p.dueDate.slice(0, 10);
          if (from && pDate < from) return false;
          if (to && pDate > to) return false;
          return true;
        });
      } else if (paymentFilters.dueDate === 'this-month' || paymentFilters.dueDate === 'oct-2026') {
        const now = new Date();
        const pad = (n) => String(n).padStart(2, '0');
        const lastDayOfMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0);
        const lastDayOfMonthStr = `${lastDayOfMonth.getFullYear()}-${pad(lastDayOfMonth.getMonth() + 1)}-${pad(lastDayOfMonth.getDate())}`;
        result = result.filter(p => p.dueDate && p.dueDate !== '—' && p.dueDate.slice(0, 10) <= lastDayOfMonthStr);
      }

      // Paid Date Filter
      if (paymentFilters.paidDateFrom || paymentFilters.paidDateTo) {
        const from = paymentFilters.paidDateFrom;
        const to = paymentFilters.paidDateTo;
        result = result.filter(p => {
          if (!p.paidDate || p.paidDate === '—') return false;
          const pDate = p.paidDate.slice(0, 10);
          if (from && pDate < from) return false;
          if (to && pDate > to) return false;
          return true;
        });
      } else if (paymentFilters.paidDate === 'this-month') {
        const curPrefix = new Date().toISOString().slice(0, 7);
        result = result.filter(p => p.paidDate && p.paidDate.startsWith(curPrefix));
      } else if (paymentFilters.paidDate === 'earlier') {
        const curPrefix = new Date().toISOString().slice(0, 7) + '-01';
        result = result.filter(p => p.paidDate && p.paidDate < curPrefix && p.paidDate !== '—');
      } else if (paymentFilters.paidDate === 'unpaid') {
        result = result.filter(p => !p.isPaid);
      }
    }

    if (searchQuery && searchQuery.trim()) {
      const q = searchQuery.trim().toLowerCase();
      result = result.filter(p =>
        (p.memberName && p.memberName.toLowerCase().includes(q)) ||
        (p.phone && p.phone.toLowerCase().includes(q)) ||
        (p.txnId && p.txnId.toLowerCase().includes(q)) ||
        (p.notes && p.notes.toLowerCase().includes(q))
      );
    }

    // Sorting by Member, Amount, Method, Due Date, Paid Date, Notes
    const { paymentSort } = this.state;
    if (paymentSort && paymentSort.field) {
      const dir = paymentSort.direction === 'desc' ? -1 : 1;
      const field = paymentSort.field;

      result.sort((a, b) => {
        if (field === 'member') {
          const nameA = (a.memberName || '').toLowerCase();
          const nameB = (b.memberName || '').toLowerCase();
          return nameA.localeCompare(nameB) * dir;
        } else if (field === 'amount') {
          const amtA = Number(a.amount) || 0;
          const amtB = Number(b.amount) || 0;
          return (amtA - amtB) * dir;
        } else if (field === 'method') {
          const methA = (a.paymentMethod || '').toLowerCase();
          const methB = (b.paymentMethod || '').toLowerCase();
          return methA.localeCompare(methB) * dir;
        } else if (field === 'dueDate') {
          const hasDateA = a.dueDate && a.dueDate !== '—';
          const hasDateB = b.dueDate && b.dueDate !== '—';
          if (!hasDateA && !hasDateB) return 0;
          if (!hasDateA) return 1;
          if (!hasDateB) return -1;
          const timeA = new Date(a.dueDate).getTime();
          const timeB = new Date(b.dueDate).getTime();
          return (timeA - timeB) * dir;
        } else if (field === 'paidDate') {
          const hasDateA = a.paidDate && a.paidDate !== '—';
          const hasDateB = b.paidDate && b.paidDate !== '—';
          if (!hasDateA && !hasDateB) return 0;
          if (!hasDateA) return 1;
          if (!hasDateB) return -1;
          const timeA = new Date(a.paidDate).getTime();
          const timeB = new Date(b.paidDate).getTime();
          return (timeA - timeB) * dir;
        } else if (field === 'notes') {
          const noteA = (a.notes || a.note || '').toLowerCase();
          const noteB = (b.notes || b.note || '').toLowerCase();
          return noteA.localeCompare(noteB) * dir;
        }
        return 0;
      });
    }

    return result;
  }

  getFilteredPlans() {
    let result = [...(this.state.availablePlans || [])];
    const { searchQuery, planFilters } = this.state;

    if (planFilters) {
      if (planFilters.status === 'active') {
        result = result.filter(p => p.isActive !== false);
      } else if (planFilters.status === 'deactivated') {
        result = result.filter(p => p.isActive === false);
      }
    }

    if (searchQuery && searchQuery.trim()) {
      const q = searchQuery.trim().toLowerCase();
      result = result.filter(p => 
        (p.name && p.name.toLowerCase().includes(q)) ||
        (p.description && p.description.toLowerCase().includes(q))
      );
    }

    return result;
  }

  getKPIs() {
    const members = this.state.members || [];
    const payments = this.state.payments || [];

    const now = new Date();
    now.setHours(0, 0, 0, 0);
    const pad = (n) => String(n).padStart(2, '0');
    const todayStr = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;

    // Last day of current month (e.g. 31st Oct 2026)
    const lastDayThisMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0);
    const lastDayThisMonthStr = `${lastDayThisMonth.getFullYear()}-${pad(lastDayThisMonth.getMonth() + 1)}-${pad(lastDayThisMonth.getDate())}`;

    let activeMembersCount = 0;
    let renewalsThisMonthCount = 0;
    let expiredMembersCount = 0;
    let potentialMembersCount = 0;

    for (const m of members) {
      const subs = m.subscriptions || [];
      if (!subs.length) {
        // Never had a plan -> Potential member
        potentialMembersCount++;
        continue;
      }

      // Check member's latest expiry date
      let latestExpiry = null;
      for (const s of subs) {
        if (!s.endDate) {
          latestExpiry = 'indefinite';
          break;
        }
        const sEnd = s.endDate.slice(0, 10);
        if (!latestExpiry || sEnd > latestExpiry) {
          latestExpiry = sEnd;
        }
      }

      if (latestExpiry === 'indefinite' || latestExpiry > lastDayThisMonthStr) {
        // Valid beyond this month
        activeMembersCount++;
      } else if (latestExpiry >= todayStr && latestExpiry <= lastDayThisMonthStr) {
        // Expiring this month
        renewalsThisMonthCount++;
      } else {
        // Expired before today
        expiredMembersCount++;
      }
    }

    let overdueTotal = 0;
    let upcomingTotal = 0;
    let allTimePendingTotal = 0;

    for (const p of payments) {
      if (!p.isPaid && p.status !== 'paid') {
        const amt = Number(p.amount) || 0;
        const paidAmt = Number(p.paidAmount) || 0;
        const pending = (amt > paidAmt) ? (amt - paidAmt) : amt;
        const pDueDate = (p.dueDate && p.dueDate !== '—') ? p.dueDate.slice(0, 10) : '';

        allTimePendingTotal += pending;

        if (pDueDate && pDueDate < todayStr) {
          // Overdue
          overdueTotal += pending;
        } else if (pDueDate && pDueDate <= lastDayThisMonthStr) {
          // Upcoming within this month
          upcomingTotal += pending;
        }
      }
    }

    // Money to be collected this month:
    const totalPendingThisMonth = overdueTotal + upcomingTotal;

    return {
      totalMembers: members.length,
      activeMembersCount,
      renewalsThisMonthCount,
      expiredMembersCount,
      expiredCount: expiredMembersCount,
      potentialMembersCount,
      overdueTotal,
      upcomingTotal,
      totalPending: totalPendingThisMonth,
      totalPendingThisMonth,
      allTimePendingTotal,
      lastDayThisMonthStr,
      totalPlansCount: this.state.availablePlans ? this.state.availablePlans.length : 0,
      enrolledMembersCount: activeMembersCount + renewalsThisMonthCount
    };
  }

  async ensurePlanExists({ name, defaultPrice, defaultDurationDays }) {
    const trimmedName = (name || 'Custom Plan').trim();

    // 1. Check local state cache
    let existing = this.state.availablePlans.find(
      p => p.name && p.name.toLowerCase() === trimmedName.toLowerCase()
    );
    if (existing) {
      if (existing.isActive === false) {
        await this.reactivatePlan(existing.id);
      }
      return existing.id;
    }

    // 2. Query backend /plan/list to check if already created
    try {
      const listRes = await api.get('/plan/list');
      const backendPlans = Array.isArray(listRes) ? listRes : (listRes?.plans || []);
      existing = backendPlans.find(
        p => p.name && p.name.toLowerCase() === trimmedName.toLowerCase()
      );
      if (existing) {
        this.state.availablePlans = backendPlans.map(p => ({
          id: p.id,
          name: p.name,
          price: Number(p.defaultPrice) || 0,
          defaultPrice: Number(p.defaultPrice) || 0,
          durationDays: p.defaultDuration || 30,
          durationMonths: Math.max(1, Math.round((p.defaultDuration || 30) / 30)),
          description: p.description || '',
          isActive: p.isActive !== false
        }));
        if (existing.isActive === false) {
          await this.reactivatePlan(existing.id);
        }
        this.notify();
        return existing.id;
      }
    } catch {
      // Proceed to create
    }

    // 3. Lazy Seed: create plan via POST /plan/new
    const created = await api.post('/plan/new', {
      name: trimmedName,
      description: 'Custom plan generated during member enrollment',
      defaultPrice: Number(defaultPrice) || 0,
      defaultDuration: Number(defaultDurationDays) || 30
    });

    if (created && created.id) {
      const newPlan = {
        id: created.id,
        name: created.name || trimmedName,
        price: Number(created.defaultPrice) || Number(defaultPrice) || 0,
        defaultPrice: Number(created.defaultPrice) || Number(defaultPrice) || 0,
        durationDays: created.defaultDuration || Number(defaultDurationDays) || 30,
        durationMonths: Math.max(1, Math.round((created.defaultDuration || 30) / 30)),
        description: created.description || '',
        isActive: true
      };
      this.state.availablePlans.push(newPlan);
      this.notify();
      return created.id;
    }

    throw new Error(`Failed to create custom plan "${trimmedName}"`);
  }

  async createPlan({ name, description, price, durationDays, durationMonths }) {
    const trimmedName = (name || '').trim();
    if (!trimmedName) throw new Error('Plan name is required');
    const days = Number(durationDays) || (Number(durationMonths) ? Number(durationMonths) * 30 : 30);

    // If an inactive plan with the exact same name already exists, reactivate and update it
    const existing = this.state.availablePlans.find(
      p => p.name && p.name.toLowerCase() === trimmedName.toLowerCase()
    );
    if (existing) {
      if (existing.isActive === false) {
        return await this.updatePlan({
          id: existing.id,
          name: trimmedName,
          description: description || existing.description || 'Gym workout membership with standard equipment and facility usage.',
          price: Number(price) || 0,
          durationDays: days,
          durationMonths: Math.max(1, Math.round(days / 30)),
          isActive: true
        });
      } else {
        throw new Error(`An active plan named "${trimmedName}" already exists.`);
      }
    }

    const payload = {
      name: trimmedName,
      description: description || 'Gym workout membership with standard equipment and facility usage.',
      defaultPrice: Number(price) || 0,
      defaultDuration: days
    };
    const res = await api.post('/plan/new', payload);
    ErrorMapper.notifySuccess(`Plan "${trimmedName}" created successfully!`);
    await this.loadInitialData({ forceRefresh: true });
    this.closePlanModal();
    return res;
  }

  async updatePlan({ id, name, description, price, durationDays, durationMonths, isActive = true }) {
    const trimmedName = (name || '').trim();
    if (!trimmedName) throw new Error('Plan name is required');
    const days = Number(durationDays) || (Number(durationMonths) ? Number(durationMonths) * 30 : 30);
    const payload = {
      planId: id,
      name: trimmedName,
      description: description || '',
      defaultPrice: Number(price) || 0,
      defaultDuration: days,
      isActive
    };
    const res = await api.post('/plan/update', payload);
    ErrorMapper.notifySuccess(`Plan "${trimmedName}" updated successfully!`);
    await this.loadInitialData({ forceRefresh: true });
    this.closePlanModal();
    return res;
  }

  async reactivatePlan(planId) {
    const target = this.state.availablePlans.find(p => p.id === planId);
    if (!target) throw new Error('Plan not found');
    const payload = {
      planId: target.id,
      name: target.name,
      description: target.description || '',
      defaultPrice: target.defaultPrice || target.price || 0,
      defaultDuration: target.durationDays || (target.durationMonths ? target.durationMonths * 30 : 30),
      isActive: true
    };
    const res = await api.post('/plan/update', payload);
    ErrorMapper.notifySuccess(`Plan "${target.name}" reactivated successfully!`);
    await this.loadInitialData({ forceRefresh: true });
    return res;
  }

  async deletePlan(planId) {
    const target = this.state.availablePlans.find(p => p.id === planId);
    const planName = target ? target.name : 'Plan';
    const payload = { planId };
    const res = await api.post('/plan/delete', payload);
    ErrorMapper.notifySuccess(`Plan "${planName}" deactivated successfully!`);
    await this.loadInitialData({ forceRefresh: true });
    return res;
  }

  buildPaymentSchedule(totalAmount, schedule = 'full_paid', startDateIso = null) {
    const today = new Date();
    const pad = (n) => String(n).padStart(2, '0');
    const toIso = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
    const todayIso = toIso(today);
    const start = startDateIso ? new Date(startDateIso) : today;
    const payments = [];
    const amt = Math.max(0, Number(totalAmount) || 0);

    if (schedule === 'full_paid') {
      payments.push({
        amount: amt,
        paidAmount: amt,
        dueDate: todayIso,
        paidDate: todayIso,
        paymentMethod: 'UPI',
        transactionId: null,
        notes: 'Full Payment - Paid Now'
      });
    } else if (schedule === 'full_pending') {
      payments.push({
        amount: amt,
        paidAmount: 0,
        dueDate: todayIso,
        paidDate: null,
        paymentMethod: 'NONE',
        transactionId: null,
        notes: 'Full Payment - Pending'
      });
    } else if (schedule === '2_installments') {
      const half = Math.round(amt / 2);
      const secondHalf = amt - half;
      const nextMonth = new Date(start);
      nextMonth.setMonth(nextMonth.getMonth() + 1);

      payments.push({
        amount: half,
        paidAmount: half,
        dueDate: todayIso,
        paidDate: todayIso,
        paymentMethod: 'UPI',
        transactionId: null,
        notes: 'Installment 1 of 2 (Paid)'
      });
      payments.push({
        amount: secondHalf,
        paidAmount: 0,
        dueDate: toIso(nextMonth),
        paidDate: null,
        paymentMethod: 'NONE',
        transactionId: null,
        notes: 'Installment 2 of 2 (Upcoming)'
      });
    } else if (schedule === '3_installments') {
      const part = Math.round(amt / 3);
      const lastPart = amt - (part * 2);
      const m1 = new Date(start);
      m1.setMonth(m1.getMonth() + 1);
      const m2 = new Date(start);
      m2.setMonth(m2.getMonth() + 2);

      payments.push({
        amount: part,
        paidAmount: part,
        dueDate: todayIso,
        paidDate: todayIso,
        paymentMethod: 'UPI',
        transactionId: null,
        notes: 'Installment 1 of 3 (Paid)'
      });
      payments.push({
        amount: part,
        paidAmount: 0,
        dueDate: toIso(m1),
        paidDate: null,
        paymentMethod: 'NONE',
        transactionId: null,
        notes: 'Installment 2 of 3 (Upcoming)'
      });
      payments.push({
        amount: lastPart,
        paidAmount: 0,
        dueDate: toIso(m2),
        paidDate: null,
        paymentMethod: 'NONE',
        transactionId: null,
        notes: 'Installment 3 of 3 (Upcoming)'
      });
    }
    return payments;
  }

  async addMember({ firstName, lastName, phone, plans, schedule, planId, customPlanData, startDate, payments: customPayments }) {
    const rawNumber = String(phone).replace(/\D/g, '');
    const countryCode = AppConfig.getCountryCode() || '91';
    const cleanNumber = rawNumber.length > 10 ? rawNumber.slice(-10) : rawNumber;

    const pad = (n) => String(n).padStart(2, '0');
    const toIso = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

    // Normalize plans input
    let itemsToProcess = [];
    if (Array.isArray(plans)) {
      itemsToProcess = plans;
    } else if (planId) {
      itemsToProcess = [{
        planId,
        customPlanData,
        startDate: startDate || toIso(new Date()),
        durationDays: 30,
        fee: null
      }];
    }

    const resolvedPlans = [];
    let aggregateTotalFee = 0;

    for (const item of itemsToProcess) {
      if (!item.planId) continue;
      let finalPlanId = item.planId;
      let planPrice = Number(item.fee);
      let durationDays = Number(item.durationDays) || 30;

      if (item.planId === '__CUSTOM__' && item.customPlanData) {
        const customName = (item.customPlanData?.name || item.name || 'Custom Plan').trim();
        const customPrice = Number(item.customPlanData?.price || item.fee) || 0;
        const customUnit = item.customPlanData?.unit || item.unit || 'months';
        const customVal = Number(item.customPlanData?.value || item.durationValue) || 1;
        durationDays = customUnit === 'months' ? customVal * 30 : customVal;
        planPrice = isNaN(planPrice) || planPrice === 0 ? customPrice : planPrice;

        finalPlanId = await this.ensurePlanExists({
          name: customName,
          defaultPrice: planPrice,
          defaultDurationDays: durationDays
        });
      } else {
        const found = this.state.availablePlans.find(p => p.id === item.planId);
        if (found) {
          if (isNaN(planPrice) || planPrice === 0) {
            planPrice = found.price || 0;
          }
          if (!item.durationDays) {
            durationDays = found.durationDays || (found.durationMonths ? found.durationMonths * 30 : 30);
          }
        }
      }

      const start = new Date(item.startDate || new Date());
      let endIso;
      if (item.endDate) {
        endIso = item.endDate;
      } else {
        const end = new Date(start);
        end.setDate(end.getDate() + durationDays);
        endIso = toIso(end);
      }

      resolvedPlans.push({
        planId: finalPlanId,
        startDate: toIso(start),
        endDate: endIso,
        fee: planPrice || 0
      });

      aggregateTotalFee += (planPrice || 0);
    }

    // Build Payments for aggregate total (or use custom payments if provided)
    let payments = [];
    if (Array.isArray(customPayments) && customPayments.length > 0) {
      payments = customPayments;
    } else if (aggregateTotalFee > 0) {
      payments = this.buildPaymentSchedule(
        aggregateTotalFee,
        schedule || 'full_paid',
        resolvedPlans[0]?.startDate
      );
    }

    const payload = {
      firstName,
      lastName,
      phoneNumber: {
        countryCode,
        number: cleanNumber
      },
      plans: resolvedPlans.map(p => ({
        planId: p.planId,
        startDate: p.startDate,
        endDate: p.endDate
      })),
      payments
    };

    const res = await api.post('/members/new', payload);
    if (resolvedPlans.length === 0) {
      ErrorMapper.notifySuccess(`Prospect "${firstName} ${lastName}" saved successfully!`);
    } else {
      ErrorMapper.notifySuccess(`Member "${firstName} ${lastName}" registered successfully!`);
    }
    await this.loadInitialData({ forceRefresh: true });
    this.closeAddMemberModal();
    return res;
  }

  async autoSaveProspect({ firstName, lastName, phone }) {
    let cleanNumber = phone ? String(phone).replace(/\D/g, '') : '';
    let countryCode = AppConfig.getCountryCode() || '91';
    if (cleanNumber.length > 10) {
      countryCode = cleanNumber.slice(0, cleanNumber.length - 10);
      cleanNumber = cleanNumber.slice(-10);
    }

    const payload = {
      firstName: firstName ? firstName.trim() : '',
      lastName: lastName ? lastName.trim() : '',
      phoneNumber: {
        countryCode,
        number: cleanNumber
      },
      plans: [],
      payments: []
    };

    const res = await api.post('/members/new', payload);
    if (res && res.userId) {
      const formattedName = `${res.firstName || firstName || ''} ${res.lastName || lastName || ''}`.trim() || 'Prospect';
      const formattedPhone = `+${countryCode} ${cleanNumber}`;

      const existingMemberIdx = this.state.members.findIndex(m => m.id === res.userId);
      const prospectMember = {
        id: res.userId,
        userId: res.userId,
        name: formattedName,
        firstName: res.firstName || firstName || '',
        lastName: res.lastName || lastName || '',
        phone: formattedPhone,
        rawPhone: cleanNumber,
        plans: 'No Plan',
        activePlansList: [],
        subscriptions: [],
        payments: [],
        pendingAmount: 0,
        totalSpent: 0,
        status: 'prospect',
        statusLabel: 'Prospect',
        statusVariant: 'neutral',
        expiryDate: '—',
        memberSince: new Date().toISOString().slice(0, 10),
        planBatches: [],
        rawRecord: res
      };

      if (existingMemberIdx >= 0) {
        this.state.members[existingMemberIdx] = { ...this.state.members[existingMemberIdx], ...prospectMember };
      } else {
        this.state.members.push(prospectMember);
      }

      if (!this.state.conversations.some(c => c.id === res.userId)) {
        this.state.conversations.push({
          id: res.userId,
          memberId: res.userId,
          name: formattedName,
          phone: formattedPhone,
          status: 'prospect',
          statusLabel: 'Prospect',
          plans: 'No Plan',
          pendingAmount: 0,
          expiryDate: '—',
          isHumanRequired: false,
          unreadCount: 0,
          lastMessage: 'No messages yet',
          lastTime: '',
          messages: []
        });
      }
    }
    return res;
  }

  async renewMember({ userId, plans, schedule, payments: customPayments }) {
    const pad = (n) => String(n).padStart(2, '0');
    const toIso = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

    const resolvedPlans = [];
    let aggregateTotalFee = 0;

    for (const item of (plans || [])) {
      let finalPlanId = item.planId;
      let planPrice = Number(item.fee);
      let durationDays = Number(item.durationDays) || 30;

      if (item.planId === '__CUSTOM__') {
        const customName = (item.customPlanData?.name || item.name || 'Custom Plan').trim();
        const customPrice = Number(item.customPlanData?.price || item.fee) || 0;
        const customUnit = item.customPlanData?.unit || item.unit || 'months';
        const customVal = Number(item.customPlanData?.value || item.durationValue) || 1;
        durationDays = customUnit === 'months' ? customVal * 30 : customVal;
        planPrice = isNaN(planPrice) || planPrice === 0 ? customPrice : planPrice;

        finalPlanId = await this.ensurePlanExists({
          name: customName,
          defaultPrice: planPrice,
          defaultDurationDays: durationDays
        });
      } else {
        const found = this.state.availablePlans.find(p => p.id === item.planId);
        if (found) {
          if (isNaN(planPrice) || planPrice === 0) {
            planPrice = found.price || 0;
          }
          if (!item.durationDays) {
            durationDays = found.durationDays || (found.durationMonths ? found.durationMonths * 30 : 30);
          }
        }
      }

      let startDateIso = item.startDate ? toIso(new Date(item.startDate)) : null;
      let endDateIso = item.endDate ? toIso(new Date(item.endDate)) : null;

      if (!endDateIso && startDateIso) {
        const start = new Date(startDateIso);
        const end = new Date(start);
        end.setDate(end.getDate() + durationDays);
        endDateIso = toIso(end);
      }

      resolvedPlans.push({
        planId: finalPlanId,
        startDate: startDateIso,
        endDate: endDateIso,
        fee: planPrice || 0
      });

      aggregateTotalFee += (planPrice || 0);
    }

    const payments = (Array.isArray(customPayments) && customPayments.length > 0)
      ? customPayments
      : this.buildPaymentSchedule(
          aggregateTotalFee,
          schedule || 'full_paid',
          resolvedPlans[0]?.startDate
        );

    const payload = {
      userId,
      plans: resolvedPlans.map(p => ({
        planId: p.planId,
        startDate: p.startDate,
        endDate: p.endDate
      })),
      payments
    };

    const targetMember = this.state.members.find(m => (m.id === userId || m.userId === userId)) || this.state.renewMemberModal?.member;
    const isProspect = targetMember ? (targetMember.status === 'prospect' || (!targetMember.subscriptions || targetMember.subscriptions.length === 0)) : false;

    const res = await api.post('/members/renew', payload);
    ErrorMapper.notifySuccess(isProspect ? 'Member enrolled with plan successfully!' : 'Membership renewed successfully!');
    await this.loadInitialData({ forceRefresh: true });
    this.closeRenewMemberModal();
    return res;
  }

  async updateMember({ userId, firstName, lastName, phoneNumber, plans = [], payments = [], paidPaymentPlanLinks = [], silent = false, successMessage = 'Member profile updated successfully!' }) {
    let cleanPhone = null;
    if (phoneNumber) {
      if (typeof phoneNumber === 'object' && phoneNumber.number) {
        cleanPhone = phoneNumber;
      } else {
        const raw = String(phoneNumber).replace(/\D/g, '');
        const cleanNumber = raw.length > 10 ? raw.slice(-10) : raw;
        const countryCode = raw.length > 10 ? raw.slice(0, -10) : (AppConfig.getCountryCode() || '91');
        cleanPhone = { countryCode, number: cleanNumber };
      }
    }

    const payload = {
      userId,
      profile: {
        firstName: firstName ? firstName.trim() : '',
        lastName: lastName ? lastName.trim() : '',
        phoneNumber: cleanPhone
      },
      plans: Array.isArray(plans) ? plans.map(p => ({
        planId: p.planId,
        startDate: p.startDate,
        endDate: p.endDate
      })) : [],
      payments: Array.isArray(payments) ? payments.map(p => ({
        amount: Number(p.amount) || 0,
        paidAmount: Number(p.paidAmount) || 0,
        dueDate: p.dueDate,
        paidDate: p.paidDate || null,
        paymentMethod: p.paymentMethod || 'UPI',
        transactionId: p.transactionId || null,
        notes: p.notes || null,
        planIndices: Array.isArray(p.planIndices) ? p.planIndices : null
      })) : [],
      paidPaymentPlanLinks: Array.isArray(paidPaymentPlanLinks) ? paidPaymentPlanLinks.map(l => ({
        paymentId: l.paymentId,
        planIndices: l.planIndices || []
      })) : []
    };

    const res = await api.post('/members/update', payload);
    if (!silent && successMessage) {
      ErrorMapper.notifySuccess(successMessage);
    }
    await this.loadInitialData({ forceRefresh: true });
    return res;
  }

  async collectPayment({ paymentId, paidAmount, paidDate, paymentMethod, transactionId, notes }) {
    const pad = (n) => String(n).padStart(2, '0');
    const toIso = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

    const payload = {
      paymentId,
      paidAmount: Number(paidAmount) || 0,
      paidDate: paidDate || toIso(new Date()),
      paymentMethod: paymentMethod || 'UPI',
      transactionId: transactionId || null,
      notes: notes || null
    };

    const res = await api.post('/members/payment/collect', payload);
    ErrorMapper.notifySuccess('Payment marked as paid successfully!');
    await this.loadInitialData({ forceRefresh: true });
    this.closeCollectPaymentModal();
    return res;
  }

  async sendPaymentReceipt(paymentId, userId) {
    try {
      const payload = { paymentId, userId };
      const res = await api.post('/members/payment/receipt/send', payload);
      ErrorMapper.notifySuccess('Receipt sent via WhatsApp successfully!');
      return res;
    } catch (e) {
      ErrorMapper.handle(e, 'Failed to send payment receipt');
      throw e;
    }
  }

  async sendPaymentReminder(payment, member) {
    try {
      const todayDate = new Date();
      todayDate.setHours(0, 0, 0, 0);
      const isOverdue = payment && (
        payment.isOverdue ||
        payment.status === 'overdue' ||
        (payment.dueDate && payment.dueDate !== '—' && !isNaN(new Date(payment.dueDate).getTime()) && new Date(payment.dueDate) < todayDate)
      );
      const tplKey = isOverdue ? 'payment_overdue_reminder' : 'upcoming_payment_reminder';
      const availableTemplates = AppConfig.getQuickTemplates();
      let templateObj = availableTemplates.find(t => (t.templateKey || t.key) === tplKey);
      if (!templateObj) {
        templateObj = {
          id: null,
          key: tplKey,
          templateKey: tplKey,
          name: isOverdue ? 'Payment Overdue Reminder' : 'Upcoming Payment Reminder',
          category: 'UTILITY',
          channel: 'WHATSAPP'
        };
      }
      const memberId = member.id || member.userId;
      const res = await this.sendTemplateMessage(memberId, templateObj, null, null);
      return res;
    } catch (e) {
      ErrorMapper.handle(e, 'Failed to send payment reminder');
      throw e;
    }
  }

  // ==========================================
  // WhatsApp Chats & Alerts Integration
  // ==========================================
  setActiveChatId(chatId) {
    if (!chatId) return;
    if (String(this.state.activeChatId) === String(chatId)) return;
    this.state.activeChatId = chatId;
    this.notify();
    this.markChatAsRead(chatId);

    // If messages are already present in memory from conversations or SSE stream, skip redundant timeline API hits
    const conv = (this.state.conversations || []).find(c => String(c.id) === String(chatId));
    if (!conv || !Array.isArray(conv.messages) || conv.messages.length === 0) {
      this.loadConversationTimeline(chatId);
    }
  }

  setChatFilterTab(tab) {
    this.state.chatFilterTab = tab;
    this.notify();
  }

  setChatSearchQuery(query) {
    this.state.chatSearchQuery = query;
    this.notify();
  }

  openTemplateModal() {
    const activeConvId = this.state.activeChatId;
    const conv = (this.state.conversations || []).find(c => c.id === activeConvId);
    if (conv) {
      const member = (this.state.members || []).find(m => m.id === (conv.memberId || conv.id)) || {
        id: conv.memberId || conv.id,
        name: conv.name,
        phone: conv.phone,
        status: conv.statusLabel || 'Active',
        statusLabel: conv.statusLabel || 'Active',
        pendingAmount: conv.pendingAmount,
        expiryDate: conv.expiryDate,
        plan: conv.plans,
        plans: conv.plans
      };
      this.openWhatsAppModal(member);
    }
  }

  closeTemplateModal() {
    this.closeWhatsAppModal();
  }

  getFilteredConversations() {
    const { conversations, chatSearchQuery } = this.state;
    let list = [...(conversations || [])];

    // Only list conversations that have messages or active history
    list = list.filter(c => (c.messages && c.messages.length > 0) || c.isHumanRequired);

    const q = (chatSearchQuery || '').toLowerCase().trim();
    if (q) {
      list = (conversations || []).filter(c => {
        const matchesQuery = (c.name && c.name.toLowerCase().includes(q)) ||
          (c.phone && c.phone.includes(q)) ||
          (c.lastMessage && c.lastMessage.toLowerCase().includes(q));
        return matchesQuery;
      });
    }

    // Sort conversations: Newest message / activity first
    list.sort((a, b) => {
      const getTimestamp = (conv) => {
        if (conv.messages && conv.messages.length > 0) {
          const lastMsg = conv.messages[conv.messages.length - 1];
          if (lastMsg && lastMsg.timestamp) {
            const t = new Date(lastMsg.timestamp).getTime();
            if (!isNaN(t) && t > 0) return t;
          }
        }
        if (conv.lastTimestamp) {
          const t = new Date(conv.lastTimestamp).getTime();
          if (!isNaN(t) && t > 0) return t;
        }
        return 0;
      };
      return getTimestamp(b) - getTimestamp(a);
    });

    return list;
  }

  getNeedsAttentionCount() {
    return (this.state.conversations || []).filter(c => c.isHumanRequired).length;
  }

  getActiveChatsCount() {
    return (this.state.conversations || []).filter(c => c.messages && c.messages.length > 0).length;
  }

  async loadAllConversations(force = false) {
    const currentTenant = SessionManager.getTenantId();

    // 1. Instant 0ms load from IndexedDB cache if in-memory conversations are empty
    if (!this._conversationsLoaded && (!this.state.conversations || this.state.conversations.length === 0)) {
      try {
        const cachedConvs = await GymCache.getChatCache(currentTenant);
        if (Array.isArray(cachedConvs) && cachedConvs.length > 0) {
          this.state.conversations = cachedConvs;
          if (!this.state.activeChatId) {
            const withMessages = cachedConvs.find(c => (c.messages && c.messages.length > 0) || c.isHumanRequired);
            if (withMessages) this.state.activeChatId = withMessages.id;
          }
          this.notify();
        }
      } catch (_) {}
    }

    if (!force && this._conversationsLoaded) {
      return;
    }
    try {
      const res = await api.get('/alerts/chat/conversations', { silentError: true });
      this._conversationsLoaded = true;
      if (Array.isArray(res) && res.length > 0) {
        for (const item of res) {
          let conv = (this.state.conversations || []).find(c => String(c.id) === String(item.userId));
          if (!conv) {
            const rawMember = (this.state.rawMembers || []).find(m => String(m.id) === String(item.userId));
            conv = {
              id: item.userId,
              memberId: item.userId,
              name: (rawMember ? `${rawMember.firstName || ''} ${rawMember.lastName || ''}`.trim() : '') || item.userName || 'Prospect Lead',
              phone: (rawMember && rawMember.phone) || item.contactValue || '',
              status: 'prospect',
              statusLabel: 'Prospect',
              plans: [],
              pendingAmount: 0,
              expiryDate: null,
              isHumanRequired: Boolean(item.isHumanRequired),
              unreadCount: item.isHumanRequired ? 1 : 0,
              lastMessage: '',
              lastTime: '',
              messages: []
            };
            this.state.conversations.push(conv);
          }

          if (conv) {
            if (item.userName && (!conv.name || conv.name === 'Prospect Lead' || conv.name === 'WhatsApp Contact')) {
              conv.name = item.userName;
            }
            if (item.contactValue && !conv.phone) {
              conv.phone = item.contactValue;
            }
            if (item.isHumanRequired !== undefined) {
              conv.isHumanRequired = Boolean(item.isHumanRequired);
              conv.unreadCount = conv.isHumanRequired ? 1 : 0;
            }
            if (Array.isArray(item.messages) && item.messages.length > 0) {
              const incoming = item.messages.map(m => ({
                id: m.id,
                sender: m.direction === 'OUTBOUND' ? 'staff' : 'member',
                direction: m.direction,
                isOutgoing: m.direction === 'OUTBOUND',
                text: m.content || '',
                time: m.timestamp ? new Date(m.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '',
                timestamp: m.timestamp,
                status: m.status || 'PENDING',
                buttons: m.buttons || [],
                mediaUrl: m.mediaUrl,
                mediaType: m.mediaType,
                replyToMessageId: m.replyToMessageId,
                replyToSender: m.replyToSender,
                replyToText: m.replyToText,
                fileName: m.fileName || (String(m.mediaType).toUpperCase() === 'DOCUMENT'
                  ? `Payment_Receipt_${(conv.name || 'Member').trim().split(/\s+/)[0]}_${(m.timestamp ? new Date(m.timestamp) : new Date()).toISOString().slice(0, 10)}.pdf`
                  : 'Attachment')
              }));

              // Merge incoming conservative messages with any existing messages without dropping older history
              const oldestIncomingTime = incoming.length > 0 && incoming[0].timestamp ? new Date(incoming[0].timestamp).getTime() : 0;
              const olderExisting = (conv.messages || []).filter(m => {
                const t = m.timestamp ? new Date(m.timestamp).getTime() : 0;
                return t < oldestIncomingTime && !incoming.some(inc => String(inc.id) === String(m.id));
              });
              conv.messages = [...olderExisting, ...incoming];

              const last = conv.messages[conv.messages.length - 1];
              if (last) {
                conv.lastMessage = last.text;
                conv.lastTime = last.time;
              }
              const unreadInbound = conv.messages.filter(m => m.direction === 'INBOUND' && (m.status || '').toUpperCase() !== 'READ').length;
              conv.unreadCount = String(conv.id) === String(this.state.activeChatId) ? 0 : unreadInbound;
            } else if (!conv.messages || conv.messages.length === 0) {
              conv.unreadCount = 0;
            }
          }
        }

        // If no active chat selected yet, pick the first conversation with messages or attention
        if (!this.state.activeChatId) {
          const withMessages = (this.state.conversations || []).find(c => (c.messages && c.messages.length > 0) || c.isHumanRequired);
          if (withMessages) {
            this.state.activeChatId = withMessages.id;
          }
        }

        // Persist conservative conversations & messages to IndexedDB
        GymCache.setChatCache(currentTenant, this.state.conversations);
        this.notify();
      }
    } catch (_) {
      // Retain memory conversations if API endpoint fails
    }
  }

  async loadOlderMessages(userId) {
    if (!userId) return;
    const conv = (this.state.conversations || []).find(c => String(c.id) === String(userId));
    if (!conv || conv._loadingOlder || conv._hasMoreOlder === false) return;

    conv._loadingOlder = true;
    this.notify();

    try {
      let oldestTimestamp = null;
      for (const m of (conv.messages || [])) {
        if (m.timestamp) {
          const t = new Date(m.timestamp).getTime();
          if (!isNaN(t)) {
            if (oldestTimestamp === null || t < oldestTimestamp) {
              oldestTimestamp = t;
            }
          }
        }
      }

      const beforeTimestamp = oldestTimestamp ? new Date(oldestTimestamp).toISOString() : null;
      if (!beforeTimestamp) {
        conv._hasMoreOlder = false;
        return;
      }

      const res = await api.post('/alerts/chat/timeline', {
        userId,
        channel: 'WHATSAPP',
        limit: 30,
        beforeTimestamp
      }, { silentError: true });

      if (res && Array.isArray(res.messages)) {
        if (res.messages.length < 30) {
          conv._hasMoreOlder = false;
        }

        if (res.messages.length > 0) {
          const olderFormatted = res.messages.map(m => {
            const mType = (m.messageType || '').toUpperCase();
            let resolvedFileName = m.fileName;
            if (!resolvedFileName) {
              if (mType === 'DOCUMENT' || String(m.mediaType).toUpperCase() === 'DOCUMENT') {
                const cleanText = (m.content || '').trim();
                if (cleanText && cleanText !== '[Document]' && cleanText.endsWith('.pdf')) {
                  resolvedFileName = cleanText;
                } else if (m.direction === 'OUTBOUND') {
                  const firstName = (conv.name || 'Member').trim().split(/\s+/)[0];
                  const dateStr = (m.timestamp ? new Date(m.timestamp) : new Date()).toISOString().slice(0, 10);
                  resolvedFileName = `Payment_Receipt_${firstName}_${dateStr}.pdf`;
                } else {
                  resolvedFileName = 'Attachment.pdf';
                }
              } else {
                resolvedFileName = 'Attachment';
              }
            }

            return {
              id: m.id,
              sender: m.direction === 'OUTBOUND' ? 'staff' : 'member',
              direction: m.direction,
              isOutgoing: m.direction === 'OUTBOUND',
              messageType: m.messageType || 'TEXT',
              text: m.content || '',
              time: m.timestamp ? new Date(m.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '',
              timestamp: m.timestamp,
              status: m.status || 'PENDING',
              buttons: m.buttons || [],
              mediaUrl: m.mediaUrl,
              mediaType: m.mediaType,
              replyToMessageId: m.replyToMessageId,
              replyToSender: m.replyToSender,
              replyToText: m.replyToText,
              fileName: resolvedFileName
            };
          });

          const existingIds = new Set((conv.messages || []).map(m => String(m.id)));
          const uniqueOlder = olderFormatted.filter(m => !existingIds.has(String(m.id)));
          if (uniqueOlder.length > 0) {
            conv.messages = [...uniqueOlder, ...(conv.messages || [])];
            const currentTenant = SessionManager.getTenantId();
            GymCache.setChatCache(currentTenant, this.state.conversations);
            return true;
          }
        }
      } else {
        conv._hasMoreOlder = false;
      }
      return false;
    } catch (err) {
      console.error('Failed to load older messages:', err);
      return false;
    } finally {
      conv._loadingOlder = false;
      this.notify();
    }
  }

  async findOrLoadMessage(userId, targetMessageId) {
    if (!userId || !targetMessageId) return null;
    const conv = (this.state.conversations || []).find(c => String(c.id) === String(userId));
    if (!conv) return null;

    let found = (conv.messages || []).find(m => String(m.id) === String(targetMessageId));
    if (found) return found;

    let attempts = 0;
    while (!found && conv._hasMoreOlder !== false && attempts < 5) {
      attempts++;
      const loaded = await this.loadOlderMessages(userId);
      found = (conv.messages || []).find(m => String(m.id) === String(targetMessageId));
      if (!loaded) break;
    }
    return found || null;
  }

  async loadConversationTimeline(userId) {
    if (!userId) return;
    try {
      const res = await api.post('/alerts/chat/timeline', {
        userId,
        channel: 'WHATSAPP',
        limit: 200
      }, { silentError: true });

      if (res) {
        const conv = (this.state.conversations || []).find(c => String(c.id) === String(userId));
        if (conv) {
          if (res.isHumanRequired !== undefined) {
            conv.isHumanRequired = Boolean(res.isHumanRequired);
          }
          if (Array.isArray(res.messages) && res.messages.length > 0) {
            const serverMessages = res.messages.map(m => {
              const mType = (m.messageType || '').toUpperCase();
              let resolvedFileName = m.fileName;
              if (!resolvedFileName) {
                if (mType === 'DOCUMENT' || String(m.mediaType).toUpperCase() === 'DOCUMENT') {
                  // Derive a clean file name from text/caption or default
                  const cleanText = (m.content || '').trim();
                  if (cleanText && cleanText !== '[Document]' && cleanText.endsWith('.pdf')) {
                    resolvedFileName = cleanText;
                  } else if (m.direction === 'OUTBOUND') {
                    const firstName = (conv.name || 'Member').trim().split(/\s+/)[0];
                    const dateStr = (m.timestamp ? new Date(m.timestamp) : new Date()).toISOString().slice(0, 10);
                    resolvedFileName = `Payment_Receipt_${firstName}_${dateStr}.pdf`;
                  } else {
                    resolvedFileName = 'Attachment.pdf';
                  }
                } else {
                  resolvedFileName = 'Attachment';
                }
              }

              return {
                id: m.id,
                sender: m.direction === 'OUTBOUND' ? 'staff' : 'member',
                direction: m.direction,
                isOutgoing: m.direction === 'OUTBOUND',
                messageType: m.messageType || 'TEXT',
                text: m.content || '',
                time: m.timestamp ? new Date(m.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '',
                timestamp: m.timestamp,
                status: m.status || 'PENDING',
                buttons: m.buttons || [],
                mediaUrl: m.mediaUrl,
                mediaType: m.mediaType,
                replyToMessageId: m.replyToMessageId,
                replyToSender: m.replyToSender,
                replyToText: m.replyToText,
                fileName: resolvedFileName
              };
            });

            const prevSign = `${conv.isHumanRequired}|${(conv.messages || []).map(m => `${m.id}:${m.status}`).join('|')}`;

            // Keep any active in-flight pending messages that haven't been picked up by server yet
            const pendingOptimistic = (conv.messages || []).filter(m => 
              m.status === 'PENDING' && !serverMessages.some(sm => sm.id === m.id || (sm.direction === 'OUTBOUND' && sm.text === m.text))
            );
            conv.messages = [...serverMessages, ...pendingOptimistic];

            const last = conv.messages[conv.messages.length - 1];
            if (last) {
              conv.lastMessage = last.text;
              conv.lastTime = last.time;
            }

            const newSign = `${conv.isHumanRequired}|${(conv.messages || []).map(m => `${m.id}:${m.status}`).join('|')}`;
            if (prevSign !== newSign) {
              this.notify();
            }

            if (String(userId) === String(this.state.activeChatId)) {
              const hasUnread = serverMessages.some(m => m.direction === 'INBOUND' && (m.status || '').toUpperCase() !== 'READ');
              if (hasUnread) {
                this.markChatAsRead(userId);
              }
            }
          }
        }
      }
    } catch {
      // Retain memory messages
    }
  }

  async markChatAsRead(userId) {
    if (!userId) return;
    const conv = (this.state.conversations || []).find(c => String(c.id) === String(userId));
    if (!conv) return;

    const hasUnreadCount = (conv.unreadCount || 0) > 0;
    const hasUnreadInbound = Array.isArray(conv.messages) && conv.messages.some(m => m.direction === 'INBOUND' && m.status !== 'READ');

    // If there are no unread incoming messages, avoid unnecessary API calls
    if (!hasUnreadCount && !hasUnreadInbound) {
      return;
    }

    conv.unreadCount = 0;
    if (Array.isArray(conv.messages)) {
      conv.messages.forEach(m => {
        if (m.direction === 'INBOUND') {
          m.status = 'READ';
        }
      });
    }
    this.notify();

    try {
      await api.post('/alerts/chat/read', {
        userId,
        channel: 'WHATSAPP'
      }, { silentError: true });
    } catch (_) {}
  }

  handleIncomingStreamMessage(m) {
    if (!m || !m.userId) return;

    let conv = (this.state.conversations || []).find(c => String(c.id) === String(m.userId));
    if (!conv) {
      const member = (this.state.members || []).find(mem => String(mem.id) === String(m.userId));
      conv = {
        id: m.userId,
        name: member ? member.name : (m.displayName || 'Member'),
        phone: member ? member.phone : (m.contactPhone || ''),
        planName: member ? (member.planName || 'Active') : 'General',
        unreadCount: 0,
        messages: []
      };
      this.state.conversations.unshift(conv);
    }

    conv.messages = conv.messages || [];

    const isOutbound = m.direction === 'OUTBOUND';
    const formatted = {
      id: m.id,
      sender: isOutbound ? 'staff' : 'member',
      direction: m.direction || (isOutbound ? 'OUTBOUND' : 'INBOUND'),
      isOutgoing: isOutbound,
      messageType: m.messageType || 'TEXT',
      text: m.content || '',
      time: m.timestamp ? new Date(m.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '',
      timestamp: m.timestamp || new Date().toISOString(),
      status: m.status || (isOutbound ? 'SENT' : 'DELIVERED'),
      buttons: m.buttons || [],
      mediaUrl: m.mediaUrl,
      mediaId: m.mediaId,
      mediaType: m.mediaType,
      replyToMessageId: m.replyToMessageId,
      replyToSender: m.replyToSender,
      replyToText: m.replyToText
    };

    const existingIdx = conv.messages.findIndex(ex => 
      String(ex.id) === String(m.id) || 
      (isOutbound && (ex.status === 'PENDING' || String(ex.id).startsWith('msg_')) && ex.text === formatted.text)
    );

    if (existingIdx !== -1) {
      conv.messages[existingIdx] = { ...conv.messages[existingIdx], ...formatted };
    } else {
      conv.messages.push(formatted);
    }

    conv.lastMessage = formatted.text || (formatted.mediaType ? `[${formatted.mediaType}]` : '[Attachment]');
    conv.lastTime = formatted.time;
    conv.lastMessageTime = formatted.timestamp;

    const isCurrentlyActive = String(this.state.activeChatId) === String(conv.id) && !document.hidden;
    if (isCurrentlyActive && !isOutbound) {
      conv.unreadCount = 0;
      formatted.status = 'READ';
      this.markChatAsRead(conv.id);
    } else if (!isOutbound) {
      conv.unreadCount = (conv.unreadCount || 0) + 1;
    }

    this.notify();
    const currentTenant = SessionManager.getTenantId();
    GymCache.setChatCache(currentTenant, this.state.conversations);
  }

  handleStreamMessageStatus(update) {
    if (!update || !update.messageId) return;
    const conv = (this.state.conversations || []).find(c => String(c.id) === String(update.userId));
    if (!conv || !Array.isArray(conv.messages)) return;

    const msg = conv.messages.find(m => String(m.id) === String(update.messageId));
    if (msg) {
      msg.status = update.status;
      this.notify();
      const currentTenant = SessionManager.getTenantId();
      GymCache.setChatCache(currentTenant, this.state.conversations);
    }
  }

  async catchUpChatsAfterReconnect() {
    try {
      const currentTenant = SessionManager.getTenantId();
      if (currentTenant) {
        await GymCache.clearChatCache(currentTenant);
      }
      await this.loadAllConversations(true);
    } catch (err) {
      console.warn('[HomeStore] Failed to catch up chats after reconnect:', err);
    }
  }



  async sendChatMessage(conversationId, text, attachment = null) {
    const conv = this.state.conversations.find(c => c.id === conversationId);
    if (!conv) return;

    const now = new Date();
    const timeStr = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    let mediaType = null;
    let mediaUrl = null;
    let fileName = null;

    if (attachment) {
      fileName = attachment.fileName;
      mediaType = attachment.mimeType && attachment.mimeType.startsWith('image/') ? 'IMAGE' : 'DOCUMENT';
      mediaUrl = attachment.dataBase64;
    }

    const optimisticMsg = {
      id: `msg_${Date.now()}`,
      sender: 'staff',
      direction: 'OUTBOUND',
      isOutgoing: true,
      text: text || (mediaType === 'IMAGE' ? '[Image]' : '[Document]'),
      time: timeStr,
      timestamp: now.toISOString(),
      mediaUrl,
      mediaType,
      fileName,
      status: 'PENDING'
    };

    conv.messages = conv.messages || [];
    conv.messages.push(optimisticMsg);
    conv.lastMessage = optimisticMsg.text;
    conv.lastTime = timeStr;
    // Keep isHumanRequired active during staff conversation until explicitly resolved!
    conv.unreadCount = 0;

    this.notify();

    try {
      let uploadedMediaId = null;
      let uploadedMediaUrl = null;

      if (attachment && attachment.dataBase64) {
        try {
          const uploadRes = await api.post('/alerts/media/upload', {
            fileName: attachment.fileName,
            mimeType: attachment.mimeType,
            dataBase64: attachment.dataBase64
          }, { silentError: true });
          if (uploadRes && uploadRes.mediaId) {
            uploadedMediaId = uploadRes.mediaId;
            uploadedMediaUrl = uploadRes.mediaUrl;
            optimisticMsg.mediaId = uploadedMediaId;
            optimisticMsg.mediaUrl = uploadedMediaUrl;
          }
        } catch (e) {
          console.warn('Alerts media upload fallback:', e);
        }
      }

      const replyRes = await api.post('/alerts/chat/reply', {
        userId: conv.id,
        channel: 'WHATSAPP',
        message: text,
        mediaUrl: uploadedMediaUrl,
        mediaId: uploadedMediaId,
        mediaType
      }, { silentError: true });

      if (replyRes && replyRes.id) {
        optimisticMsg.id = replyRes.id;
        optimisticMsg.status = replyRes.status || 'PENDING';
      }
    } catch (err) {
      console.warn('Alerts chat reply fallback:', err);
      optimisticMsg.status = 'FAILED';
    }

    this.notify();
  }

  async sendTemplateMessage(conversationId, template, customText = null, customParameters = null) {
    let conv = this.state.conversations.find(c => c.id === conversationId);
    let member = this.state.members.find(m => m.id === conversationId);

    if (!member && this.state.whatsAppModal?.member && (this.state.whatsAppModal.member.id === conversationId || this.state.whatsAppModal.member.userId === conversationId)) {
      member = this.state.whatsAppModal.member;
    }

    if (!conv && member) {
      conv = {
        id: member.id || member.userId,
        memberId: member.id || member.userId,
        name: member.name,
        phone: member.phone,
        status: member.status || 'prospect',
        statusLabel: member.statusLabel || member.status || 'Prospect',
        pendingAmount: member.pendingAmount || 0,
        expiryDate: member.expiryDate || '—',
        plans: member.plan || member.plans || 'No Plan',
        isHumanRequired: false,
        unreadCount: 0,
        lastMessage: 'No messages yet',
        lastTime: '',
        messages: []
      };
      this.state.conversations.push(conv);
      if (!this.state.members.some(m => m.id === (member.id || member.userId))) {
        this.state.members.push(member);
      }
    }

    if (!conv) {
      throw new Error(`Recipient conversation with ID ${conversationId} could not be resolved.`);
    }

    const now = new Date();
    const timeStr = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    // Resolve expiring subscriptions/plans for PLAN.* variables
    const subs = Array.isArray(member?.subscriptions) && member.subscriptions.length > 0
      ? member.subscriptions
      : (Array.isArray(member?.plans) ? member.plans : []);

    let targetSubs = [];
    if (subs.length > 0) {
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      const lastDayOfMonth = new Date(today.getFullYear(), today.getMonth() + 1, 0);
      lastDayOfMonth.setHours(23, 59, 59, 999);

      const activeSubs = subs
        .filter(s => s && s.endDate && !isNaN(new Date(s.endDate).getTime()) && new Date(s.endDate) >= today)
        .sort((a, b) => new Date(a.endDate) - new Date(b.endDate));

      if (activeSubs.length > 0) {
        // 1. All active plans expiring on or before the end of this month
        const thisMonthSubs = activeSubs.filter(s => new Date(s.endDate) <= lastDayOfMonth);
        if (thisMonthSubs.length > 0) {
          targetSubs = thisMonthSubs;
        } else {
          // 2. All active plans expiring within 30 days
          const within30Subs = activeSubs.filter(s => {
            const exp = new Date(s.endDate);
            exp.setHours(0, 0, 0, 0);
            const days = Math.ceil((exp - today) / (1000 * 60 * 60 * 24));
            return days <= 30;
          });

          if (within30Subs.length > 0) {
            targetSubs = within30Subs;
          } else {
            // 3. Earliest active expiry date
            const minTime = new Date(activeSubs[0].endDate).getTime();
            targetSubs = activeSubs.filter(s => new Date(s.endDate).getTime() === minTime);
          }
        }
      } else {
        // Fallback: all plans on most recently expired date
        const expiredSubs = subs
          .filter(s => s && s.endDate && !isNaN(new Date(s.endDate).getTime()))
          .sort((a, b) => new Date(b.endDate) - new Date(a.endDate));
        if (expiredSubs.length > 0) {
          const maxTime = new Date(expiredSubs[0].endDate).getTime();
          targetSubs = expiredSubs.filter(s => new Date(s.endDate).getTime() === maxTime);
        } else {
          targetSubs = subs.slice(0, 1);
        }
      }
    }

    const targetPlanNames = targetSubs
      .map(s => s.planName || s.name || s.customName)
      .filter(Boolean);
    const uniquePlanNames = [...new Set(targetPlanNames)];
    const resolvedPlanName = uniquePlanNames.length > 0
      ? uniquePlanNames.join(', ')
      : (conv.plans || member?.plans || 'General Access');

    const earliestTargetSub = targetSubs.length > 0
      ? targetSubs.reduce((min, s) => (!min || new Date(s.endDate) < new Date(min.endDate) ? s : min), null)
      : null;

    const rawExpiry = earliestTargetSub?.endDate || conv.expiryDate || member?.expiryDate;
    const formattedExpiry = rawExpiry ? this.formatDateDisplay(rawExpiry) : '31 Oct 2026';
    let daysLeft = 0;
    if (rawExpiry) {
      try {
        const exp = new Date(rawExpiry);
        if (!isNaN(exp.getTime())) {
          const today = new Date();
          today.setHours(0, 0, 0, 0);
          exp.setHours(0, 0, 0, 0);
          daysLeft = Math.max(0, Math.ceil((exp - today) / (1000 * 60 * 60 * 24)));
        }
      } catch (_) {}
    }

    // Resolve target upcoming/unpaid payment installment for PAYMENT.* variables
    const unpaidPayments = (member?.payments || []).filter(p => !p.isPaid && p.status !== 'paid' && (Number(p.pendingAmount) > 0 || Number(p.amount) > Number(p.paidAmount || 0)));
    let targetPayment = null;
    if (unpaidPayments.length > 0) {
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      const upcoming = unpaidPayments
        .filter(p => p.dueDate && p.dueDate !== '—' && !isNaN(new Date(p.dueDate).getTime()) && new Date(p.dueDate) >= today)
        .sort((a, b) => new Date(a.dueDate) - new Date(b.dueDate));
      if (upcoming.length > 0) {
        targetPayment = upcoming[0];
      } else {
        const overdue = unpaidPayments
          .filter(p => p.dueDate && p.dueDate !== '—' && !isNaN(new Date(p.dueDate).getTime()))
          .sort((a, b) => new Date(b.dueDate) - new Date(a.dueDate));
        targetPayment = overdue.length > 0 ? overdue[0] : unpaidPayments[0];
      }
    }

    const paymentAmountVal = targetPayment
      ? (targetPayment.pendingAmount !== undefined ? targetPayment.pendingAmount : targetPayment.amount)
      : (conv.pendingAmount || member?.pendingAmount || 0);
    const formattedAmount = AppConfig.formatCurrency(paymentAmountVal);

    const formattedDueDate = targetPayment && targetPayment.dueDate && targetPayment.dueDate !== '—'
      ? this.formatDateDisplay(targetPayment.dueDate)
      : 'soon';

    const paymentDaysLeft = (() => {
      if (!targetPayment?.dueDate || targetPayment.dueDate === '—') return 0;
      try {
        const due = new Date(targetPayment.dueDate);
        if (isNaN(due.getTime())) return 0;
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        due.setHours(0, 0, 0, 0);
        return Math.max(0, Math.ceil((due - today) / (1000 * 60 * 60 * 24)));
      } catch (_) { return 0; }
    })();

    const paymentPlanName = targetPayment
      ? (targetPayment.notes || (Array.isArray(targetPayment.plans) && targetPayment.plans[0]?.planName) || (Array.isArray(targetPayment.coveredPlans) && targetPayment.coveredPlans[0]) || null)
      : null;

    const tplKey = template.templateKey || template.key;
    const isPaymentTemplate = (tplKey === 'upcoming_payment_reminder' || tplKey === 'payment_overdue_reminder');
    const finalPlanName = (isPaymentTemplate && paymentPlanName) ? paymentPlanName : resolvedPlanName;

    const todayDate = new Date();
    todayDate.setHours(0, 0, 0, 0);

    const overduePayments = (member?.payments || []).filter(p => {
      const isUnpaid = !p.isPaid && p.status !== 'paid' && (Number(p.pendingAmount) > 0 || Number(p.amount) > Number(p.paidAmount || 0));
      if (!isUnpaid || !p.dueDate || p.dueDate === '—' || isNaN(new Date(p.dueDate).getTime())) return false;
      const due = new Date(p.dueDate);
      due.setHours(0, 0, 0, 0);
      return due < todayDate;
    }).sort((a, b) => new Date(a.dueDate) - new Date(b.dueDate));

    const totalOverdue = overduePayments.reduce((sum, p) => {
      const remaining = p.pendingAmount !== undefined ? Number(p.pendingAmount) : (Number(p.amount) - Number(p.paidAmount || 0));
      return sum + Math.max(0, remaining);
    }, 0);

    const overdueBreakdown = overduePayments.length > 0
      ? overduePayments.map(p => {
          const remaining = p.pendingAmount !== undefined ? Number(p.pendingAmount) : (Number(p.amount) - Number(p.paidAmount || 0));
          const dueStr = this.formatDateDisplay(p.dueDate);
          return `• ${AppConfig.formatCurrency(remaining)} (Due: ${dueStr})`;
        }).join('\n')
      : '• No overdue installments on record.';

    const earliestOverdueDueDate = overduePayments.length > 0 ? overduePayments[0].dueDate : null;
    const overdueSince = earliestOverdueDueDate ? this.formatDateDisplay(earliestOverdueDueDate) : '—';

    const mergedVars = {
      name: conv.name,
      username: conv.name,
      'USER.first_name': conv.name ? conv.name.trim().split(' ')[0] : 'Member',
      'USER.name': conv.name,
      amount: formattedAmount,
      'PAYMENT.amount': formattedAmount,
      dueDate: formattedDueDate,
      'PAYMENT.due_date': formattedDueDate,
      'PAYMENT.days_left': paymentDaysLeft,
      'PAYMENT.pending_balance': (tplKey === 'payment_overdue_reminder')
        ? AppConfig.formatCurrency(totalOverdue)
        : AppConfig.formatCurrency(conv.pendingAmount || member?.pendingAmount || 0),
      'PAYMENT.overdue_breakdown': overdueBreakdown,
      'PAYMENT.overdue_since': overdueSince,
      expiryDate: formattedExpiry,
      'PLAN.expiry_date': formattedExpiry,
      'PLAN.days_left': daysLeft,
      plan: finalPlanName,
      'PLAN.name': finalPlanName,
      'GYM.name': AppConfig.getBrandName(),
      'GYM.payment_qr': AppConfig.gymPaymentQr || `upi://pay?pa=ironpulse@upi&pn=${encodeURIComponent(AppConfig.getBrandName() || 'Fitness Center')}`,
      brand: AppConfig.getBrandName(),
      brandName: AppConfig.getBrandName(),
      ...(customParameters || {})
    };

    const finalText = (customText && customText.trim()) ? customText.trim() : AppConfig.formatTemplate(template.content || template.text, mergedVars);

    const optimisticMsg = {
      id: `msg_tpl_${Date.now()}`,
      sender: 'staff',
      direction: 'OUTBOUND',
      isOutgoing: true,
      text: finalText,
      time: timeStr,
      timestamp: now.toISOString(),
      status: 'PENDING',
      templateKey: template.templateKey || template.key,
      mediaUrl: template.mediaUrl || (isPaymentTemplate ? (AppConfig.gymPaymentQr || '/alerts/media/med_upi_qr_code') : null),
      mediaType: template.mediaType || (template.mediaUrl || isPaymentTemplate ? 'IMAGE' : null),
      buttons: template.buttons || []
    };

    conv.messages = conv.messages || [];
    conv.messages.push(optimisticMsg);
    conv.lastMessage = finalText;
    conv.lastTime = timeStr;
    // Keep isHumanRequired active if already in human support session
    conv.unreadCount = 0;
    this.state.activeChatId = conv.id;

    this.notify();

    try {
      if (template.id && !String(template.id).startsWith('tpl_')) {
        const emitPayload = {
          templateId: template.id,
          userIds: [conv.id]
        };
        if (customParameters && Object.keys(customParameters).length > 0) {
          emitPayload.customParameters = customParameters;
        }
        if (customText && customText.trim()) {
          emitPayload.rawContent = customText.trim();
        }
        await api.post('/alerts/emit', emitPayload);
      } else {
        const replyRes = await api.post('/alerts/chat/reply', {
          userId: conv.id,
          channel: 'WHATSAPP',
          message: finalText
        });
        if (replyRes && replyRes.id) {
          optimisticMsg.id = replyRes.id;
          optimisticMsg.status = replyRes.status || 'PENDING';
        }
      }
    } catch (err) {
      console.warn('Template emit fallback:', err);
      optimisticMsg.status = 'FAILED';
    }

    ErrorMapper.notifySuccess(`WhatsApp template sent to ${conv.name}!`);
    this.closeTemplateModal();
    this.closeWhatsAppModal();
    this.notify();
  }

  async sendBulkTemplateMessage(template, customParameters = {}, recipients = null) {
    const targetRecipients = (recipients && recipients.length > 0)
      ? recipients
      : (this.state.whatsAppModal.recipients || []);
    const userIds = targetRecipients.map(r => r.id || r.userId).filter(Boolean);
    if (!userIds.length) {
      ErrorMapper.showErrorWindow('No recipients selected for campaign broadcast.', 'Validation Error');
      return;
    }

    const payload = {
      templateId: template.id,
      userIds,
      customParameters: (customParameters && Object.keys(customParameters).length > 0) ? customParameters : null
    };

    try {
      await api.post('/alerts/emit', payload);
      ErrorMapper.notifySuccess(`WhatsApp broadcast dispatched to ${userIds.length} members!`);
      this.closeWhatsAppModal();
      this.clearMemberSelection();
    } catch (err) {
      ErrorMapper.handle(err);
    }
  }
}

export const HomeStore = new HomeStoreClass();
