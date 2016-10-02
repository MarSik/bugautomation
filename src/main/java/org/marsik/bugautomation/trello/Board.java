package org.marsik.bugautomation.trello;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class Board extends BaseObject {
    String name;
    List<Card> cards;
    List<Member> members;
    List<TrelloList> lists;
    List<Label> labels;
}
