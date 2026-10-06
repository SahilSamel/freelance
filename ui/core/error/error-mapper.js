/**
 * Freelance Frontend Framework - Global Error Mapper & Notification Dispatcher
 * Translates backend error structures (Validation, Constraints, 401, 403, 500) into UI actions.
 */

import { FormBinder } from './form-binder.js';

class ErrorMapperClass {
  constructor() {
    this.toastListeners = [];
  }

  /**
   * Dispatches top-center red error window
   */
  showErrorWindow(message, title = 'Error', errorCode = '', duration = 6000) {
    window.dispatchEvent(new CustomEvent('fl:error-window', {
      detail: {
        title,
        message,
        errorCode,
        duration
      }
    }));
  }

  /**
   * Dispatches standard toast notification
   */
  toast(message, type = 'info', title = '', duration = 4000) {
    const detail = {
      id: 'toast_' + Math.random().toString(36).substring(2, 9),
      message,
      type, // 'success', 'danger', 'warning', 'info'
      title,
      duration
    };

    window.dispatchEvent(new CustomEvent('fl:toast', { detail }));
  }

  notifySuccess(message, title = 'Success') {
    this.toast(message, 'success', title);
  }

  notifyWarning(message, title = 'Warning') {
    this.toast(message, 'warning', title);
  }

  notifyInfo(message, title = 'Information') {
    this.toast(message, 'info', title);
  }

  notifyUnauthorized() {
    this.showErrorWindow('Your session has expired. Redirecting to login...', 'Session Expired', '401', 3000);
    window.dispatchEvent(new CustomEvent('fl:unauthorized'));
  }

  handle(error, formElement = null) {
    if (!error) return;

    const status = error.status || 500;
    const data = error.data;
    const errorCode = (data && data.errorCode) || (status ? `HTTP_${status}` : 'UNKNOWN_ERROR');

    // Case 1: 401 Unauthorized -> redirect immediately
    if (status === 401) {
      this.notifyUnauthorized();
      return;
    }

    // Case 2: Validation Errors (Array of field error items or object with fieldErrors)
    const fieldErrors = (data && Array.isArray(data.message) && data.message)
      || (data && Array.isArray(data.fieldErrors) && data.fieldErrors)
      || (data && Array.isArray(data.errors) && data.errors);

    if (fieldErrors && fieldErrors.length > 0) {
      if (formElement) {
        FormBinder.applyFieldErrors(formElement, fieldErrors);
      }
      return;
    }

    // Case 3: Standard error message extraction
    let displayMessage = '';
    if (data && typeof data.message === 'string') {
      displayMessage = data.message;
    } else if (data && typeof data.error === 'string') {
      displayMessage = data.error;
    } else if (error.message) {
      displayMessage = error.message;
    } else {
      displayMessage = 'An unexpected error occurred. Please try again.';
    }

    // Case 4: 403 Forbidden
    if (status === 403) {
      this.showErrorWindow(displayMessage || 'You do not have permission to perform this action.', 'Access Denied', '403');
      return;
    }

    // Case 5: 404 Not Found
    if (status === 404) {
      this.showErrorWindow(displayMessage || 'Requested resource was not found.', 'Not Found', '404');
      return;
    }

    // Case 6: Server error or any other unhandled error -> Red window at top center
    const title = status >= 500 ? 'Server Error' : 'Action Failed';
    this.showErrorWindow(displayMessage, title, errorCode);
  }
}

export const ErrorMapper = new ErrorMapperClass();
