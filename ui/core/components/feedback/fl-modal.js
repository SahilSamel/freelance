/**
 * <fl-modal> Native Web Component
 * Dialog modal with backdrop, header, customizable body slot, and action buttons.
 */

import { Icons } from '../../../assets/icons/icons.js';

export class FlModal extends HTMLElement {
  static get observedAttributes() {
    return ['open', 'title'];
  }

  constructor() {
    super();
    this.attachShadow({ mode: 'open' });
  }

  connectedCallback() {
    this.render();
  }

  attributeChangedCallback() {
    this.render();
  }

  open() {
    this.setAttribute('open', '');
  }

  close() {
    this.removeAttribute('open');
    this.dispatchEvent(new CustomEvent('fl-modal-close', { bubbles: true }));
  }

  render() {
    const isOpen = this.hasAttribute('open');
    const title = this.getAttribute('title') || 'Dialog';

    this.style.position = 'fixed';
    this.style.top = '0';
    this.style.left = '0';
    this.style.right = '0';
    this.style.bottom = '0';
    this.style.backgroundColor = 'rgba(15, 23, 42, 0.45)';
    this.style.backdropFilter = 'blur(3px)';
    this.style.zIndex = 'var(--fl-z-modal, 1000)';
    this.style.display = isOpen ? 'flex' : 'none';
    this.style.alignItems = 'center';
    this.style.justifyContent = 'center';
    this.style.padding = '20px';

    this.shadowRoot.innerHTML = `
      <style>
        :host {
          box-sizing: border-box;
          font-family: inherit;
        }
        * {
          box-sizing: border-box;
        }
        @keyframes flModalFadeIn {
          from { opacity: 0; transform: scale(0.96); }
          to { opacity: 1; transform: scale(1); }
        }
      </style>
      <div style="
        background: #ffffff;
        border-radius: var(--fl-radius-lg, 12px);
        width: 100%;
        max-width: 540px;
        box-shadow: 0 20px 25px -5px rgba(0,0,0,0.1), 0 10px 10px -5px rgba(0,0,0,0.04);
        overflow: hidden;
        display: flex;
        flex-direction: column;
        animation: flModalFadeIn 180ms ease-out forwards;
      " id="fl-modal-dialog">
        <!-- Header -->
        <div style="
          padding: 16px 20px;
          border-bottom: 1px solid var(--fl-border, #e2e8f0);
          display: flex;
          align-items: center;
          justify-content: space-between;
          background: #ffffff;
        ">
          <h3 style="font-size: 16px; font-weight: 700; color: var(--fl-text, #0f172a); margin: 0;">${title}</h3>
          <button type="button" id="fl-modal-close-btn" style="
            background: transparent;
            border: none;
            color: var(--fl-text-secondary, #64748b);
            cursor: pointer;
            padding: 4px;
            border-radius: var(--fl-radius-md, 6px);
            display: flex;
            align-items: center;
            justify-content: center;
          " onmouseover="this.style.background='#f1f5f9'" onmouseout="this.style.background='transparent'">
            ${Icons.close}
          </button>
        </div>

        <!-- Body -->
        <div style="padding: 20px; max-height: 70vh; overflow-y: auto;" id="fl-modal-content-body">
          <slot></slot>
        </div>
      </div>
    `;

    const closeBtn = this.shadowRoot.querySelector('#fl-modal-close-btn');
    if (closeBtn) {
      closeBtn.addEventListener('click', () => this.close());
    }

    this.addEventListener('click', (e) => {
      if (e.target === this) {
        this.close();
      }
    });
  }
}

customElements.define('fl-modal', FlModal);
