/**
 * <fl-chats-section> Native Web Component
 * WhatsApp Messaging & Communications section: two-pane thread layout, quick templates, media attachments, and real-time messaging.
 */

import { HomeStore } from '../home-store.js';
import { api } from '../../../core/api/client.js';
import { AppConfig } from '../../../core/config/app-config.js';
import '../../../core/components/shell/fl-avatar.js';

export class FlChatsSection extends HTMLElement {
  constructor() {
    super();
    this.activeChatId = null;
    this.searchChat = '';
    this.pendingAttachment = null;
  }

  connectedCallback() {
    this.unsubscribe = HomeStore.subscribe(() => this.render());
    const state = HomeStore.getState();
    if (!this.activeChatId && state.conversations.length > 0) {
      this.activeChatId = state.conversations[0].id;
    }
    this.render();
  }

  disconnectedCallback() {
    if (this.unsubscribe) this.unsubscribe();
  }

  render() {
    const state = HomeStore.getState();
    const { conversations } = state;

    // Filter conversations by search
    const filteredConvs = conversations.filter(c => 
      !this.searchChat ||
      c.name.toLowerCase().includes(this.searchChat) ||
      (c.phone && c.phone.includes(this.searchChat)) ||
      (c.lastMessage && c.lastMessage.toLowerCase().includes(this.searchChat))
    );

    const activeConv = conversations.find(c => c.id === this.activeChatId) || filteredConvs[0] || null;

    this.innerHTML = `
      <div style="
        display: grid;
        grid-template-columns: 320px 1fr;
        height: calc(100vh - 240px);
        min-height: 520px;
        background: #ffffff;
        border: 1px solid var(--fl-border);
        border-radius: 12px;
        overflow: hidden;
        box-shadow: var(--fl-shadow-sm);
      ">
        
        <!-- Left Column: Conversations List -->
        <div style="border-right: 1px solid var(--fl-border); display: flex; flex-direction: column; background: #fafafa;">
          <!-- Search Header -->
          <div style="padding: 12px; border-bottom: 1px solid var(--fl-border); background: #ffffff;">
            <input 
              type="text" 
              id="fl-chat-search" 
              placeholder="Search chats or phone..." 
              value="${this.searchChat}"
              style="width: 100%; height: 34px; padding: 0 10px; border: 1px solid var(--fl-border); border-radius: 6px; font-size: 13px;"
            />
          </div>

          <!-- Conversations scroll list -->
          <div style="flex: 1; overflow-y: auto;">
            ${filteredConvs.length === 0 ? `
              <div style="padding: 24px; text-align: center; color: var(--fl-text-muted); font-size: 13px;">
                No conversations found.
              </div>
            ` : filteredConvs.map(conv => {
              const isSelected = activeConv && activeConv.id === conv.id;
              return `
                <div 
                  class="fl-conv-item ${isSelected ? 'selected' : ''}" 
                  data-id="${conv.id}"
                  style="
                    padding: 12px 14px;
                    border-bottom: 1px solid #f1f5f9;
                    cursor: pointer;
                    background: ${isSelected ? '#e0f2fe' : 'transparent'};
                    transition: background 0.15s;
                    display: flex;
                    align-items: center;
                    gap: 10px;
                  "
                >
                  <div style="position: relative;">
                    <div style="
                      width: 40px; 
                      height: 40px; 
                      border-radius: 50%; 
                      background: #e2e8f0; 
                      display: flex; 
                      align-items: center; 
                      justify-content: center; 
                      font-weight: 700;
                      color: var(--fl-text-secondary);
                      font-size: 14px;
                    ">
                      ${(conv.name || 'M').charAt(0)}
                    </div>
                    ${conv.online ? `
                      <span style="position: absolute; bottom: 0; right: 0; width: 10px; height: 10px; background: #22c55e; border-radius: 50%; border: 2px solid #ffffff;"></span>
                    ` : ''}
                  </div>

                  <div style="flex: 1; min-width: 0;">
                    <div style="display: flex; justify-content: space-between; align-items: baseline;">
                      <div style="font-weight: 600; font-size: 13.5px; color: var(--fl-text); overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                        ${conv.name}
                      </div>
                      <span style="font-size: 11px; color: var(--fl-text-muted);">${conv.time || ''}</span>
                    </div>
                    <div style="font-size: 12px; color: var(--fl-text-secondary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-top: 2px;">
                      ${conv.lastMessage || 'Click to view chat'}
                    </div>
                  </div>

                  ${conv.unreadCount > 0 ? `
                    <span style="background: #25d366; color: #ffffff; font-size: 11px; font-weight: 700; border-radius: 9999px; padding: 2px 6px;">
                      ${conv.unreadCount}
                    </span>
                  ` : ''}
                </div>
              `;
            }).join('')}
          </div>
        </div>

        <!-- Right Column: Active Conversation Messages & Composer -->
        <div style="display: flex; flex-direction: column; height: 100%; background: #ece5dd;">
          ${!activeConv ? `
            <div style="display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; color: var(--fl-text-muted);">
              <div style="font-size: 32px; margin-bottom: 8px;">💬</div>
              <div style="font-weight: 600;">Select a conversation to start chatting</div>
            </div>
          ` : `
            <!-- Top Chat Header -->
            <div style="padding: 10px 16px; background: #ffffff; border-bottom: 1px solid var(--fl-border); display: flex; align-items: center; justify-content: space-between;">
              <div style="display: flex; align-items: center; gap: 10px;">
                <div style="width: 36px; height: 36px; border-radius: 50%; background: #fee2e2; color: #dc2626; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 13px;">
                  ${(activeConv.name || 'M').charAt(0)}
                </div>
                <div>
                  <div style="font-weight: 700; font-size: 14px; color: var(--fl-text);">${activeConv.name}</div>
                  <div style="font-size: 11.5px; color: var(--fl-text-muted);">${activeConv.phone || 'WhatsApp User'}</div>
                </div>
              </div>

              <!-- Direct WhatsApp Web Launch -->
              <button type="button" id="fl-open-wa-external" style="
                background: #25d366;
                color: #ffffff;
                border: none;
                border-radius: 6px;
                padding: 6px 12px;
                font-size: 12px;
                font-weight: 600;
                cursor: pointer;
              ">
                Open in WhatsApp ↗
              </button>
            </div>

            <!-- Messages Stream -->
            <div id="fl-messages-container" style="flex: 1; overflow-y: auto; padding: 16px; display: flex; flex-direction: column; gap: 10px;">
              ${(activeConv.messages || []).map(msg => {
                const isOut = msg.isOutgoing;
                const textBody = msg.text || msg.content || '';
                const isImg = msg.mediaUrl && (
                  (msg.mediaType && msg.mediaType.toUpperCase() === 'IMAGE') ||
                  msg.mediaUrl.match(/\.(jpg|jpeg|png|webp)/i) ||
                  textBody === '[Image]'
                );
                const isDoc = msg.mediaUrl && (
                  (msg.mediaType && msg.mediaType.toUpperCase() === 'DOCUMENT') ||
                  msg.mediaUrl.match(/\.pdf/i) ||
                  textBody === '[Document]'
                );
                const showText = textBody && textBody !== '[Image]' && textBody !== '[Document]';

                return `
                  <div style="display: flex; justify-content: ${isOut ? 'flex-end' : 'flex-start'};">
                    <div style="
                      max-width: 72%;
                      padding: 8px 12px;
                      border-radius: 8px;
                      background: ${isOut ? '#dcf8c6' : '#ffffff'};
                      box-shadow: 0 1px 2px rgba(0,0,0,0.1);
                      font-size: 13px;
                      line-height: 1.4;
                      color: #111827;
                      position: relative;
                    ">
                      <!-- Image Media Attachment -->
                      ${isImg ? `
                        <div class="fl-chat-img-thumb" data-url="${msg.mediaUrl}" style="margin-bottom: 6px; cursor: pointer; border-radius: 6px; overflow: hidden; max-width: 260px;">
                          <img 
                            src="${msg.mediaUrl}" 
                            alt="Media Preview" 
                            style="width: 100%; max-height: 220px; object-fit: cover; display: block; border-radius: 6px;"
                            onerror="this.onerror=null; this.src='data:image/svg+xml,<svg xmlns=\\'http://www.w3.org/2000/svg\\' width=\\'200\\' height=\\'120\\'><rect fill=\\'%23cbd5e1\\' width=\\'200\\' height=\\'120\\'/><text fill=\\'%23475569\\' x=\\'50%\\' y=\\'50%\\' dominant-baseline=\\'middle\\' text-anchor=\\'middle\\' font-size=\\'13\\'>Photo</text></svg>';"
                          />
                        </div>
                      ` : ''}

                      <!-- Document Media Attachment -->
                      ${isDoc ? `
                        <div style="
                          margin-bottom: 6px;
                          padding: 8px 10px;
                          background: rgba(0,0,0,0.05);
                          border-radius: 6px;
                          border: 1px solid rgba(0,0,0,0.08);
                          display: flex;
                          align-items: center;
                          justify-content: space-between;
                          gap: 10px;
                        ">
                          <div style="display: flex; align-items: center; gap: 8px; min-width: 0;">
                            <span style="font-size: 22px;">📄</span>
                            <div style="min-width: 0;">
                              <div style="font-weight: 600; font-size: 12px; color: #1e293b; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                                ${msg.fileName || 'Document.pdf'}
                              </div>
                              <div style="font-size: 10.5px; color: #64748b;">PDF Document</div>
                            </div>
                          </div>
                          <a href="${msg.mediaUrl}" target="_blank" download style="
                            background: #25d366;
                            color: #ffffff;
                            text-decoration: none;
                            padding: 4px 10px;
                            border-radius: 4px;
                            font-size: 11px;
                            font-weight: 700;
                            white-space: nowrap;
                          ">
                            Download ⤓
                          </a>
                        </div>
                      ` : ''}

                      <!-- Caption / Text -->
                      ${showText ? `
                        <div style="word-break: break-word; white-space: pre-wrap; margin-bottom: 2px;">
                          ${textBody}
                        </div>
                      ` : ''}

                      <div style="text-align: right; font-size: 10px; color: #6b7280; margin-top: 4px;">
                        ${msg.time || ''} ${isOut ? '✓✓' : ''}
                      </div>
                    </div>
                  </div>
                `;
              }).join('')}
            </div>

            <!-- Composer Bar -->
            <div style="padding: 10px 14px; background: #f0f2f5; border-top: 1px solid var(--fl-border); display: flex; flex-direction: column; gap: 6px;">
              <!-- Pending Attachment Preview Chip -->
              ${this.pendingAttachment ? `
                <div style="
                  display: flex;
                  align-items: center;
                  justify-content: space-between;
                  background: #ffffff;
                  border: 1px solid #cbd5e1;
                  border-radius: 8px;
                  padding: 6px 12px;
                  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
                  animation: flFadeIn 150ms ease-out;
                ">
                  <div style="display: flex; align-items: center; gap: 8px; min-width: 0;">
                    <span style="font-size: 18px;">${this.pendingAttachment.mimeType.startsWith('image/') ? '🖼️' : '📄'}</span>
                    <div style="min-width: 0;">
                      <div style="font-size: 12.5px; font-weight: 600; color: #1e293b; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                        ${this.pendingAttachment.fileName}
                      </div>
                      <div style="font-size: 10.5px; color: #64748b;">
                        ${Math.round(this.pendingAttachment.size / 1024)} KB ready to send
                      </div>
                    </div>
                  </div>
                  <button type="button" id="fl-remove-attachment-btn" title="Remove attachment" style="
                    background: none;
                    border: none;
                    color: #ef4444;
                    font-size: 16px;
                    font-weight: 700;
                    cursor: pointer;
                    padding: 4px 8px;
                  ">✕</button>
                </div>
              ` : ''}

              <!-- Quick Templates row -->
              ${AppConfig.getQuickTemplates().length > 0 ? `
                <div style="display: flex; gap: 6px; overflow-x: auto; padding-bottom: 2px;">
                  ${AppConfig.getQuickTemplates().slice(0, 3).map(tpl => `
                    <button type="button" class="fl-quick-reply-pill" data-key="${tpl.key}" style="
                      background: #ffffff;
                      border: 1px solid var(--fl-border);
                      border-radius: 9999px;
                      padding: 4px 10px;
                      font-size: 11.5px;
                      font-weight: 600;
                      color: var(--fl-text-secondary);
                      white-space: nowrap;
                      cursor: pointer;
                    ">
                      ${tpl.title}
                    </button>
                  `).join('')}
                </div>
              ` : ''}

              <!-- Input row -->
              <div style="display: flex; gap: 8px; align-items: center;">
                <!-- Hidden Native File Input -->
                <input 
                  type="file" 
                  id="fl-chat-file-input" 
                  accept="image/jpeg,image/png,image/webp,application/pdf" 
                  style="display: none;" 
                />

                <!-- Paperclip Attachment Trigger -->
                <button 
                  type="button" 
                  id="fl-attach-btn" 
                  title="Attach Image or PDF Document" 
                  style="
                    width: 38px;
                    height: 38px;
                    border-radius: 50%;
                    background: #ffffff;
                    border: 1px solid var(--fl-border);
                    color: #475569;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    cursor: pointer;
                    font-size: 16px;
                    flex-shrink: 0;
                    transition: background 0.15s;
                  "
                  onmouseover="this.style.background='#f1f5f9'"
                  onmouseout="this.style.background='#ffffff'"
                >
                  📎
                </button>

                <input 
                  type="text" 
                  id="fl-chat-input" 
                  placeholder="${this.pendingAttachment ? 'Add a caption (optional)...' : 'Type a message or reminder...'}" 
                  style="flex: 1; height: 38px; padding: 0 12px; border: 1px solid var(--fl-border); border-radius: 20px; font-size: 13px; background: #ffffff;"
                />

                <button type="button" id="fl-send-msg-btn" style="
                  height: 38px;
                  padding: 0 18px;
                  background: #25d366;
                  color: #ffffff;
                  border: none;
                  border-radius: 20px;
                  font-weight: 600;
                  font-size: 13px;
                  cursor: pointer;
                ">
                  Send
                </button>
              </div>
            </div>
          `}
        </div>

      </div>
    `;

    this.bindEvents(activeConv);
  }

