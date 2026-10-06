/**
 * <fl-whatsapp-modal> Native Web Component
 * Quick WhatsApp composer and reminder notification modal with automated template replacement.
 */

import { HomeStore } from '../home-store.js';
import { AppConfig } from '../../../core/config/app-config.js';
import { ErrorMapper } from '../../../core/error/error-mapper.js';
import '../../../core/components/feedback/fl-modal.js';

export class FlWhatsAppModal extends HTMLElement {
  connectedCallback() {
    this.unsubscribe = HomeStore.subscribe(() => this.render());
    this.render();
  }

  disconnectedCallback() {
    if (this.unsubscribe) this.unsubscribe();
  }

  render() {
    const { whatsAppModal } = HomeStore.getState();
    const { isOpen, member, defaultTemplate, paymentInfo } = whatsAppModal;

    if (!isOpen || !member) {
      this.innerHTML = '';
      return;
    }

    const templates = AppConfig.getQuickTemplates();
    const currentTemplateKey = this.currentTemplateKey || defaultTemplate || (templates[0] ? templates[0].key : '');
    const templateObj = templates.find(t => t.key === currentTemplateKey) || templates[0];

    const formattedAmount = paymentInfo 
      ? AppConfig.formatCurrency(paymentInfo.amount) 
      : (member.pendingAmount ? AppConfig.formatCurrency(member.pendingAmount) : AppConfig.formatCurrency(0));

    // Interpolate template placeholders or fallback to empty for custom message
    const messageBody = templateObj ? AppConfig.formatTemplate(templateObj.text, {
      name: member.name || 'Member',
      expiryDate: member.expiryDate ? HomeStore.formatDateDisplay(member.expiryDate) : 'soon',
      amount: formattedAmount,
      id: member.id || ''
    }) : '';

    this.innerHTML = `
      <fl-modal title="💬 Send WhatsApp Notification" max-width="580px">
        <div style="display: flex; flex-direction: column; gap: 16px;">
          
          <!-- Recipient summary -->
          <div style="background: #f8fafc; border: 1px solid var(--fl-border); border-radius: 8px; padding: 12px 14px; display: flex; justify-content: space-between; align-items: center;">
            <div>
              <div style="font-size: 14px; font-weight: 700; color: var(--fl-text);">${member.name}</div>
              <div style="font-size: 12px; color: var(--fl-text-secondary); margin-top: 2px;">Phone: ${member.phone}</div>
            </div>
            <div style="font-size: 12px; font-weight: 600; background: #e0f2fe; color: #0369a1; padding: 3px 8px; border-radius: 6px;">
              ${member.statusLabel || member.status}
            </div>
          </div>

          <!-- Template selection or empty state alert -->
          ${templates.length > 0 ? `
            <div>
              <label style="display: block; font-size: 12.5px; font-weight: 600; color: var(--fl-text); margin-bottom: 6px;">
                Select Message Template:
              </label>
              <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px;">
                ${templates.map(tpl => `
                  <button 
                    type="button" 
                    class="fl-template-btn ${tpl.key === currentTemplateKey ? 'active' : ''}" 
                    data-key="${tpl.key}"
                    style="
                      text-align: left;
                      padding: 8px 12px;
                      border: 1px solid ${tpl.key === currentTemplateKey ? 'var(--fl-primary, #dc2626)' : 'var(--fl-border)'};
                      background: ${tpl.key === currentTemplateKey ? 'var(--fl-primary-light, #fef2f2)' : '#ffffff'};
                      color: ${tpl.key === currentTemplateKey ? 'var(--fl-primary, #dc2626)' : 'var(--fl-text)'};
                      border-radius: 6px;
                      font-size: 12px;
                      font-weight: 600;
                      cursor: pointer;
                    "
                  >
                    ${tpl.title}
                  </button>
                `).join('')}
              </div>
            </div>
          ` : `
            <div style="background: #f8fafc; border: 1px dashed var(--fl-border); border-radius: 8px; padding: 12px 14px; display: flex; align-items: center; gap: 10px;">
              <span style="font-size: 18px;">ℹ️</span>
              <span style="font-size: 12.5px; color: var(--fl-text-secondary); line-height: 1.4;">
                No approved WhatsApp templates found yet. You can type and send a custom direct message below.
              </span>
            </div>
          `}

          <!-- Message textarea -->
          <div>
            <label style="display: block; font-size: 12.5px; font-weight: 600; color: var(--fl-text); margin-bottom: 6px;">
              Message Preview / Content:
            </label>
            <textarea 
              id="fl-wa-msg-text"
              rows="5"
              placeholder="${templates.length > 0 ? 'Message content preview...' : 'Type your direct message here...'}"
              style="width: 100%; padding: 10px 12px; border: 1px solid var(--fl-border); border-radius: 8px; font-size: 13px; font-family: inherit; resize: vertical; box-sizing: border-box;"
            >${messageBody}</textarea>
          </div>

          <!-- Actions -->
          <div style="display: flex; justify-content: flex-end; gap: 10px; border-top: 1px solid var(--fl-border); padding-top: 14px;">
            <button 
              type="button" 
              id="fl-wa-cancel-btn" 
              style="padding: 9px 16px; border: 1px solid var(--fl-border); border-radius: 8px; background: #ffffff; font-weight: 600; font-size: 13px; cursor: pointer;"
            >
              Cancel
            </button>
            <button 
              type="button" 
              id="fl-wa-send-btn" 
              style="padding: 9px 20px; border: none; border-radius: 8px; background: #25d366; color: #ffffff; font-weight: 600; font-size: 13px; cursor: pointer; display: flex; align-items: center; gap: 6px;"
            >
              Send via WhatsApp →
            </button>
          </div>

        </div>
      </fl-modal>
    `;

    const modal = this.querySelector('fl-modal');
    if (modal) {
      modal.addEventListener('close', () => HomeStore.closeWhatsAppModal());
    }

    const cancelBtn = this.querySelector('#fl-wa-cancel-btn');
    if (cancelBtn) {
      cancelBtn.addEventListener('click', () => HomeStore.closeWhatsAppModal());
    }

    // Template selection clicks
    this.querySelectorAll('.fl-template-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        this.currentTemplateKey = btn.getAttribute('data-key');
        this.render();
      });
    });

    // Send button click
    const sendBtn = this.querySelector('#fl-wa-send-btn');
    if (sendBtn) {
      sendBtn.addEventListener('click', () => {
        const text = this.querySelector('#fl-wa-msg-text').value;
        const cleanPhone = (member.rawPhone || member.phone || '').replace(/\D/g, '');
        const phoneWithCountry = cleanPhone.length === 10 ? `91${cleanPhone}` : cleanPhone;

        const waUrl = `https://wa.me/${phoneWithCountry}?text=${encodeURIComponent(text)}`;
        window.open(waUrl, '_blank');

        ErrorMapper.notifySuccess(`WhatsApp chat opened for ${member.name}!`);
        HomeStore.closeWhatsAppModal();
      });
    }
  }
}

customElements.define('fl-whatsapp-modal', FlWhatsAppModal);
