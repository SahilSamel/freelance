/**
 * Freelance Frontend Framework - Brand Metadata & Theme Engine
 * Delegates brand configuration to AppConfig and handles theme/styling.
 */

import { AppConfig } from '../config/app-config.js';

class ThemeEngineClass {
  getBrandName() {
    return AppConfig.getBrandName();
  }

  getBrandSubtitle() {
    return AppConfig.getBrandSubtitle();
  }

  getBrandLogoUrl() {
    return AppConfig.getBrandLogoUrl();
  }

  setBrand(config) {
    AppConfig.setBranding(config);
  }

  async loadFromBackend(tenantId = null) {
    await AppConfig.loadFromBackend(tenantId);
  }

  init() {
    AppConfig.init();
  }
}

export const ThemeEngine = new ThemeEngineClass();
