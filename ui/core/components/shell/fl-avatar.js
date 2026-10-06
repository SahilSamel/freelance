/**
 * <fl-avatar> Native Web Component
 * Displays user profile image with fallback initials and optional status indicator dot.
 */

export class FlAvatar extends HTMLElement {
  static get observedAttributes() {
    return ['src', 'name', 'size', 'online'];
  }

  connectedCallback() {
    this.render();
  }

  attributeChangedCallback() {
    this.render();
  }

  getInitials(name) {
    if (!name) return 'U';
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }

  render() {
    const src = this.getAttribute('src');
    const name = this.getAttribute('name') || '';
    const size = this.getAttribute('size') || '40px';
    const isOnline = this.hasAttribute('online') && this.getAttribute('online') !== 'false';

    this.style.display = 'inline-block';
    this.style.position = 'relative';
    this.style.width = size;
    this.style.height = size;
    this.style.flexShrink = '0';

    const initials = this.getInitials(name);

    this.innerHTML = `
      <div style="
        width: 100%;
        height: 100%;
        border-radius: 50%;
        overflow: hidden;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #e2e8f0;
        color: #475569;
        font-weight: 600;
        font-size: calc(${size} * 0.38);
        user-select: none;
        box-shadow: inset 0 0 0 1px rgba(0,0,0,0.06);
      ">
        ${src 
          ? `<img src="${src}" alt="${name}" style="width:100%; height:100%; object-fit:cover;" onerror="this.style.display='none'; this.nextElementSibling.style.display='flex';" />
             <span style="display:none; width:100%; height:100%; align-items:center; justify-content:center;">${initials}</span>`
          : `<span>${initials}</span>`
        }
      </div>
      ${isOnline ? `
        <span style="
          position: absolute;
          bottom: 0;
          right: 0;
          width: calc(${size} * 0.28);
          height: calc(${size} * 0.28);
          min-width: 8px;
          min-height: 8px;
          background-color: var(--fl-primary, #10b981);
          border: 2px solid #ffffff;
          border-radius: 50%;
        "></span>
      ` : ''}
    `;
  }
}

customElements.define('fl-avatar', FlAvatar);
