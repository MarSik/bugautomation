package org.marsik.bugautomation.facts;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Data
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = {"board", "color"})
public class TrelloLabel {
    TrelloBoard board;
    String color;

    String id;
    String name;
}
