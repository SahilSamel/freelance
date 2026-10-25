import { api } from '../api/client.js';
import { SessionManager } from '../auth/session.js';

class AppConfigClass {
  constructor() {
    this.reset();
    this.init();
  }

  /**
   * Resets branding and configuration back to clean generic defaults.
   * Ensures no cross-tenant leakage occurs on logout, failed fetches, or tenant switching.
   */
  reset() {
    this.brandName = 'Fitness Center';
    this.brandSubtitle = 'OPERATIONS MANAGEMENT';
    this.brandLogoUrl = '';
    this.countryCode = '91';
    this.gymTimings = 'Mon-Sat 6:00 AM - 10:00 PM';
    this.gymSupportPhone = '+91 98765 43210';
    this.gymAddress = 'Platinum Hub, Link Road';
    this.gymGoogleMapsUrl = 'https://maps.google.com';
    this.gymWhatsappChannelUrl = 'https://whatsapp.com/channel/ironpulse';
    this.gymPaymentQr = '';
    this.backendTemplates = [];
    this._loadedTenant = null;

    if (typeof document !== 'undefined') {
      document.title = `${this.brandName} - Management Console`;
    }

    this.notifyConfigChanged();
  }

  getBrandName() {
    return this.brandName;
  }

  getBrandSubtitle() {
    return this.brandSubtitle;
  }

  getBrandLogoUrl() {
    return this.brandLogoUrl;
  }

  getCountryCode() {
    return this.countryCode;
  }

