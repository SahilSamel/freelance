/**
 * <fl-input> & <fl-button> Native Web Components
 * Form input primitives with built-in validation error display and button variants.
 */

export class FlInput extends HTMLElement {
  static get observedAttributes() {
    return ['label', 'name', 'type', 'placeholder', 'value', 'required', 'disabled'];
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

  setError(errorMessage) {
    const wrapper = this.querySelector('.fl-input-wrapper');
    const errText = this.querySelector('.fl-input-err-text');
    if (wrapper) {
      wrapper.style.borderColor = 'var(--fl-danger, #ef4444)';
      wrapper.style.boxShadow = 'none';
    }
    if (errText) {
      errText.textContent = errorMessage;
      errText.style.display = 'block';
    }
  }

  clearError() {
    const wrapper = this.querySelector('.fl-input-wrapper');
    const errText = this.querySelector('.fl-input-err-text');
    if (wrapper) {
      wrapper.style.borderColor = 'var(--fl-border, #e2e8f0)';
      wrapper.style.boxShadow = 'none';
      wrapper.classList.remove('fl-input--error');
    }
    this.classList.remove('fl-input--error');
    const input = this.querySelector('input');
    if (input) input.classList.remove('fl-input--error');
    if (errText) {
      errText.textContent = '';
      errText.style.display = 'none';
    }
  }

  render() {
    const label = this.getAttribute('label') || '';
    const name = this.getAttribute('name') || '';
    const type = this.getAttribute('type') || 'text';
    const placeholder = this.getAttribute('placeholder') || '';
    const value = this.getAttribute('value') || '';
    const isRequired = this.hasAttribute('required');
    const isDisabled = this.hasAttribute('disabled');

    this.style.display = 'block';
    this.style.marginBottom = '14px';

    this.innerHTML = `
      ${label ? `
        <label style="
          display: block;
          font-size: 13px;
          font-weight: 600;
          color: var(--fl-text, #0f172a);
          margin-bottom: 6px;
        ">
          ${label} ${isRequired ? '<span style="color:var(--fl-danger)">*</span>' : ''}
        </label>
      ` : ''}
      <div class="fl-input-wrapper" style="
        border: 1px solid var(--fl-border, #e2e8f0);
        border-radius: var(--fl-radius-md, 8px);
        background: ${isDisabled ? '#f1f5f9' : '#ffffff'};
        overflow: hidden;
        transition: border-color var(--fl-transition-fast), box-shadow var(--fl-transition-fast);
      ">
        <input 
          type="${type}" 
          name="${name}" 
          placeholder="${placeholder}" 
          value="${value}"
          ${isRequired ? 'required' : ''}
          ${isDisabled ? 'disabled' : ''}
          style="
            width: 100%;
            height: 38px;
            padding: 0 12px;
            border: none;
            background: transparent;
            font-size: 13.5px;
            color: var(--fl-text);
            outline: none;
          "
        />
      </div>
      <div class="fl-input-err-text" style="
        display: none;
        color: var(--fl-danger, #ef4444);
        font-size: 12px;
        margin-top: 4px;
      "></div>
    `;

    const input = this.querySelector('input');
    const wrapper = this.querySelector('.fl-input-wrapper');

    if (input && wrapper) {
      input.addEventListener('focus', () => {
        wrapper.style.borderColor = 'var(--fl-primary, #10b981)';
        wrapper.style.boxShadow = '0 0 0 2px var(--fl-primary-subtle, #d1fae5)';
      });
      input.addEventListener('blur', () => {
        if (!this.querySelector('.fl-input-err-text').textContent) {
          wrapper.style.borderColor = 'var(--fl-border, #e2e8f0)';
        }
        wrapper.style.boxShadow = 'none';
      });
      input.addEventListener('input', (e) => {
        this.clearError();
        this.dispatchEvent(new CustomEvent('fl-input', { bubbles: true, detail: { value: e.target.value } }));
      });
    }
  }
}

export class FlButton extends HTMLElement {
  static get observedAttributes() {
    return ['variant', 'loading', 'disabled'];
  }

  connectedCallback() {
    this.render();
  }

  attributeChangedCallback() {
    this.render();
  }

  render() {
    const variant = this.getAttribute('variant') || 'primary'; // primary, secondary, outline, danger
    const isLoading = this.hasAttribute('loading');
    const isDisabled = this.hasAttribute('disabled') || isLoading;
    const text = this.textContent.trim();

    const styles = {
      primary: {
        bg: 'var(--fl-primary, #10b981)',
        color: '#ffffff',
        border: 'none',
        hover: 'var(--fl-primary-hover, #059669)'
      },
      secondary: {
        bg: '#f1f5f9',
        color: 'var(--fl-text, #0f172a)',
        border: 'none',
        hover: '#e2e8f0'
      },
      outline: {
        bg: 'transparent',
        color: 'var(--fl-text, #0f172a)',
        border: '1px solid var(--fl-border, #e2e8f0)',
        hover: '#f8fafc'
      },
      danger: {
        bg: 'var(--fl-danger, #ef4444)',
        color: '#ffffff',
        border: 'none',
        hover: '#dc2626'
      }
    };

    const current = styles[variant] || styles.primary;

    this.style.display = 'inline-block';

    this.innerHTML = `
      <button type="button" style="
        width: 100%;
        height: 38px;
        padding: 0 18px;
        border-radius: var(--fl-radius-md, 8px);
        background-color: ${current.bg};
        color: ${current.color};
        border: ${current.border};
        font-size: 13.5px;
        font-weight: 600;
        cursor: ${isDisabled ? 'not-allowed' : 'pointer'};
        opacity: ${isDisabled ? '0.65' : '1'};
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 8px;
        transition: all var(--fl-transition-fast);
      " id="fl-btn-inner">
        ${isLoading ? `<span style="display:inline-block; width:14px; height:14px; border:2px solid currentColor; border-top-color:transparent; border-radius:50%; animation:flSpin 600ms linear infinite;"></span>` : ''}
        <span>${text}</span>
      </button>
    `;
  }
}

customElements.define('fl-input', FlInput);
customElements.define('fl-button', FlButton);
