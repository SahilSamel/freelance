package com.freelance.authorization.endpoint;

import com.freelance.authorization.dto.RoleCreateRequestDTO;
import com.freelance.authorization.dto.RoleDTO;
import com.freelance.authorization.dto.RoleUpdateRequestDTO;
import com.freelance.authorization.service.RoleService;
import com.freelance.rest.app.endpoint.annotation.Authorization;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/roles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoleEndpoint {

    private final RoleService roleService;

    public RoleEndpoint() {
        this.roleService = new RoleService();
    }

}
