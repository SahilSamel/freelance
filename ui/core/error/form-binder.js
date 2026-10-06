/**
 * Freelance Frontend Framework - Automated Form Error Binder
 * Maps backend field-level validation errors into red borders and inline helper messages.
 */

export class FormBinder {
  static clearFormErrors(formElement) {
    if (!formElement) return;

    // Clear error classes on inputs
    const inputs = formElement.querySelectorAll('input, select, textarea, fl-input, .fl-input-wrapper');
    inputs.forEach(input => {
      input.classList.remove('fl-input--error');
      if (typeof input.clearError === 'function') {
        input.clearError();
      }
    });

    // Remove existing inline error message nodes
    const errorContainers = formElement.querySelectorAll('.fl-field-error-msg');
    errorContainers.forEach(el => el.remove());
  }

  static applyFieldErrors(formElement, validationErrors = []) {
    if (!formElement || !Array.isArray(validationErrors)) return;

    this.clearFormErrors(formElement);

    let firstOffendingElement = null;

    validationErrors.forEach(item => {
      const fieldName = item.fieldName || item.field || item.name;
      const message = item.message || item.defaultMessage || 'Invalid value';
      if (!fieldName) return;

      // Find the input element by name, id or data-field
      const input = formElement.querySelector(
        `[name="${fieldName}"], [data-field="${fieldName}"], fl-input[name="${fieldName}"], #${fieldName}`
      );

      if (input) {
        if (!firstOffendingElement) {
          firstOffendingElement = input;
        }

        // Apply error class (red line / border)
        input.classList.add('fl-input--error');
        const wrapper = input.closest('.fl-input-wrapper');
        if (wrapper) wrapper.classList.add('fl-input--error');

        // If custom element with setError
        if (typeof input.setError === 'function') {
          input.setError(message);
        } else {
          // Add inline red error message below input
          const parent = input.closest('.fl-form-group') || input.parentElement;
          let msgEl = parent.querySelector(`.fl-field-error-msg[data-for="${fieldName}"]`);
          if (!msgEl) {
            msgEl = document.createElement('div');
            msgEl.className = 'fl-field-error-msg';
            msgEl.setAttribute('data-for', fieldName);
            parent.appendChild(msgEl);
          }
          msgEl.innerHTML = `
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" style="vertical-align: middle;">
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="12" y1="8" x2="12" y2="12"></line>
              <line x1="12" y1="16" x2="12.01" y2="16"></line>
            </svg>
            <span>${message}</span>
          `;
        }
      }
    });

    if (firstOffendingElement && typeof firstOffendingElement.focus === 'function') {
      firstOffendingElement.focus();
    }
  }
}
