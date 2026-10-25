/**
 * <fl-member-drawer> Native Web Component
 * Member 360 Profile & Modular Editing:
 * - Read mode: Pixel-faithful layout of member details, stats, itemized plans, and payments.
 *   Always stays clean, spacious, and readable (matching design specs).
 * - Edit Profile: Clicking [Edit] at the header opens a focused modal for First/Last Name and Phone.
 * - Edit Schedule: Clicking [Edit] on any payment-plan card opens a popup modal styled exactly
 *   like the Create Member / Renew Member flow (Covered Plans + Installments + Totals).
 * - Add Schedule: Allows appending a past or new membership & payment schedule.
 * - Locked PAID records: Already paid installments remain tamper-proof and locked.
 */

import { HomeStore } from '../home-store.js';
import { Icons } from '../../../assets/icons/icons.js';
import { AppConfig } from '../../../core/config/app-config.js';
import '../../../core/components/feedback/fl-modal.js';

export class FlMemberDrawer extends HTMLElement {
  constructor() {
    super();
    this.openAccordions = new Set();
    this.wasOpen = false;
    this.activeMemberId = null;

    // Modular edit sub-modal states
    this.isEditingProfile = false;
    this.profileDraft = { firstName: '', lastName: '', phone: '' };

    this.editingSchedule = null; // { groupIndex: number, plans: [], payments: [] } or null

    // Cached payment groups for the active member
    this.paymentGroups = [];
  }

  connectedCallback() {
    this.unsubscribe = HomeStore.subscribe(() => this.render());
    this.render();
  }

  disconnectedCallback() {
    if (this.unsubscribe) this.unsubscribe();
  }

