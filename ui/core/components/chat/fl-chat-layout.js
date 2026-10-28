/**
 * <fl-chat-layout>, <fl-chat-list>, <fl-chat-thread>, <fl-chat-input>
 * Modern WhatsApp-style two-way chat timeline with conversation list, message thread, and composer.
 */

import { Icons } from '../../../assets/icons/icons.js';
import { AppConfig } from '../../config/app-config.js';
import '../shell/fl-avatar.js';

export class FlChatLayout extends HTMLElement {
  connectedCallback() {
    this.style.display = 'flex';
    this.style.height = 'calc(100vh - 88px)';
    this.style.maxHeight = '920px';
    this.style.backgroundColor = '#ffffff';
    this.style.border = '1px solid var(--fl-border, #e2e8f0)';
    this.style.borderRadius = 'var(--fl-radius-lg, 12px)';
    this.style.overflow = 'hidden';
    this.style.boxShadow = '0 4px 20px -2px rgba(15, 23, 42, 0.08), 0 2px 6px -1px rgba(15, 23, 42, 0.04)';
    this.style.marginTop = '6px';
  }
}

export class FlChatList extends HTMLElement {
  constructor() {
    super();
    this.conversations = [];
    this.activeId = null;
    this.searchQuery = '';
  }

  setConversations(list = [], activeId = null) {
    this.conversations = list;
    if (activeId) {
      this.activeId = activeId;
    } else if (!this.activeId && list.length > 0) {
      this.activeId = list[0].id;
    }
    this.render();
  }

  connectedCallback() {
    this.render();
  }

  formatLastTime(conv) {
    if (!conv.timestamp) return '';
    const date = new Date(conv.timestamp);
    const now = new Date();
    const isToday = date.toDateString() === now.toDateString();
    if (isToday) {
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }
    const yesterday = new Date();
    yesterday.setDate(now.getDate() - 1);
    if (date.toDateString() === yesterday.toDateString()) {
      return 'Yesterday';
    }
    return date.toLocaleDateString([], { month: 'short', day: 'numeric' });
  }

