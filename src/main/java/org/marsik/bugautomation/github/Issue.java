package org.marsik.bugautomation.github;

import java.util.List;

import lombok.Data;

@Data
public class Issue {
    /**
     * GitHub global unique id
     */
    Integer id;
    /**
     * Repository local id
     */
    Integer number;
    String title;
    String body;
    User assignee;
    List<User> assignees;
    State state;
    String html_url;

    public enum State {
        open,
        closed
    }
}
