package com.freelance.endpoint.user;

import com.freelance.authorization.SystemRoles;
import com.freelance.rest.app.endpoint.AbstractEndpoint;
import com.freelance.rest.app.endpoint.annotation.Authorization;
import com.freelance.user.create.UserCreateRequestDTO;
import com.freelance.user.create.UserCreateResponseDTO;
import com.freelance.user.create.UserCreateService;
import com.freelance.user.delete.UserDeleteRequestDTO;
import com.freelance.user.delete.UserDeleteResponseDTO;
import com.freelance.user.delete.UserDeleteService;
import com.freelance.user.read.User;
import com.freelance.user.read.UserReadRequestDTO;
import com.freelance.user.read.UserReadResponseDTO;
import com.freelance.user.update.UserUpdateRequestDTO;
import com.freelance.user.update.UserUpdateResponseDTO;
import com.freelance.user.update.UserUpdateService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserEndpoint extends AbstractEndpoint {

    @POST
    @Path("/create")
    @Authorization(
            key = "USER_CREATE",
            name = "Create User",
            description = "Creates a new user account",
            group = "IdentityManagement",
            role = SystemRoles.SUPER_ADMIN
    )
    public Response createUser(UserCreateRequestDTO request) {
        UserCreateService createService = new UserCreateService();
        UserCreateResponseDTO response = createService.createUser(request);
        return buildResponse(response, 201);
    }

    @POST
    @Path("/read")
    @Authorization(
            key = "USER_READ",
            name = "Read User",
            description = "Retrieves user account details",
            group = "IdentityManagement",
            role = SystemRoles.SUPER_ADMIN
    )
    public Response readUser(UserReadRequestDTO request) {
        User readService = new User();
        UserReadResponseDTO response = readService.readUser(request);
        return buildResponse(response, 200);
    }

    @POST
    @Path("/update")
    @Authorization(
            key = "USER_UPDATE",
            name = "Update User",
            description = "Updates an existing user account",
            group = "IdentityManagement",
            role = SystemRoles.SUPER_ADMIN
    )
    public Response updateUser(UserUpdateRequestDTO request) {
        UserUpdateService updateService = new UserUpdateService();
        UserUpdateResponseDTO response = updateService.updateUser(request);
        return buildResponse(response, 200);
    }

    @POST
    @Path("/delete")
    @Authorization(
            key = "USER_DELETE",
            name = "Delete User",
            description = "Deletes an existing user account",
            group = "IdentityManagement",
            role = SystemRoles.SUPER_ADMIN
    )
    public Response deleteUser(UserDeleteRequestDTO request) {
        UserDeleteService deleteService = new UserDeleteService();
        UserDeleteResponseDTO response = deleteService.deleteUser(request);
        return buildResponse(response, 200);
    }
}
