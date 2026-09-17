package com.freelance.endpoint.support;

import com.freelance.auth.dto.*;
import com.freelance.auth.support.SupportAccessService;
import com.freelance.rest.app.endpoint.AbstractEndpoint;
import com.freelance.rest.app.endpoint.annotation.Anonymous;
import com.freelance.rest.app.endpoint.annotation.Authorization;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/support")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SupportEndpoint extends AbstractEndpoint {

    @POST
    @Path("/request-access")
    @Anonymous
    public Response requestAccess(SupportRequestDTO request) {
        SupportAccessService service = new SupportAccessService();
        SupportRequestResponseDTO response = service.requestAccess(request);
        return buildResponse(response, 200);
    }

    @POST
    @Path("/status")
    @Authorization(
            key = "SUPPORT_ACCESS_MANAGE",
            name = "Manage Support Access",
            description = "Checks tech support window and pending access requests",
            group = "Support.Management"
    )
    public Response getStatus() {
        SupportAccessService service = new SupportAccessService();
        SupportStatusResponseDTO response = service.getStatus();
        return buildResponse(response, 200);
    }

    @POST
    @Path("/grant-access")
    @Authorization(
            key = "SUPPORT_ACCESS_MANAGE",
            name = "Manage Support Access",
            description = "Grants time-boxed support access window to tech support",
            group = "Support.Management"
    )
    public Response grantAccess(SupportGrantRequestDTO request) {
        SupportAccessService service = new SupportAccessService();
        SupportGrantResponseDTO response = service.grantAccess(request);
        return buildResponse(response, 200);
    }

    @POST
    @Path("/revoke-access")
    @Authorization(
            key = "SUPPORT_ACCESS_MANAGE",
            name = "Manage Support Access",
            description = "Instantly revokes tech support access window",
            group = "Support.Management"
    )
    public Response revokeAccess() {
        SupportAccessService service = new SupportAccessService();
        SupportRevokeResponseDTO response = service.revokeAccess();
        return buildResponse(response, 200);
    }
}
