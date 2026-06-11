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
        Login loginService = new Login();
        LoginResponseDTO loginResponseDTO = loginService.login(loginRequestDTO);
        return Response.status(Response.Status.ACCEPTED).entity(loginResponseDTO).build();
    }
}
