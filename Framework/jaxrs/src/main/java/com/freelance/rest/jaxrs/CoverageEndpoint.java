package com.freelance.rest.jaxrs;

import com.freelance.rest.app.endpoint.annotation.Anonymous;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/testing/coverage")
@Anonymous
public class CoverageEndpoint {

    @POST
    @Path("/dump")
    @Produces(MediaType.APPLICATION_JSON)
    public Response dumpCoverage() {
        try {
            Class<?> rtClass = Class.forName("org.jacoco.agent.rt.RT");
            Object agent = rtClass.getMethod("getAgent").invoke(null);
            agent.getClass().getMethod("dump", boolean.class).invoke(agent, false);
            return Response.ok("{\"status\":\"dumped\"}").build();
        } catch (ClassNotFoundException e) {
            return Response.ok("{\"status\":\"jacoco_not_attached\"}").build();
        } catch (Exception e) {
            return Response.serverError().entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
}
