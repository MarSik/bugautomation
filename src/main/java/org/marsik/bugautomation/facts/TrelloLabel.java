package org.marsik.bugautomation.facts;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@AllArgsConstructor
@Builder
public class TrelloLabel {
    String color;
    String name;
    TrelloCard card;
}
