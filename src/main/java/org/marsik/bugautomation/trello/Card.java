package org.marsik.bugautomation.trello;

import javax.xml.bind.annotation.XmlRootElement;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class Card extends BaseObject {
    String name;
    String desc;

    private String idList;
    private String idBoard;
    private List<String> idMembers;

    private double pos;

    // Due date, we parse it later
    private String due;

    private List<Label> labels;

    private Boolean closed;

    String dateLastActivity;
}
