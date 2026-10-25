/**
 * <fl-navbar> Native Web Component
 * App Shell Header with dynamic branding, data-driven section tabs, and user profile menu.
 */

import { Icons } from '../../../assets/icons/icons.js';
import { SessionManager } from '../../auth/session.js';
import { AppConfig } from '../../config/app-config.js';
import { getAllSections } from '../../../pages/home/section-registry.js';
import { HomeStore } from '../../../pages/home/home-store.js';
import './fl-tabs.js';
import './fl-avatar.js';

export class FlNavbar extends HTMLElement {
  static get observedAttributes() {
    return ['title', 'subtitle', 'active-tab'];
  }

  connectedCallback() {
    this.brandListener = () => this.render();
    window.addEventListener('fl:brand-changed', this.brandListener);
    window.addEventListener('fl:config-changed', this.brandListener);

    this.sectionsListener = () => this.render();
    window.addEventListener('fl:sections-updated', this.sectionsListener);

    this.badgeListener = (e) => {
      window._fl_chat_attention_count = e.detail?.count || 0;
      this.render();
    };
    window.addEventListener('fl:chat-badge-update', this.badgeListener);

    this.storeUnsub = HomeStore.subscribe(() => this.render());

    this.render();
  }

  disconnectedCallback() {
    if (this.brandListener) {
      window.removeEventListener('fl:brand-changed', this.brandListener);
      window.removeEventListener('fl:config-changed', this.brandListener);
    }
    if (this.sectionsListener) {
      window.removeEventListener('fl:sections-updated', this.sectionsListener);
    }
    if (this.badgeListener) {
      window.removeEventListener('fl:chat-badge-update', this.badgeListener);
    }
    if (this.storeUnsub) {
      this.storeUnsub();
    }
  }

  attributeChangedCallback() {
    this.render();
  }

