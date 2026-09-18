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

    @POST
    @Anonymous
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response receiveWebhook(WhatsAppWebhookPayloadDTO payload) {
        if (payload != null) {
            EventManagementEngine.emit(AlertConsumer.INBOUND_WHATSAPP, payload, EventPriority.HIGH);
        }
        return Response.ok("{\"status\":\"EVENT_RECEIVED\"}").build();
    }
}
