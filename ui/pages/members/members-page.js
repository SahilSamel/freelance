/**
 * Deprecated: Replaced by modular <fl-home-page> in ui/pages/home/home-page.js
 */
import { FlHomePage } from '../home/home-page.js';

export { FlHomePage as FlMembersPage };
if (!customElements.get('fl-members-page')) {
  customElements.define('fl-members-page', class extends FlHomePage {});
}
