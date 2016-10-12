package org.marsik.bugautomation.rest;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.marsik.bugautomation.services.StatsService;

@Path("/metrics")
public class MetricsEndpoint {
    @Inject
    private StatsService statsService;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response metrics() {
        return Response.ok(statsService.getStats().toPrometheusString())
                .build();
    }
}
