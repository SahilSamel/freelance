/**
 * <fl-tabs> & <fl-tab> Native Web Component
 * Segmented navigation control matching the top navbar design.
 */

export class FlTabs extends HTMLElement {
  connectedCallback() {
    this.render();
  }

  render() {
    this.style.display = 'inline-flex';
    this.style.alignItems = 'center';
    this.style.gap = '8px';
  }
}

export class FlTab extends HTMLElement {
  static get observedAttributes() {
    return ['active', 'badge', 'to'];
  }

  connectedCallback() {
    this.addEventListener('click', () => this.handleClick());
    this.render();
  }

  attributeChangedCallback() {
    this.render();
  }

  handleClick() {
    const to = this.getAttribute('to');
    if (to && window.fl && window.fl.router) {
      window.fl.router.navigate(to);
    }
    this.dispatchEvent(new CustomEvent('fl-tab-select', { bubbles: true, detail: { to, label: this.textContent } }));
  }

  render() {
    const isActive = this.hasAttribute('active');
    const badge = this.getAttribute('badge');
    const label = this.getAttribute('label') || this.innerText.trim();

    this.style.display = 'inline-flex';
    this.style.alignItems = 'center';
    this.style.gap = '6px';
    this.style.padding = '8px 18px';
    this.style.borderRadius = 'var(--fl-radius-md, 8px)';
    this.style.fontSize = '14px';
    this.style.fontWeight = '500';
    this.style.cursor = 'pointer';
    this.style.userSelect = 'none';
    this.style.transition = 'all var(--fl-transition-fast)';

    if (isActive) {
      this.style.backgroundColor = '#ffffff';
      this.style.color = 'var(--fl-primary, #dc2626)';
      this.style.border = '1.5px solid var(--fl-primary, #dc2626)';
      this.style.boxShadow = 'var(--fl-shadow-xs)';
    } else {
      this.style.backgroundColor = 'transparent';
      this.style.color = 'var(--fl-text-secondary, #475569)';
      this.style.border = '1.5px solid transparent';
    }

    this.innerHTML = `
      <span>${label}</span>
      ${badge ? `
        <span style="
          background-color: var(--fl-danger, #ef4444);
          color: #ffffff;
          font-size: 11px;
          font-weight: 700;
          padding: 1px 6px;
          border-radius: var(--fl-radius-full);
          line-height: 1.2;
        ">${badge}</span>
      ` : ''}
    `;
  }
}

customElements.define('fl-tabs', FlTabs);
customElements.define('fl-tab', FlTab);
