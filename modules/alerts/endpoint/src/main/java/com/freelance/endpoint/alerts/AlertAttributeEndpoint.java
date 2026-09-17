package com.freelance.endpoint.alerts;

import com.freelance.alerts.dto.WhitelistedAttributeDTO;
import com.freelance.alerts.dto.WhitelistedAttributeListResponseDTO;
import com.freelance.attribute.AttributeResolutionEngine;
import com.freelance.rest.app.endpoint.AbstractEndpoint;
import com.freelance.rest.app.endpoint.annotation.Authorization;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/alerts/attributes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AlertAttributeEndpoint extends AbstractEndpoint {

    @POST
    @Path("/whitelisted")
    @Authorization(
            key = "ALERT_ATTRIBUTE_LIST",
            name = "List Whitelisted Attributes",
            description = "Lists all available domain placeholder attributes for templates",
            group = "Alerts.Attributes"
    )
    public Response listWhitelistedAttributes() {
        List<WhitelistedAttributeDTO> list = AttributeResolutionEngine.getInstance().listWhitelistedAttributes();
        return buildResponse(new WhitelistedAttributeListResponseDTO(list), 200);
    }
}