package com.freelance.endpoint.control;

import com.freelance.rest.app.endpoint.AbstractEndpoint;
import com.freelance.rest.app.endpoint.annotation.Authorization;
import com.freelance.tenant.control.TenantControlService;
import com.freelance.tenant.dto.*;
import com.freelance.tenant.onboarding.TenantOnboardingService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/control/tenants")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TenantControlEndpoint extends AbstractEndpoint {

    private final TenantOnboardingService onboardingService;
    private final TenantControlService controlService;

    public TenantControlEndpoint() {
        this(new TenantOnboardingService(), new TenantControlService());
    }

    public TenantControlEndpoint(TenantOnboardingService onboardingService, TenantControlService controlService) {
        this.onboardingService = onboardingService;
        this.controlService = controlService;
    }

    @POST
    @Authorization(
            key = "TENANT_CREATE",
            name = "Onboard Tenant",
            description = "Provisions a new tenant schema, runs migrations, seeds domain defaults, and issues Day-0 credentials",
            group = "Control.Tenant"
    )
    public Response onboardTenant(TenantCreateRequestDTO request) {
        TenantOnboardResponseDTO response = onboardingService.onboardTenant(request);
        return buildResponse(response, 201);
    }

    @GET
    @Authorization(
            key = "TENANT_LIST",
            name = "List Tenants",
            description = "Lists all registered tenants with their operational statuses",
            group = "Control.Tenant"
    )
    public Response listTenants() {
        TenantListResponseDTO response = controlService.listTenants();
        return buildResponse(response, 200);
    }

    @DELETE
    @Path("/{tenantId}")
    @Authorization(
            key = "TENANT_DELETE",
            name = "Deactivate Tenant",
            description = "Soft-deletes / deactivates a customer tenant, revoking access",
            group = "Control.Tenant"
    )
    public Response deactivateTenant(@PathParam("tenantId") String tenantId) {
        TenantResponseDTO response = controlService.deactivateTenant(tenantId);
        return buildResponse(response, 200);
    }

    @POST
    @Path("/{tenantId}/support-session")
    @Authorization(
            key = "SUPPORT_VENDOR_SESSION",
            name = "Mint Vendor Support Session",
            description = "Mints an ephemeral 1-hour vendor support token for an authorized customer tenant",
            group = "Control.Support"
    )
    public Response generateSupportSession(@PathParam("tenantId") String tenantId) {
        SupportSessionResponseDTO response = controlService.generateSupportSession(tenantId);
        return buildResponse(response, 200);
    }
}
