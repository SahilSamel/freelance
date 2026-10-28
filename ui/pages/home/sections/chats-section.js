/**
 * <fl-chats-section> Native Web Component
 * Professional WhatsApp Business API Messaging Console.
 * Built strictly according to the alerts module architecture:
 * - Meta Pre-Approved HSM Templates (Utility, Marketing, Authentication)
 * - Human Agent Attention Escalation State Machine (requiresHumanAgent / Needs Attention)
 * - Direct Staff Replies via WhatsApp Business API (clearing human attention flag)
 * - Media Attachments (Images & PDF Documents)
 */

import { HomeStore } from '../home-store.js';
import { AppConfig } from '../../../core/config/app-config.js';
import { ErrorMapper } from '../../../core/error/error-mapper.js';
import '../../../core/components/shell/fl-avatar.js';

const WA_DOODLE_LIGHT = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='240' height='240' viewBox='0 0 240 240'%3E%3Cdefs%3E%3Cpattern id='p' width='120' height='120' patternUnits='userSpaceOnUse'%3E%3Cpath d='M20 18h14a6 6 0 0 1 6 6v8a6 6 0 0 1-6 6h-6l-5 5v-5h-3a6 6 0 0 1-6-6v-8a6 6 0 0 1 6-6z' fill='none' stroke='%23000000' stroke-width='1.2' stroke-opacity='0.06' stroke-linejoin='round'/%3E%3Cpath d='M85 25a8 8 0 1 1-16 0 8 8 0 0 1 16 0z' fill='none' stroke='%23000000' stroke-width='1.2' stroke-opacity='0.06'/%3E%3Cpath d='M77 21v8m-4-4h8' fill='none' stroke='%23000000' stroke-width='1.2' stroke-opacity='0.06' stroke-linecap='round'/%3E%3Cpath d='M22 80l6-6 6 6-6 6z' fill='none' stroke='%23000000' stroke-width='1.2' stroke-opacity='0.06'/%3E%3Cpath d='M75 75c-4-6-11-4-11 4 0 7 11 12 11 12s11-5 11-12c0-8-7-10-11-4z' fill='none' stroke='%23000000' stroke-width='1.2' stroke-opacity='0.06'/%3E%3Cpath d='M10 50h8m-4-4v8' fill='none' stroke='%23000000' stroke-width='1.2' stroke-opacity='0.05' stroke-linecap='round'/%3E%3Ccircle cx='105' cy='55' r='3' fill='none' stroke='%23000000' stroke-width='1.2' stroke-opacity='0.06'/%3E%3Ccircle cx='45' cy='105' r='2.5' fill='none' stroke='%23000000' stroke-width='1' stroke-opacity='0.05'/%3E%3Cpath d='M95 100h12v12h-12z' fill='none' stroke='%23000000' stroke-width='1.2' stroke-opacity='0.06'/%3E%3C/pattern%3E%3C/defs%3E%3Crect width='100%25' height='100%25' fill='%23efeae2'/%3E%3Crect width='100%25' height='100%25' fill='url(%23p)'/%3E%3C/svg%3E";

function formatWhatsAppText(text) {
  return AppConfig.formatWhatsAppText(text);
}

export class FlChatsSection extends HTMLElement {
  constructor() {
    super();
    this.pendingAttachment = null;
    this._isLoadingOlder = false;
  }

  connectedCallback() {
    this.unsubscribe = HomeStore.subscribe(() => this.render());
    HomeStore.loadAllConversations();
    const state = HomeStore.getState();
    if (!state.activeChatId) {
      const filtered = HomeStore.getFilteredConversations();
      if (filtered.length > 0) {
        HomeStore.setActiveChatId(filtered[0].id);
      }
    } else {
      const conv = (state.conversations || []).find(c => String(c.id) === String(state.activeChatId));
      if (!conv || !Array.isArray(conv.messages) || conv.messages.length === 0) {
        HomeStore.loadConversationTimeline(state.activeChatId);
      }
    }
    this.render();
  }

  disconnectedCallback() {
    if (this.unsubscribe) this.unsubscribe();
  }

