import { api } from '../api/client.js';
import { SessionManager } from '../auth/session.js';

class AppConfigClass {
  constructor() {
    this.reset();
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
    this.backendTemplates = [];

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

  formatTemplate(templateString, vars = {}) {
    if (!templateString) return '';
    let result = templateString;
    const allVars = {
      brand: this.getBrandName(),
      brandName: this.getBrandName(),
      currency: this.getCurrencySymbol(),
      ...vars
    };
    for (const [k, v] of Object.entries(allVars)) {
      result = result.replace(new RegExp(`\\{\\{${k}\\}\\}`, 'g'), v !== undefined && v !== null ? String(v) : '');
    }
    return result;
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
        this.backendTemplates = res.templates.map(t => ({
          key: t.templateKey || t.id,
          title: t.name || t.templateKey,
          text: t.content || '',
          status: t.status || 'APPROVED'
        }));
      } else {
        this.backendTemplates = [];
      }
    } catch {
      this.backendTemplates = [];
    }
  }

  async loadFromBackend(tenantId = null) {
    const targetTenant = tenantId || SessionManager.getTenantId();
    if (!targetTenant) {
      this.reset();
      return;
    }

    try {
      const res = await api.post('/configurations/system/get', {
        keys: [
          'gym.brand.name',
          'gym.brand.subtitle',
          'gym.brand.logoUrl'
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
