package endpoint.Login;

import Login.LoginRequestDTO;
import Login.LoginResponseDTO;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import rest.AbstractRest;
import transaction.Transaction;


@Path("/auth")
public class Login extends AbstractRest {

    @POST
    @Path("/login")
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(LoginRequestDTO loginRequestDTO){
        Response response;

        try {
            Transaction.start();

            service.login.Login loginService = new service.login.Login();
            LoginResponseDTO loginResponseDTO = loginService.login(loginRequestDTO);

            response = buildResponse(loginResponseDTO, 200);

        } catch (Exception e) {
            response = buildResponse(e);
        } finally {
            try {
                Transaction.finish();
            } catch (Exception e) {
                response = buildResponse(e);
            }
        }

        return response;
    }
}