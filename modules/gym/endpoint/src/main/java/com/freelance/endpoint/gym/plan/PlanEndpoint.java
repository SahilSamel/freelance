package com.freelance.endpoint.gym.plan;

import com.freelance.gym.plan.PlanService;
import com.freelance.gym.plan.dto.PlanCreateRequestDTO;
import com.freelance.gym.plan.dto.PlanDeleteRequestDTO;
import com.freelance.gym.plan.dto.PlanListResponseDTO;
import com.freelance.gym.plan.dto.PlanResponseDTO;
import com.freelance.rest.app.endpoint.AbstractEndpoint;
import com.freelance.rest.app.endpoint.annotation.Authorization;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/plan")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PlanEndpoint extends AbstractEndpoint {

    private final PlanService planService;

    public PlanEndpoint() {
        this(new PlanService());
    }

    public PlanEndpoint(PlanService planService) {
        this.planService = planService;
    }

    @POST
    @Path("/new")
    @Authorization(
            key = "PLAN_CREATE",
            name = "Create Plan",
            description = "Creates a new gym plan with optional dependencies",
            group = "Gym.Plan"
    )
    public Response createPlan(PlanCreateRequestDTO request) {
        PlanResponseDTO response = planService.createPlan(request);
        return buildResponse(response, 201);
    }

    @POST
    @Path("/delete")
    @Authorization(
            key = "PLAN_DELETE",
            name = "Delete Plan",
            description = "Soft-deletes a gym plan",
            group = "Gym.Plan"
    )
    public Response deletePlan(PlanDeleteRequestDTO request) {
        PlanResponseDTO response = planService.deletePlan(request);
        return buildResponse(response, 200);
    }

    @GET
    @Path("/list")
    @Authorization(
            key = "PLAN_LIST",
            name = "List Plans",
            description = "Lists all gym plans and their dependency mappings",
            group = "Gym.Plan"
    )
    public Response listPlans() {
        PlanListResponseDTO response = planService.listPlans();
        return buildResponse(response, 200);
    }
}
