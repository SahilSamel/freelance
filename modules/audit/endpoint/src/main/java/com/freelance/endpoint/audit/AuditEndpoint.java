package com.freelance.endpoint.audit;

import com.freelance.audit.dto.list.AuditLogListRequestDTO;
import com.freelance.audit.dto.list.AuditLogListResponseDTO;
import com.freelance.audit.service.AuditService;
import com.freelance.rest.app.endpoint.AbstractEndpoint;
import com.freelance.rest.app.endpoint.annotation.Authorization;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/audit")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuditEndpoint extends AbstractEndpoint {

    @POST
    @Path("/list")
    @Authorization(
            key = "AUDIT_LOG_LIST",
            name = "List Audit Logs",
            description = "Lists HTTP and system audit logs with optional filters",
            group = "Audit.Management"
    )
    public Response listAuditLogs(AuditLogListRequestDTO request) {
        AuditService auditService = new AuditService();
        AuditLogListResponseDTO response = auditService.listAuditLogs(request);
        return buildResponse(response, 200);
    }
}
