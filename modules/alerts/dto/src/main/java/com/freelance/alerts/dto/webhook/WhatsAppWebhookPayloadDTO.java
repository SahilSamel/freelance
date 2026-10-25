package com.freelance.alerts.dto.webhook;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WhatsAppWebhookPayloadDTO(
        String object,
        List<WebhookEntryDTO> entry,

        // Flat MSG91 Webhook & DLR properties
        @JsonProperty("request_id") @JsonAlias({"requestId", "crqid"}) String requestId,
        @JsonProperty("status") @JsonAlias({"eventName", "event_name", "event", "reason", "delivery_status", "deliveryStatus"}) String status,
        @JsonAlias({"sender", "from"}) String sender,
        String recipient,
        String message,
        @JsonProperty("msg_id") @JsonAlias({"msgId"}) String msgId,
        @JsonProperty("timestamp") @JsonAlias({"ts", "requestedAt", "readAt", "read_at", "deliveredAt", "delivered_at"}) String timestamp,

        // Additional MSG91 Live & Test Payload attributes
        @JsonProperty("customer_number") @JsonAlias({"customerNumber", "customer_no"}) String customerNumber,
        @JsonProperty("customer_name") @JsonAlias({"customerName"}) String customerName,
        @JsonProperty("content") Object content,
        @JsonProperty("uuid") @JsonAlias({"messageUuid", "message_uuid", "msg_uuid"}) String uuid,
        @JsonProperty("message_type") @JsonAlias({"messageType"}) String messageType,
        @JsonProperty("contacts") Object contacts,
        @JsonProperty("contentType") @JsonAlias({"content_type", "type", "mime_type", "mediaType"}) String contentType,
        @JsonProperty("text") Object text,
        @JsonProperty("caption") Object caption,
        @JsonProperty("messages") Object messages,

        // Rich Media, Location, Interactive & Reply Attributes
        @JsonProperty("url") @JsonAlias({"mediaUrl", "media_url", "file", "attachment", "link"}) String url,
        @JsonProperty("filename") @JsonAlias({"fileName", "file_name", "documentName", "name"}) String filename,
        @JsonProperty("button") Object button,
        @JsonProperty("interactive") Object interactive,
        @JsonProperty("latitude") String latitude,
        @JsonProperty("longitude") String longitude,
        @JsonProperty("reaction") Object reaction,
        @JsonProperty("replyMsgId") @JsonAlias({"reply_msg_id", "reply_to", "contextMsgId"}) String replyMsgId
) {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public WhatsAppWebhookPayloadDTO(String object, List<WebhookEntryDTO> entry) {
        this(object, entry, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public List<WebhookEntryDTO> resolveEntries() {
        if (entry != null && !entry.isEmpty()) {
            return entry;
        }

        String rawStatus = status != null ? status.trim().toLowerCase() : null;
        boolean isDeliveryReport = rawStatus != null && (
                rawStatus.equals("sent") ||
                rawStatus.equals("submitted") ||
                rawStatus.equals("delivered") ||
                rawStatus.equals("read") ||
                rawStatus.equals("viewed") ||
                rawStatus.equals("failed") ||
                rawStatus.equals("rejected") ||
                rawStatus.equals("undelivered")
        );

        // Check if payload contains customer inbound message content
        boolean hasMessageContent = !isDeliveryReport && (
                (text != null && !String.valueOf(text).isBlank() && !String.valueOf(text).startsWith("{{"))
                || (caption != null && !String.valueOf(caption).isBlank() && !String.valueOf(caption).startsWith("{{"))
                || (messages != null && !String.valueOf(messages).isBlank() && !String.valueOf(messages).startsWith("{{"))
                || (content != null && !String.valueOf(content).isBlank() && !String.valueOf(content).startsWith("{{")
                    && !"Sample message content for webhook test".equalsIgnoreCase(String.valueOf(content).trim()))
                || (contentType != null && !contentType.isBlank() && !contentType.startsWith("{{"))
                || (url != null && !url.isBlank() && !url.startsWith("{{"))
                || (button != null && !String.valueOf(button).isBlank() && !String.valueOf(button).startsWith("{{"))
                || (latitude != null && !latitude.isBlank() && !latitude.startsWith("{{"))
        );

        // 1. Handle MSG91 flat status / delivery reports (prioritize over echoed content)
        if (isDeliveryReport || !hasMessageContent) {
            String effectiveStatus = rawStatus;
            if ((effectiveStatus == null || effectiveStatus.isBlank() || effectiveStatus.startsWith("{{")) && timestamp != null && !timestamp.startsWith("{{")) {
                effectiveStatus = "read";
            }

            if (effectiveStatus != null && !effectiveStatus.isBlank() && !effectiveStatus.startsWith("{{") && !"inbound".equalsIgnoreCase(effectiveStatus)) {
                String effectiveReqId = (requestId != null && !requestId.startsWith("{{") && !requestId.isBlank())
                        ? requestId
                        : ((uuid != null && !uuid.startsWith("{{") && !uuid.isBlank())
                            ? uuid
                            : msgId);
                String effectiveRecipient = (customerNumber != null && !customerNumber.startsWith("{{") && !customerNumber.isBlank())
                        ? customerNumber
                        : recipient;
                WebhookStatusDTO statusDto = new WebhookStatusDTO(effectiveReqId, effectiveStatus, timestamp, effectiveRecipient, null);
                WebhookValueDTO val = new WebhookValueDTO("whatsapp", null, List.of(statusDto), null, null);
                WebhookChangeDTO change = new WebhookChangeDTO("messages", val);
                return List.of(new WebhookEntryDTO(effectiveReqId, List.of(change)));
            }
        }

        // 2. Handle MSG91 Inbound Messages (Live Webhook & Test Run Modal)
        String effectiveSender = null;

        // In MSG91 webhooks, the sender is always in customerNumber, messages[0].from, or sender.
        // NOTE: The 'contacts' payload field is NOT the sender; it is an attachment when someone shares a contact card.
        if (customerNumber != null && !customerNumber.isBlank()
                && !customerNumber.startsWith("{{") && !customerNumber.contains("X") && !customerNumber.contains("x")) {
            effectiveSender = customerNumber.trim();
        }

        if (effectiveSender == null && messages != null && !String.valueOf(messages).isBlank() && !String.valueOf(messages).startsWith("{{")) {
            Matcher mFrom = Pattern.compile("\"from\"\\s*:\\s*\"?([0-9+]+)\"?").matcher(String.valueOf(messages));
            if (mFrom.find()) {
                effectiveSender = mFrom.group(1).trim();
            }
        }

        if (effectiveSender == null && sender != null && !sender.isBlank()
                && !sender.startsWith("{{") && !sender.contains("X") && !sender.contains("x")) {
            effectiveSender = sender.trim();
        }

        // Last-resort fallback for customerNumber or sender even if dummy
        if (effectiveSender == null && customerNumber != null && !customerNumber.isBlank() && !customerNumber.startsWith("{{")) {
            effectiveSender = customerNumber.trim();
        }
        if (effectiveSender == null && sender != null && !sender.isBlank() && !sender.startsWith("{{")) {
            effectiveSender = sender.trim();
        }

        String effectiveMessage = null;
        if (text != null) {
            String s = String.valueOf(text).trim();
            if (!s.isBlank() && !s.startsWith("{{")) {
                effectiveMessage = s;
            }
        }
        if (effectiveMessage == null && caption != null) {
            String s = String.valueOf(caption).trim();
            if (!s.isBlank() && !s.startsWith("{{")) {
                effectiveMessage = s;
            }
        }
        if (effectiveMessage == null && message != null) {
            String s = message.trim();
            if (!s.isBlank() && !s.startsWith("{{")) {
                effectiveMessage = s;
            }
        }
        if (effectiveMessage == null && content != null) {
            if (content instanceof Map<?, ?> map && map.containsKey("text")) {
                effectiveMessage = String.valueOf(map.get("text")).trim();
            } else {
                String s = String.valueOf(content).trim();
                if (!s.isBlank() && !s.startsWith("{{")) {
                    effectiveMessage = s;
                }
            }
        }

        if (effectiveSender != null) {
            String rawType = contentType != null && !contentType.isBlank() && !contentType.startsWith("{{")
                    ? contentType.trim().toLowerCase()
                    : (messageType != null && !messageType.isBlank() && !messageType.startsWith("{{")
                        ? messageType.trim().toLowerCase()
                        : "text");

            String effectiveMediaUrl = (url != null && !url.isBlank() && !url.startsWith("{{")) ? url.trim() : null;
            String effectiveFilename = (filename != null && !filename.isBlank() && !filename.startsWith("{{")) ? filename.trim() : null;
            String extractedReplyMsgId = (replyMsgId != null && !replyMsgId.isBlank() && !replyMsgId.startsWith("{{")) ? replyMsgId.trim() : null;

            // Deep parsing of raw `messages` payload from Meta Cloud API / MSG91
            if (messages != null && !String.valueOf(messages).isBlank() && !String.valueOf(messages).startsWith("{{")) {
                try {
                    JsonNode msgNode = OBJECT_MAPPER.readTree(String.valueOf(messages));
                    if (msgNode.isArray() && !msgNode.isEmpty()) {
                        msgNode = msgNode.get(0);
                    }
                    if (msgNode != null && msgNode.isObject()) {
                        // Extract context / quoted message ID
                        if (msgNode.has("context") && msgNode.get("context").has("id")) {
                            String ctxId = msgNode.get("context").get("id").asText();
                            if (ctxId != null && !ctxId.isBlank()) {
                                extractedReplyMsgId = ctxId.trim();
                            }
                        }

                        // Type detection
                        if (msgNode.has("type")) {
                            String t = msgNode.get("type").asText();
                            if (t != null && !t.isBlank()) {
                                rawType = t.trim().toLowerCase();
                            }
                        }

                        // Document / PDF
                        if (msgNode.has("document")) {
                            JsonNode docNode = msgNode.get("document");
                            rawType = "document";
                            if (docNode.has("filename") && !docNode.get("filename").asText().isBlank()) {
                                effectiveFilename = docNode.get("filename").asText().trim();
                            }
                            if (docNode.has("caption") && !docNode.get("caption").asText().isBlank()) {
                                effectiveMessage = docNode.get("caption").asText().trim();
                            }
                            String docId = docNode.has("id") ? docNode.get("id").asText().trim() : null;
                            if (docNode.has("url") && !docNode.get("url").asText().isBlank()) {
                                effectiveMediaUrl = docNode.get("url").asText().trim();
                            } else if (docNode.has("link") && !docNode.get("link").asText().isBlank()) {
                                effectiveMediaUrl = docNode.get("link").asText().trim();
                            }
                            effectiveMediaUrl = resolvePublicMediaUrl(effectiveMediaUrl, docId, recipient);
                        }

                        // Sticker
                        if (msgNode.has("sticker")) {
                            JsonNode stkNode = msgNode.get("sticker");
                            rawType = "sticker";
                            String stkId = stkNode.has("id") ? stkNode.get("id").asText().trim() : null;
                            if (stkNode.has("url") && !stkNode.get("url").asText().isBlank()) {
                                effectiveMediaUrl = stkNode.get("url").asText().trim();
                            } else if (stkNode.has("link") && !stkNode.get("link").asText().isBlank()) {
                                effectiveMediaUrl = stkNode.get("link").asText().trim();
                            }
                            effectiveMediaUrl = resolvePublicMediaUrl(effectiveMediaUrl, stkId, recipient);
                            if (effectiveMessage == null) effectiveMessage = "🎨 [Sticker]";
                        }

                        // Image
                        if (msgNode.has("image")) {
                            JsonNode imgNode = msgNode.get("image");
                            rawType = "image";
                            if (imgNode.has("caption") && !imgNode.get("caption").asText().isBlank()) {
                                effectiveMessage = imgNode.get("caption").asText().trim();
                            }
                            String imgId = imgNode.has("id") ? imgNode.get("id").asText().trim() : null;
                            if (imgNode.has("url") && !imgNode.get("url").asText().isBlank()) {
                                effectiveMediaUrl = imgNode.get("url").asText().trim();
                            } else if (imgNode.has("link") && !imgNode.get("link").asText().isBlank()) {
                                effectiveMediaUrl = imgNode.get("link").asText().trim();
                            }
                            effectiveMediaUrl = resolvePublicMediaUrl(effectiveMediaUrl, imgId, recipient);
                        }

                        // Video / GIF
                        if (msgNode.has("video")) {
                            JsonNode vidNode = msgNode.get("video");
                            rawType = "video";
                            if (vidNode.has("caption") && !vidNode.get("caption").asText().isBlank()) {
                                effectiveMessage = vidNode.get("caption").asText().trim();
                            }
                            String vidId = vidNode.has("id") ? vidNode.get("id").asText().trim() : null;
                            if (vidNode.has("url") && !vidNode.get("url").asText().isBlank()) {
                                effectiveMediaUrl = vidNode.get("url").asText().trim();
                            } else if (vidNode.has("link") && !vidNode.get("link").asText().isBlank()) {
                                effectiveMediaUrl = vidNode.get("link").asText().trim();
                            }
                            effectiveMediaUrl = resolvePublicMediaUrl(effectiveMediaUrl, vidId, recipient);
                            if (effectiveMessage == null) effectiveMessage = "🎬 [Video/GIF]";
                        }

                        // Audio / Voice
                        if (msgNode.has("audio")) {
                            JsonNode audNode = msgNode.get("audio");
                            rawType = "audio";
                            String audId = audNode.has("id") ? audNode.get("id").asText().trim() : null;
                            if (audNode.has("url") && !audNode.get("url").asText().isBlank()) {
                                effectiveMediaUrl = audNode.get("url").asText().trim();
                            } else if (audNode.has("link") && !audNode.get("link").asText().isBlank()) {
                                effectiveMediaUrl = audNode.get("link").asText().trim();
                            }
                            effectiveMediaUrl = resolvePublicMediaUrl(effectiveMediaUrl, audId, recipient);
                            if (effectiveMessage == null) effectiveMessage = "🎵 [Audio Message]";
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            effectiveMediaUrl = resolvePublicMediaUrl(effectiveMediaUrl, null, recipient);

            WebhookButtonDTO buttonDto = null;
            if (button != null && !String.valueOf(button).isBlank() && !String.valueOf(button).startsWith("{{")) {
                String btnText = String.valueOf(button).trim();
                buttonDto = new WebhookButtonDTO(btnText, btnText);
                rawType = "button";
                if (effectiveMessage == null) effectiveMessage = btnText;
            }

            // Location support
            if ((latitude != null && !latitude.isBlank() && !latitude.startsWith("{{"))
                    || "location".equalsIgnoreCase(rawType)) {
                rawType = "location";
                effectiveMessage = "📍 Location: https://maps.google.com/?q=" + (latitude != null ? latitude.trim() : "") + "," + (longitude != null ? longitude.trim() : "");
            }

            // Clean Contact card parsing (clean name & phone without vcard base64 payload dump)
            if ("contacts".equalsIgnoreCase(rawType) || "contact".equalsIgnoreCase(rawType) || (contacts != null && !String.valueOf(contacts).isBlank() && !String.valueOf(contacts).startsWith("{{"))) {
                String parsedContact = parseContactCard(contacts);
                if (parsedContact != null) {
                    effectiveMessage = parsedContact;
                }
            }

            WebhookMediaDTO imageDto = null;
            WebhookMediaDTO docDto = null;
            WebhookMediaDTO audioDto = null;
            WebhookMediaDTO videoDto = null;

            if ("image".equalsIgnoreCase(rawType) || "sticker".equalsIgnoreCase(rawType)) {
                imageDto = new WebhookMediaDTO(uuid, "image/jpeg", null, effectiveMessage, null, effectiveMediaUrl);
                if (effectiveMessage == null) effectiveMessage = "sticker".equalsIgnoreCase(rawType) ? "🎨 [Sticker]" : "[Image]";
            } else if ("document".equalsIgnoreCase(rawType) || "pdf".equalsIgnoreCase(rawType)) {
                docDto = new WebhookMediaDTO(uuid, "application/pdf", null, effectiveMessage, effectiveFilename != null ? effectiveFilename : "Document.pdf", effectiveMediaUrl);
                if (effectiveMessage == null) effectiveMessage = effectiveFilename != null ? ("📄 " + effectiveFilename) : "📄 [Document]";
            } else if ("audio".equalsIgnoreCase(rawType) || "voice".equalsIgnoreCase(rawType)) {
                audioDto = new WebhookMediaDTO(uuid, "audio/mpeg", null, effectiveMessage, null, effectiveMediaUrl);
                if (effectiveMessage == null) effectiveMessage = "🎵 [Audio Message]";
            } else if ("video".equalsIgnoreCase(rawType)) {
                videoDto = new WebhookMediaDTO(uuid, "video/mp4", null, effectiveMessage, null, effectiveMediaUrl);
                if (effectiveMessage == null) effectiveMessage = "🎬 [Video/GIF]";
            }

            if (effectiveMessage == null || effectiveMessage.isBlank()) {
                effectiveMessage = "[Inbound Message]";
            }

            String id = (uuid != null && !uuid.isBlank() && !uuid.startsWith("{{"))
                    ? ("wamid.sample-uuid-for-webhook-test".equalsIgnoreCase(uuid) ? (uuid + "_" + System.currentTimeMillis()) : uuid)
                    : ((msgId != null && !msgId.isBlank() && !msgId.startsWith("{{"))
                        ? msgId
                        : ("msg91_" + System.currentTimeMillis()));

            WebhookTextMessageDTO txt = new WebhookTextMessageDTO(effectiveMessage);
            WebhookMessageDTO msg = new WebhookMessageDTO(
                    effectiveSender,
                    id,
                    timestamp,
                    rawType,
                    txt,
                    null,
                    buttonDto,
                    imageDto,
                    docDto,
                    audioDto,
                    videoDto,
                    extractedReplyMsgId
            );
            WebhookValueDTO val = new WebhookValueDTO("whatsapp", null, null, List.of(msg), null);
            WebhookChangeDTO change = new WebhookChangeDTO("messages", val);
            return List.of(new WebhookEntryDTO(effectiveSender, List.of(change)));
        }

        return Collections.emptyList();
    }

    private static String parseContactCard(Object contacts) {
        if (contacts == null) return null;
        String s = String.valueOf(contacts);
        if (s.isBlank() || s.startsWith("{{")) return null;

        String name = null;
        Matcher mName = Pattern.compile("\"formatted_name\"\\s*:\\s*\"([^\"]+)\"").matcher(s);
        if (mName.find()) {
            name = mName.group(1).trim();
        } else {
            Matcher mFirst = Pattern.compile("\"first_name\"\\s*:\\s*\"([^\"]+)\"").matcher(s);
            if (mFirst.find()) {
                name = mFirst.group(1).trim();
            }
        }

        String phone = null;
        Matcher mPhone = Pattern.compile("\"phone\"\\s*:\\s*\"([^\"]+)\"").matcher(s);
        if (mPhone.find()) {
            phone = mPhone.group(1).trim();
        } else {
            Matcher mWa = Pattern.compile("\"wa_id\"\\s*:\\s*\"([^\"]+)\"").matcher(s);
            if (mWa.find()) {
                phone = mWa.group(1).trim();
            }
        }

        if (name != null && phone != null) {
            return "👤 Contact: " + name + " (" + phone + ")";
        } else if (name != null) {
            return "👤 Contact: " + name;
        } else if (phone != null) {
            return "👤 Contact: " + phone;
        }
        return "👤 Shared Contact";
    }

    public static String resolvePublicMediaUrl(String rawUrl, String mediaId, String recipient) {
        if (rawUrl != null && rawUrl.contains("phone91.com")) {
            return rawUrl;
        }
        String effectiveMid = (mediaId != null && !mediaId.isBlank()) ? mediaId.trim() : null;
        if (effectiveMid == null && rawUrl != null) {
            Matcher m = Pattern.compile("[?&]mid=([0-9]+)").matcher(rawUrl);
            if (m.find()) {
                effectiveMid = m.group(1);
            }
        }
        if (effectiveMid != null && !effectiveMid.isBlank()) {
            String num = (recipient != null && !recipient.isBlank() && !recipient.startsWith("{{") && recipient.replaceAll("[^0-9]", "").length() >= 10)
                    ? recipient.replaceAll("[^0-9]", "")
                    : "919137300805";
            return "https://whatsapp.phone91.com/whatsapp-haptik-media/" + num + "/" + effectiveMid;
        }
        return rawUrl;
    }
}
