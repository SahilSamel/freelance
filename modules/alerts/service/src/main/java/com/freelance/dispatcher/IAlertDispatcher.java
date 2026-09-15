package com.freelance.dispatcher;

import com.freelance.alerts.AlertChannel;
import com.freelance.dispatcher.data.AlertDispatchRequest;
import com.freelance.dispatcher.data.AlertDispatchResult;

/**
 * Service provider interface (SPI) for communication channel alert dispatchers.
 * <p>
 * Each implementation encapsulates the logic required to deliver outbound alert messages
 * over a specific communication channel (e.g., {@link AlertChannel#WHATSAPP}, SMS, Email, Push).
 * </p>
 *
 * <h3>Implementation Guidelines:</h3>
 * <ul>
 *   <li><b>No-Arg Constructor:</b> Implementations must provide a public no-arg default constructor
 *       to enable automatic discovery and registration by {@link AlertDispatcherFactory} via classpath reflection scanning.</li>
 *   <li><b>Thread Safety:</b> Dispatchers are instantiated as singletons or shared instances and must be thread-safe
 *       for concurrent invocation across multiple worker threads.</li>
 *   <li><b>Exception Safety:</b> Implementations should handle provider communication errors internally and return
 *       a structured {@link AlertDispatchResult#failure(String, String, String)} rather than allowing unhandled runtime exceptions to escape.</li>
 *   <li><b>Audit & Logging:</b> Implementations are expected to record outbound messages (e.g., in message audit tables)
 *       and track external provider message identifiers (such as Meta's {@code wamid}).</li>
 * </ul>
 */
public interface IAlertDispatcher {

    /**
     * Returns the distinct communication channel supported by this dispatcher.
     * <p>
     * Used by {@link AlertDispatcherFactory} to route outbound alert payloads to the correct provider.
     * </p>
     *
     * @return non-null {@link AlertChannel} handled by this dispatcher
     */
    AlertChannel getChannel();

    /**
     * Dispatches an outbound alert message to the target recipient over this channel.
     * <p>
     * Implementations are responsible for:
     * <ol>
     *   <li>Verifying channel-specific prerequisites (e.g., active opt-in status, rate limits, queue pause state).</li>
     *   <li>Formatting the payload according to external provider specifications (e.g., template components, quick-reply buttons).</li>
     *   <li>Executing the HTTP/network request to the external communication service provider.</li>
     *   <li>Logging/persisting the dispatched message state and capturing the provider's external reference ID.</li>
     * </ol>
     * </p>
     *
     * @param request the consolidated dispatch request containing template, recipient contact, rendered content,
     *                resolved dynamic attributes, and event priority
     * @return an {@link AlertDispatchResult} indicating {@code SUCCESS} (with provider message ID),
     *         {@code FAILURE} (with error code and description), or {@code DELAYED} (if transmission is temporarily deferred)
     */
    AlertDispatchResult dispatch(AlertDispatchRequest request);
}