  getCurrencySymbol() {
    try {
      const parts = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).formatToParts(0);
      const symbolPart = parts.find(p => p.type === 'currency');
      return symbolPart ? symbolPart.value : '₹';
    } catch {
      return '₹';
    }
  }

  formatCurrency(amount) {
    const num = Number(amount) || 0;
    try {
      return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        maximumFractionDigits: 0
      }).format(num);
    } catch {
      return '₹' + num.toLocaleString('en-IN');
    }
  }

  formatWhatsAppText(text) {
    if (!text) return '';
    let escaped = String(text)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');

    // Code blocks: ```code```
    escaped = escaped.replace(/```([^`]+)```/g, '<code style="font-family: monospace; font-size: 12.5px; background: rgba(0,0,0,0.06); padding: 1px 4px; border-radius: 3px;">$1</code>');
    // Bold: *bold text* (support emojis and punctuation on either side)
    escaped = escaped.replace(/(^|[^\p{L}\p{N}])\*([^*\r\n]+)\*([^\p{L}\p{N}]|$)/gu, '$1<strong style="font-weight: 700;">$2</strong>$3');
    // Italic: _italic text_
    escaped = escaped.replace(/(^|[^\p{L}\p{N}])_([^_\r\n]+)_([^\p{L}\p{N}]|$)/gu, '$1<em>$2</em>$3');
    // Strikethrough: ~strikethrough~
    escaped = escaped.replace(/(^|[^\p{L}\p{N}])~([^~\r\n]+)~([^\p{L}\p{N}]|$)/gu, '$1<del style="text-decoration: line-through;">$2</del>$3');

    // URLs and UPI payment links
    escaped = escaped.replace(/(upi:\/\/[^\s\)<>]+)/g, '<a href="$1" style="color: #027eb5; text-decoration: none; word-break: break-all;">$1</a>');
    escaped = escaped.replace(/(https?:\/\/[^\s\)<>]+)/g, '<a href="$1" target="_blank" rel="noopener noreferrer" style="color: #027eb5; text-decoration: none; word-break: break-all;">$1</a>');

    return escaped;
  }

  formatTemplate(templateString, vars = {}) {
    if (!templateString) return '';
    let result = templateString;

    // Helper to calculate days left if expiry date is available
    let computedDaysLeft = vars['PLAN.days_left'] !== undefined ? vars['PLAN.days_left'] : (vars.daysLeft !== undefined ? vars.daysLeft : null);
    const expiryVal = vars['PLAN.expiry_date'] || vars.expiryDate;
    if ((computedDaysLeft === null || computedDaysLeft === undefined) && expiryVal && expiryVal !== '-') {
      try {
        const expDate = new Date(expiryVal);
        if (!isNaN(expDate.getTime())) {
          const today = new Date();
          today.setHours(0, 0, 0, 0);
          const exp = new Date(expDate);
          exp.setHours(0, 0, 0, 0);
          const diffDays = Math.ceil((exp - today) / (1000 * 60 * 60 * 24));
          computedDaysLeft = Math.max(0, diffDays);
        }
      } catch (_) {}
    }
    if (computedDaysLeft === null || computedDaysLeft === undefined) {
      computedDaysLeft = 0;
    }

    const allVars = {
      brand: this.getBrandName(),
      brandName: this.getBrandName(),
      'GYM.name': this.getBrandName(),
      'GYM.timings': this.gymTimings || 'Mon-Sat 6:00 AM - 10:00 PM',
      'GYM.support_phone': this.gymSupportPhone || '+91 98765 43210',
      'GYM.address': this.gymAddress || 'Platinum Hub, Link Road',
      'GYM.google_maps_url': this.gymGoogleMapsUrl || 'https://maps.google.com',
      'GYM.whatsapp_channel_url': this.gymWhatsappChannelUrl || 'https://whatsapp.com/channel/ironpulse',
      'GYM.payment_qr': this.gymPaymentQr || `upi://pay?pa=ironpulse@upi&pn=${encodeURIComponent(this.getBrandName() || 'Fitness Center')}`,
      currency: this.getCurrencySymbol(),
      // Smart domain fallbacks:
      'USER.first_name': vars['USER.first_name'] || (vars.name ? String(vars.name).trim().split(' ')[0] : 'Member'),
      'USER.name': vars['USER.name'] || vars.name || vars.username || 'Member',
      'PLAN.name': vars['PLAN.name'] || vars.plan || 'General Access',
      'PLAN.expiry_date': vars['PLAN.expiry_date'] || vars.expiryDate || '-',
      'PLAN.days_left': computedDaysLeft,
      'PAYMENT.amount': vars['PAYMENT.amount'] || vars.amount || this.formatCurrency(0),
      'PAYMENT.due_date': vars['PAYMENT.due_date'] || vars.dueDate || 'soon',
      ...vars
    };
    for (const [k, v] of Object.entries(allVars)) {
      if (v !== undefined && v !== null) {
        const escapedKey = k.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
        result = result.replace(new RegExp(`\\{\\{${escapedKey}\\}\\}`, 'gi'), String(v));
      }
    }
    return result;
  }

  getDefaultTemplates() {
    return [];
  }

  getQuickTemplates() {
    if (this.backendTemplates && this.backendTemplates.length > 0) {
      return this.backendTemplates;
    }
    return [];
  }

  setBranding({ name, subtitle, logoUrl } = {}) {
    this.brandName = name || 'Fitness Center';
    this.brandSubtitle = subtitle || 'OPERATIONS MANAGEMENT';
    this.brandLogoUrl = logoUrl || '';

    if (typeof document !== 'undefined' && this.brandName) {
      document.title = `${this.brandName} - Management Console`;
    }

    this.notifyConfigChanged();
  }

  notifyConfigChanged() {
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('fl:config-changed', {
        detail: {
          brandName: this.brandName,
          brandSubtitle: this.brandSubtitle,
          brandLogoUrl: this.brandLogoUrl,
          currencySymbol: this.getCurrencySymbol()
        }
      }));
    }
  }

  async loadTemplatesFromBackend(targetTenant) {
    if (!targetTenant) {
      this.backendTemplates = [];
      return;
    }
    try {
      const res = await api.post('/alerts/templates/list', { status: 'APPROVED' }, {
        headers: { 'X-Tenant-ID': targetTenant },
        silentError: true
      });

      if (res && Array.isArray(res.templates)) {
        this.backendTemplates = res.templates
          .filter(t => (t.channel || 'WHATSAPP').toUpperCase() === 'WHATSAPP')
          .map(t => ({
            id: t.id,
            key: t.templateKey || t.id,
            templateKey: t.templateKey || t.id,
            title: t.name || t.templateKey,
            name: t.name || t.templateKey,
            category: t.category || 'UTILITY',
            text: t.content || '',
            content: t.content || '',
            mediaUrl: t.mediaUrl || null,
            mediaType: t.mediaType || null,
            status: t.status || 'APPROVED',
            dispatchScope: t.dispatchScope || 'BOTH',
            requiredAttributes: t.requiredAttributes || [],
            buttons: t.buttons || []
          }));
      } else {
        this.backendTemplates = [];
      }
    } catch {
      this.backendTemplates = [];
    }
  }

  async loadFromBackend(tenantId = null, force = false) {
    const targetTenant = tenantId || SessionManager.getTenantId();
    if (!targetTenant) {
      this.reset();
      return;
    }

    if (!force && this._loadedTenant === targetTenant) {
      return;
    }

    try {
      const res = await api.post('/configurations/system/get', {
        keys: [
          'gym.brand.name',
          'gym.brand.subtitle',
          'gym.brand.logoUrl',
          'gym.profile.timings',
          'gym.profile.support_phone',
          'gym.profile.address',
          'gym.profile.google_maps_url',
          'gym.profile.whatsapp_channel_url',
          'gym.profile.upi_qr_media_id'
        ]
      }, {
        headers: { 'X-Tenant-ID': targetTenant },
        silentError: true
      });

      if (res && res.values) {
        this.setBranding({
          name: res.values['gym.brand.name'],
          subtitle: res.values['gym.brand.subtitle'],
          logoUrl: res.values['gym.brand.logoUrl']
        });
        if (res.values['gym.profile.timings']) this.gymTimings = res.values['gym.profile.timings'];
        if (res.values['gym.profile.support_phone']) this.gymSupportPhone = res.values['gym.profile.support_phone'];
        if (res.values['gym.profile.address']) this.gymAddress = res.values['gym.profile.address'];
        if (res.values['gym.profile.google_maps_url']) this.gymGoogleMapsUrl = res.values['gym.profile.google_maps_url'];
        if (res.values['gym.profile.whatsapp_channel_url']) this.gymWhatsappChannelUrl = res.values['gym.profile.whatsapp_channel_url'];
        if (res.values['gym.profile.upi_qr_media_id']) this.gymPaymentQr = res.values['gym.profile.upi_qr_media_id'];
      } else {
        // Tenant unconfigured - fallback to clean defaults to prevent cross-tenant data leak
        this.reset();
      }
    } catch {
      // Backend not reached or error - reset to clean defaults to ensure no other gym's branding leaks
      this.reset();
    }

    // Also fetch templates from alerts service if available
    await this.loadTemplatesFromBackend(targetTenant);
    this._loadedTenant = targetTenant;
  }

  init() {
    if (typeof window !== 'undefined') {
      if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => this.loadFromBackend());
      } else {
        this.loadFromBackend();
      }

      window.addEventListener('fl:auth-changed', (event) => {
        if (event && event.detail && event.detail.isAuthenticated) {
          this.loadFromBackend();
        } else {
          this.reset();
        }
      });
    }
  }
}

export const AppConfig = new AppConfigClass();
