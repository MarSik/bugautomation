package org.marsik.bugautomation.facts;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * This is the identifier that groups together cards and bugs.
 *
 * It is not supposed to be added to the fact database with one exception:
 * A Bug with custom created id (meaning the id does not come from bugzilla)
 * has to be added to prevent the cards from being automatically closed.
 */
@Value
@AllArgsConstructor
public class Bug {
    String id;
}
