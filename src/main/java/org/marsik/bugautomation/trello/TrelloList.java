package org.marsik.bugautomation.trello;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrelloList extends BaseObject{
    String name;
    String idBoard;
}
