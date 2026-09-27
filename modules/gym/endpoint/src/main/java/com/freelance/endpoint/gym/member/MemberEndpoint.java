package com.freelance.endpoint.gym.member;

import com.freelance.gym.member.MemberService;
import com.freelance.gym.member.dto.*;
import com.freelance.rest.app.endpoint.AbstractEndpoint;
import com.freelance.rest.app.endpoint.annotation.Authorization;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/members")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MemberEndpoint extends AbstractEndpoint {

    private final MemberService memberService;

    public MemberEndpoint() {
        this(new MemberService());
    }

    public MemberEndpoint(MemberService memberService) {
        this.memberService = memberService;
    }

    @POST
    @Path("/new")
    @Authorization(
            key = "MEMBER_CREATE",
            name = "Create Member",
            description = "Creates a prospect or active gym member with subscriptions and payment schedules",
            group = "Gym.Member"
    )
    public Response createMember(MemberCreateRequestDTO request) {
        MemberCreateResponseDTO response = memberService.createMember(request);
        return buildResponse(response, 201);
    }

    @GET
    @Path("/list")
    @Authorization(
            key = "MEMBER_LIST",
            name = "List Members",
            description = "Lists all members with complete relational table data as-is",
            group = "Gym.Member"
    )
    public Response listMembers() {
        MemberListResponseDTO response = memberService.listMembers();
        return buildResponse(response, 200);
    }

    @POST
    @Path("/renew")
    @Authorization(
            key = "MEMBER_RENEW",
            name = "Renew Member",
            description = "Appends renewed plan periods and installment schedules without modifying past history",
            group = "Gym.Member"
    )
    public Response renewMember(MemberRenewRequestDTO request) {
        MemberRenewResponseDTO response = memberService.renewMember(request);
        return buildResponse(response, 200);
    }

    @POST
    @Path("/update")
    @Authorization(
            key = "MEMBER_UPDATE",
            name = "Update Member",
            description = "Reconciles member state via delete-insert for active plans and pending installments, locking paid records",
            group = "Gym.Member"
    )
    public Response updateMember(MemberUpdateRequestDTO request) {
        MemberUpdateResponseDTO response = memberService.updateMember(request);
        return buildResponse(response, 200);
    }
}
