/**
 * Legacy Chat Page wrapper
 * Re-exports FlChatsSection from modular home sections.
 */

import { FlChatsSection } from '../home/sections/chats-section.js';

export { FlChatsSection as FlChatPage };

if (!customElements.get('fl-chat-page')) {
  customElements.define('fl-chat-page', FlChatsSection);
}