  render() {
    const activeEl = document.activeElement;
    const isChatSearchFocused = !!(activeEl && (activeEl.id === 'fl-chat-search' || activeEl === this.querySelector('#fl-chat-search')));
    const selStart = isChatSearchFocused ? activeEl.selectionStart : null;
    const selEnd = isChatSearchFocused ? activeEl.selectionEnd : null;

    const chatInputEl = this.querySelector('#fl-chat-input');
    const isChatInputFocused = !!(activeEl && (activeEl.id === 'fl-chat-input' || activeEl === chatInputEl));
    const savedChatInputValue = chatInputEl ? chatInputEl.value : '';
    const chatInputSelStart = isChatInputFocused && chatInputEl ? chatInputEl.selectionStart : null;
    const chatInputSelEnd = isChatInputFocused && chatInputEl ? chatInputEl.selectionEnd : null;

    const state = HomeStore.getState();
    const { conversations, activeChatId, chatFilterTab, chatSearchQuery } = state;
    const filteredConvs = HomeStore.getFilteredConversations();
    const activeConv = filteredConvs.find(c => String(c.id) === String(activeChatId)) || (filteredConvs.length > 0 ? filteredConvs[0] : null);
    const isHumanRequired = Boolean(activeConv && activeConv.isHumanRequired);

    const prevMsgContainer = this.querySelector('#fl-messages-container');
    const isChatSwitch = this._currentRenderedChatId && activeConv && String(this._currentRenderedChatId) !== String(activeConv.id);
    const wasNearBottom = isChatSwitch ? true : (prevMsgContainer ? (prevMsgContainer.scrollHeight - prevMsgContainer.scrollTop - prevMsgContainer.clientHeight < 80) : true);
    const prevScrollTop = isChatSwitch ? null : (prevMsgContainer ? prevMsgContainer.scrollTop : null);
    const prevScrollHeight = isChatSwitch ? null : (prevMsgContainer ? prevMsgContainer.scrollHeight : null);
    if (activeConv) {
      this._currentRenderedChatId = activeConv.id;
    }

    const totalCount = HomeStore.getActiveChatsCount();
    const needsAttentionCount = HomeStore.getNeedsAttentionCount();
    const activeCount = HomeStore.getActiveChatsCount();

    // Quick templates available for active member
    const allTemplates = AppConfig.getQuickTemplates();

    this.innerHTML = `
      <style>
        @keyframes flMsgHighlight {
          0% { background-color: rgba(37, 211, 102, 0.45); border-radius: 8px; }
          60% { background-color: rgba(37, 211, 102, 0.25); border-radius: 8px; }
          100% { background-color: transparent; }
        }
        .fl-msg-highlight > div {
          animation: flMsgHighlight 1.6s ease-out;
        }
        @keyframes flSpin {
          to { transform: rotate(360deg); }
        }
        .fl-chat-quote-card:hover {
          filter: brightness(0.95);
        }
      </style>
      <div style="
        display: grid;
        grid-template-columns: 360px 1fr;
        flex: 1;
        height: 100%;
        min-height: 0;
        background: #ffffff;
        border: 1px solid #d1d7db;
        border-radius: 12px;
        overflow: hidden;
        box-shadow: 0 4px 20px rgba(11, 20, 26, 0.08);
        position: relative;
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
      ">
        
        <!-- ======================================================== -->
        <!-- LEFT COLUMN: CONVERSATIONS SIDEBAR                      -->
        <!-- ======================================================== -->
        <div style="
          border-right: 1px solid #e9edef; 
          display: flex; 
          flex-direction: column; 
          background: #ffffff; 
          height: 100%; 
          min-height: 0; 
          overflow: hidden;
        ">
          <!-- Top Header: WhatsApp Title & Count -->
          <div style="
            padding: 10px 16px; 
            height: 60px;
            box-sizing: border-box;
            border-bottom: 1px solid #f0f2f5; 
            background: #ffffff;
            display: flex; 
            align-items: center; 
            justify-content: space-between; 
            flex-shrink: 0;
          ">
            <div style="display: flex; align-items: center; gap: 10px;">
              <span style="font-weight: 700; font-size: 20px; color: #111b21; letter-spacing: -0.2px;">Chats</span>
            </div>
            <div style="display: flex; align-items: center; gap: 10px;">
              <span style="font-size: 12.5px; color: #54656f; font-weight: 500;">
                ${totalCount} active
              </span>
              <div style="
                width: 30px; 
                height: 30px; 
                border-radius: 50%; 
                background: #25d366; 
                display: flex; 
                align-items: center; 
                justify-content: center; 
                color: #ffffff;
                box-shadow: 0 1px 3px rgba(37,211,102,0.3);
              " title="WhatsApp Business Active">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12.04 2C6.58 2 2.13 6.45 2.13 11.91C2.13 13.66 2.59 15.36 3.45 16.86L2.05 22L7.3 20.63C8.75 21.41 10.38 21.83 12.04 21.83C17.5 21.83 21.95 17.38 21.95 11.92C21.95 6.46 17.5 2 12.04 2M12.05 3.67C16.58 3.67 20.28 7.37 20.28 11.91C20.28 16.45 16.58 20.15 12.04 20.15C10.56 20.15 9.13 19.75 7.89 19L7.54 18.79L4.43 19.61L5.26 16.58L5.03 16.22C4.22 14.93 3.8 13.44 3.8 11.91C3.8 7.37 7.5 3.67 12.05 3.67Z"/>
                </svg>
              </div>
            </div>
          </div>

          <!-- Search Input Box -->
          <div style="padding: 8px 12px; background: #ffffff; border-bottom: 1px solid #f0f2f5; flex-shrink: 0;">
            <div style="position: relative; width: 100%;">
              <span style="position: absolute; left: 12px; top: 50%; transform: translateY(-50%); color: #54656f; display: flex; pointer-events: none;">
                <svg width="15" height="15" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="8.5" cy="8.5" r="5.5"></circle>
                  <path d="M13 13l4.5 4.5"></path>
                </svg>
              </span>
              <input 
                type="text" 
                id="fl-chat-search" 
                placeholder="Search or start a new chat" 
                value="${chatSearchQuery || ''}"
                style="
                  width: 100%; 
                  height: 35px; 
                  padding: 0 10px 0 38px; 
                  border: none; 
                  border-radius: 8px; 
                  font-size: 13.5px; 
                  background: #f0f2f5;
                  color: #111b21;
                  outline: none;
                  box-sizing: border-box;
                  transition: all 0.15s;
                "
              />
            </div>
          </div>

          <!-- Conversations Scroll List -->
          <div style="flex: 1; min-height: 0; overflow-y: auto; background: #ffffff;">
            ${filteredConvs.length === 0 ? `
              <div style="padding: 32px 20px; text-align: center; color: #667781; font-size: 13px;">
                No conversations found.
              </div>
            ` : filteredConvs.map(conv => {
              const isSelected = activeConv && String(activeConv.id) === String(conv.id);
              const initials = (conv.name || 'Member')
                .split(' ')
                .map(n => n.charAt(0))
                .slice(0, 2)
                .join('')
                .toUpperCase();

              return `
                <div 
                  class="fl-conv-item ${isSelected ? 'selected' : ''}" 
                  data-id="${conv.id}"
                  style="
                    height: 72px;
                    padding: 0 16px;
                    border-bottom: 1px solid #f0f2f5;
                    background: ${isSelected ? '#f0f2f5' : 'transparent'};
                    cursor: pointer;
                    display: flex;
                    align-items: center;
                    gap: 14px;
                    transition: background 0.15s ease;
                  "
                  onmouseover="if (!${isSelected}) this.style.background='#f5f6f6';"
                  onmouseout="if (!${isSelected}) this.style.background='transparent';"
                >
                  <!-- Avatar Container -->
                  <div style="position: relative; flex-shrink: 0;">
                    <div style="
                      width: 48px; 
                      height: 48px; 
                      border-radius: 50%; 
                      background: ${isSelected ? '#d9fdd3' : '#dfe5e7'}; 
                      color: ${isSelected ? '#008069' : '#54656f'};
                      display: flex; 
                      align-items: center; 
                      justify-content: center; 
                      font-weight: 600;
                      font-size: 15px;
                    ">
                      ${initials}
                    </div>
                  </div>

                  <!-- Details -->
                  <div style="flex: 1; min-width: 0;">
                    <div style="display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 4px;">
                      <span style="
                        font-weight: 500; 
                        font-size: 16px; 
                        color: #111b21; 
                        overflow: hidden; 
                        text-overflow: ellipsis; 
                        white-space: nowrap;
                      ">
                        ${conv.name}
                      </span>
                      <span style="font-size: 12px; color: ${conv.unreadCount > 0 ? '#25d366' : '#667781'}; font-weight: ${conv.unreadCount > 0 ? '600' : '400'}; white-space: nowrap;">
                        ${conv.lastTime || ''}
                      </span>
                    </div>

                    <div style="
                      font-size: 13.5px; 
                      color: #667781; 
                      font-weight: ${conv.unreadCount > 0 ? '600' : '400'};
                      overflow: hidden; 
                      text-overflow: ellipsis; 
                      white-space: nowrap;
                    ">
                      ${conv.lastMessage || 'No messages yet'}
                    </div>
                  </div>

                  <!-- Unread indicator badge -->
                  ${conv.unreadCount > 0 ? `
                    <span style="
                      background: #25d366; 
                      color: #ffffff; 
                      font-size: 11px; 
                      font-weight: 700; 
                      border-radius: 9999px; 
                      min-width: 20px;
                      height: 20px;
                      padding: 0 6px;
                      display: flex;
                      align-items: center;
                      justify-content: center;
                      flex-shrink: 0;
                    ">
                      ${conv.unreadCount}
                    </span>
                  ` : ''}
                </div>
              `;
            }).join('')}
          </div>
        </div>

        <!-- ======================================================== -->
        <!-- RIGHT COLUMN: CHAT THREAD & COMPOSER                     -->
        <!-- ======================================================== -->
        <div style="
          display: flex; 
          flex-direction: column; 
          height: 100%; 
          min-height: 0; 
          background: #efeae2; 
          overflow: hidden;
        ">
          ${!activeConv ? `
            <div style="display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; color: #667781; background: #f0f2f5;">
              <div style="font-size: 40px; margin-bottom: 12px;">💬</div>
              <div style="font-weight: 600; font-size: 16px; color: #111b21;">WhatsApp Web</div>
              <div style="font-size: 13px; color: #667781; margin-top: 4px;">Select a conversation to view and send messages</div>
            </div>
          ` : `
            <!-- Top Chat Header -->
            <div style="
              padding: 10px 18px; 
              height: 60px;
              box-sizing: border-box;
              background: #f0f2f5; 
              border-bottom: 1px solid #e9edef; 
              display: flex; 
              align-items: center; 
              justify-content: space-between; 
              flex-shrink: 0;
            ">
              <div style="display: flex; align-items: center; gap: 12px;">
                <div style="
                  width: 40px; 
                  height: 40px; 
                  border-radius: 50%; 
                  background: #dfe5e7; 
                  color: #54656f; 
                  display: flex; 
                  align-items: center; 
                  justify-content: center; 
                  font-weight: 600; 
                  font-size: 15px;
                ">
                  ${(activeConv.name || 'M').charAt(0)}
                </div>
                <div>
                  <div style="display: flex; align-items: center; gap: 8px;">
                    <span 
                      class="fl-chat-member-link" 
                      data-member-id="${activeConv.memberId || activeConv.id}"
                      style="
                        font-weight: 500; 
                        font-size: 16px; 
                        color: #111b21; 
                        cursor: pointer;
                        transition: color 0.15s ease;
                      "
                      title="Open member profile & payment plans"
                      onmouseover="this.style.color='#008069';"
                      onmouseout="this.style.color='#111b21';"
                    >
                      ${activeConv.name}
                    </span>
                    ${(() => {
                      const memberId = activeConv.memberId || activeConv.id;
                      const member = (HomeStore.getState().members || []).find(m => m.id === memberId);
                      const subs = member ? (member.subscriptions || []) : [];
                      const isPros = (activeConv.status || '').toLowerCase() === 'prospect' || (member ? subs.length === 0 : false);
                      const isExp = !isPros && ((activeConv.status || '').toLowerCase() === 'expired' || (activeConv.statusLabel || '').toLowerCase() === 'expired');

                      let text = 'Active';
                      let bg = '#ecfdf5';
                      let color = '#16a34a';
                      let border = '#a7f3d0';

                      if (isPros) {
                        text = 'Prospect';
                        bg = '#eff6ff';
                        color = '#1d4ed8';
                        border = '#bfdbfe';
                      } else if (isExp) {
                        text = 'Expired';
                        bg = '#f1f5f9';
                        color = '#475569';
                        border = '#cbd5e1';
                      }

                      return `
                        <span style="
                          background: ${bg}; 
                          color: ${color}; 
                          border: 1px solid ${border};
                          border-radius: 9999px; 
                          padding: 1px 8px; 
                          font-size: 11px; 
                          font-weight: 600;
                        ">
                          ${text}
                        </span>
                      `;
                    })()}

                    <!-- Open Profile Button -->
                    <button 
                      type="button" 
                      id="fl-chat-open-profile-btn" 
                      class="fl-btn fl-btn-secondary" 
                      data-member-id="${activeConv.memberId || activeConv.id}"
                      title="Click to view and edit member profile, plans, and payments"
                      style="
                        height: 24px;
                        padding: 0 8px;
                        font-size: 11.5px;
                        margin-left: 4px;
                      "
                    >
                      Open Profile
                    </button>
                  </div>
                  <div style="font-size: 12px; color: #667781; margin-top: 1px;">
                    <span>${activeConv.phone || '+91 9820011223'}</span>
                  </div>
                </div>
              </div>

              <!-- Header Right Controls -->
              <div style="display: flex; align-items: center; gap: 10px;">
              </div>
            </div>

            <!-- Messages Stream Area with WhatsApp Doodle Wallpaper -->
            <div id="fl-messages-container" style="
              flex: 1; 
              min-height: 0; 
              overflow-y: auto; 
              padding: 16px 24px; 
              display: flex; 
              flex-direction: column; 
              gap: 6px;
              background-color: #efeae2;
              background-image: url('${WA_DOODLE_LIGHT}');
              background-repeat: repeat;
              background-size: 240px 240px;
            ">
              ${(activeConv && activeConv._loadingOlder) ? `
                <div style="display: flex; align-items: center; justify-content: center; gap: 8px; padding: 6px 0; color: #54656f; font-size: 12px; font-weight: 500;">
                  <span style="display: inline-block; width: 14px; height: 14px; border: 2px solid #54656f; border-top-color: transparent; border-radius: 50%; animation: flSpin 0.7s linear infinite;"></span>
                  Loading older messages...
                </div>
              ` : ''}
              ${(!activeConv.messages || activeConv.messages.length === 0) ? `
                <!-- Empty Conversation State -->
                <div style="
                  margin: auto; 
                  max-width: 380px; 
                  text-align: center; 
                  padding: 28px 20px; 
                  background: #ffffff; 
                  border: 1px solid #e2e8f0; 
                  border-radius: 12px; 
                  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
                ">
                  <div style="
                    width: 48px; 
                    height: 48px; 
                    border-radius: 50%; 
                    background: #d9fdd3; 
                    color: #008069; 
                    display: inline-flex; 
                    align-items: center; 
                    justify-content: center; 
                    font-size: 22px; 
                    margin-bottom: 10px;
                  ">
                    💬
                  </div>
                  <div style="font-size: 15px; font-weight: 600; color: #111b21; margin-bottom: 6px;">
                    No Conversation History
                  </div>
                  <div style="font-size: 13px; color: #667781; line-height: 1.5;">
                    Send a WhatsApp reply or choose a pre-approved template below to initiate messaging with ${activeConv.name}.
                  </div>
                </div>
              ` : (() => {
                let lastDateKey = null;
                const today = new Date();
                const yesterday = new Date();
                yesterday.setDate(today.getDate() - 1);

                const todayKey = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
                const yesterdayKey = `${yesterday.getFullYear()}-${String(yesterday.getMonth() + 1).padStart(2, '0')}-${String(yesterday.getDate()).padStart(2, '0')}`;

                return activeConv.messages.map(msg => {
                  let dateBadgeHtml = '';
                  const msgDate = msg.timestamp ? new Date(msg.timestamp) : null;
                  if (msgDate && !isNaN(msgDate.getTime())) {
                    const dateKey = `${msgDate.getFullYear()}-${String(msgDate.getMonth() + 1).padStart(2, '0')}-${String(msgDate.getDate()).padStart(2, '0')}`;
                    if (dateKey !== lastDateKey) {
                      lastDateKey = dateKey;
                      let label = '';
                      if (dateKey === todayKey) {
                        label = 'TODAY';
                      } else if (dateKey === yesterdayKey) {
                        label = 'YESTERDAY';
                      } else {
                        // e.g. "OCTOBER 15, 2026" or "15/10/2026"
                        label = msgDate.toLocaleDateString(undefined, {
                          day: 'numeric',
                          month: 'short',
                          year: msgDate.getFullYear() !== today.getFullYear() ? 'numeric' : undefined
                        }).toUpperCase();
                      }
                      dateBadgeHtml = `
                        <div style="
                          align-self: center; 
                          background: #ffffff; 
                          color: #54656f; 
                          font-size: 12px; 
                          font-weight: 500; 
                          padding: 5px 12px; 
                          border-radius: 7.5px; 
                          box-shadow: 0 1px 0.5px rgba(11,20,26,0.13); 
                          margin: 10px 0 6px 0; 
                          text-transform: uppercase; 
                          letter-spacing: 0.3px;
                          user-select: none;
                        ">${label}</div>
                      `;
                    }
                  } else if (!lastDateKey) {
                    lastDateKey = 'today';
                    dateBadgeHtml = `
                      <div style="
                        align-self: center; 
                        background: #ffffff; 
                        color: #54656f; 
                        font-size: 12px; 
                        font-weight: 500; 
                        padding: 5px 12px; 
                        border-radius: 7.5px; 
                        box-shadow: 0 1px 0.5px rgba(11,20,26,0.13); 
                        margin: 10px 0 6px 0; 
                        text-transform: uppercase; 
                        letter-spacing: 0.3px;
                        user-select: none;
                      ">TODAY</div>
                    `;
                  }
                const isOut = msg.isOutgoing;
                const textBody = msg.text || '';

                // Quoted reply resolution (Native replyToMessageId)
                let quoteSnippet = null;
                const cleanTextBody = textBody;
                let quoteSender = isOut ? 'Staff' : 'Member';
                let quoteText = '';
                const targetMsgId = msg.replyToMessageId || null;
                const targetMsg = targetMsgId && activeConv?.messages ? activeConv.messages.find(other => String(other.id) === String(targetMsgId)) : null;

                // Native relational quote resolution from parent message
                if (targetMsg) {
                  const targetIsOut = targetMsg.isOutgoing || targetMsg.direction === 'OUTBOUND';
                  quoteSender = targetIsOut ? 'Staff' : (activeConv?.name || 'Member');
                  const tRawMedia = String(targetMsg.mediaType || '').toUpperCase();
                  if (targetMsg.text && targetMsg.text.trim()) {
                    quoteText = targetMsg.text.trim();
                  } else if (tRawMedia === 'IMAGE') {
                    quoteText = '📷 Photo';
                  } else if (tRawMedia === 'DOCUMENT' || tRawMedia === 'PDF') {
                    quoteText = '📄 ' + (targetMsg.fileName || 'Document');
                  } else if (tRawMedia === 'VIDEO') {
                    quoteText = '🎬 Video';
                  } else if (tRawMedia === 'AUDIO') {
                    quoteText = '🎵 Audio';
                  } else {
                    quoteText = 'Attachment';
                  }
                  quoteSnippet = quoteText;
                } else if (msg.replyToText) {
                  const rawSender = msg.replyToSender || '';
                  quoteSender = (rawSender === 'Staff') ? 'Staff' : (activeConv?.name || 'Member');
                  quoteText = msg.replyToText.trim();
                  quoteSnippet = quoteText;
                }

                // Self-reply vs Opposite Party Reply Colors:
                // Self-reply: Staff replying to Staff, or Member replying to Member -> GREEN (#00a884 / #008069)
                // Opposite party: Member replying to Staff, or Staff replying to Member -> BLUE/CYAN (#0284c7)
                const isSelfReply = targetMsg
                  ? (targetMsg.direction === msg.direction)
                  : ((isOut && (quoteSender === 'Staff' || quoteSender === 'You')) ||
                     (!isOut && (quoteSender === 'Member' || quoteSender === 'You' || quoteSender === (activeConv?.name || ''))));

                const quoteBorderColor = isSelfReply ? '#00a884' : '#0284c7';
                const quoteTextColor = isSelfReply ? '#008069' : '#0284c7';
                const quoteBgColor = isOut ? 'rgba(0,0,0,0.06)' : (isSelfReply ? 'rgba(0,168,132,0.08)' : 'rgba(2,132,199,0.08)');
                const displaySender = isSelfReply ? 'You' : (isOut ? (activeConv?.name || 'Member') : 'Staff');

                // Media detection
                const rawMediaType = String(msg.mediaType || '').toUpperCase();
                const mediaUrlStr = String(msg.mediaUrl || '');
                const isImg = msg.mediaUrl && (rawMediaType === 'IMAGE' || cleanTextBody === '[Image]' || mediaUrlStr.match(/\.(jpg|jpeg|png|webp|gif)/i));
                const isDoc = msg.mediaUrl && (rawMediaType === 'DOCUMENT' || cleanTextBody === '[Document]' || mediaUrlStr.match(/\.pdf/i));
                const isAudio = msg.mediaUrl && (rawMediaType === 'AUDIO' || cleanTextBody.includes('[Audio]') || mediaUrlStr.match(/\.(mp3|ogg|wav|m4a|aac|opus)/i));
                const isVideo = msg.mediaUrl && (rawMediaType === 'VIDEO' || cleanTextBody.includes('[Video]') || cleanTextBody.includes('[Video/GIF]') || mediaUrlStr.match(/\.(mp4|webm|mov|mkv)/i));

                let docName = msg.fileName;
                if (!docName) {
                  if (cleanTextBody && cleanTextBody.startsWith('📄 ')) {
                    docName = cleanTextBody.substring(2).trim();
                  } else if (cleanTextBody && cleanTextBody.toLowerCase().endsWith('.pdf')) {
                    docName = cleanTextBody;
                  } else if (isDoc && (cleanTextBody.toLowerCase().includes('receipt') || cleanTextBody.toLowerCase().includes('payment'))) {
                    const rawName = (activeConv?.name || 'Member').trim().split(/\s+/)[0];
                    const dateStr = (msg.timestamp ? new Date(msg.timestamp) : new Date()).toISOString().slice(0, 10);
                    docName = `Payment_Receipt_${rawName}_${dateStr}.pdf`;
                  } else {
                    docName = 'Attachment.pdf';
                  }
                }

                const isDocDuplicate = isDoc && (cleanTextBody === docName || cleanTextBody === ('📄 ' + docName) || cleanTextBody === '[Document]');
                const showText = cleanTextBody && 
                  cleanTextBody !== '[Image]' && 
                  cleanTextBody !== '[Document]' && 
                  cleanTextBody !== '[Audio]' && 
                  cleanTextBody !== '[Video]' && 
                  cleanTextBody !== '🎬 [Video/GIF]' && 
                  !isDocDuplicate;
                const formattedText = formatWhatsAppText(cleanTextBody);

                const isBtnReply = (msg.messageType || '').toUpperCase() === 'BUTTON_REPLY';
                const isConsent = !isOut && (cleanTextBody === 'STOP' || cleanTextBody === 'START');

                return dateBadgeHtml + `
                  <div id="msg-${msg.id}" data-msg-id="${msg.id}" style="display: flex; justify-content: ${isOut ? 'flex-end' : 'flex-start'}; margin: 3px 0;">
                    <div style="
                      max-width: 440px;
                      min-width: 140px;
                      width: fit-content;
                      padding: ${isImg ? '4px 4px 6px 4px' : '6px 9px 8px 9px'};
                      border-radius: 8px;
                      ${isOut ? 'border-top-right-radius: 0px;' : 'border-top-left-radius: 0px;'}
                      background: ${isOut ? '#d9fdd3' : '#ffffff'};
                      box-shadow: 0 1px 0.5px rgba(11,20,26,0.13);
                      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                      font-size: 14.2px;
                      line-height: 19px;
                      color: #111b21;
                      position: relative;
                      word-break: break-word;
                    ">
                      <!-- Quoted Reply Card (WhatsApp Style) -->
                      ${quoteSnippet ? `
                        <div 
                          class="fl-chat-quote-card" 
                          ${targetMsgId ? `data-target-id="${targetMsgId}"` : ''} 
                          style="
                            border-left: 4px solid ${quoteBorderColor};
                            background: ${quoteBgColor};
                            border-radius: 4px;
                            padding: 5px 8px;
                            margin-bottom: 6px;
                            font-size: 12px;
                            cursor: ${targetMsgId ? 'pointer' : 'default'};
                            user-select: none;
                            transition: filter 0.15s ease;
                          "
                          title="${targetMsgId ? 'Click to jump to quoted message' : ''}"
                        >
                          <div style="font-weight: 700; font-size: 11.5px; color: ${quoteTextColor}; margin-bottom: 2px;">
                            ${displaySender}
                          </div>
                          <div style="color: #54656f; font-size: 12px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 340px;">
                            ${quoteText}
                          </div>
                        </div>
                      ` : ''}

                      <!-- Context Banner for Button Quick-Replies -->
                      ${isBtnReply ? `
                        <div style="
                          display: flex;
                          align-items: center;
                          gap: 5px;
                          font-size: 11px;
                          font-weight: 600;
                          color: #008069;
                          background: #e8f5e9;
                          border-left: 3px solid #00a884;
                          border-radius: 4px;
                          padding: 3px 8px;
                          margin-bottom: 6px;
                          letter-spacing: 0.2px;
                        ">
                          <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor">
                            <path d="M10 9V5l-7 7 7 7v-4.1c5 0 8.5 1.6 11 5.1-1-5-4-10-11-11z"/>
                          </svg>
                          <span>QUICK REPLY SELECTED</span>
                        </div>
                      ` : ''}

                      <!-- Context Banner for Consent Keywords -->
                      ${isConsent ? `
                        <div style="
                          display: flex;
                          align-items: center;
                          gap: 5px;
                          font-size: 10.5px;
                          font-weight: 600;
                          color: ${cleanTextBody === 'STOP' ? '#dc2626' : '#15803d'};
                          background: ${cleanTextBody === 'STOP' ? '#fee2e2' : '#dcfce7'};
                          border-radius: 4px;
                          padding: 2px 7px;
                          margin-bottom: 5px;
                          width: fit-content;
                        ">
                          <span>${cleanTextBody === 'STOP' ? '🚫 OPT-OUT REQUEST' : '✅ OPT-IN CONFIRMATION'}</span>
                        </div>
                      ` : ''}

                      <!-- Video Media Attachment -->
                      ${isVideo ? `
                        <div style="
                          margin-bottom: ${showText ? '6px' : '0'};
                          border-radius: 6px; 
                          overflow: hidden; 
                          width: 100%;
                          max-width: 380px;
                          background: #000000;
                        ">
                          <video 
                            controls 
                            playsinline
                            src="${msg.mediaUrl}" 
                            style="width: 100%; max-height: 320px; display: block;"
                          ></video>
                        </div>
                      ` : ''}

                      <!-- Audio Media Attachment -->
                      ${isAudio ? `
                        <div style="margin-bottom: ${showText ? '6px' : '0'}; padding: 4px 0;">
                          <audio controls src="${msg.mediaUrl}" style="width: 100%; max-width: 320px; height: 38px; display: block;"></audio>
                        </div>
                      ` : ''}

                      <!-- Image Media Attachment -->
                      ${isImg ? `
                        <div class="fl-chat-img-thumb" data-url="${msg.mediaUrl}" style="
                          cursor: pointer; 
                          border-radius: ${showText ? '6px 6px 4px 4px' : '6px'}; 
                          overflow: hidden; 
                          width: 100%;
                          background: #f1f5f9; 
                          display: flex; 
                          flex-direction: column;
                          align-items: center; 
                          justify-content: center;
                          margin-bottom: ${showText ? '6px' : '0'};
                          border: 1px solid #e2e8f0;
                          min-height: 140px;
                          position: relative;
                        ">
                          <img 
                            src="${msg.mediaUrl}" 
                            alt="Attachment" 
                            style="width: 100%; max-height: 340px; object-fit: contain; display: block;" 
                            onerror="this.style.display='none'; this.nextElementSibling.style.display='flex';"
                          />
                          <div style="display: none; flex-direction: column; align-items: center; justify-content: center; padding: 24px 16px; text-align: center; gap: 8px;">
                            <div style="width: 44px; height: 44px; border-radius: 50%; background: #e2e8f0; display: flex; align-items: center; justify-content: center; font-size: 22px;">
                              🖼️
                            </div>
                            <div style="font-size: 13px; font-weight: 600; color: #334155;">Photo Attachment</div>
                            <div style="font-size: 11px; color: #64748b;">Tap to view in high resolution</div>
                          </div>
                        </div>
                      ` : ''}

                      <!-- Document Media Attachment -->
                      ${isDoc ? `
                        <div style="
                          margin-bottom: ${showText ? '6px' : '0'};
                          padding: 8px 12px;
                          background: rgba(0,0,0,0.04);
                          border-radius: 6px;
                          display: flex;
                          align-items: center;
                          justify-content: space-between;
                          gap: 12px;
                        ">
                          <div style="display: flex; align-items: center; gap: 8px; min-width: 0;">
                            <span style="font-size: 20px;">📄</span>
                            <div style="min-width: 0;">
                              <div style="font-weight: 600; font-size: 12px; color: #1e293b; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                                ${docName}
                              </div>
                              <div style="font-size: 10.5px; color: #64748b;">PDF Document</div>
                            </div>
                          </div>
                          <a href="${msg.mediaUrl}${msg.mediaUrl.includes('?') ? '&' : '?'}filename=${encodeURIComponent(docName)}" target="_blank" download="${docName}" style="
                            background: #00a884;
                            color: #ffffff;
                            text-decoration: none;
                            padding: 3px 8px;
                            border-radius: 4px;
                            font-size: 11px;
                            font-weight: 600;
                            white-space: nowrap;
                          ">
                            Download
                          </a>
                        </div>
                      ` : ''}

                      <!-- Text Body & Timestamp Container -->
                      <div style="
                        padding: ${isImg ? '2px 6px 2px 6px' : '0'};
                        position: relative;
                      ">
                        ${showText ? `
                          <div style="
                            white-space: pre-wrap; 
                            font-size: 14.2px; 
                            line-height: 19px; 
                            color: #111b21;
                          ">${formattedText}</div>
                        ` : ''}

                        <!-- Timestamp & Status Checkmarks (Aligned to bottom-right of text block) -->
                        <div style="
                          display: flex;
                          align-items: center;
                          justify-content: flex-end;
                          gap: 3px;
                          margin-top: 2px;
                          margin-bottom: ${(msg.buttons && msg.buttons.length > 0) ? '6px' : '0'};
                          font-size: 11px;
                          color: #667781;
                          user-select: none;
                        ">
                          <span>${msg.time || ''}</span>
                          ${isOut ? this.renderMessageStatus(msg) : ''}
                        </div>
                      </div>

                      <!-- Interactive Quick Reply & CTA Buttons (Rendered cleanly below text and timestamp) -->
                      ${(msg.buttons && msg.buttons.length > 0) ? `
                        <div style="
                          margin: 0 ${isImg ? '-4px -6px' : '-9px -8px -8px -9px'};
                          border-top: 1px solid rgba(11,20,26,0.08);
                          display: flex;
                          flex-direction: column;
                        ">
                          ${msg.buttons.map((btn, idx) => {
                            const isLast = idx === msg.buttons.length - 1;
                            return `
                              <div style="
                                padding: 8px 12px;
                                display: flex;
                                align-items: center;
                                justify-content: center;
                                gap: 6px;
                                color: #008069;
                                font-size: 13.5px;
                                font-weight: 500;
                                cursor: pointer;
                                transition: background 0.15s;
                                border-top: ${idx > 0 ? '1px solid rgba(11,20,26,0.08)' : 'none'};
                                ${isLast ? 'border-bottom-left-radius: 8px; border-bottom-right-radius: 8px;' : ''}
                              "
                              onmouseover="this.style.background='rgba(11,20,26,0.04)';"
                              onmouseout="this.style.background='transparent';"
                              >
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                                  <path d="M10 9V5l-7 7 7 7v-4.1c5 0 8.5 1.6 11 5.1-1-5-4-10-11-11z"/>
                                </svg>
                                <span>${btn.text || btn.buttonText}</span>
                              </div>
                            `;
                          }).join('')}
                        </div>
                      ` : ''}
                    </div>
                  </div>
                `;
              }).join('');
            })()}
            </div>

            <!-- Composer Bar Area -->
            <div style="
              padding: 10px 18px 12px 18px; 
              background: #f0f2f5; 
              border-top: 1px solid #e9edef; 
              display: flex; 
              flex-direction: column; 
              gap: 8px; 
              flex-shrink: 0;
            ">
              <!-- Pending Attachment Preview Chip -->
              ${this.pendingAttachment ? `
                <div style="
                  display: flex;
                  align-items: center;
                  justify-content: space-between;
                  background: #ffffff;
                  border: 1px solid #d1d7db;
                  border-radius: 8px;
                  padding: 6px 12px;
                ">
                  <div style="display: flex; align-items: center; gap: 8px; min-width: 0;">
                    <span style="font-size: 18px;">${this.pendingAttachment.mimeType.startsWith('image/') ? '🖼️' : '📄'}</span>
                    <div style="min-width: 0;">
                      <div style="font-size: 12px; font-weight: 600; color: #1e293b; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                        ${this.pendingAttachment.fileName}
                      </div>
                      <div style="font-size: 10.5px; color: #64748b;">
                        ${Math.round(this.pendingAttachment.size / 1024)} KB ready to dispatch
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
                    padding: 2px 6px;
                  ">✕</button>
                </div>
              ` : ''}

              <!-- Status Notice regarding human assistance requirement -->
              ${!isHumanRequired ? `
                <div style="
                  display: flex;
                  align-items: center;
                  gap: 8px;
                  padding: 7px 12px;
                  background: #ffffff;
                  border: 1px solid #e2e8f0;
                  border-radius: 8px;
                  font-size: 12px;
                  color: #54656f;
                ">
                  <span style="font-size: 13px;">🔒</span>
                  <span>24-Hour Service Session Expired. Customer must send a message to re-open live chat, or choose a pre-approved Template below.</span>
                </div>
              ` : `
                <div style="
                  display: flex;
                  align-items: center;
                  gap: 8px;
                  padding: 6px 12px;
                  background: #dcfce7;
                  border: 1px solid #bbf7d0;
                  border-radius: 8px;
                  font-size: 12px;
                  color: #166534;
                  font-weight: 500;
                ">
                  <span style="width: 7px; height: 7px; border-radius: 50%; background: #22c55e; display: inline-block;"></span>
                  <span>24-Hour Service Session Active. Free-form messaging enabled.</span>
                </div>
              `}

              <!-- Input row (WhatsApp Web modern style with (+) action menu) -->
              <div style="display: flex; gap: 8px; align-items: center; position: relative;">
                <!-- Hidden file inputs for specific attachments -->
                <input 
                  type="file" 
                  id="fl-chat-img-input" 
                  accept="image/jpeg,image/png,image/webp" 
                  style="display: none;" 
                />
                <input 
                  type="file" 
                  id="fl-chat-doc-input" 
                  accept="application/pdf" 
                  style="display: none;" 
                />

                <!-- (+) Action Menu Container -->
                <div style="position: relative;">
                  <button 
                    type="button" 
                    id="fl-chat-plus-btn" 
                    class="fl-btn fl-btn-secondary"
                    title="Attach or use templates" 
                    style="
                      width: 40px;
                      height: 40px;
                      border-radius: 50%;
                      padding: 0;
                      display: flex;
                      align-items: center;
                      justify-content: center;
                      font-size: 20px;
                      color: #54656f;
                      border: none;
                      background: #ffffff;
                      box-shadow: 0 1px 3px rgba(11,20,26,0.08);
                      cursor: pointer;
                      transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1), background 0.15s;
                    "
                    onmouseover="this.style.background='#f0f2f5';"
                    onmouseout="this.style.background='#ffffff';"
                  >
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                      <line x1="12" y1="5" x2="12" y2="19"></line>
                      <line x1="5" y1="12" x2="19" y2="12"></line>
                    </svg>
                  </button>

