package org.marsik.bugautomation.trello;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import lombok.Data;
import lombok.EqualsAndHashCode;

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

    private List<Label> labels;
}
