/**
 * <fl-whatsapp-modal> Native Web Component
 * Professional WhatsApp Notification Composer and Template Dispatcher.
 * Supports both 1-on-1 personalized messaging and multi-recipient Bulk Broadcasts.
 * Features:
 * - Scope-based filtering (INDIVIDUAL, BULK, BOTH)
 * - Meta HSM Templates (Utility & Marketing)
 * - Dynamic Variable Interpolation (GYM.*, PLAN.*, PAYMENT.*, USER.*)
 * - Interactive Plan & Pricing Quote Builder for Prospect Walk-in Recap
 * - Dynamic Custom Placeholder Detection (CUSTOM.*) with live blank inputs
 * - Live real-time WYSIWYG WhatsApp message bubble preview as the operator types
 * - Interactive Buttons Preview
 */

import { HomeStore } from '../home-store.js';
import { Icons } from '../../../assets/icons/icons.js';
import { AppConfig } from '../../../core/config/app-config.js';
import { ErrorMapper } from '../../../core/error/error-mapper.js';
import '../../../core/components/feedback/fl-modal.js';

export class FlWhatsAppModal extends HTMLElement {
  constructor() {
    super();
    this.selectedCategory = 'ALL';
    this.currentTemplateKey = null;
    this.customParameters = {};
    this.customTextOverride = null;
    this.quotedPlans = [];
  }

  getTemplateGroup(tpl) {
    if (!tpl) return 'PROSPECT';
    const key = String(tpl.key || tpl.templateKey || '').toLowerCase();
    const cat = String(tpl.category || '').toUpperCase();

    if (key.includes('payment') || key.includes('receipt') || key.includes('overdue')) {
      return 'PAYMENT';
    }
    if (key.includes('review')) {
      return 'REQUEST';
    }
    if (key === 'plan_renewal_reminder' || (key.includes('renewal') && !key.includes('prospect'))) {
      return 'MEMBER';
    }
    if (key.includes('prospect') || cat === 'MARKETING' || key.includes('campaign') || key.includes('winback')) {
      return 'PROSPECT';
    }
    return 'PROSPECT';
  }

  connectedCallback() {
    this.unsubscribe = HomeStore.subscribe(() => this.render());
    this.render();
  }

  disconnectedCallback() {
    if (this.unsubscribe) this.unsubscribe();
  }