  initPaymentGroups(member) {
    const pad = (n) => String(n).padStart(2, '0');
    const today = new Date();
    const todayIso = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`;

    const { availablePlans } = HomeStore.getState();
    const payments = member.payments || [];
    const subscriptions = member.subscriptions || [];

    const groups = [];
    const handledSubIds = new Set();
    const usedPaymentIds = new Set();

    if (subscriptions.length > 0) {
      subscriptions.forEach((sub, idx) => {
        if (handledSubIds.has(sub.id)) return;

        // Find payments directly tied to this subscription
        let matchedPayments = payments.filter(p => 
          (p.userPlanIds && p.userPlanIds.includes(sub.id)) ||
          (p.plans && p.plans.some(pl => pl.id === sub.id))
        );

        // Fallback matching by plan name & date proximity
        if (matchedPayments.length === 0 && payments.length > 0) {
          const subStart = sub.startDate ? new Date(sub.startDate).getTime() : 0;
          const subEnd = sub.endDate ? new Date(sub.endDate).getTime() : Infinity;
          matchedPayments = payments.filter(p => {
            if (usedPaymentIds.has(p.id)) return false;
            const pDate = p.paidDate && p.paidDate !== '—' ? new Date(p.paidDate).getTime() : (p.dueDate && p.dueDate !== '—' ? new Date(p.dueDate).getTime() : 0);
            const matchesPlan = p.coveredPlans?.includes(sub.planName) || p.notes?.includes(sub.planName) || p.coveredPlanNames?.includes(sub.planName);
            if (!matchesPlan) return false;
            return pDate >= (subStart - 30 * 86400000) && pDate <= (subEnd + 30 * 86400000);
          });
        }

        // Bundled subscriptions that share common payments
        const bundledSubs = [sub];
        handledSubIds.add(sub.id);

        if (matchedPayments.length > 0) {
          matchedPayments.forEach(p => usedPaymentIds.add(p.id));

          subscriptions.forEach(otherSub => {
            if (!handledSubIds.has(otherSub.id)) {
              const otherPayments = payments.filter(p => 
                (p.userPlanIds && p.userPlanIds.includes(otherSub.id)) ||
                (p.plans && p.plans.some(pl => pl.id === otherSub.id))
              );
              const sharesPayment = otherPayments.some(op => matchedPayments.some(mp => mp.id === op.id));
              if (sharesPayment) {
                bundledSubs.push(otherSub);
                handledSubIds.add(otherSub.id);
                otherPayments.forEach(op => {
                  if (!matchedPayments.some(mp => mp.id === op.id)) {
                    matchedPayments.push(op);
                    usedPaymentIds.add(op.id);
                  }
                });
              }
            }
          });
        }

        const groupPlans = bundledSubs.map((s, sIdx) => {
          const foundPlan = (availablePlans || []).find(p => p.id === s.planId);
          return {
            id: s.id || `plan-${Date.now()}-${idx}-${sIdx}`,
            userPlanId: s.id,
            planId: s.planId,
            planName: s.planName || foundPlan?.name || 'Gym Service',
            startDate: s.startDate || todayIso,
            endDate: s.endDate || todayIso,
            fee: foundPlan?.price || s.fee || 0
          };
        });

        const groupPayments = matchedPayments.map((p, pIdx) => {
          const isPaid = p.isPaid || p.status === 'paid' || (Number(p.paidAmount) > 0 && Number(p.paidAmount) >= Number(p.amount));
          return {
            id: p.id || `pay-${Date.now()}-${idx}-${pIdx}`,
            amount: Number(p.amount) || 0,
            paidAmount: Number(p.paidAmount) || (isPaid ? Number(p.amount) : 0),
            dueDate: p.dueDate || p.paidDate || todayIso,
            paidDate: p.paidDate || null,
            paymentMethod: p.paymentMethod || 'UPI',
            transactionId: p.transactionId || null,
            notes: p.notes || p.note || '',
            isPaid: Boolean(isPaid)
          };
        });

        // Compute group title and formatted range across all bundled subscriptions
        const startDates = bundledSubs.map(s => s.startDate).filter(Boolean).sort();
        const endDates = bundledSubs.map(s => s.endDate).filter(Boolean).sort();
        const earliestStart = startDates[0] || sub.startDate;
        const latestEnd = endDates[endDates.length - 1] || sub.endDate;

        const planNames = [...new Set(groupPlans.map(p => p.planName))].join(', ');
        const dateRangeStr = (earliestStart && latestEnd)
          ? `${HomeStore.formatDateDisplay(earliestStart)} – ${HomeStore.formatDateDisplay(latestEnd)}`
          : (earliestStart ? `Started ${HomeStore.formatDateDisplay(earliestStart)}` : 'Active Subscription');

        const totalAmt = groupPayments.length > 0
          ? groupPayments.reduce((acc, curr) => acc + (curr.amount || 0), 0)
          : groupPlans.reduce((acc, curr) => acc + (curr.fee || 0), 0);

        groups.push({
          id: `group-card-${idx + 1}`,
          startDate: earliestStart,
          dateRange: dateRangeStr,
          amountFormatted: AppConfig.formatCurrency(totalAmt),
          planName: planNames,
          plans: groupPlans,
          payments: groupPayments
        });
      });

      // Sort latest first
      groups.sort((a, b) => {
        const da = a.startDate ? new Date(a.startDate).getTime() : 0;
        const db = b.startDate ? new Date(b.startDate).getTime() : 0;
        return db - da;
      });
    }

    // Leftover payments
    const leftoverPayments = payments.filter(p => !usedPaymentIds.has(p.id));
    if (leftoverPayments.length > 0) {
      const invoiceGroups = new Map();
      leftoverPayments.forEach(p => {
        const key = p.coveredPlanNames || p.notes || 'Additional Payment';
        if (!invoiceGroups.has(key)) invoiceGroups.set(key, []);
        invoiceGroups.get(key).push(p);
      });

      invoiceGroups.forEach((instList, key) => {
        const totalAmt = instList.reduce((acc, curr) => acc + (curr.amount || 0), 0);
        const mappedInst = instList.map((p, pIdx) => {
          const isPaid = p.isPaid || p.status === 'paid' || (Number(p.paidAmount) > 0 && Number(p.paidAmount) >= Number(p.amount));
          return {
            id: p.id || `leftover-pay-${Date.now()}-${pIdx}`,
            amount: Number(p.amount) || 0,
            paidAmount: Number(p.paidAmount) || (isPaid ? Number(p.amount) : 0),
            dueDate: p.dueDate || p.paidDate || todayIso,
            paidDate: p.paidDate || null,
            paymentMethod: p.paymentMethod || 'UPI',
            transactionId: p.transactionId || null,
            notes: p.notes || p.note || '',
            isPaid: Boolean(isPaid)
          };
        });

        groups.push({
          id: `group-card-extra-${groups.length + 1}`,
          startDate: instList[0]?.paidDate || instList[0]?.dueDate,
          dateRange: instList[0]?.paidDate || instList[0]?.dueDate || 'Payment Record',
          amountFormatted: AppConfig.formatCurrency(totalAmt),
          planName: key,
          plans: [],
          payments: mappedInst
        });
      });
    }

    this.paymentGroups = groups;
  }

  initProfileDraft(member) {
    const rawRecord = member.rawRecord || {};
    const firstName = rawRecord.firstName || (member.name ? member.name.trim().split(' ')[0] : '');
    const lastName = rawRecord.lastName || (member.name ? member.name.trim().split(' ').slice(1).join(' ') : '');
    const phone = member.rawPhone || (member.phone ? member.phone.replace(/\D/g, '').slice(-10) : '');
    this.profileDraft = { firstName, lastName, phone };
  }

  render() {
    const { memberDrawer } = HomeStore.getState();
    const { isOpen, member } = memberDrawer;

    if (!isOpen || !member) {
      this.wasOpen = false;
      this.activeMemberId = null;
      this.isEditingProfile = false;
      this.editingSchedule = null;
      this.openAccordions.clear();
      this.innerHTML = '';
      return;
    }

    if (this.activeMemberId !== member.id) {
      this.activeMemberId = member.id;
      this.isEditingProfile = false;
      this.editingSchedule = null;
      this.openAccordions.clear();
      this.initPaymentGroups(member);
      this.initProfileDraft(member);
    }

    const isFirstOpen = !this.wasOpen;
    this.wasOpen = true;

    this.renderReadMode(member, isFirstOpen);
    this.bindEvents();
  }

  renderReadMode(member, isFirstOpen) {
    const { availablePlans } = HomeStore.getState();
    const scheduleCards = this.paymentGroups || [];

    const isProspect = (member.status || '').toLowerCase() === 'prospect' || (!member.subscriptions || member.subscriptions.length === 0);
    const isExpired = !isProspect && ((member.status || '').toLowerCase() === 'expired' || (member.statusLabel || '').toLowerCase() === 'expired');

    let badgeText = 'Active';
    let badgeBg = '#ecfdf5';
    let badgeColor = '#16a34a';
    let badgeBorder = '#a7f3d0';

    if (isProspect) {
      badgeText = 'Prospect';
      badgeBg = '#eff6ff';
      badgeColor = '#1d4ed8';
      badgeBorder = '#bfdbfe';
    } else if (isExpired) {
      badgeText = 'Expired';
      badgeBg = '#f1f5f9';
      badgeColor = '#475569';
      badgeBorder = '#cbd5e1';
    }

    this.innerHTML = `
      <div id="fl-drawer-backdrop" style="
        position: fixed;
        top: 0; left: 0; right: 0; bottom: 0;
        background: rgba(15, 23, 42, 0.45);
        backdrop-filter: blur(6px);
        -webkit-backdrop-filter: blur(6px);
        z-index: var(--fl-z-drawer, 800);
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 20px;
        animation: ${isFirstOpen ? 'flFadeIn 0.2s ease-out' : 'none'};
      ">
        <div id="fl-drawer-panel" style="
          background: #ffffff;
          border-radius: 16px;
          width: 100%;
          max-width: 580px;
          max-height: 88vh;
          box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
          display: flex;
          flex-direction: column;
          overflow: hidden;
          animation: ${isFirstOpen ? 'flModalFadeIn 0.2s cubic-bezier(0.16, 1, 0.3, 1)' : 'none'};
        ">
        
          <div style="flex: 1; overflow-y: auto; padding: 24px 28px;">
            <!-- Header Row -->
            <div style="display: flex; justify-content: space-between; align-items: flex-start;">
              <div>
                <div style="display: flex; align-items: center; gap: 10px;">
                  <h2 style="margin: 0; font-size: 22px; font-weight: 800; color: #0f172a; line-height: 1.1;">
                    ${member.name}
                  </h2>
                  <span style="
                    background: ${badgeBg};
                    color: ${badgeColor};
                    border: 1px solid ${badgeBorder};
                    border-radius: 9999px;
                    padding: 2px 10px;
                    font-size: 11.5px;
                    font-weight: 600;
                  ">
                    ${badgeText}
                  </span>
                </div>
                <div style="margin-top: 4px; font-size: 13.5px; color: #64748b; font-weight: 500;">
                  ${member.phone}
                </div>
              </div>

              <!-- Top Right Actions: Edit Profile & Close -->
              <div style="display: flex; align-items: center; gap: 8px;">
                <button type="button" id="fl-drawer-edit-btn" class="fl-btn fl-btn-secondary" style="
                  padding: 6px 14px;
                  font-size: 12.5px;
                ">
                  ${Icons.edit || '✎'}
                  <span>Edit Contact</span>
                </button>
                <button type="button" class="fl-drawer-close-btn" style="
                  border: none;
                  background: transparent;
                  color: #64748b;
                  cursor: pointer;
                  padding: 4px;
                  display: flex;
                  align-items: center;
                  justify-content: center;
                  border-radius: 6px;
                " title="Close">
                  ${Icons.close || '✕'}
                </button>
              </div>
            </div>

            <div style="border-bottom: 1px solid #f1f5f9; margin: 20px 0;"></div>

            <!-- 3 Stat Summary Columns -->
            <div style="display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 16px; margin-bottom: 20px;">
              <div>
                <div style="font-size: 11.5px; font-weight: 600; color: #64748b; margin-bottom: 4px;">Member Since</div>
                <div style="font-size: 14.5px; font-weight: 700; color: #0f172a;">
                  ${member.memberSince ? HomeStore.formatDateDisplay(member.memberSince) : 'Today'}
                </div>
              </div>

              <div>
                <div style="font-size: 11.5px; font-weight: 600; color: #64748b; margin-bottom: 4px;">Lifetime Paid</div>
                <div style="font-size: 14.5px; font-weight: 700; color: #16a34a;">
                  ${AppConfig.formatCurrency(member.totalSpent || 0)}
                </div>
              </div>

              <div>
                <div style="font-size: 11.5px; font-weight: 600; color: #64748b; margin-bottom: 4px;">Pending Dues</div>
                <div style="font-size: 14.5px; font-weight: 700; color: ${(member.pendingAmount || 0) > 0 ? '#dc2626' : '#0f172a'};">
                  ${AppConfig.formatCurrency(member.pendingAmount || 0)}
                </div>
              </div>
            </div>

            <div style="border-bottom: 1px solid #f1f5f9; margin-bottom: 20px;"></div>

            <!-- Section Title -->
            <div style="font-size: 11.5px; font-weight: 700; color: #64748b; letter-spacing: 0.5px; text-transform: uppercase; margin-bottom: 14px;">
              MEMBERSHIP PLANS & PAYMENT SCHEDULES
            </div>

            <!-- Schedule / Plan Cards -->
            <div style="display: flex; flex-direction: column; gap: 14px;">
              ${scheduleCards.length === 0 ? `
                <div style="border: 1px dashed #cbd5e1; border-radius: 12px; padding: 24px; text-align: center; color: #64748b;">
                  <div style="font-size: 14px; font-weight: 600;">No plans or payment schedules yet</div>
                  <div style="font-size: 12.5px; margin-top: 4px;">Click "Renew Plan" below to assign a plan.</div>
                </div>
              ` : scheduleCards.map((card, idx) => {
                const isOpen = this.openAccordions.has(card.id);
                const installments = card.payments || [];
                const paidPayments = installments.filter(p => p.isPaid || p.status === 'paid' || (Number(p.paidAmount) > 0));
                const latestPaidPayment = paidPayments.length > 0 ? paidPayments[paidPayments.length - 1] : null;

                return `
                  <div style="
                    border: 1px solid #e2e8f0;
                    border-radius: 12px;
                    background: #ffffff;
                    padding: 16px 18px;
                    box-shadow: var(--fl-shadow-xs, 0 1px 3px rgba(0,0,0,0.04));
                  ">
                    <!-- Plans List with dates on the right -->
                    <div style="display: flex; flex-direction: column; gap: 8px;">
                      ${(card.plans && card.plans.length > 0) ? card.plans.map(p => {
                        const pStart = p.startDate ? HomeStore.formatDateDisplay(p.startDate) : '—';
                        const pEnd = p.endDate ? HomeStore.formatDateDisplay(p.endDate) : '—';
                        const pRange = (p.startDate && p.endDate) ? `${pStart} – ${pEnd}` : (p.startDate ? `From ${pStart}` : '');
                        return `
                          <div style="display: flex; justify-content: space-between; align-items: baseline; gap: 12px;">
                            <span style="font-size: 15px; font-weight: 700; color: #0f172a; line-height: 1.2;">
                              ${p.planName}
                            </span>
                            <span style="font-size: 13px; color: #64748b; font-weight: 500; white-space: nowrap;">
                              ${pRange}
                            </span>
                          </div>
                        `;
                      }).join('') : `
                        <div style="display: flex; justify-content: space-between; align-items: baseline; gap: 12px;">
                          <span style="font-size: 15px; font-weight: 700; color: #0f172a; line-height: 1.2;">
                            ${card.planName}
                          </span>
                          <span style="font-size: 13px; color: #64748b; font-weight: 500; white-space: nowrap;">
                            ${card.dateRange}
                          </span>
                        </div>
                      `}
                    </div>

                    <!-- Bottom Action Bar: Installments on Left | Send Receipt & Circular Edit on Right -->
                    <div style="margin-top: 14px; display: flex; justify-content: space-between; align-items: center; gap: 8px;">
                      ${installments.length > 0 ? `
                        <button 
                          type="button" 
                          class="fl-accordion-toggle-btn"
                          data-card-id="${card.id}"
                          style="
                            height: 32px;
                            box-sizing: border-box;
                            background: #ffffff;
                            border: 1px solid #e2e8f0;
                            border-radius: 6px;
                            padding: 0 10px;
                            font-size: 12px;
                            font-weight: 600;
                            color: #334155;
                            cursor: pointer;
                            display: inline-flex;
                            align-items: center;
                            gap: 6px;
                            transition: all 0.15s ease;
                            line-height: 1;
                          "
                          onmouseover="this.style.background='#f8fafc'; this.style.borderColor='#cbd5e1';"
                          onmouseout="this.style.background='#ffffff'; this.style.borderColor='#e2e8f0';"
                        >
                          <span>${installments.length} ${installments.length === 1 ? 'Installment' : 'Installments'}</span>
                          <svg class="fl-accordion-arrow" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#64748b" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" style="transition: transform 0.2s ease; transform: ${isOpen ? 'rotate(180deg)' : 'rotate(0deg)'};">
                            <polyline points="6 9 12 15 18 9"></polyline>
                          </svg>
                        </button>
                      ` : `<div></div>`}

                      <div style="display: flex; align-items: center; gap: 8px;">
                        ${latestPaidPayment ? `
                          <button 
                            type="button" 
                            class="fl-btn fl-btn-whatsapp fl-drawer-send-receipt-btn" 
                            data-payment-id="${latestPaidPayment.id}" 
                            style="
                              height: 32px;
                              padding: 0 12px;
                              font-size: 12px;
                            "
                            title="Send official payment receipt via WhatsApp"
                          >
                            ${Icons.whatsapp || '💬'}
                            <span>Send Receipt</span>
                          </button>
                        ` : ''}

                        <button 
                          type="button" 
                          class="fl-btn fl-btn-secondary fl-edit-schedule-card-btn" 
                          data-group-index="${idx}"
                          style="
                            height: 32px;
                            padding: 0 10px;
                            font-size: 12px;
                          "
                          title="Edit this plan & payment schedule"
                        >
                          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                            <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                          </svg>
                          <span>Edit</span>
                        </button>
                      </div>
                    </div>

                    ${installments.length > 0 ? `
                      <div id="fl-accordion-content-${card.id}" style="border-top: 1px solid #f1f5f9; padding-top: 12px; margin-top: 12px; display: ${isOpen ? 'flex' : 'none'}; flex-direction: column; gap: 8px;">
                        ${installments.map(inst => {
                          const isPaid = inst.isPaid || inst.status === 'paid';
                          const badgeHtml = isPaid
                            ? `<span style="background: #ecfdf5; color: #16a34a; border-radius: 4px; padding: 2px 8px; font-size: 11px; font-weight: 700;">PAID</span>`
                            : `<span style="background: #fef3c7; color: #b45309; border-radius: 4px; padding: 2px 8px; font-size: 11px; font-weight: 700;">DUE</span>`;

                          return `
                            <div style="background: #ffffff; border: 1px solid #f1f5f9; border-radius: 8px; padding: 10px 14px;">
                              <div style="display: flex; justify-content: space-between; align-items: center;">
                                <div style="display: flex; align-items: center; gap: 8px;">
                                  <span style="font-size: 14.5px; font-weight: 800; color: #0f172a;">${AppConfig.formatCurrency(inst.amount)}</span>
                                  ${isPaid ? `<span style="background: #f1f5f9; border: 1px solid #e2e8f0; border-radius: 4px; padding: 2px 6px; font-size: 11px; font-weight: 600; color: #475569;">${inst.paymentMethod || 'UPI'}</span>` : ''}
                                </div>
                                <div>
                                  ${badgeHtml}
                                </div>
                              </div>

                              <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 6px; font-size: 12px; color: #64748b;">
                                <div style="display: flex; flex-wrap: wrap; gap: 8px; align-items: center;">
                                  ${isPaid ? `
                                    <span>Due: ${inst.dueDate || '—'}</span>
                                    <span>•</span>
                                    <span>Paid: ${inst.paidDate || inst.dueDate || '—'}</span>
                                    ${inst.transactionId ? `<span>•</span><span>Txn: ${inst.transactionId}</span>` : ''}
                                  ` : `
                                    <span>Due: ${inst.dueDate || '—'}</span>
                                  `}
                                </div>
                                ${!isPaid ? `
                                  <div style="display: inline-flex; align-items: center; gap: 6px;">
                                    <button type="button" class="fl-drawer-remind-btn" data-payment-id="${inst.id}" style="
                                      background: #f0fdf4;
                                      border: 1px solid #bbf7d0;
                                      border-radius: 4px;
                                      padding: 3px 8px;
                                      font-size: 11px;
                                      font-weight: 600;
                                      color: #15803d;
                                      cursor: pointer;
                                      display: inline-flex;
                                      align-items: center;
                                      gap: 4px;
                                      line-height: 1;
                                    "
                                    onmouseover="this.style.background='#dcfce7'; this.style.borderColor='#86efac';"
                                    onmouseout="this.style.background='#f0fdf4'; this.style.borderColor='#bbf7d0';"
                                    title="Send payment reminder via WhatsApp"
                                    >
                                      <svg width="12" height="12" viewBox="0 0 24 24" style="flex-shrink: 0;">
                                        <path fill="#25D366" d="M12.04 2C6.58 2 2.13 6.45 2.13 11.91C2.13 13.66 2.59 15.36 3.45 16.86L2.05 22L7.3 20.62C8.75 21.41 10.38 21.83 12.04 21.83C17.5 21.83 21.95 17.38 21.95 11.92C21.95 9.27 20.92 6.78 19.05 4.91C17.18 3.03 14.69 2 12.04 2Z"/>
                                        <path fill="#FFFFFF" d="M17.52 14.33C17.22 14.18 15.75 13.45 15.48 13.35C15.2 13.25 15 13.2 14.8 13.5C14.6 13.8 14.03 14.48 13.85 14.68C13.68 14.88 13.5 14.9 13.2 14.75C12.9 14.6 11.94 14.29 10.8 13.27C9.91 12.48 9.31 11.51 9.14 11.21C8.96 10.91 9.12 10.75 9.27 10.6C9.4 10.47 9.56 10.26 9.71 10.08C9.86 9.9 9.91 9.78 10.01 9.58C10.11 9.38 10.06 9.2 9.99 9.05C9.91 8.9 9.31 7.43 9.06 6.83C8.82 6.25 8.57 6.33 8.39 6.32C8.21 6.31 8.01 6.31 7.81 6.31C7.61 6.31 7.29 6.39 7.01 6.69C6.74 6.99 5.96 7.71 5.96 9.19C5.96 10.66 7.04 12.09 7.19 12.29C7.34 12.49 9.31 15.52 12.33 16.82C13.05 17.13 13.61 17.32 14.05 17.46C14.77 17.69 15.42 17.65 15.94 17.58C16.52 17.49 17.72 16.85 17.97 16.15C18.22 15.45 18.22 14.85 18.15 14.72C18.07 14.6 17.82 14.48 17.52 14.33Z"/>
                                      </svg>
                                      <span>Remind</span>
                                    </button>
                                    <button type="button" class="fl-drawer-mark-paid-btn" data-payment-id="${inst.id}" style="
                                      background: #ffffff;
                                      border: 1px solid #e2e8f0;
                                      border-radius: 4px;
                                      padding: 3px 8px;
                                      font-size: 11px;
                                      font-weight: 600;
                                      color: #0f172a;
                                      cursor: pointer;
                                    ">
                                      Collect
                                    </button>
                                  </div>
                                ` : ''}
                              </div>
                            </div>
                          `;
                        }).join('')}
                      </div>
                    ` : ''}
                  </div>
                `;
              }).join('')}
            </div>
          </div>

          <!-- Bottom Footer -->
          <div style="
            padding: 16px 28px 20px 28px;
            border-top: 1px solid #e2e8f0;
            background: #ffffff;
            display: flex;
            gap: 12px;
          ">
            <button type="button" id="fl-drawer-renew-btn" class="fl-btn fl-btn-primary" style="
              flex: 1;
              height: 42px;
              font-size: 14px;
            ">
              ${isProspect ? 'Enroll Plan' : 'Renew Plan'}
            </button>
            <button type="button" id="fl-drawer-wa-btn" class="fl-btn fl-btn-whatsapp" style="
              flex: 1;
              height: 42px;
              font-size: 14px;
            ">
              ${Icons.whatsapp || '💬'} <span>WhatsApp</span>
            </button>
          </div>

        </div>
      </div>

      <!-- ============================================================== -->
      <!-- POPUP MODAL 1: EDIT PROFILE -->
      <!-- ============================================================== -->
      ${this.isEditingProfile ? `
        <fl-modal id="fl-edit-profile-modal" open title="Edit Member Profile" max-width="440px" z-index="1100">
          <form id="fl-edit-profile-form" style="display: flex; flex-direction: column; gap: 14px; padding: 4px 0;">
            <div>
              <label style="display: block; font-size: 11.5px; font-weight: 700; color: #475569; margin-bottom: 4px;">First Name *</label>
              <input 
                type="text" 
                id="fl-profile-first-name" 
                value="${this.profileDraft.firstName || ''}" 
                required 
                placeholder="First name"
                style="width: 100%; height: 36px; padding: 0 10px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 13.5px;" 
              />
            </div>

            <div>
              <label style="display: block; font-size: 11.5px; font-weight: 700; color: #475569; margin-bottom: 4px;">Last Name</label>
              <input 
                type="text" 
                id="fl-profile-last-name" 
                value="${this.profileDraft.lastName || ''}" 
                placeholder="Last name"
                style="width: 100%; height: 36px; padding: 0 10px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 13.5px;" 
              />
            </div>

            <div>
              <label style="display: block; font-size: 11.5px; font-weight: 700; color: #475569; margin-bottom: 4px;">WhatsApp Phone Number *</label>
              <div style="display: flex; align-items: center; border: 1px solid #cbd5e1; border-radius: 6px; overflow: hidden; background: #ffffff;">
                <span style="background: #f8fafc; padding: 0 10px; height: 36px; display: flex; align-items: center; font-size: 13px; font-weight: 600; color: #64748b; border-right: 1px solid #cbd5e1;">+91</span>
                <input 
                  type="tel" 
                  id="fl-profile-phone" 
                  maxlength="10" 
                  value="${this.profileDraft.phone || ''}" 
                  required 
                  placeholder="10-digit number"
                  style="flex: 1; height: 36px; padding: 0 10px; border: none; font-size: 13.5px;" 
                />
              </div>
            </div>

            <div id="fl-modal-profile-err" style="display: none; padding: 8px 12px; background: #fef2f2; border: 1px solid #fecaca; border-radius: 6px; color: #dc2626; font-size: 12px; font-weight: 600;"></div>

            <div style="display: flex; justify-content: flex-end; gap: 8px; border-top: 1px solid #e2e8f0; padding-top: 14px; margin-top: 4px;">
              <button type="button" id="fl-modal-profile-cancel" style="padding: 7px 14px; border: 1px solid #cbd5e1; border-radius: 6px; background: #ffffff; font-weight: 600; font-size: 13px; cursor: pointer;">
                Cancel
              </button>
              <button type="submit" id="fl-modal-profile-save" style="padding: 7px 18px; border: none; border-radius: 6px; background: #0f172a; color: #ffffff; font-weight: 700; font-size: 13px; cursor: pointer;">
                Save Profile
              </button>
            </div>
          </form>
        </fl-modal>
      ` : ''}

      <!-- ============================================================== -->
      <!-- POPUP MODAL 2: EDIT / ADD SCHEDULE (EXACT CREATE/RENEW STYLE) -->
      <!-- ============================================================== -->
      ${this.editingSchedule ? (() => {
        const isNew = this.editingSchedule.groupIndex < 0;
        const totalRecorded = (this.editingSchedule.payments || []).reduce((sum, p) => sum + (Number(p.amount) || 0), 0);
        const totalPaid = (this.editingSchedule.payments || [])
          .filter(p => p.isPaid || (Number(p.paidAmount) > 0 && Number(p.paidAmount) >= Number(p.amount)))
          .reduce((sum, p) => sum + (Number(p.amount) || 0), 0);
        const totalDue = Math.max(0, totalRecorded - totalPaid);

        return `
          <fl-modal id="fl-edit-schedule-modal" open title="${isNew ? 'Add Membership & Payment' : 'Edit Membership & Payment'}" max-width="560px" z-index="1100">
            <form id="fl-edit-schedule-form" style="display: flex; flex-direction: column; gap: 16px; padding: 4px 0;">
              
              <!-- Section 1: Plans & Services -->
              <div>
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                  <span style="font-size: 11.5px; font-weight: 700; color: #475569; letter-spacing: 0.5px; text-transform: uppercase;">
                    Covered Plans / Services
                  </span>
                  <button type="button" id="fl-modal-add-plan-btn" style="
                    background: #f1f5f9;
                    color: #0f172a;
                    border: 1px solid #cbd5e1;
                    border-radius: 6px;
                    padding: 4px 10px;
                    font-size: 12px;
                    font-weight: 700;
                    cursor: pointer;
                  ">
                    + Add Another Plan
                  </button>
                </div>

                <div style="display: flex; flex-direction: column; gap: 8px;">
                  ${this.editingSchedule.plans.map((p, pIdx) => `
                    <div style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 10px 12px;">
                      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
                        <span style="font-size: 11px; font-weight: 700; color: #64748b; text-transform: uppercase;">Service #${pIdx + 1}</span>
                        ${this.editingSchedule.plans.length > 1 ? `
                          <button type="button" class="fl-modal-remove-plan-btn" data-plan-index="${pIdx}" style="background: none; border: none; color: #dc2626; font-size: 11.5px; font-weight: 600; cursor: pointer;">
                            ✕ Remove
                          </button>
                        ` : ''}
                      </div>

                      <div style="margin-bottom: 6px;">
                        <select class="fl-modal-plan-select" data-plan-index="${pIdx}" style="width: 100%; height: 34px; padding: 0 8px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 13px; font-weight: 600; background: #ffffff;">
                          <option value="">Select Plan...</option>
                          ${availablePlans.map(ap => `
                            <option value="${ap.id}" ${ap.id === p.planId ? 'selected' : ''}>
                              ${ap.name} — ${ap.durationMonths ? ap.durationMonths + ' Mo' : ((ap.durationDays || 30) + ' Days')}
                            </option>
                          `).join('')}
                        </select>
                      </div>

                      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px;">
                        <div>
                          <label style="display: block; font-size: 10.5px; font-weight: 600; color: #64748b; margin-bottom: 2px;">Start Date</label>
                          <input type="date" class="fl-modal-plan-start" data-plan-index="${pIdx}" value="${p.startDate || ''}" style="width: 100%; height: 32px; padding: 0 8px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 12px; background: #ffffff;" />
                        </div>
                        <div>
                          <label style="display: block; font-size: 10.5px; font-weight: 600; color: #64748b; margin-bottom: 2px;">Valid Until</label>
                          <input type="date" class="fl-modal-plan-end" data-plan-index="${pIdx}" value="${p.endDate || ''}" style="width: 100%; height: 32px; padding: 0 8px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 12px; background: #ffffff;" />
                        </div>
                      </div>
                    </div>
                  `).join('')}
                </div>
              </div>

              <!-- Section 2: Payments & Installments -->
              <div>
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                  <span style="font-size: 11.5px; font-weight: 700; color: #475569; letter-spacing: 0.5px; text-transform: uppercase;">
                    Payment & Installments
                  </span>
                  <button type="button" id="fl-modal-add-pay-btn" style="
                    background: #f1f5f9;
                    color: #0f172a;
                    border: 1px solid #cbd5e1;
                    border-radius: 6px;
                    padding: 4px 10px;
                    font-size: 12px;
                    font-weight: 700;
                    cursor: pointer;
                  ">
                    + Add Installment
                  </button>
                </div>

                <div style="display: flex; flex-direction: column; gap: 8px; max-height: 220px; overflow-y: auto; padding-right: 2px;">
                  ${this.editingSchedule.payments.map((pay, iIdx) => {
                    if (pay.isPaid) {
                      return `
                        <div style="background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 8px; padding: 10px 12px; display: flex; justify-content: space-between; align-items: center;">
                          <div>
                            <div style="display: flex; align-items: center; gap: 8px;">
                              <span style="font-size: 14.5px; font-weight: 800; color: #166534;">${AppConfig.formatCurrency(pay.amount)}</span>
                              <span style="background: #dcfce7; border: 1px solid #86efac; border-radius: 4px; padding: 1px 6px; font-size: 10.5px; font-weight: 700; color: #15803d;">PAID</span>
                              <span style="font-size: 11.5px; font-weight: 600; color: #475569;">${pay.paymentMethod || 'UPI'}</span>
                            </div>
                            <div style="font-size: 11.5px; color: #15803d; margin-top: 3px;">
                              Paid: ${pay.paidDate || pay.dueDate || '—'}${pay.transactionId ? ` • Ref: ${pay.transactionId}` : ''}
                            </div>
                          </div>
                          <span style="font-size: 11px; font-weight: 600; color: #15803d; background: #ffffff; border: 1px solid #bbf7d0; border-radius: 4px; padding: 3px 8px;">
                            🔒 Locked
                          </span>
                        </div>
                      `;
                    } else {
                      return `
                        <div style="background: #fffbeb; border: 1px solid #fef3c7; border-radius: 8px; padding: 10px 12px;">
                          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
                            <span style="font-size: 11px; font-weight: 700; color: #b45309; text-transform: uppercase;">
                              Installment #${iIdx + 1} • Unpaid (Due)
                            </span>
                            <button type="button" class="fl-modal-remove-pay-btn" data-pay-index="${iIdx}" style="background: none; border: none; color: #dc2626; font-size: 11.5px; font-weight: 600; cursor: pointer;">
                              ✕ Remove
                            </button>
                          </div>

                          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-bottom: 6px;">
                            <div>
                              <label style="display: block; font-size: 10.5px; font-weight: 600; color: #475569; margin-bottom: 2px;">Amount (₹)</label>
                              <input type="number" min="0" class="fl-modal-pay-amount" data-pay-index="${iIdx}" value="${pay.amount !== undefined ? pay.amount : ''}" style="width: 100%; height: 32px; padding: 0 8px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 12.5px; background: #ffffff;" />
                            </div>
                            <div>
                              <label style="display: block; font-size: 10.5px; font-weight: 600; color: #475569; margin-bottom: 2px;">Due Date</label>
                              <input type="date" class="fl-modal-pay-due" data-pay-index="${iIdx}" value="${pay.dueDate || ''}" style="width: 100%; height: 32px; padding: 0 8px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 12px; background: #ffffff;" />
                            </div>
                          </div>

                          <div>
                            <input type="text" class="fl-modal-pay-notes" data-pay-index="${iIdx}" value="${pay.notes || ''}" placeholder="Note (e.g. 2nd Installment)" style="width: 100%; height: 28px; padding: 0 8px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 11.5px; background: #ffffff;" />
                          </div>
                        </div>
                      `;
                    }
                  }).join('')}
                </div>

                <!-- Totals Bar -->
                <div style="margin-top: 10px; padding: 10px 14px; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; display: flex; justify-content: space-between; align-items: center; font-size: 12.5px;">
                  <div>
                    <span style="color: #64748b;">Total Amount:</span> 
                    <strong id="fl-modal-total-amt" style="color: #0f172a; font-size: 14px; margin-left: 4px;">${AppConfig.formatCurrency(totalRecorded)}</strong>
                  </div>
                  <div style="display: flex; gap: 14px;">
                    <div>
                      <span style="color: #64748b;">Paid:</span> 
                      <strong id="fl-modal-paid-amt" style="color: #16a34a; margin-left: 4px;">${AppConfig.formatCurrency(totalPaid)}</strong>
                    </div>
                    <div>
                      <span style="color: #64748b;">Due:</span> 
                      <strong id="fl-modal-due-amt" style="color: ${totalDue > 0 ? '#b45309' : '#64748b'}; margin-left: 4px;">
                        ${totalDue > 0 ? AppConfig.formatCurrency(totalDue) : 'None (Settled ✓)'}
                      </strong>
                    </div>
                  </div>
                </div>
              </div>

              <div id="fl-modal-schedule-err" style="display: none; padding: 8px 12px; background: #fef2f2; border: 1px solid #fecaca; border-radius: 6px; color: #dc2626; font-size: 12px; font-weight: 600;"></div>

              <!-- Modal Footer Actions -->
              <div style="display: flex; justify-content: space-between; align-items: center; border-top: 1px solid #e2e8f0; padding-top: 14px; margin-top: 4px;">
                <div>
                  ${(!isNew && !(this.editingSchedule.payments || []).some(p => p.isPaid)) ? `
                    <button type="button" id="fl-modal-schedule-delete" style="padding: 7px 12px; border: 1px solid #fecaca; border-radius: 6px; background: #fef2f2; color: #dc2626; font-weight: 600; font-size: 12.5px; cursor: pointer;">
                      Delete Schedule
                    </button>
                  ` : ''}
                </div>
                <div style="display: flex; gap: 8px;">
                  <button type="button" id="fl-modal-schedule-cancel" style="padding: 7px 14px; border: 1px solid #cbd5e1; border-radius: 6px; background: #ffffff; font-weight: 600; font-size: 13px; cursor: pointer;">
                    Cancel
                  </button>
                  <button type="submit" id="fl-modal-schedule-save" style="padding: 7px 18px; border: none; border-radius: 6px; background: #0f172a; color: #ffffff; font-weight: 700; font-size: 13px; cursor: pointer;">
                    ${isNew ? 'Add Schedule' : 'Save Changes'}
                  </button>
                </div>
              </div>
            </form>
          </fl-modal>
        `;
      })() : ''}

      <style>
        @keyframes flModalFadeIn {
          from { opacity: 0; transform: scale(0.96); }
          to { opacity: 1; transform: scale(1); }
        }
        @keyframes flFadeIn {
          from { opacity: 0; }
          to { opacity: 1; }
        }
      </style>
    `;
  }

  saveModalInputsToDraft() {
    if (!this.editingSchedule) return;

    // Plans
    this.querySelectorAll('.fl-modal-plan-select').forEach(sel => {
      const pIdx = Number(sel.getAttribute('data-plan-index'));
      if (this.editingSchedule.plans[pIdx]) {
        this.editingSchedule.plans[pIdx].planId = sel.value;
      }
    });

    this.querySelectorAll('.fl-modal-plan-start').forEach(input => {
      const pIdx = Number(input.getAttribute('data-plan-index'));
      if (this.editingSchedule.plans[pIdx]) {
        this.editingSchedule.plans[pIdx].startDate = input.value;
      }
    });

    this.querySelectorAll('.fl-modal-plan-end').forEach(input => {
      const pIdx = Number(input.getAttribute('data-plan-index'));
      if (this.editingSchedule.plans[pIdx]) {
        this.editingSchedule.plans[pIdx].endDate = input.value;
      }
    });

    // Payments
    this.querySelectorAll('.fl-modal-pay-amount').forEach(input => {
      const iIdx = Number(input.getAttribute('data-pay-index'));
      if (this.editingSchedule.payments[iIdx] && !this.editingSchedule.payments[iIdx].isPaid) {
        this.editingSchedule.payments[iIdx].amount = Number(input.value) || 0;
      }
    });

    this.querySelectorAll('.fl-modal-pay-due').forEach(input => {
      const iIdx = Number(input.getAttribute('data-pay-index'));
      if (this.editingSchedule.payments[iIdx] && !this.editingSchedule.payments[iIdx].isPaid) {
        this.editingSchedule.payments[iIdx].dueDate = input.value;
      }
    });

    this.querySelectorAll('.fl-modal-pay-notes').forEach(input => {
      const iIdx = Number(input.getAttribute('data-pay-index'));
      if (this.editingSchedule.payments[iIdx] && !this.editingSchedule.payments[iIdx].isPaid) {
        this.editingSchedule.payments[iIdx].notes = input.value;
      }
    });
  }

  bindEvents() {
    const { memberDrawer } = HomeStore.getState();
    const currMember = memberDrawer?.member;
    if (!currMember) return;

    // Backdrop click closes main drawer if no sub-modal is open
    const backdrop = this.querySelector('#fl-drawer-backdrop');
    if (backdrop) {
      backdrop.addEventListener('click', (e) => {
        if (e.target === backdrop && !this.isEditingProfile && !this.editingSchedule) {
          HomeStore.closeMemberDrawer();
        }
      });
    }

    // Main Drawer Close buttons
    this.querySelectorAll('.fl-drawer-close-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        HomeStore.closeMemberDrawer();
      });
    });

    // Top Header [Edit Profile] button
    const editProfileBtn = this.querySelector('#fl-drawer-edit-btn');
    if (editProfileBtn) {
      editProfileBtn.addEventListener('click', () => {
        this.initProfileDraft(currMember);
        this.isEditingProfile = true;
        this.editingSchedule = null;
        this.render();
      });
    }

    // Accordion toggle clicks
    this.querySelectorAll('.fl-accordion-toggle-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        e.preventDefault();
        const cardId = btn.getAttribute('data-card-id');
        const contentEl = this.querySelector(`#fl-accordion-content-${cardId}`);
        const arrowEl = btn.querySelector('.fl-accordion-arrow');

        if (contentEl) {
          const isCurrentlyHidden = contentEl.style.display === 'none';
          if (isCurrentlyHidden) {
            contentEl.style.display = 'flex';
            this.openAccordions.add(cardId);
            if (arrowEl) arrowEl.style.transform = 'rotate(180deg)';
          } else {
            contentEl.style.display = 'none';
            this.openAccordions.delete(cardId);
            if (arrowEl) arrowEl.style.transform = 'rotate(0deg)';
          }
        }
      });
    });

    // Edit Schedule Card click
    this.querySelectorAll('.fl-edit-schedule-card-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const gIdx = Number(btn.getAttribute('data-group-index'));
        const grp = this.paymentGroups[gIdx];
        if (!grp) return;

        const pad = (n) => String(n).padStart(2, '0');
        const today = new Date();
        const todayIso = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`;

        const plansCopy = (grp.plans && grp.plans.length > 0)
          ? JSON.parse(JSON.stringify(grp.plans))
          : [{
              id: `plan-init-${Date.now()}`,
              planId: '',
              planName: 'Service',
              startDate: todayIso,
              endDate: todayIso,
              fee: 0
            }];

        const paymentsCopy = (grp.payments && grp.payments.length > 0)
          ? JSON.parse(JSON.stringify(grp.payments))
          : [{
              id: `pay-init-${Date.now()}`,
              amount: 0,
              paidAmount: 0,
              dueDate: todayIso,
              paidDate: null,
              paymentMethod: 'UPI',
              transactionId: null,
              notes: 'Installment #1',
              isPaid: false
            }];

        this.editingSchedule = {
          groupIndex: gIdx,
          plans: plansCopy,
          payments: paymentsCopy
        };
        this.isEditingProfile = false;
        this.render();
      });
    });



    // Remind payment button
    this.querySelectorAll('.fl-drawer-remind-btn').forEach(btn => {
      btn.addEventListener('click', async (e) => {
        e.stopPropagation();
        const paymentId = btn.getAttribute('data-payment-id');
        const payment = (currMember.payments || []).find(p => p.id === paymentId);
        if (!payment) return;

        const origContent = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = `<span style="font-size: 11px; font-weight: 700; color: #16a34a;">Sending...</span>`;
        try {
          await HomeStore.sendPaymentReminder(payment, currMember);
          btn.innerHTML = `<span style="font-size: 11px; font-weight: 700; color: #16a34a;">Sent! ✓</span>`;
          setTimeout(() => {
            btn.innerHTML = origContent;
            btn.disabled = false;
          }, 2500);
        } catch (err) {
          btn.disabled = false;
          btn.innerHTML = origContent;
        }
      });
    });

    // Mark as Paid button
    this.querySelectorAll('.fl-drawer-mark-paid-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const paymentId = btn.getAttribute('data-payment-id');
        const payment = (currMember.payments || []).find(p => p.id === paymentId);
        if (payment) {
          HomeStore.openCollectPaymentModal(payment, currMember);
        }
      });
    });

    // Send Receipt button
    this.querySelectorAll('.fl-drawer-send-receipt-btn').forEach(btn => {
      btn.addEventListener('click', async (e) => {
        e.stopPropagation();
        const paymentId = btn.getAttribute('data-payment-id');
        if (!paymentId) return;

        const origContent = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = `<span style="font-size: 11px; font-weight: 700; color: #16a34a;">Sending...</span>`;
        try {
          await HomeStore.sendPaymentReceipt(paymentId, currMember.id);
          btn.innerHTML = `<span style="font-size: 11px; font-weight: 700; color: #16a34a;">Sent! ✓</span>`;
          setTimeout(() => {
            btn.innerHTML = origContent;
            btn.disabled = false;
          }, 2500);
        } catch (err) {
          btn.disabled = false;
          btn.innerHTML = origContent;
        }
      });
    });

    // Renew button at bottom
    const renewBtn = this.querySelector('#fl-drawer-renew-btn');
    if (renewBtn) {
      renewBtn.addEventListener('click', () => {
        HomeStore.openRenewMemberModal(currMember);
      });
    }

    // WhatsApp button
    const waBtn = this.querySelector('#fl-drawer-wa-btn');
    if (waBtn) {
      waBtn.addEventListener('click', () => {
        const isProspect = currMember.status === 'prospect' || (!currMember.subscriptions || currMember.subscriptions.length === 0);
        const hasOverdue = (currMember.payments || []).some(p => !p.isPaid && (p.isOverdue || p.status === 'overdue'));
        const pendingTpl = hasOverdue ? 'payment_overdue_reminder' : 'upcoming_payment_reminder';
        const defaultTpl = currMember.status === 'renewals'
          ? 'plan_renewal_reminder'
          : currMember.status === 'expired'
          ? 'member_lapsed_winback'
          : isProspect
          ? 'prospect_walkin_recap'
          : (currMember.pendingAmount > 0 ? pendingTpl : 'google_review_request');
        HomeStore.openWhatsAppModal(currMember, defaultTpl);
      });
    }

    // ==============================================================
    // EVENT LISTENERS FOR MODAL 1: EDIT PROFILE
    // ==============================================================
    if (this.isEditingProfile) {
      const profileModal = this.querySelector('#fl-edit-profile-modal');
      const closeProfileModal = () => {
        this.isEditingProfile = false;
        this.render();
      };
      if (profileModal) {
        profileModal.addEventListener('close', closeProfileModal);
        profileModal.addEventListener('fl-modal-close', closeProfileModal);
      }
      const cancelProfileBtn = this.querySelector('#fl-modal-profile-cancel');
      if (cancelProfileBtn) cancelProfileBtn.addEventListener('click', closeProfileModal);

      const fNameInput = this.querySelector('#fl-profile-first-name');
      const lNameInput = this.querySelector('#fl-profile-last-name');
      const phoneInput = this.querySelector('#fl-profile-phone');
      if (fNameInput) fNameInput.addEventListener('input', () => { this.profileDraft.firstName = fNameInput.value; });
      if (lNameInput) lNameInput.addEventListener('input', () => { this.profileDraft.lastName = lNameInput.value; });
      if (phoneInput) phoneInput.addEventListener('input', () => { this.profileDraft.phone = phoneInput.value; });

      const profileForm = this.querySelector('#fl-edit-profile-form');
      if (profileForm) {
        profileForm.addEventListener('submit', async (e) => {
          e.preventDefault();
          const errEl = this.querySelector('#fl-modal-profile-err');
          if (errEl) errEl.style.display = 'none';

          const fn = fNameInput ? fNameInput.value.trim() : '';
          const ln = lNameInput ? lNameInput.value.trim() : '';
          const ph = phoneInput ? phoneInput.value.trim().replace(/\D/g, '') : '';

          if (!fn) {
            if (errEl) { errEl.textContent = 'First name is required.'; errEl.style.display = 'block'; }
            return;
          }
          if (!ph || ph.length < 10) {
            if (errEl) { errEl.textContent = 'Please enter a valid 10-digit WhatsApp phone number.'; errEl.style.display = 'block'; }
            return;
          }

          const saveBtn = this.querySelector('#fl-modal-profile-save');
          if (saveBtn) { saveBtn.disabled = true; saveBtn.textContent = 'Saving...'; }

          try {
            // Flatten current plans & payments so none are altered
            const { allPlansToSend, unpaidPaymentsToSend, paidPaymentPlanLinks } = this.flattenAllGroups(this.paymentGroups);

            await HomeStore.updateMember({
              userId: currMember.id,
              firstName: fn,
              lastName: ln,
              phoneNumber: ph,
              plans: allPlansToSend,
              payments: unpaidPaymentsToSend,
              paidPaymentPlanLinks: paidPaymentPlanLinks
            });

            this.isEditingProfile = false;
            // Refetch or update active member drawer in store
            const updatedMembers = HomeStore.getState().members;
            const fresh = updatedMembers.find(m => m.id === currMember.id);
            if (fresh) {
              HomeStore.openMemberDrawer(fresh);
            } else {
              this.render();
            }
          } catch (err) {
            if (errEl) {
              errEl.textContent = err.message || 'Failed to update member profile.';
              errEl.style.display = 'block';
            }
          } finally {
            if (saveBtn) { saveBtn.disabled = false; saveBtn.textContent = 'Save Profile'; }
          }
        });
      }
    }

    // ==============================================================
    // EVENT LISTENERS FOR MODAL 2: EDIT / ADD SCHEDULE
    // ==============================================================
    if (this.editingSchedule) {
      const pad = (n) => String(n).padStart(2, '0');
      const toIso = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
      const { availablePlans } = HomeStore.getState();

      const schedModal = this.querySelector('#fl-edit-schedule-modal');
      const closeSchedModal = () => {
        this.editingSchedule = null;
        this.render();
      };
      if (schedModal) {
        schedModal.addEventListener('close', closeSchedModal);
        schedModal.addEventListener('fl-modal-close', closeSchedModal);
      }
      const cancelSchedBtn = this.querySelector('#fl-modal-schedule-cancel');
      if (cancelSchedBtn) cancelSchedBtn.addEventListener('click', closeSchedModal);

      // Add Another Plan to Schedule
      const addPlanBtn = this.querySelector('#fl-modal-add-plan-btn');
      if (addPlanBtn) {
        addPlanBtn.addEventListener('click', () => {
          this.saveModalInputsToDraft();
          const today = new Date();
          const todayIso = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`;
          const defaultStart = (this.editingSchedule.plans[0] && this.editingSchedule.plans[0].startDate) || todayIso;
          const defaultEnd = (this.editingSchedule.plans[0] && this.editingSchedule.plans[0].endDate) || todayIso;

          this.editingSchedule.plans.push({
            id: `plan-add-${Date.now()}-${this.editingSchedule.plans.length}`,
            planId: '',
            planName: 'New Service',
            startDate: defaultStart,
            endDate: defaultEnd,
            fee: 0
          });
          this.render();
        });
      }

      // Remove Plan from Schedule
      this.querySelectorAll('.fl-modal-remove-plan-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          this.saveModalInputsToDraft();
          const pIdx = Number(btn.getAttribute('data-plan-index'));
          this.editingSchedule.plans.splice(pIdx, 1);
          this.render();
        });
      });

      // Plan select change (Auto-calculate duration & fee)
      this.querySelectorAll('.fl-modal-plan-select').forEach(sel => {
        sel.addEventListener('change', () => {
          this.saveModalInputsToDraft();
          const pIdx = Number(sel.getAttribute('data-plan-index'));
          const planRow = this.editingSchedule.plans[pIdx];
          if (!planRow) return;

          planRow.planId = sel.value;
          const found = (availablePlans || []).find(p => p.id === sel.value);
          if (found) {
            planRow.planName = found.name;
            planRow.fee = found.price || 0;
            const durationDays = found.durationDays || (found.durationMonths ? found.durationMonths * 30 : 30);
            const startD = planRow.startDate ? new Date(planRow.startDate) : new Date();
            const endD = new Date(startD);
            endD.setDate(endD.getDate() + durationDays);
            planRow.endDate = toIso(endD);
          }
          this.render();
        });
      });

      // Plan Start Date change (Auto shift Valid Until date)
      this.querySelectorAll('.fl-modal-plan-start').forEach(input => {
        input.addEventListener('change', () => {
          this.saveModalInputsToDraft();
          const pIdx = Number(input.getAttribute('data-plan-index'));
          const planRow = this.editingSchedule.plans[pIdx];
          if (!planRow || !planRow.planId) return;

          const found = (availablePlans || []).find(p => p.id === planRow.planId);
          if (found && input.value) {
            const durationDays = found.durationDays || (found.durationMonths ? found.durationMonths * 30 : 30);
            const startD = new Date(input.value);
            const endD = new Date(startD);
            endD.setDate(endD.getDate() + durationDays);
            planRow.endDate = toIso(endD);
          }
          this.render();
        });
      });

      // Add Installment to Schedule
      const addPayBtn = this.querySelector('#fl-modal-add-pay-btn');
      if (addPayBtn) {
        addPayBtn.addEventListener('click', () => {
          this.saveModalInputsToDraft();
          const today = new Date();
          const todayIso = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`;

          this.editingSchedule.payments.push({
            id: `pay-add-${Date.now()}-${this.editingSchedule.payments.length}`,
            amount: 0,
            paidAmount: 0,
            dueDate: todayIso,
            paidDate: null,
            paymentMethod: 'UPI',
            transactionId: null,
            notes: `Installment #${this.editingSchedule.payments.length + 1}`,
            isPaid: false
          });
          this.render();
        });
      }

      // Live update Totals Bar on installment amount input
      this.querySelectorAll('.fl-modal-pay-amount').forEach(input => {
        input.addEventListener('input', () => {
          const iIdx = Number(input.getAttribute('data-pay-index'));
          if (this.editingSchedule.payments[iIdx] && !this.editingSchedule.payments[iIdx].isPaid) {
            this.editingSchedule.payments[iIdx].amount = Number(input.value) || 0;
          }
          const totalRecorded = (this.editingSchedule.payments || []).reduce((sum, p) => sum + (Number(p.amount) || 0), 0);
          const totalPaid = (this.editingSchedule.payments || [])
            .filter(p => p.isPaid || (Number(p.paidAmount) > 0 && Number(p.paidAmount) >= Number(p.amount)))
            .reduce((sum, p) => sum + (Number(p.amount) || 0), 0);
          const totalDue = Math.max(0, totalRecorded - totalPaid);

          const totalEl = this.querySelector('#fl-modal-total-amt');
          const paidEl = this.querySelector('#fl-modal-paid-amt');
          const dueEl = this.querySelector('#fl-modal-due-amt');

          if (totalEl) totalEl.textContent = AppConfig.formatCurrency(totalRecorded);
          if (paidEl) paidEl.textContent = AppConfig.formatCurrency(totalPaid);
          if (dueEl) {
            dueEl.textContent = totalDue > 0 ? AppConfig.formatCurrency(totalDue) : 'None (Settled ✓)';
            dueEl.style.color = totalDue > 0 ? '#b45309' : '#64748b';
          }
        });
      });

      // Remove Installment from Schedule (Unpaid only)
      this.querySelectorAll('.fl-modal-remove-pay-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          this.saveModalInputsToDraft();
          const iIdx = Number(btn.getAttribute('data-pay-index'));
          this.editingSchedule.payments.splice(iIdx, 1);
          this.render();
        });
      });

      // Delete entire Schedule (If unpaid)
      const deleteSchedBtn = this.querySelector('#fl-modal-schedule-delete');
      if (deleteSchedBtn) {
        deleteSchedBtn.addEventListener('click', async () => {
          if (!confirm('Are you sure you want to remove this schedule?')) return;
          deleteSchedBtn.disabled = true;
          deleteSchedBtn.textContent = 'Removing...';

          try {
            const updatedGroups = this.paymentGroups.filter((_, idx) => idx !== this.editingSchedule.groupIndex);
            const { allPlansToSend, unpaidPaymentsToSend, paidPaymentPlanLinks } = this.flattenAllGroups(updatedGroups);

            await HomeStore.updateMember({
              userId: currMember.id,
              firstName: this.profileDraft.firstName,
              lastName: this.profileDraft.lastName,
              phoneNumber: this.profileDraft.phone,
              plans: allPlansToSend,
              payments: unpaidPaymentsToSend,
              paidPaymentPlanLinks: paidPaymentPlanLinks
            });

            this.editingSchedule = null;
            const updatedMembers = HomeStore.getState().members;
            const fresh = updatedMembers.find(m => m.id === currMember.id);
            if (fresh) {
              HomeStore.openMemberDrawer(fresh);
            } else {
              this.render();
            }
          } catch (err) {
            alert(err.message || 'Failed to delete schedule.');
          } finally {
            deleteSchedBtn.disabled = false;
          }
        });
      }

      // Submit / Save Schedule
      const schedForm = this.querySelector('#fl-edit-schedule-form');
      if (schedForm) {
        schedForm.addEventListener('submit', async (e) => {
          e.preventDefault();
          this.saveModalInputsToDraft();
          const errEl = this.querySelector('#fl-modal-schedule-err');
          if (errEl) errEl.style.display = 'none';

          // Validate plans
          if (!this.editingSchedule.plans || this.editingSchedule.plans.length === 0) {
            if (errEl) { errEl.textContent = 'Please add at least one plan to this schedule.'; errEl.style.display = 'block'; }
            return;
          }

          for (let pIdx = 0; pIdx < this.editingSchedule.plans.length; pIdx++) {
            const p = this.editingSchedule.plans[pIdx];
            if (!p.planId) {
              if (errEl) { errEl.textContent = `Please select a plan for Service #${pIdx + 1}.`; errEl.style.display = 'block'; }
              return;
            }
            if (p.startDate && p.endDate && new Date(p.startDate) > new Date(p.endDate)) {
              if (errEl) { errEl.textContent = `Start date cannot be after Valid Until date for Service #${pIdx + 1}.`; errEl.style.display = 'block'; }
              return;
            }
          }

          // Validate payments
          for (let iIdx = 0; iIdx < this.editingSchedule.payments.length; iIdx++) {
            const pay = this.editingSchedule.payments[iIdx];
            if (!pay.isPaid) {
              if (pay.amount === undefined || pay.amount === null || isNaN(pay.amount) || pay.amount <= 0) {
                if (errEl) { errEl.textContent = `Please enter a valid positive amount for Installment #${iIdx + 1}.`; errEl.style.display = 'block'; }
                return;
              }
              if (!pay.dueDate) {
                if (errEl) { errEl.textContent = `Please set a due date for Installment #${iIdx + 1}.`; errEl.style.display = 'block'; }
                return;
              }
            }
          }

          const saveBtn = this.querySelector('#fl-modal-schedule-save');
          if (saveBtn) { saveBtn.disabled = true; saveBtn.textContent = 'Saving...'; }

          try {
            // Reconstruct updated groups array
            const updatedGroups = [...this.paymentGroups];
            if (this.editingSchedule.groupIndex >= 0) {
              updatedGroups[this.editingSchedule.groupIndex] = {
                ...updatedGroups[this.editingSchedule.groupIndex],
                plans: this.editingSchedule.plans,
                payments: this.editingSchedule.payments
              };
            } else {
              updatedGroups.push({
                id: `group-added-${Date.now()}`,
                startDate: this.editingSchedule.plans[0]?.startDate,
                plans: this.editingSchedule.plans,
                payments: this.editingSchedule.payments
              });
            }

            const { allPlansToSend, unpaidPaymentsToSend, paidPaymentPlanLinks } = this.flattenAllGroups(updatedGroups);

            await HomeStore.updateMember({
              userId: currMember.id,
              firstName: this.profileDraft.firstName,
              lastName: this.profileDraft.lastName,
              phoneNumber: this.profileDraft.phone,
              plans: allPlansToSend,
              payments: unpaidPaymentsToSend,
              paidPaymentPlanLinks: paidPaymentPlanLinks
            });

            this.editingSchedule = null;
            const updatedMembers = HomeStore.getState().members;
            const fresh = updatedMembers.find(m => m.id === currMember.id);
            if (fresh) {
              HomeStore.openMemberDrawer(fresh);
            } else {
              this.render();
            }
          } catch (err) {
            if (errEl) {
              errEl.textContent = err.message || 'Failed to save schedule changes.';
              errEl.style.display = 'block';
            }
          } finally {
            if (saveBtn) { saveBtn.disabled = false; saveBtn.textContent = 'Save Changes'; }
          }
        });
      }
    }
  }

  flattenAllGroups(groups) {
    const allPlansToSend = [];
    const paidPaymentPlanLinks = [];
    const unpaidPaymentsToSend = [];

    (groups || []).forEach(grp => {
      const groupPlanIndices = [];
      (grp.plans || []).forEach(p => {
        const planIndex = allPlansToSend.length;
        groupPlanIndices.push(planIndex);
        allPlansToSend.push({
          planId: p.planId,
          startDate: p.startDate,
          endDate: p.endDate
        });
      });

      (grp.payments || []).forEach((pay, i) => {
        if (pay.isPaid) {
          if (pay.id) {
            paidPaymentPlanLinks.push({
              paymentId: pay.id,
              planIndices: groupPlanIndices
            });
          }
        } else {
          unpaidPaymentsToSend.push({
            amount: Number(pay.amount),
            paidAmount: 0,
            dueDate: pay.dueDate,
            paidDate: null,
            paymentMethod: pay.paymentMethod || 'UPI',
            transactionId: null,
            notes: pay.notes || `Installment #${i + 1}`,
            planIndices: groupPlanIndices
          });
        }
      });
    });

    return { allPlansToSend, unpaidPaymentsToSend, paidPaymentPlanLinks };
  }
}

customElements.define('fl-member-drawer', FlMemberDrawer);