  render() {
    const title = this.getAttribute('title') || AppConfig.getBrandName();
    const subtitle = this.getAttribute('subtitle') || AppConfig.getBrandSubtitle();
    const logoUrl = AppConfig.getBrandLogoUrl();
    const activeTab = this.getAttribute('active-tab') || 'members';
    const user = SessionManager.getUser() || { username: 'Operator', name: 'Operator' };
    const chatBadge = window._fl_chat_attention_count > 0 ? String(window._fl_chat_attention_count) : '';
    const sections = getAllSections();

    const homeState = HomeStore.getState();
    const isSyncing = homeState.syncingGymData;
    const lastSynced = homeState.lastSyncedAt;
    let syncTimeText = 'Not synced';
    if (lastSynced) {
      const diffSec = Math.floor((Date.now() - lastSynced) / 1000);
      if (diffSec < 60) syncTimeText = 'Just now';
      else if (diffSec < 3600) syncTimeText = `${Math.floor(diffSec / 60)}m ago`;
      else {
        const d = new Date(lastSynced);
        syncTimeText = d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      }
    }

    this.style.display = 'block';
    this.style.backgroundColor = '#ffffff';
    this.style.borderBottom = '1px solid var(--fl-border, #e2e8f0)';
    this.style.padding = '0 24px';
    this.style.position = 'sticky';
    this.style.top = '0';
    this.style.zIndex = 'var(--fl-z-sticky, 200)';
    this.style.boxShadow = 'var(--fl-shadow-xs)';

    this.innerHTML = `
      <div style="
        max-width: 1440px;
        margin: 0 auto;
        height: 64px;
        display: flex;
        align-items: center;
        justify-content: space-between;
      ">
        <!-- Brand Block -->
        <div style="display: flex; align-items: center; gap: 12px; cursor: pointer;" id="fl-brand-block">
          <div style="
            width: 40px;
            height: 40px;
            border-radius: var(--fl-radius-md, 8px);
            background: #ffffff;
            border: 1px solid var(--fl-border, #e2e8f0);
            display: flex;
            align-items: center;
            justify-content: center;
            box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
            overflow: hidden;
            padding: 3px;
          ">
            ${logoUrl ? `<img src="${logoUrl}" alt="${title}" style="width:100%; height:100%; object-fit:contain;" />` : Icons.logo}
          </div>
          <div style="display: flex; flex-direction: column;">
            <span style="font-size: 17px; font-weight: 700; color: var(--fl-text, #0f172a); line-height: 1.1;">
              ${title}
            </span>
            <span style="font-size: 10px; font-weight: 600; color: var(--fl-text-muted, #64748b); letter-spacing: 0.5px; text-transform: uppercase;">
              ${subtitle}
            </span>
          </div>
        </div>

        <!-- Center Empty Spacer -->
        <div style="flex: 1;"></div>

        <!-- Right User Actions -->
        <div style="display: flex; align-items: center; gap: 14px;">
          <!-- Prominent Sync Button -->
          <button 
            type="button" 
            id="fl-navbar-sync-btn" 
            title="Refresh Members, Plans & Payments (${syncTimeText})"
            style="
              height: 36px;
              padding: 0 13px;
              display: flex;
              align-items: center;
              gap: 7px;
              background: #f8fafc;
              border: 1px solid #cbd5e1;
              border-radius: 8px;
              color: #0f172a;
              font-size: 13px;
              font-weight: 600;
              cursor: ${isSyncing ? 'not-allowed' : 'pointer'};
              transition: all 0.15s ease;
              box-shadow: 0 1px 2px rgba(0,0,0,0.04);
            "
            onmouseover="this.style.background='#f1f5f9'; this.style.borderColor='#94a3b8';"
            onmouseout="this.style.background='#f8fafc'; this.style.borderColor='#cbd5e1';"
            ${isSyncing ? 'disabled' : ''}
          >
            <span class="${isSyncing ? 'fl-sync-spin' : ''}" style="display: inline-flex; color: #0f172a;">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.2"/>
              </svg>
            </span>
            <span>Sync Data</span>
            <span style="font-size: 11px; font-weight: 500; color: #64748b; margin-left: 2px;">
              ${isSyncing ? 'Syncing...' : syncTimeText}
            </span>
          </button>

          <!-- Notification Bell -->
          <div style="
            position: relative;
            width: 36px;
            height: 36px;
            border-radius: 50%;
            background: #f1f5f9;
            color: var(--fl-text-secondary, #475569);
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            transition: background var(--fl-transition-fast);
          " class="fl-clickable" id="fl-bell-btn">
            ${Icons.bell}
            <span style="
              position: absolute;
              top: 8px;
              right: 8px;
              width: 7px;
              height: 7px;
              background-color: var(--fl-danger, #ef4444);
              border-radius: 50%;
              border: 1px solid #ffffff;
            "></span>
          </div>

          <!-- User Avatar & Dropdown -->
          <div style="position: relative; cursor: pointer;" id="fl-user-menu-wrapper">
            <div style="display: flex; align-items: center; gap: 8px;" id="fl-avatar-trigger">
              <fl-avatar 
                size="38px" 
                name="${user.name || user.username || 'Operator'}"
                ${user.avatarUrl ? `src="${user.avatarUrl}"` : ''}
              ></fl-avatar>
            </div>

            <!-- Profile Menu Dropdown -->
            <div id="fl-profile-dropdown" style="
              display: none;
              position: absolute;
              right: 0;
              top: 48px;
              width: 200px;
              background: #ffffff;
              border: 1px solid var(--fl-border, #e2e8f0);
              border-radius: var(--fl-radius-md, 8px);
              box-shadow: var(--fl-shadow-lg);
              padding: 6px;
              z-index: var(--fl-z-dropdown, 100);
            ">
              <div style="padding: 8px 12px; border-bottom: 1px solid var(--fl-border-light, #f1f5f9);">
                <div style="font-weight: 600; font-size: 13px; color: var(--fl-text);">${user.name || user.username}</div>
                <div style="font-size: 11px; color: var(--fl-text-muted);">Tenant: ${SessionManager.getTenantId() || 'Default'}</div>
              </div>
              <div id="fl-logout-btn" style="
                padding: 8px 12px;
                font-size: 13px;
                color: var(--fl-danger, #ef4444);
                font-weight: 500;
                cursor: pointer;
                border-radius: 4px;
                margin-top: 4px;
              " onmouseover="this.style.background='#fef2f2'" onmouseout="this.style.background='transparent'">
                Sign Out
              </div>
            </div>
          </div>
        </div>
      </div>
    `;

    // Dropdown Interactions
    const menuWrapper = this.querySelector('#fl-user-menu-wrapper');
    const dropdown = this.querySelector('#fl-profile-dropdown');
    const avatarTrigger = this.querySelector('#fl-avatar-trigger');
    const logoutBtn = this.querySelector('#fl-logout-btn');
    const brandBlock = this.querySelector('#fl-brand-block');

    if (avatarTrigger && dropdown) {
      avatarTrigger.addEventListener('click', (e) => {
        e.stopPropagation();
        dropdown.style.display = dropdown.style.display === 'block' ? 'none' : 'block';
      });

      document.addEventListener('click', () => {
        dropdown.style.display = 'none';
      });
    }

    if (logoutBtn) {
      logoutBtn.addEventListener('click', () => {
        SessionManager.logout();
      });
    }

    if (brandBlock) {
      brandBlock.addEventListener('click', () => {
        if (window.fl && window.fl.router) {
          window.fl.router.navigate('/');
        }
      });
    }

    const syncBtn = this.querySelector('#fl-navbar-sync-btn');
    if (syncBtn) {
      syncBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        HomeStore.refreshGymData(true);
      });
    }
  }
}

customElements.define('fl-navbar', FlNavbar);