                  <!-- WhatsApp Web Style Popover Action Menu -->
                  <div 
                    id="fl-chat-plus-menu" 
                    style="
                      display: none;
                      position: absolute;
                      bottom: 48px;
                      left: 0;
                      background: #ffffff;
                      border-radius: 14px;
                      box-shadow: 0 8px 24px rgba(11,20,26,0.18);
                      border: 1px solid rgba(11,20,26,0.08);
                      padding: 8px 6px;
                      width: 200px;
                      z-index: 1000;
                      animation: flFadeIn 120ms ease-out;
                    "
                  >
                    <!-- Photos & Videos -->
                    <button 
                      type="button" 
                      id="fl-action-photos"
                      class="fl-chat-popover-item"
                      ${!isHumanRequired ? 'disabled' : ''}
                      style="
                        width: 100%;
                        display: flex;
                        align-items: center;
                        gap: 12px;
                        padding: 9px 12px;
                        border: none;
                        background: transparent;
                        border-radius: 8px;
                        cursor: ${isHumanRequired ? 'pointer' : 'not-allowed'};
                        opacity: ${isHumanRequired ? '1' : '0.45'};
                        text-align: left;
                        transition: background 0.12s;
                      "
                      onmouseover="${isHumanRequired ? "this.style.background='#f0f2f5';" : ''}"
                      onmouseout="this.style.background='transparent';"
                    >
                      <div style="
                        width: 28px;
                        height: 28px;
                        border-radius: 50%;
                        background: #0284c7;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        color: #ffffff;
                        font-size: 14px;
                      ">🖼️</div>
                      <div>
                        <div style="font-size: 13px; font-weight: 500; color: #111b21;">Photos & Videos</div>
                      </div>
                    </button>

