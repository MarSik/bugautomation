package org.marsik.bugautomation.facts;

import javax.validation.constraints.NotNull;
import java.util.List;

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
    List<User> assignedTo;

    @NotNull
    List<TrelloLabel> labels;

    Bug bug;
}
