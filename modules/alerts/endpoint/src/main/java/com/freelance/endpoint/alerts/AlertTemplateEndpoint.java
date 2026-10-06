package com.freelance.endpoint.alerts;

import com.freelance.alerts.dto.*;
import com.freelance.template.TemplateService;
import com.freelance.rest.app.endpoint.AbstractEndpoint;
import com.freelance.rest.app.endpoint.annotation.Authorization;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;

@Path("/alerts/templates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AlertTemplateEndpoint extends AbstractEndpoint {

    @POST
    @Path("/create")
    @Authorization(
            key = "ALERT_TEMPLATE_CREATE",
            name = "Create Alert Template",
            description = "Creates a new custom alert template with buttons and required attributes",
            group = "Alerts.Templates"
    )
    public Response createTemplate(TemplateCreateRequestDTO request) {
        TemplateService service = new TemplateService();
        TemplateDTO response = service.createTemplate(request);
        return buildResponse(response, 201);
    }

    @POST
    @Path("/list")
    @Authorization(
            key = "ALERT_TEMPLATE_LIST",
            name = "List Alert Templates",
            description = "Lists all registered alert templates",
            group = "Alerts.Templates"
    )
    public Response listTemplates(TemplateListRequestDTO request) {
        TemplateService service = new TemplateService();
        TemplateListResponseDTO response = service.listTemplates(request);
        return buildResponse(response, 200);
    }

    @POST
    @Path("/read")
    @Authorization(
            key = "ALERT_TEMPLATE_READ",
            name = "Read Alert Template",
            description = "Retrieves an alert template by ID",
            group = "Alerts.Templates"
    )
    public Response readTemplate(TemplateReadRequestDTO request) {
        TemplateService service = new TemplateService();
        TemplateDTO response = service.getTemplate(request != null ? request.id() : null);
        return buildResponse(response, 200);
    }

    @POST
    @Path("/update")
    @Authorization(
            key = "ALERT_TEMPLATE_UPDATE",
            name = "Update Alert Template",
            description = "Updates an existing alert template with metadata, content, and buttons",
            group = "Alerts.Templates"
    )
    public Response updateTemplate(TemplateUpdateRequestDTO request) {
        TemplateService service = new TemplateService();
        TemplateDTO response = service.updateTemplate(request);
        return buildResponse(response, 200);
    }

    @POST
    @Path("/categories")
    @Authorization(
            key = "ALERT_TEMPLATE_CATEGORIES",
            name = "List Template Categories",
            description = "Lists all registered alert template categories",
            group = "Alerts.Templates"
    )
    public Response listCategories() {
        TemplateService service = new TemplateService();
        TemplateCategoryListResponseDTO response = service.listCategories();
        return buildResponse(response, 200);
    }

    @POST
    @Path("/{id}/submit")
    @Authorization(
            key = "ALERT_TEMPLATE_SUBMIT",
            name = "Submit Alert Template",
            description = "Submits a draft or rejected alert template to Meta Graph API for approval",
            group = "Alerts.Templates"
    )
    public Response submitTemplate(@PathParam("id") UUID id) {
        TemplateService service = new TemplateService();
        TemplateSubmitResponseDTO response = service.submitTemplateForApproval(id);
        return buildResponse(response, 200);
    }

    @POST
    @Path("/submit-all")
    @Authorization(
            key = "ALERT_TEMPLATE_SUBMIT_ALL",
            name = "Submit All Alert Templates",
            description = "Submits all unapproved alert templates to Meta Graph API for approval",
            group = "Alerts.Templates"
    )
    public Response submitAllTemplates() {
        TemplateService service = new TemplateService();
        TemplateBatchSubmitResponseDTO response = service.submitAllPendingTemplates();
        return buildResponse(response, 200);
    }
}