  render() {
    this.style.width = '350px';
    this.style.borderRight = '1px solid var(--fl-border, #e2e8f0)';
    this.style.display = 'flex';
    this.style.flexDirection = 'column';
    this.style.backgroundColor = '#ffffff';
    this.style.flexShrink = '0';

    const q = (this.searchQuery || '').toLowerCase().trim();
    let filtered = this.conversations;

    // Apply Search Filter
    if (q) {
      filtered = filtered.filter(c =>
        (c.name && c.name.toLowerCase().includes(q)) ||
        (c.phone && c.phone.toLowerCase().includes(q)) ||
        (c.rawPhone && c.rawPhone.toLowerCase().includes(q)) ||
        (c.lastMessage && c.lastMessage.toLowerCase().includes(q))
      );
    }

    this.innerHTML = `
      <!-- Sidebar Header & Title -->
      <div style="
        padding: 14px 16px 12px;
        border-bottom: 1px solid var(--fl-border, #e2e8f0);
        background: #ffffff;
      ">
        <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 12px;">
          <div style="
            width: 28px;
            height: 28px;
            border-radius: 6px;
            background: #f1f5f9;
            color: var(--fl-secondary, #232256);
            display: flex;
            align-items: center;
            justify-content: center;
          ">
            ${Icons.chat}
          </div>
          <span style="font-size: 15px; font-weight: 700; color: var(--fl-text, #0f172a);">Conversations</span>
        </div>

        <!-- Search Bar -->
        <div style="
          display: flex;
          align-items: center;
          background: #f8fafc;
          border: 1px solid var(--fl-border, #e2e8f0);
          border-radius: var(--fl-radius-md, 8px);
          padding: 0 10px;
          height: 36px;
          transition: border-color var(--fl-transition-fast);
        " id="fl-search-box-wrap">
          <span style="color: var(--fl-text-light, #94a3b8); display: flex;">${Icons.search}</span>
          <input 
            type="text" 
            id="fl-chat-search" 
            placeholder="Search member, phone, or message..." 
            value="${this.searchQuery}"
            style="
              border: none;
              background: transparent;
              outline: none;
              padding-left: 8px;
              font-size: 12.5px;
              color: var(--fl-text, #0f172a);
              width: 100%;
            "
          />
          ${this.searchQuery ? `
            <button type="button" id="fl-clear-search-btn" style="background:none; border:none; color:var(--fl-text-muted); cursor:pointer; padding:2px; display:flex;">
              ${Icons.close}
            </button>
          ` : ''}
        </div>
      </div>

      <!-- Conversations Item List -->
      <div style="flex: 1; overflow-y: auto;" id="fl-chat-items-container">
        ${filtered.length === 0 ? `
          <div style="padding: 40px 16px; text-align: center; color: var(--fl-text-secondary); font-size: 13px;">
            <div style="font-size: 24px; margin-bottom: 6px;">🔍</div>
            <div style="font-weight: 600; color: var(--fl-text, #0f172a); margin-bottom: 2px;">No conversations found</div>
            <div style="font-size: 12px; color: var(--fl-text-muted);">Try a different search query</div>
          </div>
        ` : filtered.map(c => {
          const isActive = c.id === this.activeId;
          const timeLabel = this.formatLastTime(c);
          return `
            <div class="fl-chat-item" data-id="${c.id}" style="
              display: flex;
              align-items: center;
              gap: 12px;
              padding: 12px 14px;
              cursor: pointer;
              background-color: ${isActive ? 'var(--fl-status-active-bg, #eff6ff)' : 'transparent'};
              border-left: ${isActive ? '3.5px solid var(--fl-secondary, #232256)' : '3.5px solid transparent'};
              border-bottom: 1px solid var(--fl-border-light, #f1f5f9);
              transition: background var(--fl-transition-fast);
              position: relative;
            " onmouseover="if(!${isActive}) this.style.background='#f8fafc'" onmouseout="if(!${isActive}) this.style.background='transparent'">
              
              <!-- Avatar -->
              <fl-avatar size="44px" name="${c.name}" src="${c.avatar || ''}" ${c.online ? 'online' : ''}></fl-avatar>

              <!-- Member Info & Last Message Snippet -->
              <div style="flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 3px;">
                <div style="display: flex; justify-content: space-between; align-items: baseline;">
                  <span style="font-weight: 600; font-size: 13.5px; color: var(--fl-text, #0f172a); white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
                    ${c.name}
                  </span>
                  <span style="font-size: 11px; color: var(--fl-text-muted); flex-shrink: 0; margin-left: 6px;">
                    ${timeLabel}
                  </span>
                </div>

                <div style="display: flex; justify-content: space-between; align-items: center; gap: 6px;">
                  <span style="
                    font-size: 12px;
                    color: ${c.isHumanRequired ? '#b91c1c' : 'var(--fl-text-secondary, #64748b)'};
                    font-weight: ${c.isHumanRequired ? '600' : '400'};
                    white-space: nowrap;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    max-width: 190px;
                  ">
                    ${c.isHumanRequired ? '⚠️ ' : ''}${c.lastMessage || 'No messages yet'}
                  </span>
                  
                  ${c.isHumanRequired ? `
                    <span style="
                      background: #fef2f2;
                      color: #dc2626;
                      border: 1px solid #fecaca;
                      font-size: 9.5px;
                      font-weight: 700;
                      padding: 1.5px 6px;
                      border-radius: var(--fl-radius-full, 9999px);
                      white-space: nowrap;
                      flex-shrink: 0;
                    ">Needs Human</span>
                  ` : (c.unreadCount ? `
                    <span style="
                      background: #059669;
                      color: #ffffff;
                      font-size: 10px;
                      font-weight: 700;
                      border-radius: 50%;
                      width: 18px;
                      height: 18px;
                      display: flex;
                      align-items: center;
                      justify-content: center;
                      flex-shrink: 0;
                    ">${c.unreadCount}</span>
                  ` : '')}
                </div>
              </div>
            </div>
          `;
        }).join('')}
      </div>
    `;

    // Search Input binding
    const searchInput = this.querySelector('#fl-chat-search');
    if (searchInput) {
      searchInput.addEventListener('input', (e) => {
        this.searchQuery = e.target.value;
        this.render();
        const nextInput = this.querySelector('#fl-chat-search');
        if (nextInput) {
          nextInput.focus();
          nextInput.setSelectionRange(nextInput.value.length, nextInput.value.length);
        }
      });
    }

    // Clear search
    const clearBtn = this.querySelector('#fl-clear-search-btn');
    if (clearBtn) {
      clearBtn.addEventListener('click', () => {
        this.searchQuery = '';
        this.render();
      });
    }

    // Row selection
    this.querySelectorAll('.fl-chat-item').forEach(item => {
      item.addEventListener('click', () => {
        const id = item.getAttribute('data-id');
        this.activeId = id;
        this.render();
        const conv = this.conversations.find(c => c.id === id);
        this.dispatchEvent(new CustomEvent('fl-chat-select', { bubbles: true, detail: { conversation: conv } }));
      });
    });
  }
}

export class FlChatThread extends HTMLElement {
  constructor() {
    super();
    this.messages = [];
    this.isHumanRequired = false;
    this.activeName = 'Member';
  }

