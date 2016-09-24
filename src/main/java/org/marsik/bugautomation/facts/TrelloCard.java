package org.marsik.bugautomation.facts;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;

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

    List<User> assignedTo;
    List<TrelloLabel> labels;

    Bug bug;
}
