package com.announcements.AutomateAnnouncements.integration.provider;

/**
 * Enumerates the available AI video providers. Keeping this centralized lets the factory
 * and adapters stay type-safe while still allowing runtime selection through configuration.
 */
public enum VideoProviderType {
    BLOTATO,
    SORA
}