                    <!-- Document -->
                    <button 
                      type="button" 
                      id="fl-action-document"
                      class="fl-chat-popover-item"
                      ${!isHumanRequired ? 'disabled' : ''}
                      style="
                        width: 100%;
                        display: flex;
                        align-items: center;
                        gap: 12px;
                        padding: 9px 12px;
                        border: none;
                        background: transparent;
                        border-radius: 8px;
                        cursor: ${isHumanRequired ? 'pointer' : 'not-allowed'};
                        opacity: ${isHumanRequired ? '1' : '0.45'};
                        text-align: left;
                        transition: background 0.12s;
                      "
                      onmouseover="${isHumanRequired ? "this.style.background='#f0f2f5';" : ''}"
                      onmouseout="this.style.background='transparent';"
                    >
                      <div style="
                        width: 28px;
                        height: 28px;
                        border-radius: 50%;
                        background: #7c3aed;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        color: #ffffff;
                        font-size: 14px;
                      ">📄</div>
                      <div>
                        <div style="font-size: 13px; font-weight: 500; color: #111b21;">Document (PDF)</div>
                      </div>
                    </button>

                    <div style="height: 1px; background: #e9edef; margin: 5px 8px;"></div>

                    <!-- Pre-Approved Templates (Always allowed) -->
                    <button 
                      type="button" 
                      id="fl-action-templates"
                      class="fl-chat-popover-item"
                      style="
                        width: 100%;
                        display: flex;
                        align-items: center;
                        gap: 12px;
                        padding: 9px 12px;
                        border: none;
                        background: transparent;
                        border-radius: 8px;
                        cursor: pointer;
                        text-align: left;
                        transition: background 0.12s;
                      "
                      onmouseover="this.style.background='#f0f2f5';"
                      onmouseout="this.style.background='transparent';"
                    >
                      <div style="
                        width: 28px;
                        height: 28px;
                        border-radius: 50%;
                        background: #eab308;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        color: #ffffff;
                        font-size: 14px;
                      ">⚡</div>
                      <div>
                        <div style="font-size: 13px; font-weight: 500; color: #111b21;">Templates</div>
                      </div>
                    </button>
                  </div>
                </div>

