package org.marsik.bugautomation.trello;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class Member extends BaseObject {
    String fullName;
    String initials;
    String username;
}
