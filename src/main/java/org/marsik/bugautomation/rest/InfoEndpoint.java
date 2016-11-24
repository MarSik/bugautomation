package org.marsik.bugautomation.rest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.marsik.bugautomation.services.RuleGlobalsService;

@Path("/bug")
public class InfoEndpoint {
    @Inject
    RuleGlobalsService factService;

    @Path("/{bugId:[a-zA-Z0-9-]+}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getBugInfo(@PathParam("bugId") String bugId) {
        return Response.ok(factService.getBugInfo(bugId)).build();
    }

    @Path("/index.html")
    @Produces(MediaType.TEXT_HTML)
    @GET
    public Response getFrontPage() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        String content = IOUtils.toString(classLoader.getResourceAsStream("html/bug.html"));
        return Response.ok(content).build();
    }

    @Path("")
    @Produces(MediaType.TEXT_HTML)
    @GET
    public Response redirectToFrontPage() throws IOException, URISyntaxException {
        return Response.temporaryRedirect(new URI("/bug/index.html")).build();
    }
}
