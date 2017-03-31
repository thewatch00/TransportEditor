package com.oskopek.transport.tools.executables;

/**
 * Simple {@link LogListener} subscribable.
 */
public interface LogStreamable {

    /**
     * Subscribe to events generated by this object.
     *
     * @param listener the listener to subscribe
     */
    void subscribe(LogListener listener);

    /**
     * Unsubscribe from events generated by this object.
     *
     * @param listener the listener to unsubscribe
     */
    void unsubscribe(LogListener listener);

}