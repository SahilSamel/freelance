package com.freelance.endpoint.configuration;

import com.freelance.authorization.SystemRoles;
import com.freelance.configuration.dto.error.list.ErrorConfigurationListRequestDTO;
import com.freelance.configuration.dto.error.list.ErrorConfigurationListResponseDTO;
import com.freelance.configuration.dto.error.update.ErrorConfigurationUpdateRequestDTO;
import com.freelance.configuration.dto.error.update.ErrorConfigurationUpdateResponseDTO;
import com.freelance.configuration.error.ErrorConfigurationService;
import com.freelance.rest.app.endpoint.AbstractEndpoint;
import com.freelance.rest.app.endpoint.annotation.Authorization;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/configurations/error")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ErrorConfigurationEndpoint extends AbstractEndpoint {

    @POST
    @Path("/list")
    @Authorization(
            key = "ERROR_CONFIG_LIST",
            name = "List Error Configurations",
            description = "Retrieves all application error messages and status codes",
            group = "Configuration.Error",
            role = SystemRoles.SUPER_ADMIN
    )
    public Response listErrorConfigurations(ErrorConfigurationListRequestDTO request) {
        ErrorConfigurationService service = new ErrorConfigurationService();
        ErrorConfigurationListResponseDTO response = service.listErrorConfigurations(request);
        return buildResponse(response, 200);
    }

    @POST
    @Path("/update")
    @Authorization(
            key = "ERROR_CONFIG_UPDATE",
            name = "Update Error Configuration",
            description = "Updates an application error message and status code",
            group = "Configuration.Error",
            role = SystemRoles.SUPER_ADMIN
    )
    public Response updateErrorConfiguration(ErrorConfigurationUpdateRequestDTO request) {
        ErrorConfigurationService service = new ErrorConfigurationService();
        ErrorConfigurationUpdateResponseDTO response = service.updateErrorConfiguration(request);
        return buildResponse(response, 200);
    }
}
