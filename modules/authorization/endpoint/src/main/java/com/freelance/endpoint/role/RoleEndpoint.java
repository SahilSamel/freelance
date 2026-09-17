package com.freelance.endpoint.role;

import com.freelance.authorization.dto.role.create.RoleCreateRequestDTO;
import com.freelance.authorization.dto.role.create.RoleCreateResponseDTO;
import com.freelance.authorization.dto.role.delete.RoleDeleteRequestDTO;
import com.freelance.authorization.dto.role.delete.RoleDeleteResponseDTO;
import com.freelance.authorization.dto.role.list.RoleListRequestDTO;
import com.freelance.authorization.dto.role.list.RoleListResponseDTO;
import com.freelance.authorization.dto.role.read.RoleReadRequestDTO;
import com.freelance.authorization.dto.role.read.RoleReadResponseDTO;
import com.freelance.authorization.dto.role.update.RoleUpdateRequestDTO;
import com.freelance.authorization.dto.role.update.RoleUpdateResponseDTO;
import com.freelance.rest.app.endpoint.AbstractEndpoint;
import com.freelance.rest.app.endpoint.annotation.Authorization;
import com.freelance.role.create.RoleCreateService;
import com.freelance.role.delete.RoleDeleteService;
import com.freelance.role.read.RoleReadService;
import com.freelance.role.update.RoleUpdateService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/roles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoleEndpoint extends AbstractEndpoint {

    @POST
    @Path("/create")
    @Authorization(
            key = "ROLE_CREATE",
            name = "Create Role",
            description = "Creates a new custom role with assigned permissions",
            group = "Authorization.RoleManagement"
    )
    public Response createRole(RoleCreateRequestDTO request) {
        RoleCreateService service = new RoleCreateService();
        RoleCreateResponseDTO response = service.createRole(request);
        return buildResponse(response, 201);
    }

    @POST
    @Path("/read")
    @Authorization(
            key = "ROLE_READ",
            name = "Read Role",
            description = "Retrieves role details and permissions",
            group = "Authorization.RoleManagement"
    )
    public Response readRole(RoleReadRequestDTO request) {
        RoleReadService service = new RoleReadService();
        RoleReadResponseDTO response = service.readRole(request);
        return buildResponse(response, 200);
    }

    @POST
    @Path("/list")
    @Authorization(
            key = "ROLE_LIST",
            name = "List Roles",
            description = "Lists all system and custom roles with their permissions",
            group = "Authorization.RoleManagement"
    )
    public Response listRoles(RoleListRequestDTO request) {
        RoleReadService service = new RoleReadService();
        RoleListResponseDTO response = service.listRoles(request);
        return buildResponse(response, 200);
    }

    @POST
    @Path("/update")
    @Authorization(
            key = "ROLE_UPDATE",
            name = "Update Role",
            description = "Updates custom role details and permission mappings",
            group = "Authorization.RoleManagement"
    )
    public Response updateRole(RoleUpdateRequestDTO request) {
        RoleUpdateService service = new RoleUpdateService();
        RoleUpdateResponseDTO response = service.updateRole(request);
        return buildResponse(response, 200);
    }

    @POST
    @Path("/delete")
    @Authorization(
            key = "ROLE_DELETE",
            name = "Delete Role",
            description = "Deletes a custom role",
            group = "Authorization.RoleManagement"
    )
    public Response deleteRole(RoleDeleteRequestDTO request) {
        RoleDeleteService service = new RoleDeleteService();
        RoleDeleteResponseDTO response = service.deleteRole(request);
        return buildResponse(response, 200);
    }
}
