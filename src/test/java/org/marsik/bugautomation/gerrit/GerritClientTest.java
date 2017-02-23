package org.marsik.bugautomation.gerrit;

import static org.junit.Assume.assumeTrue;

import java.util.List;

import org.junit.Test;

public class GerritClientTest {
    @Test
    public void getProjects() throws Exception {
        assumeTrue("true".equals(System.getProperty("runOnlineTests")));
        GerritClientBuilder builder = new GerritClientBuilder();
        builder.build().getProjects();
    }

    @Test
    public void getQueryChanges() throws Exception {
        assumeTrue("true".equals(System.getProperty("runOnlineTests")));
        GerritClientBuilder builder = new GerritClientBuilder();
        List<GerritChange> changes = builder.build().queryChanges("status:open owner:msivak@redhat.com", 2);
    }
}