  setMessages(messages = [], isHumanRequired = false, activeName = 'Member') {
    this.messages = messages;
    this.isHumanRequired = isHumanRequired;
    this.activeName = activeName;
    this.render();
    this.scrollToBottom();
  }

  addMessage(msg) {
    this.messages.push(msg);
    this.render();
    this.scrollToBottom();
  }

  scrollToBottom() {
    setTimeout(() => {
      this.scrollTop = this.scrollHeight;
    }, 40);
  }

  connectedCallback() {
    this.render();
  }

  linkify(text) {
    if (!text) return '';
    const formatted = AppConfig.formatWhatsAppText(text);
    const urlPattern = /(\b(https?|ftp):\/\/[-A-Z0-9+&@#\/%?=~_|!:,.;]*[-A-Z0-9+&@#\/%=~_|])/gim;
    return formatted.replace(urlPattern, '<a href="$1" target="_blank" rel="noopener noreferrer" style="color: #2563eb; text-decoration: underline; font-weight: 500;">$1</a>');
  }

  renderMessageStatus(msg) {
    const rawStatus = (msg.status || '').toUpperCase();

    // 1. Pending / Sending -> WhatsApp Timer Clock Icon (in-flight)
    if (rawStatus === 'PENDING' || rawStatus === 'SENDING' || rawStatus === 'IN_FLIGHT') {
      return `
        <span style="color: #8696a0; display: inline-flex; align-items: center; width: 14px; height: 14px;" title="Sending...">
          <svg viewBox="0 0 16 16" width="13" height="13" fill="currentColor">
            <path d="M8 1a7 7 0 1 0 7 7A7.008 7.008 0 0 0 8 1zm0 12.5A5.5 5.5 0 1 1 13.5 8 5.506 5.506 0 0 1 8 13.5zm.5-8.5H7v5l4.25 2.55.75-1.23-3.5-2.07z"/>
          </svg>
        </span>
      `;
    }

    // 2. Sent / Confirmed by server -> WhatsApp Single Grey Tick
    if (rawStatus === 'SENT' || rawStatus === 'CONFIRMED' || rawStatus === 'QUEUED') {
      return `
        <span style="color: #8696a0; display: inline-flex; align-items: center; width: 15px; height: 15px;" title="Sent">
          <svg viewBox="0 0 16 15" width="15" height="14" fill="currentColor">
            <path d="M15.01 3.316l-.478-.372a.365.365 0 0 0-.51.063L6.442 12.759 2.016 8.333a.365.365 0 0 0-.516 0l-.372.372a.365.365 0 0 0 0 .516l4.73 4.73a.365.365 0 0 0 .546-.032l8.667-10.1a.365.365 0 0 0-.063-.503z"/>
          </svg>
        </span>
      `;
    }

    // 3. Delivered -> WhatsApp Double Grey Tick
    if (rawStatus === 'DELIVERED') {
      return `
        <span style="color: #667781; display: inline-flex; align-items: center; width: 17px; height: 15px;" title="Delivered">
          <svg viewBox="0 0 16 15" width="16" height="14" fill="currentColor">
            <path d="M15.01 3.316l-.478-.372a.365.365 0 0 0-.51.063L6.442 12.759 2.016 8.333a.365.365 0 0 0-.516 0l-.372.372a.365.365 0 0 0 0 .516l4.73 4.73a.365.365 0 0 0 .546-.032l8.667-10.1a.365.365 0 0 0-.063-.503zm-3.882.063l-.478-.372a.365.365 0 0 0-.51.063L4.57 9.873l.942.942 5.094-6.936a.365.365 0 0 0-.063-.503z"/>
          </svg>
        </span>
      `;
    }

    // 4. Read / Viewed -> WhatsApp Vivid Double Blue Tick with subtle soft glow
    if (rawStatus === 'READ' || rawStatus === 'VIEWED') {
      return `
        <span style="color: #34b7f1; display: inline-flex; align-items: center; width: 17px; height: 15px;" title="Read">
          <svg viewBox="0 0 16 15" width="16" height="14" fill="#34b7f1" style="filter: drop-shadow(0 0 2px rgba(52, 183, 241, 0.45));">
            <path d="M15.01 3.316l-.478-.372a.365.365 0 0 0-.51.063L6.442 12.759 2.016 8.333a.365.365 0 0 0-.516 0l-.372.372a.365.365 0 0 0 0 .516l4.73 4.73a.365.365 0 0 0 .546-.032l8.667-10.1a.365.365 0 0 0-.063-.503zm-3.882.063l-.478-.372a.365.365 0 0 0-.51.063L4.57 9.873l.942.942 5.094-6.936a.365.365 0 0 0-.063-.503z"/>
          </svg>
        </span>
      `;
    }

    // 5. Failed -> Error Icon
    if (rawStatus === 'FAILED' || rawStatus === 'ERROR') {
      return `
        <span style="color: #ef4444; display: inline-flex; align-items: center; width: 14px; height: 14px;" title="Failed to send">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="8" x2="12" y2="12"></line>
            <line x1="12" y1="16" x2="12.01" y2="16"></line>
          </svg>
        </span>
      `;
    }

    // Default fallback: Single grey tick for sent messages
    return `
      <span style="color: #667781; display: inline-flex; align-items: center; width: 15px; height: 15px;" title="Sent">
        <svg viewBox="0 0 16 15" width="15" height="14" fill="currentColor">
          <path d="M15.01 3.316l-.478-.372a.365.365 0 0 0-.51.063L6.442 12.759 2.016 8.333a.365.365 0 0 0-.516 0l-.372.372a.365.365 0 0 0 0 .516l4.73 4.73a.365.365 0 0 0 .546-.032l8.667-10.1a.365.365 0 0 0-.063-.503z"/>
        </svg>
      </span>
    `;
  }

  render() {
    this.style.flex = '1';
    this.style.overflowY = 'auto';
    this.style.padding = '20px 24px';
    this.style.backgroundColor = '#f0f2f5';
    this.style.display = 'flex';
    this.style.flexDirection = 'column';
    this.style.gap = '14px';

    if (!this.messages || this.messages.length === 0) {
      this.innerHTML = `
        <div style="margin: auto; text-align: center; color: var(--fl-text-secondary); max-width: 340px; padding: 30px 20px; background: rgba(255, 255, 255, 0.7); border-radius: 16px; border: 1px dashed var(--fl-border, #e2e8f0);">
          <div style="font-size: 36px; margin-bottom: 8px;">💬</div>
          <div style="font-weight: 700; font-size: 15px; margin-bottom: 4px; color: var(--fl-text, #0f172a);">No Messages Yet</div>
          <div style="font-size: 12.5px; color: var(--fl-text-muted); line-height: 1.45;">
            Send a reply or select a pre-approved template below to message ${this.activeName}.
          </div>
        </div>
      `;
      return;
    }

    this.innerHTML = `
      <!-- Messages Stream -->
      ${this.messages.map(m => {
        const isOutgoing = m.direction === 'OUTBOUND' || m.sender === 'staff' || m.isOutgoing;
        const formattedContent = this.linkify(m.content || m.text || '');

        let timeStr = m.time;
        if (!timeStr && m.timestamp) {
          try {
            timeStr = new Date(m.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
          } catch {
            timeStr = '';
          }
        }

        // Quoted message detection (Native replyToMessageId)
        const rawContent = m.content || m.text || '';
        let quoteSnippet = null;
        const contentBody = rawContent;
        let quoteSender = isOutgoing ? 'Staff' : 'Member';
        let quoteText = '';

        if (m.replyToMessageId && Array.isArray(this.messages)) {
          const parentMsg = this.messages.find(other => String(other.id) === String(m.replyToMessageId));
          if (parentMsg) {
            const pIsOut = parentMsg.direction === 'OUTBOUND' || parentMsg.sender === 'staff' || parentMsg.isOutgoing;
            quoteSender = pIsOut ? 'Staff' : (this.activeName || 'Member');
            const pRawMedia = String(parentMsg.mediaType || '').toUpperCase();
            if (parentMsg.text && parentMsg.text.trim()) {
              quoteText = parentMsg.text.trim();
            } else if (parentMsg.content && parentMsg.content.trim()) {
              quoteText = parentMsg.content.trim();
            } else if (pRawMedia === 'IMAGE') {
              quoteText = '📷 Photo';
            } else if (pRawMedia === 'DOCUMENT' || pRawMedia === 'PDF') {
              quoteText = '📄 ' + (parentMsg.fileName || 'Document');
            } else if (pRawMedia === 'VIDEO') {
              quoteText = '🎬 Video';
            } else if (pRawMedia === 'AUDIO') {
              quoteText = '🎵 Audio';
            } else {
              quoteText = 'Attachment';
            }
            quoteSnippet = quoteText;
          }
        }
        if (!quoteSnippet && m.replyToText) {
          quoteSender = m.replyToSender || (isOutgoing ? (this.activeName || 'Member') : 'Staff');
          quoteText = m.replyToText.trim();
          quoteSnippet = quoteText;
        }

        const rawMediaType = String(m.mediaType || '').toUpperCase();
        const mediaUrlStr = String(m.mediaUrl || '');
        const isImg = m.mediaUrl && (
          rawMediaType === 'IMAGE' ||
          mediaUrlStr.match(/\.(jpg|jpeg|png|webp|gif)/i) ||
          (contentBody === '[Image]')
        );
        const isDoc = m.mediaUrl && (
          rawMediaType === 'DOCUMENT' ||
          mediaUrlStr.match(/\.pdf/i) ||
          (contentBody === '[Document]')
        );
        const isAudio = m.mediaUrl && (
          rawMediaType === 'AUDIO' ||
          mediaUrlStr.match(/\.(mp3|ogg|wav|m4a|aac|opus)/i) ||
          contentBody.includes('[Audio]')
        );
        const isVideo = m.mediaUrl && (
          rawMediaType === 'VIDEO' ||
          mediaUrlStr.match(/\.(mp4|webm|mov|mkv)/i) ||
          contentBody.includes('[Video]') ||
          contentBody.includes('[Video/GIF]')
        );

        let docName = (contentBody && contentBody.startsWith('📄 ')) ? contentBody.substring(2).trim() : null;
        if (!docName && isDoc && !contentBody.startsWith('📄 ') && contentBody.toLowerCase().endsWith('.pdf')) {
          docName = contentBody;
        }
        if (!docName && isDoc && (contentBody.toLowerCase().includes('receipt') || contentBody.toLowerCase().includes('payment'))) {
          const rawName = (this.activeName || 'Member').trim().split(/\s+/)[0];
          const dateStr = (m.timestamp ? new Date(m.timestamp) : new Date()).toISOString().slice(0, 10);
          docName = `Payment_Receipt_${rawName}_${dateStr}.pdf`;
        }
        if (!docName) {
          docName = 'Document.pdf';
        }

        const isDocDuplicate = isDoc && (contentBody === docName || contentBody === ('📄 ' + docName) || contentBody === '[Document]');
        const formattedContent = this.linkify(contentBody);
        const showText = formattedContent && 
          formattedContent !== '[Image]' && 
          formattedContent !== '[Document]' && 
          formattedContent !== '[Audio]' && 
          formattedContent !== '[Video]' && 
          formattedContent !== '🎬 [Video/GIF]' && 
          !isDocDuplicate;

        return `
          <div style="
            display: flex;
            justify-content: ${isOutgoing ? 'flex-end' : 'flex-start'};
          ">
            <div style="
              max-width: 72%;
              min-width: 140px;
              background: ${isOutgoing ? '#dcf8c6' : '#ffffff'};
              color: var(--fl-text, #0f172a);
              padding: 10px 14px 8px;
              border-radius: ${isOutgoing ? '12px 12px 2px 12px' : '12px 12px 12px 2px'};
              border: 1px solid ${isOutgoing ? '#c7eec0' : 'var(--fl-border, #e2e8f0)'};
              box-shadow: 0 1px 3px rgba(0,0,0,0.06);
              font-size: 13.5px;
              line-height: 1.5;
              position: relative;
            ">
              <!-- Sender Subtitle -->
              <div style="
                display: flex;
                align-items: center;
                justify-content: space-between;
                margin-bottom: 4px;
                gap: 8px;
              ">
                <span style="
                  font-size: 11px;
                  font-weight: 700;
                  color: ${isOutgoing ? '#065f46' : 'var(--fl-secondary, #232256)'};
                  text-transform: uppercase;
                  letter-spacing: 0.3px;
                ">
                  ${isOutgoing ? 'Staff' : this.activeName}
                </span>
              </div>

              <!-- Quoted / Reply Snippet (WhatsApp Native Style) -->
              ${quoteSnippet ? `
                <div style="
                  border-left: 4px solid ${isOutgoing ? '#059669' : '#2563eb'};
                  background: ${isOutgoing ? 'rgba(0, 0, 0, 0.05)' : 'rgba(37, 99, 235, 0.06)'};
                  padding: 5px 8px;
                  border-radius: 4px;
                  margin-bottom: 6px;
                  font-size: 12px;
                ">
                  <div style="font-weight: 700; font-size: 11px; color: ${isOutgoing ? '#065f46' : '#1d4ed8'}; margin-bottom: 2px;">
                    ${quoteSender}
                  </div>
                  <div style="white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 320px; color: #475569; font-size: 12px;">
                    ${quoteText}
                  </div>
                </div>
              ` : ''}

              <!-- Video Media Attachment -->
              ${isVideo ? `
                <div style="margin-bottom: 6px; border-radius: 6px; overflow: hidden; width: 100%; max-width: 380px; background: #000000;">
                  <video 
                    controls 
                    playsinline
                    src="${m.mediaUrl}" 
                    style="width: 100%; max-height: 320px; display: block;"
                  ></video>
                </div>
              ` : ''}

              <!-- Image Media Attachment -->
              ${isImg ? `
                <div class="fl-chat-img-thumb" data-url="${m.mediaUrl}" style="margin-bottom: 6px; cursor: pointer; border-radius: 6px; overflow: hidden; width: 100%; max-width: 380px; background: #ffffff;">
                  <img 
                    src="${m.mediaUrl}" 
                    alt="Photo" 
                    style="width: 100%; max-height: 320px; object-fit: contain; display: block;"
                    onerror="this.onerror=null; this.src='data:image/svg+xml,<svg xmlns=\\'http://www.w3.org/2000/svg\\' width=\\'200\\' height=\\'120\\'><rect fill=\\'%23cbd5e1\\' width=\\'200\\' height=\\'120\\'/><text fill=\\'%23475569\\' x=\\'50%\\' y=\\'50%\\' dominant-baseline=\\'middle\\' text-anchor=\\'middle\\' font-size=\\'13\\'>Photo</text></svg>';"
                  />
                </div>
              ` : ''}

              <!-- Document Media Attachment -->
              ${isDoc ? `
                <div style="margin-bottom: 6px; padding: 8px 10px; background: rgba(0,0,0,0.05); border-radius: 6px; border: 1px solid rgba(0,0,0,0.08); display: flex; align-items: center; justify-content: space-between; gap: 10px;">
                  <div style="display: flex; align-items: center; gap: 8px; min-width: 0;">
                    <span style="font-size: 22px;">📄</span>
                    <div style="min-width: 0;">
                      <div style="font-weight: 600; font-size: 12px; color: #1e293b; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                        ${docName}
                      </div>
                      <div style="font-size: 10.5px; color: #64748b;">PDF Document</div>
                    </div>
                  </div>
                  <a href="${m.mediaUrl}${m.mediaUrl.includes('?') ? '&' : '?'}filename=${encodeURIComponent(docName)}" target="_blank" download="${docName}" style="background: #25d366; color: #ffffff; text-decoration: none; padding: 4px 10px; border-radius: 4px; font-size: 11px; font-weight: 700; white-space: nowrap;">
                    Download ⤓
                  </a>
                </div>
              ` : ''}

              <!-- Audio Media Attachment -->
              ${isAudio ? `
                <div style="margin-bottom: 6px; padding: 4px 0;">
                  <audio controls src="${m.mediaUrl}" style="width: 100%; max-width: 320px; height: 38px; display: block;"></audio>
                </div>
              ` : ''}

              <!-- Content Body -->
              ${showText ? `
                <div style="word-break: break-word; white-space: pre-wrap; font-size: 13.5px; color: #1e293b;">${formattedContent}</div>
              ` : ''}
              
              <!-- Interactive Action Buttons (if template included buttons) -->
              ${m.buttons && m.buttons.length > 0 ? `
                <div style="margin-top: 8px; display: flex; flex-direction: column; gap: 4px; border-top: 1px solid ${isOutgoing ? '#bbf7d0' : 'var(--fl-border-light, #f1f5f9)'}; padding-top: 6px;">
                  ${m.buttons.map(b => `
                    <div style="
                      background: #ffffff;
                      border: 1px solid var(--fl-border, #cbd5e1);
                      border-radius: var(--fl-radius-sm, 6px);
                      padding: 6px 10px;
                      font-size: 12px;
                      font-weight: 600;
                      color: var(--fl-secondary, #232256);
                      text-align: center;
                      box-shadow: 0 1px 2px rgba(0,0,0,0.03);
                    ">
                      ${b.buttonText || b.text}
                    </div>
                  `).join('')}
                </div>
              ` : ''}

              <!-- Time & Status Indicators -->
              <div style="
                display: flex;
                align-items: center;
                justify-content: flex-end;
                gap: 4px;
                font-size: 10.5px;
                color: var(--fl-text-muted, #64748b);
                margin-top: 4px;
              ">
                <span>${timeStr || 'Just now'}</span>
                ${isOutgoing ? this.renderMessageStatus(m) : ''}
              </div>
            </div>
          </div>
        `;
      }).join('')}

      <!-- Escalation Notice Banner -->
      ${this.isHumanRequired ? `
        <div style="
          background: #fffbeb;
          border: 1px solid #fde68a;
          color: #92400e;
          border-radius: var(--fl-radius-md, 8px);
          padding: 12px 16px;
          font-size: 13px;
          display: flex;
          align-items: center;
          gap: 12px;
          margin-top: 4px;
          box-shadow: 0 1px 3px rgba(0,0,0,0.05);
          animation: flFadeIn 200ms ease-out;
        ">
          <span style="font-size: 20px;">🚨</span>
          <div style="flex: 1;">
            <strong style="color: #78350f;">Staff Response Requested:</strong>
            <div style="font-size: 12px; color: #92400e; margin-top: 2px;">
              Member has requested staff assistance. Sending a reply below will automatically acknowledge and fulfill this ticket.
            </div>
          </div>
        </div>
      ` : ''}
    `;
  }
}

export class FlChatInput extends HTMLElement {
  constructor() {
    super();
    this.isHumanRequired = false;
    this.currentTemplateText = '';
  }

  connectedCallback() {
    this.render();
  }

  setHumanRequired(required = false) {
    this.isHumanRequired = Boolean(required);
    this.currentTemplateText = '';
    this.render();
  }

  setTemplateText(text) {
    this.currentTemplateText = text;
    this.render();
  }

  setDisabled(disabled) {
    const input = this.querySelector('#fl-chat-msg-input');
    const sendBtn = this.querySelector('#fl-chat-send-btn');
    const templateBtn = this.querySelector('#fl-quick-template-btn');
    if (input) input.disabled = disabled;
    if (sendBtn) sendBtn.disabled = disabled;
    if (templateBtn) templateBtn.disabled = disabled;
  }

  render() {
    this.style.display = 'block';
    this.style.padding = '12px 18px';
    this.style.backgroundColor = '#ffffff';
    this.style.borderTop = '1px solid var(--fl-border, #e2e8f0)';

    if (this.isHumanRequired) {
      // Freeform staff chat enabled (escalation is active)
      this.innerHTML = `
        <div style="display: flex; align-items: center; gap: 10px;">
          <!-- Quick Template Trigger Button -->
          <button type="button" id="fl-quick-template-btn" style="
            display: flex;
            align-items: center;
            gap: 6px;
            background: #f1f5f9;
            border: 1px solid var(--fl-border, #e2e8f0);
            color: var(--fl-text, #0f172a);
            border-radius: var(--fl-radius-md, 8px);
            padding: 8px 14px;
            font-size: 12.5px;
            font-weight: 600;
            cursor: pointer;
            white-space: nowrap;
            transition: all var(--fl-transition-fast);
          " onmouseover="this.style.background='#e2e8f0'" onmouseout="this.style.background='#f1f5f9'">
            ${Icons.template}
            <span>Templates</span>
          </button>

          <!-- Message Input Field (Enabled) -->
          <input 
            type="text" 
            id="fl-chat-msg-input" 
            placeholder="Type a reply to member..." 
            value="${(this.currentTemplateText || '').replace(/"/g, '&quot;')}"
            autocomplete="off"
            style="
              flex: 1;
              height: 42px;
              padding: 0 16px;
              background: #f8fafc;
              border: 1px solid var(--fl-border, #e2e8f0);
              border-radius: var(--fl-radius-full, 9999px);
              font-size: 13.5px;
              color: var(--fl-text, #0f172a);
              outline: none;
              transition: all var(--fl-transition-fast);
            "
            onfocus="this.style.borderColor='#059669'; this.style.boxShadow='0 0 0 3px rgba(5, 150, 105, 0.12)'"
            onblur="this.style.borderColor='var(--fl-border, #e2e8f0)'; this.style.boxShadow='none'"
          />

          <!-- Send Button -->
          <button type="button" id="fl-chat-send-btn" title="Send Reply" style="
            width: 42px;
            height: 42px;
            border-radius: 50%;
            background-color: #059669;
            color: #ffffff;
            border: none;
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            flex-shrink: 0;
            box-shadow: 0 2px 8px rgba(5, 150, 105, 0.35);
            transition: all var(--fl-transition-fast);
          " onmouseover="this.style.background='#047857'; this.style.transform='scale(1.05)'" onmouseout="this.style.background='#059669'; this.style.transform='scale(1)'">
            ${Icons.send}
          </button>
        </div>
      `;
    } else {
      // Freeform chat disabled; only pre-approved template alerts allowed
      const hasTemplate = Boolean(this.currentTemplateText && this.currentTemplateText.trim());
      this.innerHTML = `
        <div style="display: flex; flex-direction: column; gap: 8px;">
          <!-- Restriction notice bar -->
          <div style="
            display: flex;
            align-items: center;
            justify-content: space-between;
            background: #f8fafc;
            border: 1px solid #e2e8f0;
            border-radius: 6px;
            padding: 6px 12px;
            font-size: 12px;
            color: #475569;
          ">
            <div style="display: flex; align-items: center; gap: 6px;">
              <span>ℹ️</span>
              <span><strong>Custom messaging locked:</strong> Freeform chat is disabled unless the customer requests a human agent. You can send pre-approved template alerts below.</span>
            </div>
          </div>

          <div style="display: flex; align-items: center; gap: 10px;">
            <!-- Prominent Template Alert Selector -->
            <button type="button" id="fl-quick-template-btn" style="
              display: flex;
              align-items: center;
              gap: 6px;
              background: #059669;
              border: 1px solid #047857;
              color: #ffffff;
              border-radius: var(--fl-radius-md, 8px);
              padding: 9px 16px;
              font-size: 12.5px;
              font-weight: 700;
              cursor: pointer;
              white-space: nowrap;
              box-shadow: 0 1px 3px rgba(5, 150, 105, 0.25);
              transition: all var(--fl-transition-fast);
            " onmouseover="this.style.background='#047857'" onmouseout="this.style.background='#059669'">
              ${Icons.template}
              <span>${hasTemplate ? 'Change Template' : 'Choose Template Alert'}</span>
            </button>

            <!-- Locked Input for template preview -->
            <input 
              type="text" 
              id="fl-chat-msg-input" 
              placeholder="Click 'Choose Template Alert' to select an approved template..." 
              value="${(this.currentTemplateText || '').replace(/"/g, '&quot;')}"
              readonly
              style="
                flex: 1;
                height: 42px;
                padding: 0 16px;
                background: ${hasTemplate ? '#ffffff' : '#f1f5f9'};
                border: 1px solid ${hasTemplate ? '#059669' : 'var(--fl-border, #e2e8f0)'};
                border-radius: var(--fl-radius-full, 9999px);
                font-size: 13px;
                color: ${hasTemplate ? '#0f172a' : 'var(--fl-text-muted, #94a3b8)'};
                cursor: pointer;
                outline: none;
              "
              title="Click to choose a template alert"
            />

            <!-- Send Button -->
            <button type="button" id="fl-chat-send-btn" title="${hasTemplate ? 'Send Template Alert' : 'Choose a template alert first'}" ${!hasTemplate ? 'disabled' : ''} style="
              width: 42px;
              height: 42px;
              border-radius: 50%;
              background-color: ${hasTemplate ? '#059669' : '#cbd5e1'};
              color: #ffffff;
              border: none;
              display: flex;
              align-items: center;
              justify-content: center;
              cursor: ${hasTemplate ? 'pointer' : 'not-allowed'};
              flex-shrink: 0;
              box-shadow: ${hasTemplate ? '0 2px 8px rgba(5, 150, 105, 0.35)' : 'none'};
              transition: all var(--fl-transition-fast);
            " ${hasTemplate ? `onmouseover="this.style.background='#047857'; this.style.transform='scale(1.05)'" onmouseout="this.style.background='#059669'; this.style.transform='scale(1)'"` : ''}>
              ${Icons.send}
            </button>
          </div>
        </div>
      `;
    }

    const input = this.querySelector('#fl-chat-msg-input');
    const sendBtn = this.querySelector('#fl-chat-send-btn');
    const templateBtn = this.querySelector('#fl-quick-template-btn');

    const handleSend = () => {
      const text = (input ? input.value.trim() : '') || this.currentTemplateText;
      if (!text) return;
      this.dispatchEvent(new CustomEvent('fl-chat-send', { bubbles: true, detail: { text } }));
      this.currentTemplateText = '';
      if (input) input.value = '';
      if (!this.isHumanRequired) {
        this.render();
      }
    };

    if (sendBtn) sendBtn.addEventListener('click', handleSend);
    if (input) {
      input.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
          handleSend();
        }
      });
      if (!this.isHumanRequired) {
        input.addEventListener('click', () => {
          this.dispatchEvent(new CustomEvent('fl-template-picker-open', { bubbles: true }));
        });
      }
    }

    if (templateBtn) {
      templateBtn.addEventListener('click', () => {
        this.dispatchEvent(new CustomEvent('fl-template-picker-open', { bubbles: true }));
      });
    }
  }
}

customElements.define('fl-chat-layout', FlChatLayout);
customElements.define('fl-chat-list', FlChatList);
customElements.define('fl-chat-thread', FlChatThread);
customElements.define('fl-chat-input', FlChatInput);
