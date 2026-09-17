package com.freelance.endpoint.configuration;

import com.freelance.configuration.dto.system.list.SystemConfigurationListRequestDTO;
import com.freelance.configuration.dto.system.list.SystemConfigurationListResponseDTO;
import com.freelance.configuration.dto.system.update.SystemConfigurationUpdateRequestDTO;
import com.freelance.configuration.dto.system.update.SystemConfigurationUpdateResponseDTO;
import com.freelance.configuration.system.SystemConfigurationService;
import com.freelance.rest.app.endpoint.AbstractEndpoint;
import com.freelance.rest.app.endpoint.annotation.Authorization;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/configurations/system")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SystemConfigurationEndpoint extends AbstractEndpoint {

    @POST
    @Path("/list")
    @Authorization(
            key = "SYSTEM_CONFIG_LIST",
            name = "List System Configurations",
            description = "Retrieves all application system configuration properties",
            group = "Configuration.System"
    )
    public Response listConfigurations(SystemConfigurationListRequestDTO request) {
        SystemConfigurationService service = new SystemConfigurationService();
        SystemConfigurationListResponseDTO response = service.listConfigurations(request);
        return buildResponse(response, 200);
    }

    @POST
    @Path("/update")
    @Authorization(
            key = "SYSTEM_CONFIG_UPDATE",
            name = "Update System Configuration",
            description = "Updates an application system configuration value",
            group = "Configuration.System"
    )
    public Response updateConfiguration(SystemConfigurationUpdateRequestDTO request) {
        SystemConfigurationService service = new SystemConfigurationService();
        SystemConfigurationUpdateResponseDTO response = service.updateConfiguration(request);
        return buildResponse(response, 200);
    }
}
