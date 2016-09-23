package org.marsik.bugautomation.facts;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(of = "id")
public class TrelloBoard {
    String id;
    String name;
}
