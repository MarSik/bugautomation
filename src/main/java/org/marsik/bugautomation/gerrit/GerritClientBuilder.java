package org.marsik.bugautomation.gerrit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

public class GerritClientBuilder {
    private static final String GERRIT_BASE = "https://gerrit.ovirt.org";

    Client restClient;

    public GerritClientBuilder() {
    }

    public GerritClient build() {
        restClient = ClientBuilder.newBuilder()
                .register(new GerritGsonMessageBodyHandler())
                .build();
        ResteasyWebTarget target = (ResteasyWebTarget)restClient.target(GERRIT_BASE);
        return target.proxy(GerritClient.class);
    }
}
