package org.marsik.bugautomation.trello;

import java.util.List;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

public interface TrelloClient {
    @GET
    @Path("/1/boards/{id}")
    @Consumes("application/json")
    Board getBoard(@PathParam("id") String id);

    @GET
    @Path("/1/boards/{id}")
    @Consumes("application/json")
    Board getBoardWithData(@PathParam("id") String id,
            @QueryParam("lists") @DefaultValue("open") String lists,
            @QueryParam("cards") @DefaultValue("open") String cards,
            @QueryParam("members") @DefaultValue("all") String members);

    @POST
    @Path("/1/lists/{id}/cards")
    @Consumes("application/json")
    Card createCard(@PathParam("id") String listId, Map<String, Object> card);

    @PUT
    @Path("/1/cards/{id}")
    @Consumes("application/json")
    Card updateCard(@PathParam("id") String cardId, Map<String, Object> card);

    @GET
    @Path("/1/boards/{id}/lists")
    @Consumes("application/json")
    List<TrelloList> getListByBoard(@PathParam("id") String boardId);

    @POST
    @Path("/1/cards/{id}/idMembers")
    @Consumes("application/json")
    void assignCardToUser(@PathParam("id") String cardId, @FormParam("value") String userId);

    @PUT
    @Path("/1/cards/{id}/idList")
    @Consumes("application/json")
    void moveCard(@PathParam("id") String cardId, @FormParam("value") String listId);
}
