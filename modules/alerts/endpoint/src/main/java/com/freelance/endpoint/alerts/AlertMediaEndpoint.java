package com.freelance.endpoint.alerts;

import com.freelance.alerts.dto.MediaDownloadResponseDTO;
import com.freelance.alerts.dto.MediaUploadRequestDTO;
import com.freelance.alerts.dto.MediaUploadResponseDTO;
import com.freelance.alerts.media.AlertMediaService;
import com.freelance.rest.app.endpoint.AbstractEndpoint;
import com.freelance.rest.app.endpoint.annotation.Anonymous;
import com.freelance.rest.app.endpoint.annotation.Authorization;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/alerts/media")
public class AlertMediaEndpoint extends AbstractEndpoint {

    private final AlertMediaService mediaService;

    public AlertMediaEndpoint() {
        this(new AlertMediaService());
    }

    public AlertMediaEndpoint(AlertMediaService mediaService) {
        this.mediaService = mediaService;
    }

    @POST
    @Path("/upload")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Authorization(
            key = "ALERT_CHAT_REPLY",
            name = "Upload Chat Media",
            description = "Uploads an image or document to local storage and WhatsApp Media API",
            group = "Alerts.Chat"
    )
    public Response uploadMedia(MediaUploadRequestDTO request) {
        MediaUploadResponseDTO response = mediaService.uploadMedia(request);
        return buildResponse(response, 200);
    }

    @GET
    @Path("/{mediaId}")
    @Anonymous
    public Response getMedia(@PathParam("mediaId") String mediaId) {
        MediaDownloadResponseDTO item = mediaService.getMedia(mediaId);
        return Response.ok(item.data(), item.mimeType())
                .header("Content-Disposition", "inline; filename=\"" + item.fileName() + "\"")
                .build();
    }
}
