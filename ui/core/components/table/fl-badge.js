/**
 * <fl-badge> Native Web Component
 * Semantic status pill badge for table rows, cards, and profile headers.
 * Styled via CSS tokens in base.css using fl-badge[variant="..."] selectors.
 */

export class FlBadge extends HTMLElement {
  static get observedAttributes() {
    return ['variant', 'label', 'status', 'text'];
  }

  connectedCallback() {
    this.render();
  }

  attributeChangedCallback() {
    this.render();
  }

  render() {
    const rawVariant = (this.getAttribute('variant') || this.getAttribute('status') || 'active').toLowerCase();
    const label = this.getAttribute('label') || this.getAttribute('text') || this.textContent.trim() || rawVariant;

    if (this.getAttribute('variant') !== rawVariant) {
      this.setAttribute('variant', rawVariant);
    }

    this.textContent = label;
  }
}

customElements.define('fl-badge', FlBadge);