                <!-- Message input container (WhatsApp Pill shape) -->
                <div style="
                  flex: 1;
                  display: flex;
                  align-items: center;
                  background: #ffffff;
                  border-radius: 22px;
                  border: 1px solid ${isHumanRequired ? '#d1d7db' : '#e2e8f0'};
                  padding: 2px 14px;
                  transition: border-color 0.15s, box-shadow 0.15s;
                ">
                  <input 
                    type="text" 
                    id="fl-chat-input" 
                    ${!isHumanRequired ? 'disabled' : ''}
                    placeholder="${isHumanRequired ? 'Type a message' : 'Chat locked — 24-hour session expired. Send a pre-approved template to initiate conversation.'}" 
                    style="
                      flex: 1; 
                      height: 38px; 
                      padding: 0; 
                      border: none; 
                      font-size: 14px; 
                      background: transparent;
                      color: ${isHumanRequired ? '#111b21' : '#94a3b8'};
                      outline: none;
                      box-sizing: border-box;
                      cursor: ${isHumanRequired ? 'text' : 'not-allowed'};
                    "
                  />
                </div>

                <!-- Send button with circular paper plane icon -->
                <button 
                  type="button" 
                  id="fl-send-msg-btn" 
                  class="fl-btn fl-btn-primary"
                  ${!isHumanRequired ? 'disabled' : ''}
                  title="${isHumanRequired ? 'Send message' : 'Chat locked until customer sends a message or template is sent'}"
                  style="
                    width: 40px;
                    height: 40px;
                    border-radius: 50%;
                    padding: 0;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    flex-shrink: 0;
                    box-shadow: 0 1px 3px rgba(0,168,132,0.3);
                  "
                >
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" style="transform: rotate(0deg); margin-left: 2px;">
                    <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
                  </svg>
                </button>
              </div>
            </div>
          `}
        </div>

      </div>
    `;

