package org.marsik.bugautomation.gerrit;

import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

public interface GerritClient {
    @GET
    @Path("/projects/")
    @Consumes("application/json")
    Map<String, Map<String,String>> getProjects();

    @GET
    @Path("/changes/")
    @Consumes("application/json")
    List<GerritChange> queryChanges(@QueryParam("q") String query, @QueryParam("n") int limit);

    @GET
    @Path("/accounts/{accountId}")
    @Consumes("application/json")
    GerritOwner getAccount(@PathParam("accountId") Integer id);
}
