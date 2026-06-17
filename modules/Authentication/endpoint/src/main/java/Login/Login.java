package Login;

import rest.AbstractRest;
import transaction.Transaction;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@ApplicationPath("/auth")
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