package org.marsik.bugautomation.trello;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = "id")
public class BaseObject {
    String id;
}
