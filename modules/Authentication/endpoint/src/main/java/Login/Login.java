package Login;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@ApplicationPath("/auth")
public class Login {

    @POST
    @Path("/login")
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(LoginRequestDTO loginRequestDTO){
        // Keep HTTP concerns here; credential validation/token creation belongs in the login service.
        Login loginService = new Login();
        LoginResponseDTO loginResponseDTO = loginService.login(loginRequestDTO);
        // ACCEPTED signals that the login request was received and converted into a response payload.
        return Response.status(Response.Status.ACCEPTED).entity(loginResponseDTO).build();
    }
}