  extractCustomPlaceholders(content) {
    if (!content) return [];
    const matches = content.match(/\{\{CUSTOM\.([^}]+)\}\}/g);
    if (!matches) return [];
    const unique = [...new Set(matches)];
    return unique.map(m => {
      const rawKey = m.slice(2, -2).trim(); // e.g. "CUSTOM.headline"
      const name = rawKey.replace(/^CUSTOM\./i, ''); // e.g. "headline"
      const label = name.split('_').map(w => w.charAt(0).toUpperCase() + w.slice(1)).join(' ');
      return { rawKey, name, label };
    });
  }

  formatShortDate(dStr) {
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

  formatPlansRecap(plans) {
    if (!plans || plans.length === 0) return '';
    const { availablePlans } = HomeStore.getState();
    const lines = [];
    let total = 0;

    plans.forEach(p => {
      const isCustom = p.planId === '__CUSTOM__';
      let name = p.customName;
      if (!isCustom) {
        const matched = (availablePlans || []).find(ap => ap.id === p.planId);
        name = matched ? matched.name : (p.planName || 'Membership Plan');
      }
      const fee = Number(p.fee) || 0;
      total += fee;
      const periodStr = (p.startDate && p.endDate) ? ` (${this.formatShortDate(p.startDate)} – ${this.formatShortDate(p.endDate)})` : '';
      lines.push(`• ${name}: ${AppConfig.formatCurrency(fee)}${periodStr}`);
    });

    if (lines.length === 0) return '';
    const header = lines.length === 1 ? 'Quoted Membership Plan:' : 'Quoted Membership Plans:';
    if (lines.length > 1) {
      return `${header}\n${lines.join('\n')}\nTotal: ${AppConfig.formatCurrency(total)}`;
    }
    return `${header}\n${lines.join('\n')}`;
  }

  render() {
    const { whatsAppModal, availablePlans } = HomeStore.getState();
    const { isOpen, member, isBulk, recipients, defaultTemplate, paymentInfo } = whatsAppModal;

    const targetRecipients = (recipients && recipients.length > 0)
      ? recipients
      : (member ? [member] : []);

    if (!isOpen || targetRecipients.length === 0) {
      this.wasOpen = false;
      this.innerHTML = '';
      this.currentTemplateKey = null;
      this.selectedCategory = 'ALL';
      this.customTextOverride = null;
      this.customParameters = {};
      this.quotedPlans = [];
      return;
    }

    // Reset template selection on fresh modal open
    if (!this.wasOpen) {
      this.wasOpen = true;
      this.currentTemplateKey = defaultTemplate || null;
      this.selectedCategory = 'ALL';
      this.customTextOverride = null;
      this.customParameters = {};
      this.quotedPlans = [];
    }

    const primaryMember = member || targetRecipients[0];
    const isBulkMode = Boolean(isBulk && targetRecipients.length > 1);

    const { conversations } = HomeStore.getState();
    const primaryConv = primaryMember ? (conversations || []).find(c => c.id === (primaryMember.id || primaryMember.userId)) : null;
    const isMemberHumanActive = Boolean(primaryConv && primaryConv.isHumanRequired);

    const allTemplates = AppConfig.getQuickTemplates();
    const outboundTemplates = allTemplates.filter(t => {
      const key = String(t.key || t.templateKey || '').toLowerCase();
      if (key.startsWith('inbound_')) return false;
      const scope = (t.dispatchScope || 'BOTH').toUpperCase();
      if (scope === 'AUTO') return false;
      if (key === 'member_welcome_onboarding' || key === 'member_renewal_confirmation' || key === 'support_access_requested') return false;
      return true;
    });

    // Filter by DispatchScope
    const scopeTemplates = outboundTemplates.filter(t => {
      const scope = (t.dispatchScope || 'BOTH').toUpperCase();
      if (scope === 'AUTO') return false;
      if (isBulkMode) {
        return scope === 'BULK' || scope === 'BOTH';
      }
      return scope === 'INDIVIDUAL' || scope === 'BOTH';
    });

    // Filter by Category
    const filteredTemplates = scopeTemplates.filter(t => {
      if (!this.selectedCategory || this.selectedCategory === 'ALL') return true;
      return this.getTemplateGroup(t) === this.selectedCategory;
    });

    // Select active template
    if (!this.currentTemplateKey) {
      this.currentTemplateKey = defaultTemplate || (filteredTemplates[0] ? (filteredTemplates[0].key || filteredTemplates[0].templateKey) : (scopeTemplates[0] ? (scopeTemplates[0].key || scopeTemplates[0].templateKey) : ''));
    }

    let templateObj = scopeTemplates.find(t => (t.key || t.templateKey) === this.currentTemplateKey);
    if (!templateObj && filteredTemplates.length > 0) {
      templateObj = filteredTemplates[0];
      this.currentTemplateKey = templateObj.key || templateObj.templateKey;
    } else if (!templateObj && scopeTemplates.length > 0) {
      templateObj = scopeTemplates[0];
      this.currentTemplateKey = templateObj.key || templateObj.templateKey;
    }

    const isProspectRecap = !isBulkMode && ((this.currentTemplateKey === 'prospect_walkin_recap') || (templateObj?.key === 'prospect_walkin_recap'));
    const isPaymentReceipt = (this.currentTemplateKey === 'payment_receipt_instant') || (templateObj?.key === 'payment_receipt_instant');

    // Initialize quoted plans from store if provided
    if (whatsAppModal.quotedPlans && whatsAppModal.quotedPlans.length > 0 && this.quotedPlans.length === 0) {
      this.quotedPlans = whatsAppModal.quotedPlans.map(r => ({ ...r }));
    }

    // Default 1 plan row for Prospect Recap if empty
    if (isProspectRecap && this.quotedPlans.length === 0) {
      const today = new Date();
      const pad = (n) => String(n).padStart(2, '0');
      const todayIso = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`;
      const nextMonth = new Date(today);
      nextMonth.setDate(nextMonth.getDate() + 30);
      const nextMonthIso = `${nextMonth.getFullYear()}-${pad(nextMonth.getMonth() + 1)}-${pad(nextMonth.getDate())}`;
      const firstPlan = (availablePlans || []).find(p => p.isActive !== false);

      this.quotedPlans = [{
        id: `wa-plan-${Date.now()}-0`,
        planId: firstPlan ? firstPlan.id : '',
        customName: 'Custom Plan',
        customDurationVal: 1,
        customDurationUnit: 'months',
        startDate: todayIso,
        endDate: nextMonthIso,
        fee: firstPlan ? (firstPlan.price || 0) : 0,
        durationDays: firstPlan ? (firstPlan.durationDays || 30) : 30
      }];
    }

    // Auto-populate recap details if not yet typed
    if (isProspectRecap && !this.customParameters['CUSTOM.recap_details']) {
      this.customParameters['CUSTOM.recap_details'] = this.formatPlansRecap(this.quotedPlans);
    }

    // Dynamic custom placeholders detection
    const customFields = templateObj ? this.extractCustomPlaceholders(templateObj.text || templateObj.content) : [];
    customFields.forEach(f => {
      if (this.customParameters[f.rawKey] === undefined) {
        this.customParameters[f.rawKey] = '';
      }
    });

    // Formatting sample variables for live preview
    const sampleFirstName = primaryMember?.firstName || (primaryMember?.name ? primaryMember.name.trim().split(' ')[0] : 'Member');

    // Resolve expiring subscriptions/plans for PLAN.* variables
    const subs = Array.isArray(primaryMember?.subscriptions) && primaryMember.subscriptions.length > 0
      ? primaryMember.subscriptions
      : (Array.isArray(primaryMember?.plans) ? primaryMember.plans : []);

    let targetSubs = [];
    if (subs.length > 0) {
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      const lastDayOfMonth = new Date(today.getFullYear(), today.getMonth() + 1, 0);
      lastDayOfMonth.setHours(23, 59, 59, 999);

      // Active / upcoming subs (endDate >= today), sorted by endDate ascending (soonest first)
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
    const memberPlans = uniquePlanNames.length > 0
      ? uniquePlanNames.join(', ')
      : (typeof primaryMember?.plans === 'string' ? primaryMember.plans : (primaryMember?.plan || 'General Access'));

    const earliestTargetSub = targetSubs.length > 0
      ? targetSubs.reduce((min, s) => (!min || new Date(s.endDate) < new Date(min.endDate) ? s : min), null)
      : null;

    // Resolve target upcoming/unpaid payment installment for PAYMENT.* variables
    const unpaidPayments = (primaryMember?.payments || []).filter(p => !p.isPaid && p.status !== 'paid' && (Number(p.pendingAmount) > 0 || Number(p.amount) > Number(p.paidAmount || 0)));
    let targetPayment = paymentInfo || null;
    if (!targetPayment && unpaidPayments.length > 0) {
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
      : (primaryMember?.pendingAmount || 0);
    const formattedAmount = AppConfig.formatCurrency(paymentAmountVal);

    const formattedDueDate = targetPayment && targetPayment.dueDate && targetPayment.dueDate !== '—'
      ? HomeStore.formatDateDisplay(targetPayment.dueDate)
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

    const isPaymentTemplate = (this.currentTemplateKey === 'upcoming_payment_reminder' || this.currentTemplateKey === 'payment_overdue_reminder');
    const finalPlanName = (isPaymentTemplate && paymentPlanName) ? paymentPlanName : memberPlans;

    const targetExpiryDate = earliestTargetSub?.endDate || primaryMember?.expiryDate;
    const formattedExpiry = targetExpiryDate 
      ? HomeStore.formatDateDisplay(targetExpiryDate) 
      : 'soon';

    const calculatedDaysLeft = (() => {
      if (!targetExpiryDate) return 0;
      try {
        const exp = new Date(targetExpiryDate);
        if (isNaN(exp.getTime())) return 0;
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        exp.setHours(0, 0, 0, 0);
        return Math.max(0, Math.ceil((exp - today) / (1000 * 60 * 60 * 24)));
      } catch (_) { return 0; }
    })();

    const todayDate = new Date();
    todayDate.setHours(0, 0, 0, 0);

    const overduePayments = (primaryMember?.payments || []).filter(p => {
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
          const dueStr = HomeStore.formatDateDisplay(p.dueDate);
          return `• ${AppConfig.formatCurrency(remaining)} (Due: ${dueStr})`;
        }).join('\n')
      : '• No overdue installments on record.';

    const earliestOverdueDueDate = overduePayments.length > 0 ? overduePayments[0].dueDate : null;
    const overdueSince = earliestOverdueDueDate ? HomeStore.formatDateDisplay(earliestOverdueDueDate) : '—';

    const previewVars = {
      'USER.first_name': sampleFirstName,
      'USER.name': primaryMember?.name || 'Member',
      name: sampleFirstName,
      username: sampleFirstName,
      'GYM.name': AppConfig.getBrandName(),
      'GYM.payment_qr': AppConfig.gymPaymentQr || 'at the front desk',
      'PLAN.name': finalPlanName,
      plan: finalPlanName,
      'PLAN.expiry_date': formattedExpiry,
      expiryDate: formattedExpiry,
      'PLAN.days_left': calculatedDaysLeft,
      'PAYMENT.amount': formattedAmount,
      amount: formattedAmount,
      'PAYMENT.due_date': formattedDueDate,
      dueDate: formattedDueDate,
      'PAYMENT.days_left': paymentDaysLeft,
      'PAYMENT.receipt_id': (paymentInfo?.id || targetPayment?.id) ? `#REC-${String(paymentInfo?.id || targetPayment?.id).slice(-6).toUpperCase()}` : '#REC-INSTANT',
      'PAYMENT.mode': (paymentInfo?.paymentMethod || targetPayment?.paymentMethod) || 'UPI',
      'PAYMENT.paid_date': (paymentInfo?.paidDate || targetPayment?.paidDate) ? HomeStore.formatDateDisplay(paymentInfo?.paidDate || targetPayment?.paidDate) : HomeStore.formatDateDisplay(new Date()),
      'PAYMENT.total_paid': AppConfig.formatCurrency(primaryMember?.totalSpent || 0),
      'PAYMENT.pending_balance': this.currentTemplateKey === 'payment_overdue_reminder'
        ? AppConfig.formatCurrency(totalOverdue)
        : AppConfig.formatCurrency(primaryMember?.pendingAmount || 0),
      'PAYMENT.overdue_breakdown': overdueBreakdown,
      'PAYMENT.overdue_since': overdueSince,
      'PAYMENT.next_due_summary': (primaryMember?.pendingAmount && primaryMember.pendingAmount > 0)
        ? `Next Due: ${AppConfig.formatCurrency(primaryMember.pendingAmount)} due ${formattedDueDate}`
        : 'All current installments are fully cleared.'
    };

    customFields.forEach(f => {
      const val = this.customParameters[f.rawKey];
      previewVars[f.rawKey] = (val && val.trim()) ? val.trim() : `[${f.label}]`;
    });

    this.lastPreviewVars = previewVars;
    const rawPreviewText = templateObj ? AppConfig.formatTemplate(templateObj.text || templateObj.content, previewVars) : '';
    const renderedPreviewText = AppConfig.formatWhatsAppText(rawPreviewText);

    this.innerHTML = `
      <fl-modal open title="${isBulkMode ? 'Broadcast WhatsApp Campaign' : 'Send WhatsApp Message'}" max-width="640px" z-index="1200">
        <div style="display: flex; flex-direction: column; gap: 14px;">
          
          <!-- Recipient summary -->
          ${isBulkMode ? `
            <div style="background: #f0fdf4; border: 1.5px solid #86efac; border-radius: 8px; padding: 12px 14px; display: flex; justify-content: space-between; align-items: center;">
              <div>
                <div style="font-size: 14px; font-weight: 700; color: #166534;">
                  Broadcasting to ${targetRecipients.length} Selected Members
                </div>
                <div style="font-size: 12px; color: #15803d; margin-top: 2px;">
                  Each recipient will receive a personalized message with their own name.
                </div>
              </div>
              <div style="font-size: 11.5px; font-weight: 700; background: #dcfce7; color: #15803d; padding: 4px 10px; border-radius: 9999px;">
                BULK CAMPAIGN
              </div>
            </div>
          ` : `
            <div style="background: #f8fafc; border: 1px solid var(--fl-border, #e2e8f0); border-radius: 8px; padding: 10px 14px; display: flex; justify-content: space-between; align-items: center;">
              <div>
                <div style="font-size: 14px; font-weight: 700; color: #0f172a;">${primaryMember.name}</div>
                <div style="font-size: 12px; color: #64748b; margin-top: 2px;">Phone: ${primaryMember.phone}</div>
              </div>
              <div style="display: flex; gap: 6px; align-items: center;">
                ${isMemberHumanActive ? `
                  <div style="font-size: 11px; font-weight: 700; background: #fee2e2; color: #dc2626; border: 1px solid #fecdd3; padding: 3px 8px; border-radius: 6px; display: flex; align-items: center; gap: 4px;">
                    <span style="width: 6px; height: 6px; border-radius: 50%; background: #ef4444; display: inline-block;"></span>
                    Human Support Active
                  </div>
                ` : ''}
                ${(() => {
                  const mSubs = primaryMember.subscriptions || [];
                  const isPros = (primaryMember.status || '').toLowerCase() === 'prospect' || mSubs.length === 0;
                  const isExp = !isPros && ((primaryMember.status || '').toLowerCase() === 'expired' || (primaryMember.statusLabel || '').toLowerCase() === 'expired');

                  let text = 'Active';
                  let bg = '#ecfdf5';
                  let color = '#16a34a';
                  let border = '#a7f3d0';

                  if (isPros) {
                    text = 'Prospect';
                    bg = '#eff6ff';
                    color = '#1d4ed8';
                    border = '#bfdbfe';
                  } else if (isExp) {
                    text = 'Expired';
                    bg = '#f1f5f9';
                    color = '#475569';
                    border = '#cbd5e1';
                  }

                  return `
                    <div style="font-size: 11.5px; font-weight: 600; background: ${bg}; color: ${color}; border: 1px solid ${border}; padding: 2px 10px; border-radius: 9999px;">
                      ${text}
                    </div>
                  `;
                })()}
              </div>
            </div>

            ${isMemberHumanActive ? `
              <div style="
                background: #fffbeb;
                border: 1px solid #fde68a;
                border-radius: 8px;
                padding: 9px 12px;
                font-size: 12px;
                color: #92400e;
                display: flex;
                align-items: center;
                gap: 8px;
              ">
                <span style="font-size: 15px;">⚠️</span>
                <span>This member has an active human support session. Marketing campaigns are paused during support sessions. Utility notifications will be sent directly.</span>
              </div>
            ` : ''}
          `}

          <!-- Category filter tabs -->
          <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #f1f5f9; padding-bottom: 8px; flex-wrap: wrap; gap: 8px;">
            <label style="font-size: 12px; font-weight: 700; color: #475569; text-transform: uppercase; letter-spacing: 0.4px;">
              Choose Template
            </label>
            <div style="display: flex; gap: 6px; flex-wrap: wrap;">
              ${[
                { id: 'ALL', label: 'All' },
                { id: 'PROSPECT', label: 'Prospect' },
                { id: 'MEMBER', label: 'Member' },
                { id: 'PAYMENT', label: 'Payment' },
                { id: 'REQUEST', label: 'Request' }
              ].map(cat => {
                const isActive = this.selectedCategory === cat.id;
                return `
                  <button 
                    type="button" 
                    class="fl-wa-cat-btn ${isActive ? 'active' : ''}" 
                    data-cat="${cat.id}"
                    style="
                      padding: 3px 10px;
                      font-size: 11.5px;
                      font-weight: 600;
                      border-radius: 9999px;
                      cursor: pointer;
                      border: 1px solid ${isActive ? '#0f172a' : '#cbd5e1'};
                      background: ${isActive ? '#0f172a' : '#ffffff'};
                      color: ${isActive ? '#ffffff' : '#475569'};
                      transition: all 0.15s;
                    "
                  >
                    ${cat.label}
                  </button>
                `;
              }).join('')}
            </div>
          </div>

          <!-- Template selection grid -->
          ${filteredTemplates.length > 0 ? `
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 6px; max-height: 120px; overflow-y: auto; padding: 2px;">
              ${filteredTemplates.map(tpl => {
                const key = tpl.key || tpl.templateKey;
                const isSelected = key === this.currentTemplateKey;
                const isMarketing = (tpl.category || '').toUpperCase() === 'MARKETING';
                const isMarketingBlocked = isMemberHumanActive && isMarketing;
                return `
                  <button 
                    type="button" 
                    class="fl-wa-tpl-picker-btn ${isSelected ? 'active' : ''}" 
                    data-key="${key}"
                    ${isMarketingBlocked ? 'disabled title="Marketing templates cannot be sent during an active human support session"' : ''}
                    style="
                      text-align: left;
                      padding: 7px 10px;
                      border: 1px solid ${isSelected ? '#16a34a' : '#e2e8f0'};
                      background: ${isSelected ? '#f0fdf4' : (isMarketingBlocked ? '#f8fafc' : '#ffffff')};
                      color: ${isSelected ? '#15803d' : (isMarketingBlocked ? '#94a3b8' : '#1e293b')};
                      border-radius: 6px;
                      font-size: 12px;
                      font-weight: 600;
                      cursor: ${isMarketingBlocked ? 'not-allowed' : 'pointer'};
                      opacity: ${isMarketingBlocked ? '0.5' : '1'};
                      display: flex;
                      align-items: center;
                      justify-content: space-between;
                      transition: all 0.15s;
                    "
                  >
                    <span style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${tpl.title || tpl.name}</span>
                    <span style="
                      font-size: 9.5px;
                      padding: 1px 6px;
                      border-radius: 4px;
                      font-weight: 700;
                      text-transform: uppercase;
                      ${isMarketing ? 'background: #f3e8ff; color: #7e22ce; border: 1px solid #d8b4fe;' : 'background: #e0f2fe; color: #0369a1; border: 1px solid #bae6fd;'}
                    ">
                      ${(tpl.category || 'UTILITY').toUpperCase()}
                    </span>
                  </button>
                `;
              }).join('')}
            </div>
          ` : `
            <div style="background: #f8fafc; border: 1px dashed #cbd5e1; border-radius: 8px; padding: 10px 12px; font-size: 12px; color: #64748b;">
              No templates found in this category.
            </div>
          `}

          <!-- Interactive Plan & Pricing Builder (Only for Prospect Walk-in Recap) -->
          ${isProspectRecap ? `
            <div style="background: #f8fafc; border: 1.5px solid #cbd5e1; border-radius: 10px; padding: 14px 16px; display: flex; flex-direction: column; gap: 12px;">
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <div>
                  <span style="font-size: 12.5px; font-weight: 700; color: #0f172a; text-transform: uppercase; letter-spacing: 0.3px;">
                    📋 Quoted Plans & Pricing
                  </span>
                  <div style="font-size: 11.5px; color: #64748b; margin-top: 2px;">
                    Select the membership plans offered to this prospect
                  </div>
                </div>
                <button 
                  type="button" 
                  id="fl-wa-add-plan-row-btn"
                  style="
                    display: inline-flex;
                    align-items: center;
                    gap: 5px;
                    background: #ffffff;
                    color: #0f172a;
                    border: 1px solid #cbd5e1;
                    border-radius: 6px;
                    padding: 5px 12px;
                    font-size: 11.5px;
                    font-weight: 700;
                    cursor: pointer;
                  "
                >
                  + Add Another Plan
                </button>
              </div>

              <!-- Plan Rows List -->
              <div id="fl-wa-plan-rows-container" style="display: flex; flex-direction: column; gap: 10px; max-height: 220px; overflow-y: auto; padding-right: 2px;">
                ${this.quotedPlans.map((row, idx) => {
                  const isCustom = row.planId === '__CUSTOM__';
                  return `
                    <div class="fl-wa-plan-row-card" data-row-id="${row.id}" style="
                      background: #ffffff;
                      border: 1px solid #e2e8f0;
                      border-radius: 8px;
                      padding: 10px 12px;
                      box-shadow: 0 1px 2px rgba(0,0,0,0.03);
                    ">
                      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
                        <span style="font-size: 11px; font-weight: 700; color: #64748b; text-transform: uppercase;">
                          Plan #${idx + 1}
                        </span>
                        ${this.quotedPlans.length > 1 ? `
                          <button type="button" class="fl-wa-remove-plan-btn" data-row-id="${row.id}" style="
                            background: none;
                            border: none;
                            color: #dc2626;
                            font-size: 11px;
                            font-weight: 600;
                            cursor: pointer;
                            padding: 2px 4px;
                          ">
                            ✕ Remove
                          </button>
                        ` : ''}
                      </div>

                      <div style="display: grid; grid-template-columns: 2fr 1fr; gap: 8px;">
                        <div>
                          <label style="display: block; font-size: 11px; font-weight: 600; color: #334155; margin-bottom: 2px;">
                            Plan
                          </label>
                          <select 
                            class="fl-wa-plan-select" 
                            data-row-id="${row.id}"
                            style="width: 100%; height: 32px; padding: 0 8px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 12px; background: #ffffff;"
                          >
                            <option value="">Select a plan...</option>
                            ${(availablePlans || []).filter(p => p.isActive !== false).map(p => {
                              const durLabel = p.durationDays ? (p.durationDays === 1 ? '1 day' : `${p.durationDays} days`) : `${p.durationMonths || 1} mo`;
                              return `
                                <option value="${p.id}" ${row.planId === p.id ? 'selected' : ''}>
                                  ${p.name} — ${AppConfig.formatCurrency(p.price || 0)} (${durLabel})
                                </option>
                              `;
                            }).join('')}
                            <option value="__CUSTOM__" ${isCustom ? 'selected' : ''}>⚡ Custom Plan...</option>
                          </select>
                        </div>

                        <div>
                          <label style="display: block; font-size: 11px; font-weight: 600; color: #334155; margin-bottom: 2px;">
                            Quoted Fee (₹)
                          </label>
                          <input 
                            type="number" 
                            class="fl-wa-plan-fee-input" 
                            data-row-id="${row.id}"
                            value="${row.fee !== undefined ? row.fee : ''}" 
                            min="0"
                            placeholder="0"
                            style="width: 100%; height: 32px; padding: 0 8px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 12px; background: #ffffff;"
                          />
                        </div>
                      </div>

                      ${isCustom ? `
                        <div style="margin-top: 8px; padding-top: 8px; border-top: 1px dashed #e2e8f0; display: grid; grid-template-columns: 2fr 1fr 1fr; gap: 8px;">
                          <div>
                            <label style="display: block; font-size: 10.5px; font-weight: 600; color: #334155; margin-bottom: 2px;">
                              Plan Name
                            </label>
                            <input 
                              type="text" 
                              class="fl-wa-custom-name" 
                              data-row-id="${row.id}"
                              value="${row.customName || 'Custom Plan'}" 
                              style="width: 100%; height: 30px; padding: 0 8px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 11.5px;"
                            />
                          </div>
                          <div>
                            <label style="display: block; font-size: 10.5px; font-weight: 600; color: #334155; margin-bottom: 2px;">
                              Duration
                            </label>
                            <input 
                              type="number" 
                              class="fl-wa-custom-dur-val" 
                              data-row-id="${row.id}"
                              value="${row.customDurationVal || 1}" 
                              min="1"
                              style="width: 100%; height: 30px; padding: 0 8px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 11.5px;"
                            />
                          </div>
                          <div>
                            <label style="display: block; font-size: 10.5px; font-weight: 600; color: #334155; margin-bottom: 2px;">
                              Unit
                            </label>
                            <select 
                              class="fl-wa-custom-dur-unit" 
                              data-row-id="${row.id}"
                              style="width: 100%; height: 30px; padding: 0 6px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 11.5px;"
                            >
                              <option value="months" ${(row.customDurationUnit || 'months') === 'months' ? 'selected' : ''}>Months</option>
                              <option value="days" ${row.customDurationUnit === 'days' ? 'selected' : ''}>Days</option>
                            </select>
                          </div>
                        </div>
                      ` : ''}

                      <div style="margin-top: 8px; display: grid; grid-template-columns: 1fr 1fr; gap: 8px;">
                        <div>
                          <label style="display: block; font-size: 10.5px; font-weight: 600; color: #334155; margin-bottom: 2px;">
                            Start Date
                          </label>
                          <input 
                            type="date" 
                            class="fl-wa-plan-start-date" 
                            data-row-id="${row.id}"
                            value="${row.startDate || ''}" 
                            style="width: 100%; height: 30px; padding: 0 8px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 11.5px;"
                          />
                        </div>
                        <div>
                          <label style="display: block; font-size: 10.5px; font-weight: 600; color: #334155; margin-bottom: 2px;">
                            Valid Until
                          </label>
                          <input 
                            type="date" 
                            class="fl-wa-plan-end-date" 
                            data-row-id="${row.id}"
                            value="${row.endDate || ''}" 
                            style="width: 100%; height: 30px; padding: 0 8px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 11.5px;"
                          />
                        </div>
                      </div>
                    </div>
                  `;
                }).join('')}
              </div>

              <!-- Editable Recap Text Area -->
              <div style="margin-top: 4px; border-top: 1px solid #e2e8f0; padding-top: 10px;">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px;">
                  <label style="font-size: 11.5px; font-weight: 700; color: #334155;">
                    Recap Message Breakdown (Auto-formatted & Editable)
                  </label>
                  <span style="font-size: 10.5px; color: #64748b;">
                    Updates live preview below
                  </span>
                </div>
                <textarea 
                  id="fl-recap-text-input" 
                  rows="3" 
                  style="width: 100%; padding: 8px 10px; font-size: 12px; border: 1px solid #cbd5e1; border-radius: 6px; box-sizing: border-box; background: #ffffff; resize: vertical; outline: none; font-family: inherit; line-height: 1.45;"
                  placeholder="Quoted membership details..."
                >${this.customParameters['CUSTOM.recap_details'] || ''}</textarea>
              </div>
            </div>
          ` : (customFields.length > 0 ? `
            <!-- Standard Dynamic Custom Input Fields -->
            <div style="background: #faf5ff; border: 1.5px solid #d8b4fe; border-radius: 8px; padding: 12px 14px; display: flex; flex-direction: column; gap: 10px;">
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <span style="font-size: 12px; font-weight: 700; color: #6b21a8; text-transform: uppercase; letter-spacing: 0.3px;">
                  ✏️ Custom Campaign Details
                </span>
                <span style="font-size: 11px; color: #9333ea;">
                  Required for this template
                </span>
              </div>
              
              <div style="display: flex; flex-direction: column; gap: 8px;">
                ${customFields.map(field => {
                  const val = this.customParameters[field.rawKey] || '';
                  const isTextarea = field.name.toLowerCase().includes('details') || field.name.toLowerCase().includes('recap');

                  return `
                    <div>
                      <label style="display: block; font-size: 11.5px; font-weight: 600; color: #4c1d95; margin-bottom: 3px;">
                        ${field.label}
                      </label>
                      ${isTextarea ? `
                        <textarea 
                          class="fl-custom-param-input"
                          data-param="${field.rawKey}"
                          rows="2"
                          placeholder="Type ${field.label.toLowerCase()} here..."
                          style="width: 100%; padding: 6px 10px; font-size: 12px; border: 1px solid #c084fc; border-radius: 6px; box-sizing: border-box; background: #ffffff; resize: none; outline: none; font-family: inherit;"
                        >${val}</textarea>
                      ` : `
                        <input 
                          type="text" 
                          class="fl-custom-param-input"
                          data-param="${field.rawKey}"
                          value="${val}"
                          placeholder="Type ${field.label.toLowerCase()} here..."
                          style="width: 100%; height: 32px; padding: 0 10px; font-size: 12px; border: 1px solid #c084fc; border-radius: 6px; box-sizing: border-box; background: #ffffff; outline: none;"
                        />
                      `}
                    </div>
                  `;
                }).join('')}
              </div>
            </div>
          ` : '')}

          <!-- WhatsApp Message Bubble Preview or Payment Receipt Redirect Card -->
          ${isPaymentReceipt ? `
            <div style="
              background: #f8fafc;
              border: 1.5px dashed #cbd5e1;
              border-radius: 10px;
              padding: 24px 20px;
              text-align: center;
              display: flex;
              flex-direction: column;
              align-items: center;
              justify-content: center;
              gap: 12px;
            ">
              <div style="
                width: 44px;
                height: 44px;
                border-radius: 50%;
                background: #e0f2fe;
                color: #0284c7;
                display: flex;
                align-items: center;
                justify-content: center;
                font-size: 22px;
              ">
                🧾
              </div>
              <div style="max-width: 420px;">
                <div style="font-size: 14.5px; font-weight: 700; color: #0f172a; margin-bottom: 4px;">
                  Payment Receipt Generation
                </div>
                <div style="font-size: 13px; color: #64748b; line-height: 1.5;">
                  Official payment receipts are generated with an itemized PDF attachment linked directly to a recorded payment installment.
                </div>
              </div>
              <button 
                type="button" 
                id="fl-wa-open-member-modal-btn"
                style="
                  margin-top: 4px;
                  padding: 9px 18px;
                  background: #0f172a;
                  color: #ffffff;
                  border: none;
                  border-radius: 6px;
                  font-size: 13px;
                  font-weight: 700;
                  cursor: pointer;
                  display: inline-flex;
                  align-items: center;
                  gap: 8px;
                  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
                  transition: background 0.15s;
                "
                onmouseover="this.style.background='#1e293b';"
                onmouseout="this.style.background='#0f172a';"
              >
                <span>Select Payment for Receipt →</span>
              </button>
            </div>
          ` : `
            <div>
              <div style="margin-bottom: 6px;">
                <label style="font-size: 12px; font-weight: 700; color: #0f172a; text-transform: uppercase; letter-spacing: 0.3px;">
                  Message Preview
                </label>
              </div>

              <!-- WhatsApp Chat Area -->
              <div style="
                background: #efeae2;
                border: 1px solid #d1d7db;
                border-radius: 8px;
                padding: 14px 16px;
                display: flex;
                flex-direction: column;
              ">
                <!-- WhatsApp Message Bubble Card -->
                <div style="
                  background: #ffffff;
                  border-radius: 8px 8px 8px 2px;
                  box-shadow: 0 1px 2px rgba(0,0,0,0.12);
                  max-width: 90%;
                  overflow: hidden;
                ">
                  <!-- Image Media Attachment Header Preview -->
                  ${(templateObj && ((templateObj.mediaType || '').toUpperCase() === 'IMAGE' || templateObj.mediaUrl)) ? `
                    <div style="background: #f0f2f5; padding: 10px; display: flex; align-items: center; justify-content: center; border-bottom: 1px solid #e9edef;">
                      <img 
                        src="${templateObj.mediaUrl || '/alerts/media/med_upi_qr_code'}" 
                        alt="Media Preview" 
                        style="max-width: 180px; max-height: 180px; object-fit: contain; border-radius: 6px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); background: #ffffff;"
                      />
                    </div>
                  ` : ''}

                  <!-- Message Body -->
                  <div style="
                    padding: 12px 14px 10px 14px;
                    font-size: 13px;
                    line-height: 1.5;
                    color: #111b21;
                    white-space: pre-wrap;
                    word-break: break-word;
                  " id="fl-wa-bubble-content">${renderedPreviewText}</div>

                  <!-- Integrated Interactive WhatsApp Buttons -->
                  ${(templateObj && templateObj.buttons && templateObj.buttons.length > 0) ? `
                    <div style="border-top: 1px solid #e9edef; display: flex; flex-direction: column;">
                      ${templateObj.buttons.map((b, idx) => `
                        <div style="
                          border-top: ${idx > 0 ? '1px solid #e9edef' : 'none'};
                          padding: 9px 12px;
                          display: flex;
                          align-items: center;
                          justify-content: center;
                          gap: 6px;
                          color: #00a884;
                          font-size: 12.5px;
                          font-weight: 600;
                          background: #ffffff;
                          user-select: none;
                        ">
                          ${b.type === 'URL' ? `
                            <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor">
                              <path d="M19 19H5V5h7V3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7h-2v7zM14 3v2h3.59l-9.83 9.83 1.41 1.41L19 6.41V10h2V3h-7z"/>
                            </svg>
                          ` : (b.type === 'PHONE_NUMBER' ? `
                            <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor">
                              <path d="M6.62 10.79a15.05 15.05 0 0 0 6.59 6.59l2.2-2.2c.27-.27.67-.36 1.02-.24 1.12.37 2.33.57 3.57.57.55 0 1 .45 1 1V20c0 .55-.45 1-1 1-9.39 0-17-7.61-17-17 0-.55.45-1 1-1h3.5c.55 0 1 .45 1 1 0 1.25.2 2.45.57 3.57.11.35.03.74-.25 1.02l-2.2 2.2z"/>
                            </svg>
                          ` : `
                            <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor">
                              <path d="M10 9V5l-7 7 7 7v-4.1c5 0 8.5 1.6 11 5.1-1-5-4-10-11-11z"/>
                            </svg>
                          `)}
                          <span>${b.text || b.buttonText}</span>
                        </div>
                      `).join('')}
                    </div>
                  ` : ''}
                </div>
              </div>
            </div>
          `}

          <!-- Action Buttons -->
          <div style="display: flex; justify-content: space-between; align-items: center; border-top: 1px solid #e2e8f0; padding-top: 12px; margin-top: 4px;">
            ${(!isBulkMode && HomeStore.getState().activeSection !== 'chats') ? `
              <button 
                type="button" 
                id="fl-wa-go-to-chat-btn" 
                class="fl-btn fl-btn-secondary"
                style="padding: 8px 14px; font-size: 12.5px;"
              >
                ${Icons.whatsapp || ''} <span>Open Full Chat</span>
              </button>
            ` : '<div></div>'}

            <div style="display: flex; gap: 8px;">
              <button 
                type="button" 
                id="fl-wa-cancel-btn" 
                class="fl-btn fl-btn-secondary"
                style="padding: 8px 14px; font-size: 12.5px;"
              >
                Cancel
              </button>
              ${!isPaymentReceipt ? `
                <button 
                  type="button" 
                  id="fl-wa-send-btn" 
                  class="fl-btn fl-btn-whatsapp"
                  style="padding: 8px 18px; font-size: 12.5px;"
                >
                  ${isBulkMode ? `Broadcast to ${targetRecipients.length} Members →` : 'Send WhatsApp Message →'}
                </button>
              ` : ''}
            </div>
          </div>

        </div>
      </fl-modal>
    `;

    this.bindEvents(primaryMember, templateObj, isBulkMode, targetRecipients, previewVars, customFields);
  }

  bindEvents(primaryMember, templateObj, isBulkMode, targetRecipients, previewVars, customFields = []) {
    const modal = this.querySelector('fl-modal');
    if (modal) {
      const handleClose = () => HomeStore.closeWhatsAppModal(true);
      modal.addEventListener('close', handleClose);
      modal.addEventListener('fl-modal-close', handleClose);
    }

    const cancelBtn = this.querySelector('#fl-wa-cancel-btn');
    if (cancelBtn) {
      cancelBtn.addEventListener('click', () => HomeStore.closeWhatsAppModal(true));
    }

    const openMemberModalBtn = this.querySelector('#fl-wa-open-member-modal-btn');
    if (openMemberModalBtn) {
      openMemberModalBtn.addEventListener('click', () => {
        HomeStore.closeWhatsAppModal(true);
        if (primaryMember) {
          HomeStore.openMemberDrawer(primaryMember);
        }
      });
    }

    const goToChatBtn = this.querySelector('#fl-wa-go-to-chat-btn');
    if (goToChatBtn && primaryMember) {
      goToChatBtn.addEventListener('click', () => {
        HomeStore.setActiveChatId(primaryMember.id);
        HomeStore.setActiveSection('chats');
        window.location.hash = '#/chats';
        HomeStore.closeWhatsAppModal(true);
      });
    }

    // Category filter tabs
    this.querySelectorAll('.fl-wa-cat-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        this.selectedCategory = btn.getAttribute('data-cat');
        this.render();
      });
    });

    // Template selection clicks
    this.querySelectorAll('.fl-wa-tpl-picker-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        this.currentTemplateKey = btn.getAttribute('data-key');
        this.render();
      });
    });

    // Dynamic custom input changes
    this.querySelectorAll('.fl-custom-param-input').forEach(input => {
      input.addEventListener('input', (e) => {
        const paramKey = e.target.getAttribute('data-param');
        if (paramKey) {
          this.customParameters[paramKey] = e.target.value;
          this.updateBubblePreview(templateObj, previewVars);
        }
      });
    });

    // Quoted Plans Builder events (Prospect Walk-in Recap)
    const addPlanRowBtn = this.querySelector('#fl-wa-add-plan-row-btn');
    if (addPlanRowBtn) {
      addPlanRowBtn.addEventListener('click', () => {
        const today = new Date();
        const pad = (n) => String(n).padStart(2, '0');
        const todayIso = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`;
        const nextMonth = new Date(today);
        nextMonth.setDate(nextMonth.getDate() + 30);
        const nextMonthIso = `${nextMonth.getFullYear()}-${pad(nextMonth.getMonth() + 1)}-${pad(nextMonth.getDate())}`;
        const { availablePlans } = HomeStore.getState();
        const firstPlan = (availablePlans || []).find(p => p.isActive !== false);

        this.quotedPlans.push({
          id: `wa-plan-${Date.now()}-${this.quotedPlans.length}`,
          planId: firstPlan ? firstPlan.id : '',
          customName: 'Custom Plan',
          customDurationVal: 1,
          customDurationUnit: 'months',
          startDate: todayIso,
          endDate: nextMonthIso,
          fee: firstPlan ? (firstPlan.price || 0) : 0,
          durationDays: firstPlan ? (firstPlan.durationDays || 30) : 30
        });

        this.updateQuotedPlansRecap(templateObj, previewVars);
        this.render();
      });
    }

    this.querySelectorAll('.fl-wa-remove-plan-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const rowId = btn.getAttribute('data-row-id');
        this.quotedPlans = this.quotedPlans.filter(p => p.id !== rowId);
        this.updateQuotedPlansRecap(templateObj, previewVars);
        this.render();
      });
    });

    this.querySelectorAll('.fl-wa-plan-select').forEach(sel => {
      sel.addEventListener('change', () => {
        const rowId = sel.getAttribute('data-row-id');
        const row = this.quotedPlans.find(r => r.id === rowId);
        if (row) {
          row.planId = sel.value;
          const { availablePlans } = HomeStore.getState();
          const found = (availablePlans || []).find(p => p.id === sel.value);
          if (found) {
            row.fee = found.price || 0;
            row.durationDays = found.durationDays || (found.durationMonths ? found.durationMonths * 30 : 30);
            const start = new Date(row.startDate || new Date());
            const end = new Date(start);
            end.setDate(end.getDate() + row.durationDays);
            const pad = (n) => String(n).padStart(2, '0');
            row.endDate = `${end.getFullYear()}-${pad(end.getMonth() + 1)}-${pad(end.getDate())}`;
          } else if (sel.value === '__CUSTOM__') {
            row.fee = 0;
            row.durationDays = 30;
          }
          this.updateQuotedPlansRecap(templateObj, previewVars);
          this.render();
        }
      });
    });

    this.querySelectorAll('.fl-wa-plan-fee-input').forEach(input => {
      input.addEventListener('input', () => {
        const rowId = input.getAttribute('data-row-id');
        const row = this.quotedPlans.find(r => r.id === rowId);
        if (row) {
          row.fee = Number(input.value) || 0;
          this.updateQuotedPlansRecap(templateObj, previewVars);
        }
      });
    });

    this.querySelectorAll('.fl-wa-custom-name').forEach(input => {
      input.addEventListener('input', () => {
        const rowId = input.getAttribute('data-row-id');
        const row = this.quotedPlans.find(r => r.id === rowId);
        if (row) {
          row.customName = input.value;
          this.updateQuotedPlansRecap(templateObj, previewVars);
        }
      });
    });

    this.querySelectorAll('.fl-wa-custom-dur-val').forEach(input => {
      input.addEventListener('input', () => {
        const rowId = input.getAttribute('data-row-id');
        const row = this.quotedPlans.find(r => r.id === rowId);
        if (row) {
          row.customDurationVal = Number(input.value) || 1;
          row.durationDays = row.customDurationUnit === 'months' ? row.customDurationVal * 30 : row.customDurationVal;
          const start = new Date(row.startDate || new Date());
          const end = new Date(start);
          end.setDate(end.getDate() + row.durationDays);
          const pad = (n) => String(n).padStart(2, '0');
          row.endDate = `${end.getFullYear()}-${pad(end.getMonth() + 1)}-${pad(end.getDate())}`;
          this.updateQuotedPlansRecap(templateObj, previewVars);
          const endEl = this.querySelector(`.fl-wa-plan-end-date[data-row-id="${rowId}"]`);
          if (endEl) endEl.value = row.endDate;
        }
      });
    });

    this.querySelectorAll('.fl-wa-custom-dur-unit').forEach(sel => {
      sel.addEventListener('change', () => {
        const rowId = sel.getAttribute('data-row-id');
        const row = this.quotedPlans.find(r => r.id === rowId);
        if (row) {
          row.customDurationUnit = sel.value;
          row.durationDays = row.customDurationUnit === 'months' ? (row.customDurationVal || 1) * 30 : (row.customDurationVal || 1);
          const start = new Date(row.startDate || new Date());
          const end = new Date(start);
          end.setDate(end.getDate() + row.durationDays);
          const pad = (n) => String(n).padStart(2, '0');
          row.endDate = `${end.getFullYear()}-${pad(end.getMonth() + 1)}-${pad(end.getDate())}`;
          this.updateQuotedPlansRecap(templateObj, previewVars);
          const endEl = this.querySelector(`.fl-wa-plan-end-date[data-row-id="${rowId}"]`);
          if (endEl) endEl.value = row.endDate;
        }
      });
    });

    this.querySelectorAll('.fl-wa-plan-start-date').forEach(input => {
      input.addEventListener('input', () => {
        const rowId = input.getAttribute('data-row-id');
        const row = this.quotedPlans.find(r => r.id === rowId);
        if (row) {
          row.startDate = input.value;
          if (row.durationDays) {
            const start = new Date(row.startDate);
            const end = new Date(start);
            end.setDate(end.getDate() + row.durationDays);
            const pad = (n) => String(n).padStart(2, '0');
            row.endDate = `${end.getFullYear()}-${pad(end.getMonth() + 1)}-${pad(end.getDate())}`;
            const endEl = this.querySelector(`.fl-wa-plan-end-date[data-row-id="${rowId}"]`);
            if (endEl) endEl.value = row.endDate;
          }
          this.updateQuotedPlansRecap(templateObj, previewVars);
        }
      });
    });

    this.querySelectorAll('.fl-wa-plan-end-date').forEach(input => {
      input.addEventListener('input', () => {
        const rowId = input.getAttribute('data-row-id');
        const row = this.quotedPlans.find(r => r.id === rowId);
        if (row) {
          row.endDate = input.value;
          this.updateQuotedPlansRecap(templateObj, previewVars);
        }
      });
    });

    // Editable recap text area input
    const recapTextarea = this.querySelector('#fl-recap-text-input');
    if (recapTextarea) {
      recapTextarea.addEventListener('input', (e) => {
        this.customParameters['CUSTOM.recap_details'] = e.target.value;
        this.updateBubblePreview(templateObj, previewVars);
      });
    }

    // Send button click
    const sendBtn = this.querySelector('#fl-wa-send-btn');
    if (sendBtn) {
      sendBtn.addEventListener('click', async () => {
        if (!templateObj) return;

        const missing = customFields.filter(f => !this.customParameters[f.rawKey] || !this.customParameters[f.rawKey].trim());
        if (missing.length > 0) {
          ErrorMapper.showErrorWindow(`Please enter ${missing.map(m => m.label).join(' and ')} before sending.`, 'Missing Fields');
          return;
        }

        // Block sending marketing templates if recipient is in active human support session
        const { conversations } = HomeStore.getState();
        const conv = primaryMember ? (conversations || []).find(c => c.id === (primaryMember.id || primaryMember.userId)) : null;
        if (conv && conv.isHumanRequired && (templateObj.category || '').toUpperCase() === 'MARKETING') {
          ErrorMapper.showErrorWindow('Marketing campaign templates cannot be dispatched to a customer while an active human support session is running. Please send a direct message or utility notification instead.', 'Marketing Campaign Blocked');
          return;
        }

        sendBtn.disabled = true;
        sendBtn.innerText = 'Sending...';

        try {
          if (isBulkMode) {
            await HomeStore.sendBulkTemplateMessage(templateObj, this.customParameters, targetRecipients);
          } else {
            await HomeStore.sendTemplateMessage(primaryMember.id, templateObj, null, this.customParameters);
          }
          HomeStore.closeWhatsAppModal(false);
          ErrorMapper.notifySuccess('Quote sent via WhatsApp successfully!');
        } catch (err) {
          ErrorMapper.handle(err);
        } finally {
          sendBtn.disabled = false;
        }
      });
    }
  }

  updateQuotedPlansRecap(templateObj, previewVars) {
    this.customParameters['CUSTOM.recap_details'] = this.formatPlansRecap(this.quotedPlans);
    const textarea = this.querySelector('#fl-recap-text-input');
    if (textarea) {
      textarea.value = this.customParameters['CUSTOM.recap_details'];
    }
    this.updateBubblePreview(templateObj, previewVars);
  }

  updateBubblePreview(templateObj, previewVars) {
    const bubbleEl = this.querySelector('#fl-wa-bubble-content');
    if (!bubbleEl || !templateObj) return;

    const currentVars = { ...(previewVars || this.lastPreviewVars || {}) };
    const customFields = this.extractCustomPlaceholders(templateObj.text || templateObj.content);
    customFields.forEach(f => {
      const val = this.customParameters[f.rawKey];
      currentVars[f.rawKey] = (val && val.trim()) ? val.trim() : `[${f.label}]`;
    });

    const rawText = AppConfig.formatTemplate(templateObj.text || templateObj.content, currentVars);
    bubbleEl.innerHTML = AppConfig.formatWhatsAppText(rawText);
  }
}

customElements.define('fl-whatsapp-modal', FlWhatsAppModal);
