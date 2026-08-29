package com.freelance.endpoint.authorization;

import com.freelance.authorization.SystemRoles;
import com.freelance.authorization.dto.list.AuthorizationListRequestDTO;
import com.freelance.authorization.dto.list.AuthorizationListResponseDTO;
import com.freelance.authorization.service.AuthorizationService;
import com.freelance.rest.app.endpoint.AbstractEndpoint;
import com.freelance.rest.app.endpoint.annotation.Authorization;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/authorizations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthorizationEndpoint extends AbstractEndpoint {

    @POST
    @Path("/list")
    @Authorization(
            key = "AUTHORIZATION_LIST",
            name = "List Authorizations",
            description = "Retrieves all registered system authorizations and permissions",
            group = "Authorization.Management",
            role = SystemRoles.SUPER_ADMIN
    )
    public Response listAuthorizations(AuthorizationListRequestDTO request) {
        AuthorizationService service = new AuthorizationService();
        AuthorizationListResponseDTO response = service.listAuthorizations(request);
        return buildResponse(response, 200);
    }
}
