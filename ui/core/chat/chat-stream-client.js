/**
 * Real-Time Chat Stream Client (SSE)
 * Zero-polling, event-driven Server-Sent Events client for multi-tenant WhatsApp chat.
 * Automatically handles reconnects, heartbeat pings, and cross-device sync.
 */

import { SessionManager } from '../auth/session.js';
import { HomeStore } from '../../pages/home/home-store.js';

class ChatStreamClientSingleton {
  constructor() {
    this.eventSource = null;
    this.isConnected = false;
    this.hasConnectedOnce = false;
    this.retryTimeout = null;
  }

  init() {
    if (this.eventSource) {
      return;
    }

    const token = SessionManager.getToken();
    const tenantId = SessionManager.getTenantId();

    if (!token || !tenantId) {
      return;
    }

    const streamUrl = `/alerts/chat/stream?token=${encodeURIComponent(token)}&tenant=${encodeURIComponent(tenantId)}`;

    try {
      this.eventSource = new EventSource(streamUrl);

      this.eventSource.onopen = () => {
        this.isConnected = true;
        if (this.hasConnectedOnce) {
          // Reconnection detected (e.g. laptop wake from sleep or network reconnect)
          // Hit DB once to catch up on any messages missed during disconnect
          HomeStore.catchUpChatsAfterReconnect();
        }
        this.hasConnectedOnce = true;
      };

      this.eventSource.addEventListener('CONNECTED', (e) => {
        this.isConnected = true;
      });

      this.eventSource.addEventListener('CHAT_MESSAGE', (e) => {
        try {
          const message = JSON.parse(e.data);
          HomeStore.handleIncomingStreamMessage(message);
        } catch (err) {
          console.warn('[ChatStreamClient] Could not parse CHAT_MESSAGE:', err);
        }
      });

      this.eventSource.addEventListener('MESSAGE_STATUS', (e) => {
        try {
          const update = JSON.parse(e.data);
          HomeStore.handleStreamMessageStatus(update);
        } catch (err) {
          console.warn('[ChatStreamClient] Could not parse MESSAGE_STATUS:', err);
        }
      });

      this.eventSource.addEventListener('CHAT_SESSION_STATUS', (e) => {
        try {
          const sessionUpdate = JSON.parse(e.data);
          HomeStore.handleStreamSessionStatus(sessionUpdate);
        } catch (err) {
          console.warn('[ChatStreamClient] Could not parse CHAT_SESSION_STATUS:', err);
        }
      });

      this.eventSource.onerror = (err) => {
        this.isConnected = false;
        // Native EventSource automatically attempts reconnection
      };
    } catch (e) {
      console.warn('[ChatStreamClient] Could not initialize EventSource:', e);
    }
  }

  destroy() {
    if (this.eventSource) {
      try {
        this.eventSource.close();
      } catch (_) {}
      this.eventSource = null;
    }
    this.isConnected = false;
    this.hasConnectedOnce = false;
    if (this.retryTimeout) {
      clearTimeout(this.retryTimeout);
      this.retryTimeout = null;
    }
  }
}

export const ChatStreamClient = new ChatStreamClientSingleton();
