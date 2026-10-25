package com.freelance.endpoint.alerts;

import com.freelance.alerts.NotificationReadService;
import com.freelance.alerts.dto.*;
import com.freelance.alerts.AlertEmitService;
import com.freelance.chat.ChatTimelineService;
import com.freelance.rest.app.endpoint.AbstractEndpoint;
import com.freelance.rest.app.endpoint.annotation.Authorization;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import com.freelance.chat.TenantChatStreamHub;
import com.freelance.rest.app.transaction.Transaction;
import java.util.List;
import java.util.Map;

@Path("/alerts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AlertEndpoint extends AbstractEndpoint {

    @POST
    @Path("/emit")
    @Authorization(
            key = "ALERT_EMIT",
            name = "Emit Alert Batch",
            description = "Enqueues a broadcast alert notification for multiple target users",
            group = "Alerts.Management"
    )
    public Response emitAlerts(AlertEmitRequestDTO request) {
        AlertEmitService emitService = new AlertEmitService();
        emitService.emitBatchAlert(request);
        return buildResponse(null, 202);
    }

    @GET
    @Path("/chat/conversations")
    @Authorization(
            key = "ALERT_CHAT_READ",
            name = "Read Chat Conversations",
            description = "Retrieves all active chat conversations and their latest messages",
            group = "Alerts.Chat"
    )
    public Response getChatConversations() {
        ChatTimelineService timelineService = new ChatTimelineService();
        List<ChatTimelineResponseDTO> response = timelineService.getActiveConversations();
        return buildResponse(response, 200);
    }

    @POST
    @Path("/chat/timeline")
    @Authorization(
            key = "ALERT_CHAT_READ",
            name = "Read Chat Timeline",
            description = "Retrieves user chat conversation history and human attention status",
            group = "Alerts.Chat"
    )
    public Response getChatTimeline(ChatTimelineRequestDTO request) {
        ChatTimelineService timelineService = new ChatTimelineService();
        ChatTimelineResponseDTO response = timelineService.getTimeline(request);
        return buildResponse(response, 200);
    }

    @POST
    @Path("/chat/reply")
    @Authorization(
            key = "ALERT_CHAT_REPLY",
            name = "Staff Chat Reply",
            description = "Sends a human staff reply to a user chat",
            group = "Alerts.Chat"
    )
    public Response sendChatReply(AdminChatReplyRequestDTO request) {
        ChatTimelineService timelineService = new ChatTimelineService();
        ChatMessageDTO response = timelineService.sendStaffReply(request);
        return buildResponse(response, 200);
    }

    @POST
    @Path("/chat/read")
    @Authorization(
            key = "ALERT_CHAT_READ",
            name = "Mark Chat Read",
            description = "Marks all unread incoming messages in a conversation as read",
            group = "Alerts.Chat"
    )
    public Response markChatRead(ChatReadRequestDTO request) {
        ChatTimelineService timelineService = new ChatTimelineService();
        int count = timelineService.markConversationAsRead(request);
        return buildResponse(Map.of("markedReadCount", count), 200);
    }

    @GET
    @Path("/chat/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Authorization(
            key = "ALERT_CHAT_READ",
            name = "Stream Chat Events",
            description = "Real-time Server-Sent Events stream for tenant chats",
            group = "Alerts.Chat"
    )
    public void streamChatEvents(@Context SseEventSink eventSink, @Context Sse sse) {
        String tenantId = Transaction.getRequestContext().tenantId();
        TenantChatStreamHub.register(tenantId, eventSink, sse);
    }

    @POST
    @Path("/notifications/read")
    @Authorization(
            key = "ALERT_NOTIFICATION_READ",
            name = "Mark Notifications As Read",
            description = "Marks in-app push notifications as read for a user",
            group = "Alerts.Notification"
    )
    public Response markNotificationsRead(NotificationReadRequestDTO request) {
        NotificationReadService readService = new NotificationReadService();
        readService.markAsRead(request);
        return buildResponse(null, 202);
    }
}