/**
 * <fl-not-found> Native Web Component
 * Framework-level 404 Page Not Found view with generic navigation actions.
 */

export class FlNotFound extends HTMLElement {
  connectedCallback() {
    this.style.display = 'flex';
    this.style.flexDirection = 'column';
    this.style.alignItems = 'center';
    this.style.justifyContent = 'center';
    this.style.minHeight = '60vh';
    this.style.padding = '60px 20px';
    this.style.textAlign = 'center';

    this.innerHTML = `
      <h1 style="font-size: 72px; font-weight: 800; color: var(--fl-primary, #dc2626); line-height: 1; margin: 0 0 16px 0;">404</h1>
      <h2 style="font-size: 22px; font-weight: 700; color: var(--fl-text); margin-bottom: 8px;">Page Not Found</h2>
      <p style="color: var(--fl-text-secondary); max-width: 420px; margin-bottom: 28px; line-height: 1.5;">
        The route or page you requested does not exist or may have been relocated.
      </p>
      <div style="display: flex; gap: 12px; align-items: center;">
        <button type="button" id="fl-404-back-btn" style="
          background: #ffffff;
          color: var(--fl-text, #0f172a);
          border: 1px solid var(--fl-border, #cbd5e1);
          border-radius: var(--fl-radius-md, 8px);
          padding: 10px 20px;
          font-size: 13.5px;
          font-weight: 600;
          cursor: pointer;
        ">
          ← Go Back
        </button>
        <button type="button" id="fl-404-home-btn" style="
          background: var(--fl-primary, #dc2626);
          color: #ffffff;
          border: none;
          border-radius: var(--fl-radius-md, 8px);
          padding: 10px 20px;
          font-size: 13.5px;
          font-weight: 600;
          cursor: pointer;
        ">
          Go to Home
        </button>
      </div>
    `;

    const backBtn = this.querySelector('#fl-404-back-btn');
    if (backBtn) {
      backBtn.addEventListener('click', () => {
        window.history.back();
      });
    }

    const homeBtn = this.querySelector('#fl-404-home-btn');
    if (homeBtn) {
      homeBtn.addEventListener('click', () => {
        if (window.fl && window.fl.router) {
          window.fl.router.navigate('/');
        } else {
          window.location.hash = '#/';
        }
      });
    }
  }
}

if (!customElements.get('fl-not-found')) {
  customElements.define('fl-not-found', FlNotFound);
}
// Alias for backward compatibility if referenced as fl-not-found-page
if (!customElements.get('fl-not-found-page')) {
  customElements.define('fl-not-found-page', class extends FlNotFound {});
}
