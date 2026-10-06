/**
 * <fl-search-bar> & <fl-filter-pill> Native Web Components
 * Interactive search input and dropdown filter pill buttons.
 */

import { Icons } from '../../../assets/icons/icons.js';

export class FlSearchBar extends HTMLElement {
  static get observedAttributes() {
    return ['placeholder', 'value'];
  }

  connectedCallback() {
    this.render();
  }

  attributeChangedCallback() {
    this.render();
  }

  getValue() {
    const input = this.querySelector('input');
    return input ? input.value : '';
  }

  setValue(val) {
    const input = this.querySelector('input');
    if (input) input.value = val;
  }

  render() {
    const placeholder = this.getAttribute('placeholder') || 'Search members by name or phone...';
    const value = this.getAttribute('value') || '';

    this.style.display = 'block';
    this.style.flex = '1';
    this.style.maxWidth = '360px';

    this.innerHTML = `
      <div style="
        position: relative;
        display: flex;
        align-items: center;
        width: 100%;
      ">
        <span style="
          position: absolute;
          left: 12px;
          color: var(--fl-text-light, #94a3b8);
          display: flex;
          align-items: center;
          pointer-events: none;
        ">
          ${Icons.search}
        </span>
        <input 
          type="text" 
          placeholder="${placeholder}" 
          value="${value}"
          style="
            width: 100%;
            height: 38px;
            padding: 0 14px 0 38px;
            background: #ffffff;
            border: 1px solid var(--fl-border, #e2e8f0);
            border-radius: var(--fl-radius-md, 8px);
            font-size: 13px;
            color: var(--fl-text, #0f172a);
            outline: none;
            transition: border-color var(--fl-transition-fast), box-shadow var(--fl-transition-fast);
          "
          class="fl-search-input"
        />
      </div>
    `;

    const input = this.querySelector('input');
    input.addEventListener('input', (e) => {
      this.dispatchEvent(new CustomEvent('fl-search', { bubbles: true, detail: { query: e.target.value } }));
    });
    input.addEventListener('focus', () => {
      input.style.borderColor = 'var(--fl-primary, #10b981)';
      input.style.boxShadow = '0 0 0 2px var(--fl-primary-subtle, #d1fae5)';
    });
    input.addEventListener('blur', () => {
      input.style.borderColor = 'var(--fl-border, #e2e8f0)';
      input.style.boxShadow = 'none';
    });
  }
}

export class FlFilterPill extends HTMLElement {
  static get observedAttributes() {
    return ['label', 'value', 'active', 'options'];
  }

  connectedCallback() {
    this.render();
  }

  attributeChangedCallback() {
    this.render();
  }

  render() {
    const label = this.getAttribute('label') || 'Filter';
    const value = this.getAttribute('value') || 'All';
    const isActive = this.hasAttribute('active');
    const optionsRaw = this.getAttribute('options') || '';
    const options = optionsRaw.split(',').map(s => s.trim()).filter(Boolean);

    this.style.display = 'inline-block';
    this.style.position = 'relative';

    this.innerHTML = `
      <button type="button" style="
        height: 38px;
        padding: 0 14px;
        background: ${isActive ? 'var(--fl-status-active-bg, #ecfdf5)' : '#ffffff'};
        border: 1.5px solid ${isActive ? 'var(--fl-primary, #10b981)' : 'var(--fl-border, #e2e8f0)'};
        color: ${isActive ? 'var(--fl-primary-dark, #047857)' : 'var(--fl-text-secondary, #475569)'};
        border-radius: var(--fl-radius-md, 8px);
        font-size: 13px;
        font-weight: 500;
        display: flex;
        align-items: center;
        gap: 6px;
        cursor: pointer;
        outline: none;
        user-select: none;
        transition: all var(--fl-transition-fast);
      " id="fl-filter-trigger">
        <span>${label}: <strong>${value}</strong></span>
        <span style="display: flex; align-items: center;">${Icons.chevronDown}</span>
      </button>

      <!-- Dropdown -->
      <div id="fl-filter-menu" style="
        display: none;
        position: absolute;
        top: 44px;
        left: 0;
        min-width: 160px;
        background: #ffffff;
        border: 1px solid var(--fl-border, #e2e8f0);
        border-radius: var(--fl-radius-md, 8px);
        box-shadow: var(--fl-shadow-lg);
        padding: 4px;
        z-index: var(--fl-z-dropdown, 100);
      ">
        ${options.map(opt => `
          <div class="fl-filter-option" data-value="${opt}" style="
            padding: 8px 12px;
            font-size: 13px;
            color: var(--fl-text);
            cursor: pointer;
            border-radius: 4px;
            display: flex;
            align-items: center;
            justify-content: space-between;
          " onmouseover="this.style.background='#f1f5f9'" onmouseout="this.style.background='transparent'">
            <span>${opt}</span>
            ${opt === value ? `<span style="color:var(--fl-primary);">${Icons.check}</span>` : ''}
          </div>
        `).join('')}
      </div>
    `;

    const trigger = this.querySelector('#fl-filter-trigger');
    const menu = this.querySelector('#fl-filter-menu');

    if (trigger && menu) {
      trigger.addEventListener('click', (e) => {
        e.stopPropagation();
        menu.style.display = menu.style.display === 'block' ? 'none' : 'block';
      });

      document.addEventListener('click', () => {
        menu.style.display = 'none';
      });

      menu.querySelectorAll('.fl-filter-option').forEach(item => {
        item.addEventListener('click', (e) => {
          e.stopPropagation();
          const selectedVal = item.getAttribute('data-value');
          this.setAttribute('value', selectedVal);
          menu.style.display = 'none';
          this.dispatchEvent(new CustomEvent('fl-filter-change', {
            bubbles: true,
            detail: { label, value: selectedVal }
          }));
        });
      });
    }
  }
}

customElements.define('fl-search-bar', FlSearchBar);
customElements.define('fl-filter-pill', FlFilterPill);
