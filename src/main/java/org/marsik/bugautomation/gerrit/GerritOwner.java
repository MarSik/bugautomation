package org.marsik.bugautomation.gerrit;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@XmlRootElement
@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GerritOwner extends GerritMore {
    String name;
    String email;
    String username;

    @SerializedName("_account_id")
    Integer id;
}
