/**
 * <fl-dropdown-menu> Native Web Component
 * 3-dots Kebab action popup menu for table rows and list items.
 */

import { Icons } from '../../../assets/icons/icons.js';

export class FlDropdownMenu extends HTMLElement {
  connectedCallback() {
    this.render();
  }

  setItems(items = []) {
    this.items = items;
    this.render();
  }

  render() {
    this.style.display = 'inline-block';
    this.style.position = 'relative';

    this.innerHTML = `
      <button type="button" style="
        width: 32px;
        height: 32px;
        border-radius: var(--fl-radius-md, 8px);
        background: transparent;
        border: none;
        color: var(--fl-text-secondary, #64748b);
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        outline: none;
        transition: background var(--fl-transition-fast);
      " class="fl-kebab-trigger" onmouseover="this.style.background='#f1f5f9'" onmouseout="this.style.background='transparent'">
        ${Icons.moreVertical}
      </button>

      <div class="fl-kebab-popup" style="
        display: none;
        position: absolute;
        right: 0;
        top: 36px;
        min-width: 140px;
        background: #ffffff;
        border: 1px solid var(--fl-border, #e2e8f0);
        border-radius: var(--fl-radius-md, 8px);
        box-shadow: var(--fl-shadow-lg);
        padding: 4px;
        z-index: var(--fl-z-dropdown, 100);
      ">
        <slot></slot>
      </div>
    `;

    const trigger = this.querySelector('.fl-kebab-trigger');
    const popup = this.querySelector('.fl-kebab-popup');

    if (trigger && popup) {
      trigger.addEventListener('click', (e) => {
        e.stopPropagation();
        popup.style.display = popup.style.display === 'block' ? 'none' : 'block';
      });

      document.addEventListener('click', () => {
        popup.style.display = 'none';
      });
    }
  }
}

customElements.define('fl-dropdown-menu', FlDropdownMenu);
