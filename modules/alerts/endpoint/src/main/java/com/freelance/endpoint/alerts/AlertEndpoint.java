package com.freelance.endpoint.alerts;

import com.freelance.alerts.NotificationReadService;
import com.freelance.alerts.dto.*;
import com.freelance.alerts.AlertEmitService;
import com.freelance.chat.ChatTimelineService;
import com.freelance.rest.app.endpoint.AbstractEndpoint;
import com.freelance.rest.app.endpoint.annotation.Authorization;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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
            description = "Sends a human staff reply to a user chat, clearing the human attention flag",
            group = "Alerts.Chat"
    )
    public Response sendChatReply(AdminChatReplyRequestDTO request) {
        ChatTimelineService timelineService = new ChatTimelineService();
        ChatMessageDTO response = timelineService.sendStaffReply(request);
        return buildResponse(response, 200);
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