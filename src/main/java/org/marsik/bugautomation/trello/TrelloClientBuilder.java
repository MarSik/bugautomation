package org.marsik.bugautomation.trello;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

public class TrelloClientBuilder {
    private static final String TRELLO_BASE = "https://api.trello.com";

    String applicationKey;
    String token;
    Client restClient;

    public TrelloClientBuilder(String applicationKey, String token) {
        this.applicationKey = applicationKey;
        this.token = token;
    }

    public TrelloClient build() {
        restClient = ClientBuilder.newBuilder().build();
        ResteasyWebTarget target = (ResteasyWebTarget)restClient.target(TRELLO_BASE)
                .queryParam("key", applicationKey)
                .queryParam("token", token);
        return target.proxy(TrelloClient.class);
    }
}