  bindEvents(activeConv) {
    // Search input
    const searchInput = this.querySelector('#fl-chat-search');
    if (searchInput) {
      searchInput.addEventListener('input', (e) => {
        this.searchChat = e.target.value.toLowerCase();
        this.render();
      });
    }

    // Select conversation
    this.querySelectorAll('.fl-conv-item').forEach(item => {
      item.addEventListener('click', () => {
        this.activeChatId = item.getAttribute('data-id');
        this.pendingAttachment = null;
        this.render();
      });
    });

    // Open WhatsApp external
    const openExtBtn = this.querySelector('#fl-open-wa-external');
    if (openExtBtn && activeConv) {
      openExtBtn.addEventListener('click', () => {
        const cleanPhone = (activeConv.phone || '').replace(/\D/g, '');
        window.open(`https://wa.me/${cleanPhone}`, '_blank');
      });
    }

    // Quick templates click
    this.querySelectorAll('.fl-quick-reply-pill').forEach(pill => {
      pill.addEventListener('click', () => {
        const key = pill.getAttribute('data-key');
        const tpl = AppConfig.getQuickTemplates().find(t => t.key === key);
        const input = this.querySelector('#fl-chat-input');
        if (tpl && input && activeConv) {
          input.value = AppConfig.formatTemplate(tpl.text, {
            name: activeConv.name || 'Member',
            expiryDate: 'soon',
            id: activeConv.id || ''
          });
          input.focus();
        }
      });
    });

    // Lightbox image preview click
    this.querySelectorAll('.fl-chat-img-thumb').forEach(thumb => {
      thumb.addEventListener('click', () => {
        const url = thumb.getAttribute('data-url');
        if (url) {
          this.showLightbox(url);
        }
      });
    });

    // Attachment file picker triggers
    const fileInput = this.querySelector('#fl-chat-file-input');
    const attachBtn = this.querySelector('#fl-attach-btn');
    if (attachBtn && fileInput) {
      attachBtn.addEventListener('click', () => fileInput.click());
    }

    if (fileInput) {
      fileInput.addEventListener('change', (e) => {
        const file = e.target.files[0];
        if (!file) return;

        const reader = new FileReader();
        reader.onload = (loadEvt) => {
          this.pendingAttachment = {
            file,
            fileName: file.name,
            mimeType: file.type || (file.name.toLowerCase().endsWith('.pdf') ? 'application/pdf' : 'image/jpeg'),
            dataBase64: loadEvt.target.result,
            size: file.size
          };
          this.render();
        };
        reader.readAsDataURL(file);
      });
    }

    // Remove pending attachment
    const removeAttBtn = this.querySelector('#fl-remove-attachment-btn');
    if (removeAttBtn) {
      removeAttBtn.addEventListener('click', () => {
        this.pendingAttachment = null;
        this.render();
      });
    }

    // Send message
    const sendBtn = this.querySelector('#fl-send-msg-btn');
    const input = this.querySelector('#fl-chat-input');
    const doSend = async () => {
      const text = input ? input.value.trim() : '';
      if (!text && !this.pendingAttachment) return;
      if (!activeConv) return;

      const attachment = this.pendingAttachment;
      this.pendingAttachment = null;

      const now = new Date();
      const timeStr = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

      let mediaType = null;
      let mediaUrl = null;
      let fileName = null;

      if (attachment) {
        fileName = attachment.fileName;
        mediaType = attachment.mimeType.startsWith('image/') ? 'IMAGE' : 'DOCUMENT';
        mediaUrl = attachment.dataBase64; // Immediate preview URL
      }

      const optimisticMsg = {
        id: `msg_${Date.now()}`,
        sender: 'staff',
        text: text || (mediaType === 'IMAGE' ? '[Image]' : '[Document]'),
        time: timeStr,
        isOutgoing: true,
        mediaUrl,
        mediaType,
        fileName,
        status: 'PENDING'
      };

      activeConv.messages = activeConv.messages || [];
      activeConv.messages.push(optimisticMsg);
      activeConv.lastMessage = optimisticMsg.text;
      activeConv.time = timeStr;

      if (input) input.value = '';
      this.render();

      // Scroll to bottom
      const msgContainer = this.querySelector('#fl-messages-container');
      if (msgContainer) msgContainer.scrollTop = msgContainer.scrollHeight;

      // Backend API call
      try {
        let uploadedMediaId = null;
        let uploadedMediaUrl = null;

        if (attachment) {
          const uploadRes = await api.post('/alerts/media/upload', {
            fileName: attachment.fileName,
            mimeType: attachment.mimeType,
            dataBase64: attachment.dataBase64
          });
          if (uploadRes && uploadRes.mediaId) {
            uploadedMediaId = uploadRes.mediaId;
            uploadedMediaUrl = uploadRes.mediaUrl;
            optimisticMsg.mediaId = uploadedMediaId;
            optimisticMsg.mediaUrl = uploadedMediaUrl;
          }
        }

        await api.post('/alerts/chat/reply', {
          userId: activeConv.id,
          channel: 'WHATSAPP',
          message: text,
          mediaUrl: uploadedMediaUrl,
          mediaId: uploadedMediaId,
          mediaType
        });
        optimisticMsg.status = 'SENT';
      } catch (err) {
        console.error('Failed to dispatch staff reply:', err);
        optimisticMsg.status = 'FAILED';
      }

      this.render();
      const updatedContainer = this.querySelector('#fl-messages-container');
      if (updatedContainer) updatedContainer.scrollTop = updatedContainer.scrollHeight;
    };

    if (sendBtn) sendBtn.addEventListener('click', doSend);
    if (input) {
      input.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') doSend();
      });
    }

    // Scroll to bottom initially
    const msgContainer = this.querySelector('#fl-messages-container');
    if (msgContainer) msgContainer.scrollTop = msgContainer.scrollHeight;
  }

  showLightbox(imageUrl) {
    let lb = document.getElementById('fl-media-lightbox');
    if (!lb) {
      lb = document.createElement('div');
      lb.id = 'fl-media-lightbox';
      lb.style.cssText = `
        position: fixed;
        inset: 0;
        background: rgba(15, 23, 42, 0.88);
        backdrop-filter: blur(4px);
        z-index: 99999;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: zoom-out;
        padding: 24px;
        animation: flFadeIn 150ms ease-out;
      `;
      lb.innerHTML = `
        <div style="position: relative; max-width: 90vw; max-height: 90vh; display: flex; align-items: center; justify-content: center;">
          <img id="fl-lightbox-img" style="max-width: 90vw; max-height: 85vh; border-radius: 8px; box-shadow: 0 20px 40px rgba(0,0,0,0.4); object-fit: contain;" />
          <button type="button" id="fl-lightbox-close" style="
            position: absolute;
            top: -14px;
            right: -14px;
            width: 32px;
            height: 32px;
            border-radius: 50%;
            background: #ffffff;
            border: none;
            color: #0f172a;
            font-size: 18px;
            font-weight: 700;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            box-shadow: 0 2px 8px rgba(0,0,0,0.25);
          ">&times;</button>
        </div>
      `;
      lb.addEventListener('click', (e) => {
        if (e.target === lb || e.target.id === 'fl-lightbox-close') {
          lb.remove();
        }
      });
      document.body.appendChild(lb);
    }
    const img = lb.querySelector('#fl-lightbox-img');
    if (img) img.src = imageUrl;
  }
}

customElements.define('fl-chats-section', FlChatsSection);
