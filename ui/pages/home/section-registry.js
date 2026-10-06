/**
 * Section Registry - Central Mapping for Home Application Sections
 * Defines the 4 core sections (Members, Payments, Plans, Chats).
 * To add a 5th section in the future, simply add an entry here or call registerSection().
 */

export const SECTIONS = [
  {
    id: 'members',
    label: 'Members',
    icon: 'users',
    componentTag: 'fl-members-section',
    load: () => import('./sections/members-section.js')
  },
  {
    id: 'payments',
    label: 'Payments',
    icon: 'creditCard',
    componentTag: 'fl-payments-section',
    load: () => import('./sections/payments-section.js')
  },
  {
    id: 'plans',
    label: 'Plans',
    icon: 'layers',
    componentTag: 'fl-plans-section',
    load: () => import('./sections/plans-section.js')
  },
  {
    id: 'chats',
    label: 'Chats',
    icon: 'messageCircle',
    componentTag: 'fl-chats-section',
    load: () => import('./sections/chats-section.js')
  }
];

const registeredSections = new Map(SECTIONS.map(s => [s.id, s]));

/**
 * Retrieve a section configuration by ID.
 * @param {string} id 
 * @returns {object|undefined}
 */
export function getSection(id) {
  return registeredSections.get(id);
}

/**
 * Return all registered sections in order.
 * @returns {Array<object>}
 */
export function getAllSections() {
  return Array.from(registeredSections.values());
}

/**
 * Register an additional section dynamically (e.g. 5th section).
 * @param {object} config - { id, label, icon, componentTag, load }
 */
export function registerSection(config) {
  if (!config || !config.id || !config.componentTag) {
    throw new Error('Section registration requires id and componentTag');
  }
  registeredSections.set(config.id, config);
  window.dispatchEvent(new CustomEvent('fl:sections-updated'));
}
