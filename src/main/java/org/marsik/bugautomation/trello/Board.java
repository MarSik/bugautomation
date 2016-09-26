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
public class Board extends BaseObject {
    String name;
    List<Card> cards;
    List<Member> members;
    List<TrelloList> lists;
}
