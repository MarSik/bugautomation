package org.marsik.bugautomation.rest;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.marsik.bugautomation.services.RuleGlobalsService;

@Path("/bug")
public class InfoEndpoint {
    @Inject
    RuleGlobalsService factService;

    @Path("/{bugId}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getBugInfo(@PathParam("bugId") String bugId) {
        return Response.ok(factService.getBugInfo(bugId)).build();
    }
}
