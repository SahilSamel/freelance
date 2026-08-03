package com.freelance.endpoint.auth;

import com.freelance.auth.dto.*;
import com.freelance.auth.service.Auth;
import com.freelance.rest.app.endpoint.annotation.Anonymous;
import com.freelance.rest.app.endpoint.AbstractEndpoint;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
public class AuthEndpoint extends AbstractEndpoint {

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Anonymous
    public Response login(AuthLoginRequestDTO request) {
        Auth authService = new Auth();
        AuthLoginResponseDTO response = authService.login(request);
        return buildResponse(response, 200);
    }

    @POST
    @Path("/logout")
    @Produces(MediaType.APPLICATION_JSON)
    public Response logout() {
        Auth authService = new Auth();
        authService.logout();
        return buildResponse(null, 200);
    }

    @POST
    @Path("/refresh")
    @Produces(MediaType.APPLICATION_JSON)
    public Response refresh() {
        Auth authService = new Auth();
        AuthRefreshResponseDTO response = authService.refreshSession();
        return buildResponse(response, 200);
    }
}
