package com.freelance.event.model;

/**
 * Defines priority levels for asynchronous event processing.
 * Lower numerical rank values indicate higher execution precedence
 */
public enum EventPriority {
    HIGH(1),
    NORMAL(2),
    LOW(3);

    private final int level;

    EventPriority(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
