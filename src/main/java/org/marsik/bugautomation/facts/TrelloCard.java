package org.marsik.bugautomation.facts;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(of = "id")
public class TrelloCard {
    String id;

    @NotNull
    TrelloBoard board;

    String status;
    Double pos;

    String title;
    String description;

    @NotNull
    Set<User> assignedTo;

    @NotNull
    Set<TrelloLabel> labels;

    Bug bug;
    Set<Bug> blocks;

    /**
     * This field holds the importance of the card. Higher numbers are more important.
     *
     * This field can be set from DRL rules.
     */
    Integer score;

    Map<String, String> fields;

    public String getCleanDesc() {
        return description.replaceAll("\\{\\{[^}]*\\}\\}", " ");
    }
}
