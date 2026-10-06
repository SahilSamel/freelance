/**
 * HomeStore - Centralized Reactive State Store for Home and its Sections
 * Strict Live Backend Integration without hardcoded data fallbacks.
 * Any API failures trigger the global top-center red error window.
 */

import { api } from '../../core/api/client.js';
import { AppConfig } from '../../core/config/app-config.js';
import { ErrorMapper } from '../../core/error/error-mapper.js';

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
      activeConversation: null,
      searchQuery: '',
      memberStatusTab: 'all', // 'all' | 'active' | 'renewals' | 'expired' | 'prospect'
      paymentStatusTab: 'all', // 'all' | 'paid' | 'upcoming' | 'overdue'
      selectedMemberIds: new Set(),
      selectedPaymentIds: new Set(),
      loadError: null,
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
      whatsAppModal: {
        isOpen: false,
        member: null,
        defaultTemplate: 'EXPIRY_REMINDER',
        paymentInfo: null
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
      this.notify();
    }
  }

  setSearchQuery(q) {
    this.state.searchQuery = q.trim().toLowerCase();
    this.notify();
  }

  setMemberStatusTab(tab) {
    this.state.memberStatusTab = tab;
    this.notify();
  }

  setPaymentStatusTab(tab) {
    this.state.paymentStatusTab = tab;
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
  }

  openWhatsAppModal(member, defaultTemplate = 'EXPIRY_REMINDER', paymentInfo = null) {
    this.state.whatsAppModal = {
      isOpen: true,
      member,
      defaultTemplate,
      paymentInfo
    };
    this.notify();
  }

  closeWhatsAppModal() {
    this.state.whatsAppModal = {
      isOpen: false,
      member: null,
      defaultTemplate: 'EXPIRY_REMINDER',
      paymentInfo: null
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
      let memberTotalPending = 0;
      let memberTotalAmount = 0;
      let memberTotalPaid = 0;
      let earliestDate = null;

      const userPlanMap = new Map();
      const unassignedInstallments = [];

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

        let payStatus = 'unpaid';
        let payStatusLabel = 'Unpaid';
        let payStatusVariant = 'unpaid';

        if (isPaid) {
          payStatus = 'paid';
          payStatusLabel = 'Paid';
          payStatusVariant = 'paid';
        } else if (dueDt) {
          const dueIso = p.dueDate.slice(0, 10);
          if (dueDt < today) {
            const overdueDays = Math.ceil((today.getTime() - dueDt.getTime()) / (1000 * 60 * 60 * 24));
            payStatus = 'overdue';
            if (overdueDays >= 30) {
              payStatusVariant = 'critical';
              const mVal = overdueDays / 30.4375;
              const roundedM = Math.round(mVal * 2) / 2;
              const mText = roundedM <= 1 ? '1 mo' : (roundedM >= 12 ? `${Math.round(overdueDays / 365.25 * 10) / 10} yrs` : `${roundedM} mos`);
              payStatusLabel = overdueDays >= 60 ? `Critical: ${mText} overdue ⚠️` : `Overdue by ${mText} ⚠️`;
            } else if (overdueDays >= 7) {
              payStatusVariant = 'overdue';
              const w = Math.round(overdueDays / 7);
              payStatusLabel = w <= 1 ? 'Overdue by 1 week' : `Overdue by ${w} weeks`;
            } else {
              payStatusVariant = 'overdue';
              payStatusLabel = overdueDays <= 1 ? 'Overdue by 1 day' : `Overdue by ${overdueDays} days`;
            }
          } else if (dueIso <= endOfMonthStr) {
            const upcomingDays = Math.ceil((dueDt.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
            payStatus = 'upcoming';
            payStatusVariant = 'upcoming';
            if (upcomingDays <= 0) {
              payStatusLabel = 'Due Today';
            } else if (upcomingDays === 1) {
              payStatusLabel = 'Payment in 1 day';
            } else if (upcomingDays < 7) {
              payStatusLabel = `Payment in ${upcomingDays} days`;
            } else if (upcomingDays < 28) {
              const w = Math.round(upcomingDays / 7);
              payStatusLabel = w <= 1 ? 'Payment in 1 week' : `Payment in ${w} weeks`;
            } else {
              const m = Math.round((upcomingDays / 30.4375) * 2) / 2;
              payStatusLabel = m <= 1 ? 'Payment in 1 month' : `Payment in ${m} months`;
            }
          } else {
            payStatus = 'unpaid';
            payStatusLabel = 'Unpaid';
            payStatusVariant = 'unpaid';
          }
        }

        const instStatus = isPaid ? 'PAID' : (payStatus === 'overdue' ? 'OVERDUE' : (payStatus === 'upcoming' ? 'UPCOMING' : 'UNPAID'));
        const cleanNote = p.notes 
          ? p.notes.replace(/\s*\((Overdue|Paid|Upcoming|Pending)\)/gi, '').trim()
          : (p.plans && p.plans[0] ? p.plans[0].planName : 'Installment');

        const method = p.paymentMethod || (p.transactionId && p.transactionId.toLowerCase().includes('cash') ? 'CASH' : 'UPI');

        const instObj = {
          id: p.id,
          amount: amt,
          amountFormatted: AppConfig.formatCurrency(amt),
          paidAmount: paidAmt,
          dueDate: p.dueDate || '—',
          paidDate: p.paidDate || '—',
          date: p.paidDate || p.dueDate || '—',
          txnId: p.transactionId || (isPaid ? 'TXN_CASH' : '—'),
          note: cleanNote,
          status: instStatus,
          payStatus,
          statusLabel: payStatusLabel,
          statusVariant: payStatusVariant,
          isPaid,
          paymentMethod: method
        };

        processedPayments.push({
          id: p.id,
          memberId: m.userId,
          memberName: `${m.firstName || ''} ${m.lastName || ''}`.trim() || m.username,
          phone: m.phoneNumber ? `+${m.phoneNumber.countryCode || AppConfig.getCountryCode()} ${m.phoneNumber.number}` : '—',
          rawPhone: m.phoneNumber ? m.phoneNumber.number : '',
          amount: amt,
          amountFormatted: AppConfig.formatCurrency(amt),
          dueDate: p.dueDate || '—',
          paidDate: p.paidDate || '—',
          paymentMethod: method,
          txnId: p.transactionId || (isPaid ? 'TXN_CASH' : '—'),
          notes: cleanNote,
          status: payStatus,
          statusLabel: payStatusLabel,
          statusVariant: payStatusVariant,
          memberStatus: 'active'
        });

        if (p.plans && Array.isArray(p.plans) && p.plans.length > 0) {
          for (const pl of p.plans) {
            const planKey = pl.id || `${pl.planId}_${pl.startDate}`;
            if (!userPlanMap.has(planKey)) {
              userPlanMap.set(planKey, { userPlan: pl, installments: [] });
            }
            userPlanMap.get(planKey).installments.push(instObj);
          }
        } else {
          unassignedInstallments.push(instObj);
        }
      }

      const planBatches = [];
      for (const [key, entry] of userPlanMap.entries()) {
        const pl = entry.userPlan;
        const insts = entry.installments;
        const batchTotal = insts.reduce((sum, item) => sum + item.amount, 0);
        const batchPaid = insts.reduce((sum, item) => sum + (item.isPaid ? item.paidAmount : (item.paidAmount || 0)), 0);
        const batchPending = Math.max(0, batchTotal - batchPaid);

        if (pl.startDate) {
          const dt = new Date(pl.startDate);
          if (!earliestDate || dt < earliestDate) earliestDate = dt;
        }

        const periodStr = (pl.startDate && pl.endDate)
          ? `${this.formatDateDisplay(pl.startDate)} – ${this.formatDateDisplay(pl.endDate)}`
          : (pl.startDate ? `Started ${this.formatDateDisplay(pl.startDate)}` : 'Membership Plan');

        planBatches.push({
          id: `batch-${pl.id || key}`,
          userPlanId: pl.id,
          planId: pl.planId,
          period: periodStr,
          startDate: pl.startDate,
          endDate: pl.endDate,
          plans: pl.planName || 'Membership Plan',
          totalAmount: batchTotal,
          totalAmountFormatted: AppConfig.formatCurrency(batchTotal),
          pendingAmount: batchPending,
          pendingAmountFormatted: batchPending > 0 ? `Pending: ${AppConfig.formatCurrency(batchPending)}` : '',
          installments: insts
        });
      }

      if (unassignedInstallments.length > 0) {
        const unassignedTotal = unassignedInstallments.reduce((sum, item) => sum + item.amount, 0);
        const unassignedPaid = unassignedInstallments.reduce((sum, item) => sum + item.paidAmount, 0);
        const unassignedPending = Math.max(0, unassignedTotal - unassignedPaid);
        planBatches.push({
          id: `batch-unassigned-${m.userId}`,
          period: 'Ad-hoc / Other Payments',
          startDate: null,
          endDate: null,
          plans: 'Ad-hoc Payments',
          totalAmount: unassignedTotal,
          totalAmountFormatted: AppConfig.formatCurrency(unassignedTotal),
          pendingAmount: unassignedPending,
          pendingAmountFormatted: unassignedPending > 0 ? `Pending: ${AppConfig.formatCurrency(unassignedPending)}` : '',
          installments: unassignedInstallments
        });
      }

      planBatches.sort((a, b) => {
        const dateA = a.endDate ? new Date(a.endDate).getTime() : (a.startDate ? new Date(a.startDate).getTime() : 0);
        const dateB = b.endDate ? new Date(b.endDate).getTime() : (b.startDate ? new Date(b.startDate).getTime() : 0);
        return dateB - dateA;
      });

      let maxEndDate = null;
      let latestExpiryDate = null;
      if (planBatches.length > 0 && planBatches[0].endDate) {
        latestExpiryDate = planBatches[0].endDate;
        maxEndDate = new Date(planBatches[0].endDate);
        maxEndDate.setHours(0, 0, 0, 0);
      } else {
        for (const b of planBatches) {
          if (b.endDate) {
            const ed = new Date(b.endDate);
            ed.setHours(0, 0, 0, 0);
            if (!maxEndDate || ed > maxEndDate) {
              maxEndDate = ed;
              latestExpiryDate = b.endDate;
            }
          }
        }
      }

      let memberStatus = 'expired';
      let memberStatusLabel = 'Expired';
      let memberStatusVariant = 'expired';

      if (maxEndDate) {
        const expIso = toIso(maxEndDate);
        const daysDiff = Math.ceil((maxEndDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));

        if (maxEndDate < today) {
          memberStatus = 'expired';
          memberStatusLabel = 'Expired';
          memberStatusVariant = 'expired';
        } else if (expIso <= endOfMonthStr) {
          memberStatus = 'renewals';
          memberStatusVariant = 'renewals';
          if (daysDiff <= 0) {
            memberStatusLabel = 'Renew Today';
          } else if (daysDiff === 1) {
            memberStatusLabel = 'Renew in 1 day';
          } else if (daysDiff < 7) {
            memberStatusLabel = `Renew in ${daysDiff} days`;
          } else if (daysDiff < 28) {
            const w = Math.round(daysDiff / 7);
            memberStatusLabel = w <= 1 ? 'Renew in 1 week' : `Renew in ${w} weeks`;
          } else {
            const m = Math.round((daysDiff / 30.4375) * 2) / 2;
            memberStatusLabel = m <= 1 ? 'Renew in 1 month' : `Renew in ${m} months`;
          }
        } else {
          memberStatus = 'active';
          memberStatusLabel = 'Active';
          memberStatusVariant = 'active';
        }
      } else {
        memberStatus = 'expired';
        memberStatusLabel = 'No Plan';
        memberStatusVariant = 'expired';
      }

      const planNamesList = planBatches.filter(b => b.plans && b.plans !== 'Ad-hoc Payments').map(b => b.plans);
      const displayPlanNames = planNamesList.length > 0 ? planNamesList.join(', ') : 'No Plan';

      processedPayments.forEach(p => {
        if (p.memberId === m.userId) p.memberStatus = memberStatus;
      });

      processedMembers.push({
        id: m.userId,
        userId: m.userId,
        name: `${m.firstName || ''} ${m.lastName || ''}`.trim() || m.username,
        phone: m.phoneNumber ? `+${m.phoneNumber.countryCode || AppConfig.getCountryCode()} ${m.phoneNumber.number}` : '—',
        rawPhone: m.phoneNumber ? m.phoneNumber.number : '',
        plans: displayPlanNames,
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

  async loadInitialData() {
    this.state.loading.members = true;
    this.state.loading.plans = true;
    this.state.loading.gyms = true;
    this.state.loadError = null;
    this.notify();

    let hasError = false;

    // 1. Load Gyms from live backend
    try {
      const gymsRes = await api.get('/tenants/gyms');
      if (Array.isArray(gymsRes)) {
        this.state.gyms = gymsRes;
        if (gymsRes.length > 0) {
          this.state.currentGymId = gymsRes[0].id || gymsRes[0].gymId;
        }
      }
    } catch (err) {
      hasError = true;
      this.state.loadError = err.message || 'Failed to load gyms';
      ErrorMapper.showErrorWindow(err.message || 'Failed to connect to gym backend', 'API Connection Error', err.status || 'NET_ERR');
    } finally {
      this.state.loading.gyms = false;
    }

    // 2. Load Plans from live backend
    try {
      const plansRes = await api.get('/plan/list');
      let rawPlans = [];
      if (Array.isArray(plansRes)) {
        rawPlans = plansRes;
      } else if (plansRes && Array.isArray(plansRes.plans)) {
        rawPlans = plansRes.plans;
      }
      this.state.availablePlans = rawPlans.map(p => ({
        id: p.id,
        name: p.name,
        price: Number(p.defaultPrice) || 0,
        defaultPrice: Number(p.defaultPrice) || 0,
        durationDays: p.defaultDuration || 30,
        durationMonths: Math.max(1, Math.round((p.defaultDuration || 30) / 30)),
        description: p.description || ''
      }));
    } catch (err) {
      if (!hasError) {
        hasError = true;
        this.state.loadError = err.message || 'Failed to load plans';
        ErrorMapper.showErrorWindow(err.message || 'Failed to load membership plans', 'API Error', err.status || '500');
      }
    } finally {
      this.state.loading.plans = false;
    }

    // 3. Load Members from live backend
    try {
      const membersRes = await api.get('/members/list');
      let rawList = [];
      if (Array.isArray(membersRes)) {
        rawList = membersRes;
      } else if (membersRes && Array.isArray(membersRes.members)) {
        rawList = membersRes.members;
      }
      this.state.rawMembers = rawList;
      const { processedMembers, processedPayments } = this.processRawMembers(rawList);
      this.state.members = processedMembers;
      this.state.payments = processedPayments;
    } catch (err) {
      if (!hasError) {
        hasError = true;
        this.state.loadError = err.message || 'Failed to load members';
        ErrorMapper.showErrorWindow(err.message || 'Failed to load member records', 'API Error', err.status || '500');
      }
      this.state.rawMembers = [];
      this.state.members = [];
      this.state.payments = [];
    } finally {
      this.state.loading.members = false;
    }

    // 4. Load Conversations from live backend
    try {
      const convRes = await api.get('/alerts/chat/conversations');
      if (Array.isArray(convRes)) {
        this.state.conversations = convRes;
      }
    } catch {
      this.state.conversations = [];
    }

    this.notify();
  }

  getFilteredMembers() {
    let result = [...this.state.members];
    const { searchQuery, memberStatusTab } = this.state;

    if (memberStatusTab === 'active') {
      result = result.filter(m => m.status === 'active');
    } else if (memberStatusTab === 'renewals') {
      result = result.filter(m => m.status === 'renewals');
    } else if (memberStatusTab === 'expired') {
      result = result.filter(m => m.status === 'expired');
    } else if (memberStatusTab === 'prospect') {
      result = result.filter(m => m.status === 'prospect' || (!m.planBatches || m.planBatches.length === 0));
    }

    if (searchQuery) {
      result = result.filter(m => 
        (m.name && m.name.toLowerCase().includes(searchQuery)) ||
        (m.phone && m.phone.toLowerCase().includes(searchQuery)) ||
        (m.plans && m.plans.toLowerCase().includes(searchQuery))
      );
    }

    return result;
  }

  getFilteredPayments() {
    let result = [...this.state.payments];
    const { searchQuery, paymentStatusTab } = this.state;

    if (paymentStatusTab === 'paid') {
      result = result.filter(p => p.status === 'paid');
    } else if (paymentStatusTab === 'upcoming') {
      result = result.filter(p => p.status === 'upcoming');
    } else if (paymentStatusTab === 'overdue') {
      result = result.filter(p => p.status === 'overdue');
    }

    if (searchQuery) {
      result = result.filter(p =>
        (p.memberName && p.memberName.toLowerCase().includes(searchQuery)) ||
        (p.phone && p.phone.toLowerCase().includes(searchQuery)) ||
        (p.txnId && p.txnId.toLowerCase().includes(searchQuery)) ||
        (p.notes && p.notes.toLowerCase().includes(searchQuery))
      );
    }

    return result;
  }

  getKPIs() {
    const members = this.state.members;
    const payments = this.state.payments;

    const activeMembersCount = members.filter(m => m.status === 'active').length;
    const renewalsThisMonthCount = members.filter(m => m.status === 'renewals').length;
    const expiredCount = members.filter(m => m.status === 'expired').length;

    let overdueTotal = 0;
    let dueThisMonthTotal = 0;

    for (const p of payments) {
      if (p.status === 'overdue') {
        overdueTotal += p.amount;
      } else if (p.status === 'upcoming') {
        dueThisMonthTotal += p.amount;
      }
    }

    const totalPendingThisMonth = overdueTotal + dueThisMonthTotal;

    return {
      totalMembers: members.length,
      activeMembersCount,
      renewalsThisMonthCount,
      expiredCount,
      overdueTotal,
      dueThisMonthTotal,
      totalPendingThisMonth,
      totalPlansCount: this.state.availablePlans.length,
      enrolledMembersCount: members.filter(m => m.status === 'active' || m.status === 'renewals').length
    };
  }

  async ensurePlanExists({ name, defaultPrice, defaultDurationDays }) {
    const trimmedName = (name || 'Custom Plan').trim();

    // 1. Check local state cache
    let existing = this.state.availablePlans.find(
      p => p.name && p.name.toLowerCase() === trimmedName.toLowerCase()
    );
    if (existing) {
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
          description: p.description || ''
        }));
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
        description: created.description || ''
      };
      this.state.availablePlans.push(newPlan);
      this.notify();
      return created.id;
    }

    throw new Error(`Failed to create custom plan "${trimmedName}"`);
  }

  async addMember({ firstName, lastName, phone, planId, customPlanData, startDate, schedule }) {
    let finalPlanId = planId;
    let planPrice = 0;
    let durationDays = 30;

    if (planId === '__CUSTOM__') {
      const customName = (customPlanData?.name || 'Custom Plan').trim();
      const customPrice = Number(customPlanData?.price) || 0;
      const customUnit = customPlanData?.unit || 'months';
      const customVal = Number(customPlanData?.value) || 1;

      durationDays = customUnit === 'months' ? customVal * 30 : customVal;
      planPrice = customPrice;

      // Lazy Seed: checks if plan exists or creates it via POST /plan/new
      finalPlanId = await this.ensurePlanExists({
        name: customName,
        defaultPrice: customPrice,
        defaultDurationDays: durationDays
      });
    } else {
      const foundPlan = this.state.availablePlans.find(p => p.id === planId);
      if (foundPlan) {
        planPrice = foundPlan.price || 0;
        durationDays = foundPlan.durationDays || (foundPlan.durationMonths ? foundPlan.durationMonths * 30 : 30);
      }
    }

    // Compute Dates
    const start = new Date(startDate || new Date());
    const end = new Date(start);
    end.setDate(end.getDate() + durationDays);

    const pad = (n) => String(n).padStart(2, '0');
    const toIso = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

    const startDateIso = toIso(start);
    const endDateIso = toIso(end);
    const todayIso = toIso(new Date());

    // Format Phone
    const rawNumber = String(phone).replace(/\D/g, '');
    const countryCode = AppConfig.getCountryCode() || '91';
    const cleanNumber = rawNumber.length > 10 ? rawNumber.slice(-10) : rawNumber;

    // Build Payment Schedule
    const payments = [];
    if (schedule === 'full_paid') {
      payments.push({
        amount: planPrice,
        paidAmount: planPrice,
        dueDate: todayIso,
        paidDate: todayIso,
        paymentMethod: 'UPI',
        transactionId: `TXN_${Date.now()}`,
        notes: 'Full Payment - Paid Now'
      });
    } else if (schedule === 'full_pending') {
      payments.push({
        amount: planPrice,
        paidAmount: 0,
        dueDate: todayIso,
        paidDate: null,
        paymentMethod: 'NONE',
        transactionId: null,
        notes: 'Full Payment - Pending'
      });
    } else if (schedule === '2_installments') {
      const half = Math.round(planPrice / 2);
      const secondHalf = planPrice - half;
      const nextMonth = new Date(start);
      nextMonth.setMonth(nextMonth.getMonth() + 1);

      payments.push({
        amount: half,
        paidAmount: half,
        dueDate: todayIso,
        paidDate: todayIso,
        paymentMethod: 'UPI',
        transactionId: `TXN_${Date.now()}_1`,
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
      const part = Math.round(planPrice / 3);
      const lastPart = planPrice - (part * 2);
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
        transactionId: `TXN_${Date.now()}_1`,
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

    const payload = {
      firstName,
      lastName,
      phoneNumber: {
        countryCode,
        number: cleanNumber
      },
      plans: [
        {
          planId: finalPlanId,
          startDate: startDateIso,
          endDate: endDateIso
        }
      ],
      payments
    };

    const res = await api.post('/members/new', payload);
    ErrorMapper.notifySuccess(`Member "${firstName} ${lastName}" registered successfully!`);
    await this.loadInitialData();
    this.closeAddMemberModal();
    return res;
  }
}

export const HomeStore = new HomeStoreClass();
