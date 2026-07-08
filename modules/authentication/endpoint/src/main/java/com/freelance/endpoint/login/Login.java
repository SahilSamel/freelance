package com.freelance.endpoint.login;

import com.freelance.login.LoginRequestDTO;
import com.freelance.login.LoginResponseDTO;
import com.freelance.rest.app.endpoint.AbstractEndpoint;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;


@Path("/auth")
public class Login extends AbstractEndpoint {

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(LoginRequestDTO loginRequestDTO){
        com.freelance.service.login.Login loginService = new com.freelance.service.login.Login();
        LoginResponseDTO loginResponseDTO = loginService.login(loginRequestDTO);
        return buildResponse(loginResponseDTO, 200);
    }
}