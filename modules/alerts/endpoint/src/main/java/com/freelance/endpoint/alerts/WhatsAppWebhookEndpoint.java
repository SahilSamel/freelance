package com.freelance.endpoint.alerts;

import com.freelance.ConfigurationEngine;
import com.freelance.alerts.AlertConsumer;
import com.freelance.alerts.dto.webhook.WhatsAppWebhookPayloadDTO;
import com.freelance.event.engine.EventManagementEngine;
import com.freelance.event.model.EventPriority;
import com.freelance.keys.alerts.webhook.WebhookConfigKeys;
import com.freelance.rest.app.endpoint.AbstractEndpoint;
import com.freelance.rest.app.endpoint.annotation.Anonymous;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/alerts/webhook/whatsapp")
public class WhatsAppWebhookEndpoint extends AbstractEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppWebhookEndpoint.class);

    @GET
    @Anonymous
    @Produces(MediaType.TEXT_PLAIN)
    public Response verifyWebhook(
            @QueryParam("hub.mode") String mode,
            @QueryParam("hub.verify_token") String token,
            @QueryParam("hub.challenge") String challenge
    ) {
        String expectedToken = ConfigurationEngine.get(WebhookConfigKeys.WHATSAPP_WEBHOOK_VERIFY_TOKEN);
        if ("subscribe".equals(mode) && expectedToken != null && expectedToken.equals(token)) {
            logger.info("[WhatsAppWebhookEndpoint] Meta WhatsApp webhook successfully verified.");
            return Response.ok(challenge).build();
        }
        return Response.status(Response.Status.FORBIDDEN).entity("Verification failed").build();
    }

    private static final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @POST
    @Anonymous
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response receiveWebhook(String rawPayload) {
        if (rawPayload != null && !rawPayload.isBlank()) {
                                                                     try {
                com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(rawPayload);
                if (root.isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode item : root) {
                        WhatsAppWebhookPayloadDTO payload = objectMapper.treeToValue(item, WhatsAppWebhookPayloadDTO.class);
                        if (payload != null) {
                            EventManagementEngine.emit(AlertConsumer.INBOUND_WHATSAPP, payload, EventPriority.HIGH);
                        }
                    }
                } else if (root.isObject()) {
                    WhatsAppWebhookPayloadDTO payload = objectMapper.treeToValue(root, WhatsAppWebhookPayloadDTO.class);
                    if (payload != null) {
                        EventManagementEngine.emit(AlertConsumer.INBOUND_WHATSAPP, payload, EventPriority.HIGH);
                    }
                }
            } catch (Exception e) {
                logger.error("[WhatsAppWebhookEndpoint] Failed to parse webhook payload: {}", rawPayload, e);
            }
        }
        return Response.ok("{\"status\":\"EVENT_RECEIVED\"}").build();
    }
}
