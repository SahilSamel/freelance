package com.freelance.alerts.whatsapp;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.AlertDirection;
import com.freelance.alerts.AlertErrorCode;
import com.freelance.alerts.AlertMessageStatus;
import com.freelance.alerts.AlertsEngine;
import com.freelance.alerts.TemplateCategory;
import com.freelance.alerts.TemplateStatus;
import com.freelance.alerts.channel.entity.AlertChannelConfigEntity;
import com.freelance.alerts.channel.repository.AlertChannelConfigRepository;
import com.freelance.alerts.dto.AlertEventPayloadDTO;
import com.freelance.alerts.dto.webhook.*;
import com.freelance.alerts.message.entity.AlertMessageEntity;
import com.freelance.alerts.message.repository.AlertMessageRepository;
import com.freelance.alerts.preference.entity.UserContactEntity;
import com.freelance.alerts.preference.repository.UserContactRepository;
import com.freelance.alerts.template.entity.AlertButtonReplyEntity;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.alerts.template.repository.AlertButtonReplyRepository;
import com.freelance.alerts.template.repository.AlertTemplateRepository;
import com.freelance.alerts.templates.InboundWelcomeTemplate;
import com.freelance.attribute.AttributeResolutionEngine;
import com.freelance.dispatcher.AlertDispatcherFactory;
import com.freelance.dispatcher.IAlertDispatcher;
import com.freelance.dispatcher.data.AlertDispatchRequest;
import com.freelance.dispatcher.data.AlertDispatchResult;
import com.freelance.event.exception.EventDelayedException;
import com.freelance.event.model.EventMessage;
import com.freelance.event.model.EventPriority;
import com.freelance.template.utils.TemplateRenderUtility;
import com.freelance.user.read.UserReadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class WhatsAppAlertProcessor {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppAlertProcessor.class);
    private static final AlertChannel CHANNEL = AlertChannel.WHATSAPP;

    private final UserReadService userReadService;
    private final AlertTemplateRepository templateRepository;
    private final UserContactRepository contactRepository;
    private final AlertChannelConfigRepository channelConfigRepository;
    private final AlertMessageRepository messageRepository;
    private final AlertButtonReplyRepository buttonReplyRepository;
    private final AlertDispatcherFactory dispatcherFactory;
    private final AlertsEngine alertsEngine;

    public WhatsAppAlertProcessor() {
        this(
                new UserReadService(),
                new AlertTemplateRepository(),
                new UserContactRepository(),
                new AlertChannelConfigRepository(),
                new AlertMessageRepository(),
                new AlertButtonReplyRepository(),
                new AlertDispatcherFactory(),
                AlertsEngine.getInstance()
        );
    }

    public WhatsAppAlertProcessor(
            UserReadService userReadService,
            AlertTemplateRepository templateRepository,
            UserContactRepository contactRepository,
            AlertChannelConfigRepository channelConfigRepository,
            AlertMessageRepository messageRepository,
            AlertDispatcherFactory dispatcherFactory
    ) {
        this(
                userReadService,
                templateRepository,
                contactRepository,
                channelConfigRepository,
                messageRepository,
                new AlertButtonReplyRepository(),
                dispatcherFactory != null ? dispatcherFactory : new AlertDispatcherFactory(),
                AlertsEngine.getInstance()
        );
    }

    public WhatsAppAlertProcessor(
            AlertMessageRepository messageRepository,
            UserContactRepository contactRepository,
            AlertChannelConfigRepository channelConfigRepository,
            AlertButtonReplyRepository buttonReplyRepository,
            AlertTemplateRepository templateRepository,
            AlertsEngine alertsEngine
    ) {
        this(
                new UserReadService(),
                templateRepository,
                contactRepository,
                channelConfigRepository,
                messageRepository,
                buttonReplyRepository,
                new AlertDispatcherFactory(),
                alertsEngine != null ? alertsEngine : AlertsEngine.getInstance()
        );
    }

    public WhatsAppAlertProcessor(
            UserReadService userReadService,
            AlertTemplateRepository templateRepository,
            UserContactRepository contactRepository,
            AlertChannelConfigRepository channelConfigRepository,
            AlertMessageRepository messageRepository,
            AlertButtonReplyRepository buttonReplyRepository,
            AlertDispatcherFactory dispatcherFactory,
            AlertsEngine alertsEngine
    ) {
        this.userReadService = userReadService;
        this.templateRepository = templateRepository;
        this.contactRepository = contactRepository;
        this.channelConfigRepository = channelConfigRepository;
        this.messageRepository = messageRepository;
        this.buttonReplyRepository = buttonReplyRepository;
        this.dispatcherFactory = dispatcherFactory != null ? dispatcherFactory : new AlertDispatcherFactory();
        this.alertsEngine = alertsEngine != null ? alertsEngine : AlertsEngine.getInstance();
    }

    /*
     * =========================================================================
     * OUTBOUND ALERT PROCESSING PIPELINE
     * =========================================================================
     *
     * +----------------------------------------------------------------------------------------------------+
     * |                               1. Global Channel State / Queue Pause                                |
     * |                         (FL_ALERT_CHANNEL_CONFIG: is_active=false)                                   |
     * +----------------------------------------------------------------------------------------------------+
     *               |                                                                  |
     *        Active |                                                                  | Down/Paused
     *               v                                                                  v
     * +----------------------------------+                            +----------------------------------+
     * |     2. Identity Verification     |                            |   Record FAILED + Route to DLQ   |
     * |        (UserReadService)         |                            +----------------------------------+
     * +----------------------------------+
     *        |                      |
     *   Fail |                      | Pass
     *        v                      v
     * +--------------------+ +----------------------------------+
     * |   Record FAILED:   | |      3. Reachability Check       |
     * |ERR_INVALID_USER_ID | |   (FL_USER_CONTACT row exists)   |
     * +--------------------+ +----------------------------------+
     *                               |                      |
     *                          Fail |                      | Pass
     *                               v                      v
     *                         +--------------------+ +----------------------------------+
     *                         |   Record FAILED:   | |  4. Legal Consent & Permissions  |
     *                         |ERR_NO_CONTACT_FOUND| |      (is_opted_out=true OR       |
     *                         +--------------------+ |         is_active=false)         |
     *                                                +----------------------------------+
     *                                                   |            |             |
     *                                    +--------------+            |             +--------------+
     *                          Opted Out |                  Inactive |                            | Pass
     *                                    v                           v                            v
     *                         +--------------------+   +--------------------+   +----------------------------------+
     *                         |   Record FAILED:   |   |   Record FAILED:   |   |     5. Conversational Timing     |
     *                         | ERR_USER_OPTED_OUT |   |ERR_CHANNEL_INACTIVE|   | (human_session_expires_at > now) |
     *                         +--------------------+   +--------------------+   +----------------------------------+
     *                                                                                     |                 |
     *                                                         Active & Priority != HIGH   |                 | Active & Priority == HIGH
     *                                                                                     |                 |   OR No Active Session
     *                                                                                     v                 v
     *                                                         +-------------------------------+ +----------------------------------+
     *                                                         |             DELAY             | |  6. Template Approval & Match    |
     *                                                         |    (EventDelayedException:    | |  (Template APPROVED & matches    |
     *                                                         |     consumer 24h backoff)     | |            WHATSAPP)             |
     *                                                         +-------------------------------+ +----------------------------------+
     *                                                                                                 |            |
     *                                                               +---------------------------------+            | Pass
     *                                                          Fail |                                              v
     *                                                               v                                   +----------------------------------+
     *                                                         +-------------------------------+         | 7. Provider Dispatch (Meta/MSG91)|
     *                                                         |        Record FAILED:         |         +----------------------------------+
     *                                                         |   ERR_TEMPLATE_NOT_APPROVED   |
     *                                                         |     / ERR_CHANNEL_MISMATCH    |
     *                                                         +-------------------------------+
     *
     * Note on Step 2:
     * Non-existent users are dropped gracefully with a debug log and are NOT
     * persisted to FL_ALERT_MESSAGE to prevent creating orphaned message records.
     */
    public void processOutbound(EventMessage<AlertEventPayloadDTO> message) {
        if (message == null) {
            return;
        }

        AlertEventPayloadDTO payload = message.getPayload();
        if (payload == null || payload.userId() == null || (payload.templateId() == null && (payload.rawContent() == null || payload.rawContent().isBlank()))) {
            return;
        }

        UUID userId = payload.userId();
        AlertChannel channel = CHANNEL;
        AlertTemplateEntity template = payload.templateId() != null && templateRepository != null ? templateRepository.get(payload.templateId()) : null;
        String content = payload.rawContent() != null ? payload.rawContent() : (template != null ? template.getContent() : null);
        Map<String, String> resolvedAttributes = Collections.emptyMap();

        // ── 1. Global Channel State & Queue Gatekeeping ──────────────────────
        if (WhatsAppUtility.isQueuePaused()) {
            throw new IllegalStateException("Outbound queue is currently paused due to Meta account/billing issue.");
        }

        if (channelConfigRepository != null && !channelConfigRepository.isChannelAvailable(channel)) {
            String pauseReason = channelConfigRepository.findByChannel(channel)
                    .map(AlertChannelConfigEntity::getPauseReason)
                    .orElse("Maintenance");
            String reason = AlertErrorCode.CHANNEL_PAUSED.format(channel, pauseReason);

            recordOutboundMessage(
                    payload.messageId(),
                    userId,
                    channel,
                    template,
                    content,
                    null,
                    AlertMessageStatus.FAILED,
                    AlertErrorCode.CHANNEL_PAUSED.getCode(),
                    reason
            );

            handleOutboundDeadLetter(message, new IllegalStateException(reason));
            return;
        }

        // ── 2. User Existence Verification ──────────────────────────────────
        if (!WhatsAppUtility.isUserValid(userId, userReadService)) {
            logger.debug("[WhatsAppAlertProcessor] {}", AlertErrorCode.INVALID_USER_ID.format(userId));
            return;
        }

        // ── 3. Reachability Check (Contact Resolution) ───────────────────────
        Optional<UserContactEntity> contactOpt = contactRepository.findByUserAndChannel(userId, channel);
        if (contactOpt.isEmpty()) {
            recordOutboundMessage(
                    payload.messageId(),
                    userId,
                    channel,
                    template,
                    content,
                    null,
                    AlertMessageStatus.FAILED,
                    AlertErrorCode.NO_CONTACT_FOUND.getCode(),
                    AlertErrorCode.NO_CONTACT_FOUND.format(channel)
            );
            return;
        }
        UserContactEntity contact = contactOpt.get();
        String recipientContact = contact.getContactValue();

        // ── 4. Legal Consent & Permissions ───────────────────────────────────
        boolean isUtility = template != null && (
                TemplateCategory.UTILITY.name().equalsIgnoreCase(template.getCategory()) ||
                TemplateCategory.AUTHENTICATION.name().equalsIgnoreCase(template.getCategory())
        );
        if (contact.isOptedOut() && !isUtility) {
            recordOutboundMessage(
                    payload.messageId(),
                    userId,
                    channel,
                    template,
                    content,
                    null,
                    AlertMessageStatus.FAILED,
                    AlertErrorCode.USER_OPTED_OUT.getCode(),
                    AlertErrorCode.USER_OPTED_OUT.format(userId, channel)
            );
            return;
        }

        if (!contact.isActive() && !contact.isOptedOut()) {
            recordOutboundMessage(
                    payload.messageId(),
                    userId,
                    channel,
                    template,
                    content,
                    null,
                    AlertMessageStatus.FAILED,
                    AlertErrorCode.CHANNEL_INACTIVE.getCode(),
                    AlertErrorCode.CHANNEL_INACTIVE.format(userId, channel)
            );
            return;
        }

        // ── 5. Conversational Timing / Sliding Window ────────────────────────
        // If human support session is active, marketing messages are delayed for 24 hours.
        // Utility messages, authentication alerts, and direct staff replies are allowed through.
        if (contact.isHumanSessionActive() && !isUtility && message.getPriority() != EventPriority.HIGH) {
            throw new EventDelayedException("User is waiting for human reply; delaying outbound message for 24 hours.");
        }

        // ── 6. Content Resolution & Template Preconditions ───────────────────
        if (payload.templateId() != null) {
            template = templateRepository.get(payload.templateId());
            if (template == null && (payload.rawContent() == null || payload.rawContent().isBlank())) {
                return;
            }
        }

        if (template != null) {
            if (template.getChannel() != channel) {
                String reason = AlertErrorCode.CHANNEL_MISMATCH.format(template.getChannel(), channel);
                recordOutboundMessage(
                        payload.messageId(),
                        userId,
                        channel,
                        template,
                        template.getContent(),
                        null,
                        AlertMessageStatus.FAILED,
                        AlertErrorCode.CHANNEL_MISMATCH.getCode(),
                        reason
                );
                return;
            }

            if (template.getStatus() != TemplateStatus.APPROVED) {
                String reason = AlertErrorCode.TEMPLATE_NOT_APPROVED.format(template.getTemplateKey(), template.getStatus());
                recordOutboundMessage(
                        payload.messageId(),
                        userId,
                        channel,
                        template,
                        template.getContent(),
                        null,
                        AlertMessageStatus.FAILED,
                        AlertErrorCode.TEMPLATE_NOT_APPROVED.getCode(),
                        reason
                );
                return;
            }

            if (template.getButtons() != null && template.getButtons().size() > 3) {
                if (!WhatsAppUtility.isInteractiveSessionActive(contact, messageRepository)) {
                    String reason = AlertErrorCode.SESSION_WINDOW_CLOSED.format(template.getTemplateKey());
                    recordOutboundMessage(
                            payload.messageId(),
                            userId,
                            channel,
                            template,
                            template.getContent(),
                            null,
                            AlertMessageStatus.FAILED,
                            AlertErrorCode.SESSION_WINDOW_CLOSED.getCode(),
                            reason
                    );
                    return;
                }
            }
        }

        String targetContent = (payload.rawContent() != null && !payload.rawContent().isBlank())
                ? payload.rawContent()
                : (template != null ? template.getContent() : "");

        Set<String> requiredAttrs = new HashSet<>();
        if (template != null && template.getRequiredAttributes() != null) {
            requiredAttrs.addAll(WhatsAppUtility.parseAttributes(template.getRequiredAttributes()));
        }
        if (!targetContent.isBlank()) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\{\\{([^}]+)\\}\\}").matcher(targetContent);
            while (matcher.find()) {
                requiredAttrs.add(matcher.group(1).trim());
            }
        }

        resolvedAttributes = new HashMap<>(AttributeResolutionEngine.getInstance().resolveAll(userId, requiredAttrs));
        if (payload.customParameters() != null && !payload.customParameters().isEmpty()) {
            resolvedAttributes.putAll(payload.customParameters());
        }
        content = TemplateRenderUtility.render(targetContent, resolvedAttributes);

        // ── 7. Dispatcher Resolution ─────────────────────────────────────────
        Optional<IAlertDispatcher> dispatcherOpt = dispatcherFactory.getDispatcher(channel);
        if (dispatcherOpt.isEmpty()) {
            String reason = AlertErrorCode.CHANNEL_NOT_AVAILABLE.format(channel);
            recordOutboundMessage(
                    payload.messageId(),
                    userId,
                    channel,
                    template,
                    content,
                    null,
                    AlertMessageStatus.FAILED,
                    AlertErrorCode.CHANNEL_NOT_AVAILABLE.getCode(),
                    reason
            );
            return;
        }
        IAlertDispatcher dispatcher = dispatcherOpt.get();

        // ── 8. Dispatch Execution ────────────────────────────────────────────
        String effectiveMediaUrl = payload.mediaUrl() != null && !payload.mediaUrl().isBlank()
                ? payload.mediaUrl()
                : (template != null ? template.getMediaUrl() : null);
        String effectiveMediaId = payload.mediaId() != null && !payload.mediaId().isBlank()
                ? payload.mediaId()
                : null;
        String effectiveMediaType = payload.mediaType() != null && !payload.mediaType().isBlank()
                ? payload.mediaType()
                : (template != null ? template.getMediaType() : null);

        AlertDispatchRequest dispatchRequest = new AlertDispatchRequest(
                userId,
                template,
                channel,
                recipientContact,
                content,
                resolvedAttributes,
                message.getPriority(),
                effectiveMediaUrl,
                effectiveMediaId,
                effectiveMediaType
        );
        AlertDispatchResult result = dispatcher.dispatch(dispatchRequest);

        // ── 9. Record Outcome ────────────────────────────────────────────────
        if (result.isSuccess()) {
            recordOutboundMessage(
                    payload.messageId(),
                    userId,
                    channel,
                    template,
                    content,
                    result.externalMessageId(),
                    AlertMessageStatus.SENT,
                    null,
                    result.rawResponse(),
                    effectiveMediaUrl,
                    effectiveMediaId,
                    effectiveMediaType
            );
        } else if (result.isFailure()) {
            String failReason = result.rawResponse() != null ? result.rawResponse() : ("Downstream dispatch failure: " + result.errorMessage());
            recordOutboundMessage(
                    payload.messageId(),
                    userId,
                    channel,
                    template,
                    content,
                    null,
                    AlertMessageStatus.FAILED,
                    result.errorCode() != null ? result.errorCode() : AlertErrorCode.ERR_DISPATCH_FAILED,
                    failReason,
                    effectiveMediaUrl,
                    effectiveMediaId,
                    effectiveMediaType
            );
        } else if (result.isDelayed()) {
            throw new EventDelayedException(result.errorMessage() != null ? result.errorMessage() : "Message dispatch delayed");
        }

    }

    /*
     * =========================================================================
     * INBOUND WEBHOOK PROCESSING PIPELINE & DECISION TREE
     * =========================================================================
     *
     *                      +------------------------------------------+
     *                      |       Meta Webhook Event Received        |
     *                      |       (WhatsAppWebhookPayloadDTO)        |
     *                      +------------------------------------------+
     *                                           |
     *               +---------------------------+---------------------------+
     *               |                           |                           |
     *               v                           v                           v
     *   +-----------------------+   +-----------------------+   +-----------------------+
     *   |  1. Status Receipts   |   |  2. Customer Messages |   | 3. Lifecycle / Prefs  |
     *   |   (value.statuses)    |   |   (value.messages)    |   | (template / userPref) |
     *   +-----------------------+   +-----------------------+   +-----------------------+
     *               |                           |                           |
     *               v                           |                           v
     *   +-----------------------+               |               +-----------------------+
     *   | Match wamid in        |               |               | * Template Update:    |
     *   | FL_ALERT_MESSAGE:     |               |               |   APPROVED / REJECTED |
     *   | - Errors -> FailAction|               |               | * User Preferences:   |
     *   | - SENT/DELIVERED/READ |               |               |   Opt-in / Opt-out    |
     *   +-----------------------+               |               +-----------------------+
     *                                           |
     *   ========================================v========================================
     *                     CUSTOMER MESSAGE INBOUND PIPELINE (2)
     *   =================================================================================
     *                                           |
     *                                           v
     *                          +---------------------------------+
     *                          |    Deduplication (msg.id)?      |
     *                          +---------------------------------+
     *                                    |             |
     *                           Found    |             | Absent (New)
     *                                    v             v
     *                              +----------+  +---------------------------------+
     *                              |   DROP   |  | Contact Resolution (fromPhone)  |
     *                              | (Ignore) |  | - Found: resolve userId         |
     *                              +----------+  | - Absent: auto-provision contact|
     *                                            +---------------------------------+
     *                                                           |
     *                                                           v
     *                                            +-----------------------------+
     *                                            | msg.extractButtonPayload()? |
     *                                            +-----------------------------+
     *                                                   /               \
     *                           Button / Quick-Reply   /                 \  Plain Text / Fallback
     *                                                 v                   v
     *      +-------------------------------------------+     +-----------------------------------+
     *      | Lookup FL_ALERT_BUTTON_REPLY by Payload   |     | Consent Keyword (STOP / START)?   |
     *      +-------------------------------------------+     +-----------------------------------+
     *           /                  |                 \              /                     \
     *          /                   |                  \       Yes  /                       \ No (Freeform)
     *         v                    v                   v          v                         v
     *  +-------------+      +-------------+      +------------+ +--------------------+  +--------------------+
     *  |   MATCHED   |      |   MATCHED   |      |  UNMAPPED  | | Update Consent   |  | Persist TEXT Msg   |
     *  | human=false |      | human=true  |      | (Fallback) | | FL_USER_CONTACT  |  | (Session Inactive) |
     *  +-------------+      +-------------+      +------------+ | is_opted_out = T/F |  +--------------------+
     *         |                    |                   |       +--------------------+            |
     *         |                    v                   |                 |                       v
     *         |             +---------------+          |                 v              +--------------------+
     *         |             | Extend Session|          |       +--------------------+   | Emit Automated     |
     *         |             | (now + 24h)   |          |       | Persist TEXT Msg   |   | Welcome Menu       |
     *         |             | on Contact    |          |       | in FL_ALERT_MESSAGE|   | (welcome_menu)     |
     *         |             +---------------+          |       +--------------------+   +--------------------+
     *         |                    |                   |
     *         v                    v                   v
     *  +------------------------------------+   +------------------------------------+
     *  | Persist BUTTON_REPLY to            |   | Extend Session (now + 24h)         |
     *  | FL_ALERT_MESSAGE                   |   | Persist BUTTON_REPLY to            |
     *  +------------------------------------+   | FL_ALERT_MESSAGE                   |
     *         |                                 +------------------------------------+
     *         v
     *  +------------------------------------+
     *  | Has replyTemplateId?               |
     *  | -> AlertsEngine.emitAlert()        |
     *  +------------------------------------+
     *
     * Note on Human Session:
     * The 24-hour human support session (human_session_expires_at on FL_USER_CONTACT)
     * is activated or extended via explicit interactive button clicks (requires_human_agent=true
     * or unmapped fallback) or when inbound media is sent (images/documents). Inbound freeform
     * text defaults to the automated welcome menu (inbound_welcome_menu) when no human session
     * is active, preventing notification spam until the customer requests human assistance.
     *
     * Note on Deduplication:
     * Inbound messages are deduplicated using Meta's wamid (FL_ALERT_MESSAGE.meta_message_id)
     * to maintain idempotency under webhook retries.
     */
    public void processInbound(EventMessage<WhatsAppWebhookPayloadDTO> eventMessage) {
        if (eventMessage == null) return;
        WhatsAppWebhookPayloadDTO payload = eventMessage.getPayload();
        if (payload == null) return;

        List<WebhookEntryDTO> entries = payload.resolveEntries();
        if (entries == null || entries.isEmpty()) return;

        for (WebhookEntryDTO entry : entries) {
            if (entry == null || entry.changes() == null) continue;
            for (WebhookChangeDTO change : entry.changes()) {
                if (change == null || change.value() == null) continue;
                WebhookValueDTO value = change.value();

                // ── 1. Delivery & Read Status Receipts ──────────────────────────────
                if (value.statuses() != null && !value.statuses().isEmpty()) {
                    for (WebhookStatusDTO status : value.statuses()) {
                        String wamid = status.id();
                        if (wamid == null) continue;

                        Optional<AlertMessageEntity> messageOpt = messageRepository.findByMetaMessageId(wamid);
                        if (messageOpt.isEmpty()) {
                            continue;
                        }

                        AlertMessageEntity msg = messageOpt.get();

                        if (status.errors() != null && !status.errors().isEmpty()) {
                            WebhookErrorDTO error = status.errors().get(0);
                            WhatsAppUtility.applyFailureAction(
                                    wamid,
                                    error.code(),
                                    error.errorSubcode(),
                                    error.message() != null ? error.message() : (error.errorData() != null ? error.errorData().details() : null),
                                    messageRepository,
                                    contactRepository,
                                    templateRepository
                            );
                        } else if ("delivered".equalsIgnoreCase(status.status())) {
                            if (msg.getStatus() == AlertMessageStatus.SENT || msg.getStatus() == AlertMessageStatus.PENDING) {
                                msg.setStatus(AlertMessageStatus.DELIVERED);
                                messageRepository.merge(msg);
                            }
                        } else if ("read".equalsIgnoreCase(status.status())) {
                            msg.setStatus(AlertMessageStatus.READ);
                            messageRepository.merge(msg);
                        } else if ("sent".equalsIgnoreCase(status.status())) {
                            if (msg.getStatus() == AlertMessageStatus.PENDING) {
                                msg.setStatus(AlertMessageStatus.SENT);
                                messageRepository.merge(msg);
                            }
                        } else if ("failed".equalsIgnoreCase(status.status())) {
                            msg.setStatus(AlertMessageStatus.FAILED);
                            messageRepository.merge(msg);
                        }
                    }
                }

                // ── 2. Customer Inbound Messages (text, buttons, list replies) ──────
                if (value.messages() != null && !value.messages().isEmpty()) {
                    for (WebhookMessageDTO msg : value.messages()) {
                        if (msg == null) continue;

                        if (msg.id() != null && messageRepository.findByMetaMessageId(msg.id()).isPresent()) {
                            continue;
                        }

                        String fromPhone = msg.from();
                        if (fromPhone == null || fromPhone.isBlank()) {
                            continue;
                        }

                        Optional<UserContactEntity> contactOpt = contactRepository.findByContactAndChannel(fromPhone, AlertChannel.WHATSAPP);

                        UserContactEntity contact;
                        UUID userId;
                        if (contactOpt.isPresent()) {
                            contact = contactOpt.get();
                            userId = contact.getUserId();
                        } else {
                            userId = UUID.nameUUIDFromBytes(("wa:" + fromPhone).getBytes());
                            contact = new UserContactEntity(UUID.randomUUID(), userId, AlertChannel.WHATSAPP, fromPhone, false, true, null);
                            contactRepository.persist(contact);
                        }

                        Instant messageTime = Instant.now();
                        if (msg.timestamp() != null) {
                            try {
                                messageTime = Instant.ofEpochSecond(Long.parseLong(msg.timestamp()));
                            } catch (NumberFormatException ignored) {}
                        }

                        String buttonPayload = msg.extractButtonPayload();
                        String textContent = msg.extractTextContent();
                        if (textContent == null || textContent.isBlank()) {
                            textContent = msg.type() != null ? "[" + msg.type() + "]" : "[message]";
                        }

                        // 2A. Button & Interactive List Replies
                        if (buttonPayload != null && !buttonPayload.isBlank()) {
                            Optional<AlertButtonReplyEntity> replyOpt = buttonReplyRepository.findByButtonPayload(buttonPayload);

                            if (replyOpt.isPresent()) {
                                AlertButtonReplyEntity reply = replyOpt.get();
                                if (reply.isRequiresHumanAgent()) {
                                    WhatsAppUtility.extendHumanSession(contact, contactRepository, Duration.ofHours(24));
                                }

                                AlertMessageEntity inboundMsg = new AlertMessageEntity(
                                        UUID.randomUUID(),
                                        userId,
                                        AlertChannel.WHATSAPP,
                                        AlertDirection.INBOUND,
                                        "BUTTON_REPLY",
                                        textContent,
                                        msg.id(),
                                        AlertMessageStatus.DELIVERED,
                                        null,
                                        null,
                                        messageTime
                                );
                                messageRepository.persist(inboundMsg);

                                if (reply.getReplyTemplate() != null) {
                                    alertsEngine.emitAlert(reply.getReplyTemplate().getId(), userId);
                                }
                                continue;
                            } else {
                                // Unconfigured / unmapped button payload: unrecognized interactive action, fallback to human intervention
                                WhatsAppUtility.extendHumanSession(contact, contactRepository, Duration.ofHours(24));

                                AlertMessageEntity inboundMsg = new AlertMessageEntity(
                                        UUID.randomUUID(),
                                        userId,
                                        AlertChannel.WHATSAPP,
                                        AlertDirection.INBOUND,
                                        "BUTTON_REPLY",
                                        textContent,
                                        msg.id(),
                                        AlertMessageStatus.DELIVERED,
                                        null,
                                        null,
                                        messageTime
                                );
                                messageRepository.persist(inboundMsg);
                                continue;
                            }
                        }

                        // 2B. Opt-out / Opt-in Keywords
                        String trimmedText = textContent != null ? textContent.trim() : "";
                        boolean isOptOut = WhatsAppUtility.isOptOutKeyword(trimmedText);
                        boolean isOptIn = WhatsAppUtility.isOptInKeyword(trimmedText);

                        if (isOptOut) {
                            WhatsAppUtility.updateUserConsent(userId, fromPhone, contactRepository, true, false);
                            contact.setOptedOut(true);
                            contact.setActive(false);
                        } else if (isOptIn) {
                            WhatsAppUtility.updateUserConsent(userId, fromPhone, contactRepository, false, true);
                            contact.setOptedOut(false);
                            contact.setActive(true);
                        }

                        // 2C. Inbound Message (Media or Freeform Text)
                        WebhookMediaDTO media = msg.extractMedia();
                        String inMsgType = "TEXT";
                        String mediaId = null;
                        String mediaUrl = null;
                        if ("image".equalsIgnoreCase(msg.type()) || msg.image() != null) {
                            inMsgType = "IMAGE";
                            if (media != null && media.id() != null) {
                                mediaId = media.id();
                                mediaUrl = "/alerts/media/" + mediaId;
                            }
                        } else if ("document".equalsIgnoreCase(msg.type()) || msg.document() != null) {
                            inMsgType = "DOCUMENT";
                            if (media != null && media.id() != null) {
                                mediaId = media.id();
                                mediaUrl = "/alerts/media/" + mediaId;
                            }
                        }

                        AlertMessageEntity inboundMsg = new AlertMessageEntity(
                                UUID.randomUUID(),
                                userId,
                                AlertChannel.WHATSAPP,
                                AlertDirection.INBOUND,
                                inMsgType,
                                textContent,
                                null,
                                msg.id(),
                                AlertMessageStatus.DELIVERED,
                                null,
                                null,
                                messageTime,
                                mediaUrl,
                                mediaId,
                                inMsgType
                        );
                        messageRepository.persist(inboundMsg);

                        // If media is sent, extend human session so staff can view and assist
                        if (mediaId != null) {
                            WhatsAppUtility.extendHumanSession(contact, contactRepository, Duration.ofHours(24));
                        } else if (!isOptOut && !isOptIn && !contact.isHumanSessionActive()) {
                            // If plain text, not consent keyword, and NO active human session, dispatch automated welcome menu template (triage)
                            templateRepository.findByKey(InboundWelcomeTemplate.TEMPLATE_KEY)
                                    .ifPresent(welcomeTpl -> alertsEngine.emitAlert(welcomeTpl.getId(), userId));
                        }
                    }
                }

                // ── 3. Meta Template Lifecycle Updates ──────────────────────────────
                WebhookTemplateStatusDTO rawTplUpdate = value.resolveTemplateStatusUpdate();
                if (rawTplUpdate != null) {
                    final WebhookTemplateStatusDTO tplUpdate = ((rawTplUpdate.messageTemplateName() == null || rawTplUpdate.messageTemplateName().isBlank())
                            && change.field() != null && !"message_template_status_update".equalsIgnoreCase(change.field()))
                            ? new WebhookTemplateStatusDTO(
                                    rawTplUpdate.event(),
                                    rawTplUpdate.messageTemplateId(),
                                    change.field(),
                                    rawTplUpdate.messageTemplateLanguage(),
                                    rawTplUpdate.reason()
                            )
                            : rawTplUpdate;
                    String templateName = tplUpdate.messageTemplateName();
                    if (templateName != null) {
                        final String metaTplId = tplUpdate.messageTemplateId();
                        Optional<AlertTemplateEntity> templateOpt = templateRepository.findByKey(templateName)
                                .or(() -> templateRepository.findByKeyIgnoreCase(templateName))
                                .or(() -> (metaTplId != null && !metaTplId.isBlank())
                                        ? templateRepository.findByMetaTemplateId(metaTplId)
                                        : Optional.empty());
                        if (templateOpt.isPresent()) {
                            AlertTemplateEntity tpl = templateOpt.get();
                            if ("APPROVED".equalsIgnoreCase(tplUpdate.event())) {
                                tpl.setStatus(TemplateStatus.APPROVED);
                            } else if ("REJECTED".equalsIgnoreCase(tplUpdate.event())) {
                                tpl.setStatus(TemplateStatus.REJECTED);
                            } else if ("PAUSED".equalsIgnoreCase(tplUpdate.event())) {
                                tpl.setStatus(TemplateStatus.PAUSED);
                            }
                            tpl.setMetaTemplateId(metaTplId);
                            templateRepository.merge(tpl);
                        }
                    }
                }

                // ── 4. Meta User Preferences (Native WhatsApp UI Opt-out / Opt-in) ──
                if (value.userPreferences() != null && !value.userPreferences().isEmpty()) {
                    for (WebhookUserPreferenceDTO pref : value.userPreferences()) {
                        if (pref == null) continue;
                        String waId = pref.resolveWaId();
                        if (waId == null || waId.isBlank()) continue;

                        Optional<UserContactEntity> contactOpt = contactRepository.findByContactAndChannel(waId, AlertChannel.WHATSAPP);
                        UUID userId = contactOpt.map(UserContactEntity::getUserId).orElse(null);
                        if (userId == null) {
                            userId = UUID.nameUUIDFromBytes(("wa:" + waId).getBytes());
                            UserContactEntity newContact = new UserContactEntity(
                                    UUID.randomUUID(),
                                    userId,
                                    AlertChannel.WHATSAPP,
                                    waId,
                                    false,
                                    true,
                                    null
                            );
                            contactRepository.persist(newContact);
                        }

                        boolean isOptOut = pref.isOptOut();
                        WhatsAppUtility.updateUserConsent(userId, waId, contactRepository, isOptOut, !isOptOut);
                    }
                }
            }
        }
    }

    // =========================================================================
    // Dead Letter & Persistence Helpers
    // =========================================================================

    public void handleOutboundDeadLetter(EventMessage<AlertEventPayloadDTO> message, Throwable cause) {
        AlertEventPayloadDTO payload = message != null ? message.getPayload() : null;
        if (payload != null && payload.userId() != null) {
            AlertErrorCode errCode = isDelayedCause(cause)
                    ? AlertErrorCode.HUMAN_REPLY_TIMEOUT
                    : AlertErrorCode.MAX_RETRIES_EXCEEDED;

            String errorCode = errCode.getCode();
            String errorDetails = cause != null ? cause.getMessage() : errCode.format();

            AlertTemplateEntity template = null;
            if (payload.templateId() != null) {
                try {
                    template = templateRepository.get(payload.templateId());
                } catch (Exception ignored) {
                }
            }

            AlertChannel channel = template != null ? template.getChannel() : AlertChannel.WHATSAPP;
            String content = payload.rawContent() != null ? payload.rawContent() : (template != null ? template.getContent() : "[Unknown content]");
            String messageType = template != null ? "TEMPLATE" : "TEXT";
            UUID id = payload.messageId() != null ? payload.messageId() : UUID.randomUUID();

            AlertMessageEntity messageEntity = new AlertMessageEntity(
                    id,
                    payload.userId(),
                    channel,
                    AlertDirection.OUTBOUND,
                    messageType,
                    content,
                    template,
                    null,
                    AlertMessageStatus.FAILED,
                    errorCode,
                    errorDetails,
                    Instant.now()
            );

            try {
                messageRepository.persist(messageEntity);
            } catch (Exception ignored) {
            }
        }
    }

    private boolean isDelayedCause(Throwable cause) {
        Throwable current = cause;
        while (current != null) {
            if (current instanceof EventDelayedException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void recordOutboundMessage(
            UUID messageId,
            UUID userId,
            AlertChannel channel,
            AlertTemplateEntity template,
            String content,
            String externalMessageId,
            AlertMessageStatus status,
            String errorCode,
            String rawDetails
    ) {
        recordOutboundMessage(messageId, userId, channel, template, content, externalMessageId, status, errorCode, rawDetails, null, null, null);
    }

    private void recordOutboundMessage(
            UUID messageId,
            UUID userId,
            AlertChannel channel,
            AlertTemplateEntity template,
            String content,
            String externalMessageId,
            AlertMessageStatus status,
            String errorCode,
            String rawDetails,
            String mediaUrl,
            String mediaId,
            String mediaType
    ) {
        String msgType = mediaType != null && !mediaType.isBlank()
                ? mediaType
                : (template != null ? "TEMPLATE" : "TEXT");
        UUID id = messageId != null ? messageId : UUID.randomUUID();
        String resolvedContent = content != null ? content : (template != null ? template.getContent() : "[Unknown content]");
        AlertMessageEntity messageEntity = new AlertMessageEntity(
                id,
                userId,
                channel != null ? channel : AlertChannel.WHATSAPP,
                AlertDirection.OUTBOUND,
                msgType,
                resolvedContent,
                template,
                externalMessageId,
                status,
                errorCode,
                rawDetails,
                Instant.now(),
                mediaUrl,
                mediaId,
                msgType
        );
        messageRepository.persist(messageEntity);
    }
}
