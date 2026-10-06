package com.freelance.endpoint.auth;

import com.freelance.auth.dto.*;
import com.freelance.auth.service.Authentication;
import com.freelance.rest.app.endpoint.annotation.Anonymous;
import com.freelance.rest.app.endpoint.annotation.Authenticated;
import com.freelance.rest.app.endpoint.AbstractEndpoint;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
public class AuthenticationEndpoint extends AbstractEndpoint {

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Anonymous
    public Response login(AuthLoginRequestDTO request) {
        Authentication authenticationService = new Authentication();
        AuthLoginResponseDTO response = authenticationService.login(request);
        return buildResponse(response, 200);
    }

    @POST
    @Path("/logout")
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    public Response logout() {
        Authentication authenticationService = new Authentication();
        authenticationService.logout();
        return buildResponse(null, 200);
    }

    @POST
    @Path("/refresh")
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    public Response refresh() {
        Authentication authenticationService = new Authentication();
        AuthRefreshResponseDTO response = authenticationService.refreshSession();
        return buildResponse(response, 200);
    }

    @POST
    @Path("/me")
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    public Response me() {
        Authentication authenticationService = new Authentication();
        AuthMeResponseDTO response = authenticationService.me();
        return buildResponse(response, 200);
    }

    @GET
    @Path("/me")
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    public Response getMe() {
        return me();
    }
}