    this.bindEvents(activeConv, wasNearBottom, prevScrollTop, prevScrollHeight);

    if (isChatSearchFocused) {
      const nextInput = this.querySelector('#fl-chat-search');
      if (nextInput) {
        nextInput.focus();
        if (selStart !== null && selEnd !== null) {
          try {
            nextInput.setSelectionRange(selStart, selEnd);
          } catch (_) {}
        }
      }
    }

    const nextChatInput = this.querySelector('#fl-chat-input');
    if (nextChatInput && savedChatInputValue) {
      nextChatInput.value = savedChatInputValue;
    }
    if (isChatInputFocused && nextChatInput) {
      nextChatInput.focus();
      if (chatInputSelStart !== null && chatInputSelEnd !== null) {
        try {
          nextChatInput.setSelectionRange(chatInputSelStart, chatInputSelEnd);
        } catch (_) {}
      }
    }
  }

  renderMessageStatus(msg) {
    const rawStatus = (msg.status || '').toUpperCase();

    // 1. Pending / Sending / Default in-flight -> WhatsApp Timer Clock Icon
    if (!rawStatus || rawStatus === 'PENDING' || rawStatus === 'SENDING' || rawStatus === 'IN_FLIGHT') {
      return `
        <span style="color: #8696a0; display: inline-flex; align-items: center; vertical-align: middle; margin-left: 2px;" title="Sending...">
          <svg viewBox="0 0 16 16" width="12" height="12" fill="currentColor">
            <path d="M8 1a7 7 0 1 0 7 7A7.008 7.008 0 0 0 8 1zm0 12.5A5.5 5.5 0 1 1 13.5 8 5.506 5.506 0 0 1 8 13.5zm.5-8.5H7v5l4.25 2.55.75-1.23-3.5-2.07z"/>
          </svg>
        </span>
      `;
    }

    // 2. Sent / Confirmed by server -> WhatsApp Single Grey Tick
    if (rawStatus === 'SENT' || rawStatus === 'CONFIRMED' || rawStatus === 'QUEUED') {
      return `
        <span style="color: #8696a0; display: inline-flex; align-items: center; vertical-align: middle; margin-left: 2px;" title="Sent">
          <svg viewBox="0 0 16 15" width="14" height="13" fill="currentColor">
            <path d="M15.01 3.316l-.478-.372a.365.365 0 0 0-.51.063L6.442 12.759 2.016 8.333a.365.365 0 0 0-.516 0l-.372.372a.365.365 0 0 0 0 .516l4.73 4.73a.365.365 0 0 0 .546-.032l8.667-10.1a.365.365 0 0 0-.063-.503z"/>
          </svg>
        </span>
      `;
    }

    // 3. Delivered -> WhatsApp Double Grey Tick (crisp darker grey)
    if (rawStatus === 'DELIVERED') {
      return `
        <span style="color: #667781; display: inline-flex; align-items: center; vertical-align: middle; margin-left: 2px;" title="Delivered">
          <svg viewBox="0 0 16 15" width="16" height="13" fill="currentColor">
            <path d="M15.01 3.316l-.478-.372a.365.365 0 0 0-.51.063L6.442 12.759 2.016 8.333a.365.365 0 0 0-.516 0l-.372.372a.365.365 0 0 0 0 .516l4.73 4.73a.365.365 0 0 0 .546-.032l8.667-10.1a.365.365 0 0 0-.063-.503zm-3.882.063l-.478-.372a.365.365 0 0 0-.51.063L4.57 9.873l.942.942 5.094-6.936a.365.365 0 0 0-.063-.503z"/>
          </svg>
        </span>
      `;
    }

    // 4. Read / Viewed -> WhatsApp Vivid Double Blue Tick with subtle soft glow
    if (rawStatus === 'READ' || rawStatus === 'VIEWED') {
      return `
        <span style="color: #34b7f1; display: inline-flex; align-items: center; vertical-align: middle; margin-left: 2px;" title="Read">
          <svg viewBox="0 0 16 15" width="16" height="13" fill="#34b7f1" style="filter: drop-shadow(0 0 2px rgba(52, 183, 241, 0.45));">
            <path d="M15.01 3.316l-.478-.372a.365.365 0 0 0-.51.063L6.442 12.759 2.016 8.333a.365.365 0 0 0-.516 0l-.372.372a.365.365 0 0 0 0 .516l4.73 4.73a.365.365 0 0 0 .546-.032l8.667-10.1a.365.365 0 0 0-.063-.503zm-3.882.063l-.478-.372a.365.365 0 0 0-.51.063L4.57 9.873l.942.942 5.094-6.936a.365.365 0 0 0-.063-.503z"/>
          </svg>
        </span>
      `;
    }

    // 5. Failed -> WhatsApp Red Error Icon
    if (rawStatus === 'FAILED' || rawStatus === 'ERROR') {
      return `
        <span style="color: #ef4444; display: inline-flex; align-items: center; vertical-align: middle; margin-left: 2px;" title="Failed to send">
          <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="8" x2="12" y2="12"></line>
            <line x1="12" y1="16" x2="12.01" y2="16"></line>
          </svg>
        </span>
      `;
    }

    // Default fallback: Timer Clock Icon
    return `
      <span style="color: #8696a0; display: inline-flex; align-items: center; vertical-align: middle; margin-left: 2px;" title="Sending...">
        <svg viewBox="0 0 16 16" width="12" height="12" fill="currentColor">
          <path d="M8 1a7 7 0 1 0 7 7A7.008 7.008 0 0 0 8 1zm0 12.5A5.5 5.5 0 1 1 13.5 8 5.506 5.506 0 0 1 8 13.5zm.5-8.5H7v5l4.25 2.55.75-1.23-3.5-2.07z"/>
        </svg>
      </span>
    `;
  }

  bindEvents(activeConv, wasNearBottom = true, prevScrollTop = null, prevScrollHeight = null) {
    // 1. Search input for chats
    const searchInput = this.querySelector('#fl-chat-search');
    if (searchInput) {
      searchInput.addEventListener('input', (e) => {
        HomeStore.setChatSearchQuery(e.target.value);
      });
    }

    // 2. Conversation selection
    this.querySelectorAll('.fl-conv-item').forEach(item => {
      item.addEventListener('click', () => {
        const id = item.getAttribute('data-id');
        HomeStore.setActiveChatId(id);
        this.pendingAttachment = null;
      });
    });

    // 3b. Member name click or "Open Profile" button in chat header -> Open Member Drawer
    const handleOpenProfile = (memberId) => {
      const state = HomeStore.getState();
      const member = (state.members || []).find(m => m.id === memberId || m.phone === memberId || (activeConv && m.phone === activeConv.phone));
      if (member) {
        HomeStore.openMemberDrawer(member);
      } else if (activeConv) {
        HomeStore.openMemberDrawer({
          id: activeConv.memberId || activeConv.id,
          name: activeConv.name || 'Member',
          phone: activeConv.phone || '',
          status: activeConv.status || 'active'
        });
      }
    };

    const memberLink = this.querySelector('.fl-chat-member-link');
    if (memberLink) {
      memberLink.addEventListener('click', (e) => {
        e.stopPropagation();
        handleOpenProfile(memberLink.getAttribute('data-member-id'));
      });
    }

    const openProfileBtn = this.querySelector('#fl-chat-open-profile-btn');
    if (openProfileBtn) {
      openProfileBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        handleOpenProfile(openProfileBtn.getAttribute('data-member-id'));
      });
    }

    // 4. Popover Menu (+) Toggle
    const plusBtn = this.querySelector('#fl-chat-plus-btn');
    const plusMenu = this.querySelector('#fl-chat-plus-menu');
    if (plusBtn && plusMenu) {
      plusBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        const isOpen = plusMenu.style.display === 'block';
        plusMenu.style.display = isOpen ? 'none' : 'block';
        plusBtn.style.transform = isOpen ? 'rotate(0deg)' : 'rotate(45deg)';
      });

      // Close menu when clicking outside
      const closeMenuHandler = (e) => {
        if (!plusMenu.contains(e.target) && e.target !== plusBtn) {
          plusMenu.style.display = 'none';
          plusBtn.style.transform = 'rotate(0deg)';
        }
      };
      document.addEventListener('click', closeMenuHandler);
    }

    // 5. Open Pre-Approved Templates Modal via Popover Menu
    const actionTplBtn = this.querySelector('#fl-action-templates');
    if (actionTplBtn && activeConv) {
      actionTplBtn.addEventListener('click', () => {
        if (plusMenu) {
          plusMenu.style.display = 'none';
          if (plusBtn) plusBtn.style.transform = 'rotate(0deg)';
        }
        HomeStore.openTemplateModal();
      });
    }

    // 6. File attachments from popover
    const imgInput = this.querySelector('#fl-chat-img-input');
    const docInput = this.querySelector('#fl-chat-doc-input');
    const actionPhotosBtn = this.querySelector('#fl-action-photos');
    const actionDocBtn = this.querySelector('#fl-action-document');

    if (actionPhotosBtn && imgInput) {
      actionPhotosBtn.addEventListener('click', () => {
        if (plusMenu) {
          plusMenu.style.display = 'none';
          if (plusBtn) plusBtn.style.transform = 'rotate(0deg)';
        }
        imgInput.click();
      });
    }

    if (actionDocBtn && docInput) {
      actionDocBtn.addEventListener('click', () => {
        if (plusMenu) {
          plusMenu.style.display = 'none';
          if (plusBtn) plusBtn.style.transform = 'rotate(0deg)';
        }
        docInput.click();
      });
    }

    const handleFileChosen = (file, fallbackMime) => {
      if (!file) return;
      const reader = new FileReader();
      reader.onload = (loadEvt) => {
        this.pendingAttachment = {
          file,
          fileName: file.name,
          mimeType: file.type || fallbackMime,
          dataBase64: loadEvt.target.result,
          size: file.size
        };
        this.render();
      };
      reader.readAsDataURL(file);
    };

    if (imgInput) {
      imgInput.addEventListener('change', (e) => handleFileChosen(e.target.files[0], 'image/jpeg'));
    }
    if (docInput) {
      docInput.addEventListener('change', (e) => handleFileChosen(e.target.files[0], 'application/pdf'));
    }

    // 9. Lightbox preview
    this.querySelectorAll('.fl-chat-img-thumb').forEach(thumb => {
      thumb.addEventListener('click', () => {
        const url = thumb.getAttribute('data-url');
        if (url) this.showLightbox(url);
      });
    });

    // 11. Remove pending attachment
    const removeAttBtn = this.querySelector('#fl-remove-attachment-btn');
    if (removeAttBtn) {
      removeAttBtn.addEventListener('click', () => {
        this.pendingAttachment = null;
        this.render();
      });
    }

    // 12. Dispatch staff chat reply
    const sendBtn = this.querySelector('#fl-send-msg-btn');
    const input = this.querySelector('#fl-chat-input');
    const doSend = async () => {
      const text = input ? input.value.trim() : '';
      if (!text && !this.pendingAttachment) return;
      if (!activeConv || !activeConv.isHumanRequired) return;

      const attachment = this.pendingAttachment;
      this.pendingAttachment = null;

      if (input) input.value = '';
      await HomeStore.sendChatMessage(activeConv.id, text, attachment);

      const msgContainer = this.querySelector('#fl-messages-container');
      if (msgContainer) msgContainer.scrollTop = msgContainer.scrollHeight;
    };

    if (sendBtn) sendBtn.addEventListener('click', doSend);
    if (input) {
      input.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') doSend();
      });
    }

    // 13. Jump to quoted message on quote card click (with backward pagination if not in DOM)
    this.querySelectorAll('.fl-chat-quote-card').forEach(card => {
      card.addEventListener('click', async (e) => {
        e.stopPropagation();
        const targetId = card.getAttribute('data-target-id');
        if (!targetId) return;

        let targetEl = this.querySelector(`#msg-${targetId}`);
        if (!targetEl && activeConv && !card._isFetchingTarget) {
          card._isFetchingTarget = true;
          const origTitle = card.title;
          card.title = 'Loading quoted message...';
          card.style.opacity = '0.6';
          try {
            await HomeStore.findOrLoadMessage(activeConv.id, targetId);
          } finally {
            card.style.opacity = '1';
            card.title = origTitle;
            card._isFetchingTarget = false;
          }
          targetEl = this.querySelector(`#msg-${targetId}`);
        }

        if (targetEl) {
          targetEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
          targetEl.classList.remove('fl-msg-highlight');
          void targetEl.offsetWidth; // Force CSS reflow
          targetEl.classList.add('fl-msg-highlight');
          setTimeout(() => targetEl.classList.remove('fl-msg-highlight'), 1800);
        }
      });
    });

    // Scroll position restoration
    const msgContainer = this.querySelector('#fl-messages-container');
    if (msgContainer) {
      if (wasNearBottom) {
        msgContainer.scrollTop = msgContainer.scrollHeight;
      } else if (prevScrollTop !== null) {
        if (this._isLoadingOlder && prevScrollHeight !== null) {
          const heightDiff = msgContainer.scrollHeight - prevScrollHeight;
          msgContainer.scrollTop = prevScrollTop + heightDiff;
        } else {
          msgContainer.scrollTop = prevScrollTop;
        }
      }

      // 14. Scroll-up pagination for older messages
      msgContainer.addEventListener('scroll', async () => {
        const canScroll = msgContainer.scrollHeight > msgContainer.clientHeight;
        if (canScroll && msgContainer.scrollTop < 40 && activeConv && activeConv._hasMoreOlder === true && !this._isLoadingOlder) {
          this._isLoadingOlder = true;
          await HomeStore.loadOlderMessages(activeConv.id);
          this._isLoadingOlder = false;
        }
      });
    }
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
          <img id="fl-lightbox-img" style="max-width: 90vw; max-height: 85vh; border-radius: 8px; box-shadow: 0 20px 40px rgba(0,0,0,0.4); object-fit: contain; display: block;" />
          <div id="fl-lightbox-fallback" style="display: none; flex-direction: column; align-items: center; justify-content: center; background: #ffffff; padding: 32px 40px; border-radius: 12px; gap: 12px; box-shadow: 0 20px 40px rgba(0,0,0,0.4);">
            <div style="font-size: 32px;">🖼️</div>
            <div style="font-size: 14px; font-weight: 600; color: #1e293b;">Image Preview Unavailable</div>
            <div style="font-size: 12px; color: #64748b;">The media file could not be loaded.</div>
          </div>
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
    const fallback = lb.querySelector('#fl-lightbox-fallback');
    if (img) {
      img.style.display = 'block';
      if (fallback) fallback.style.display = 'none';
      img.onerror = () => {
        img.style.display = 'none';
        if (fallback) fallback.style.display = 'flex';
      };
      img.src = imageUrl;
    }
  }
}

customElements.define('fl-chats-section', FlChatsSection);
