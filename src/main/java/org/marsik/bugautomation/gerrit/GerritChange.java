package org.marsik.bugautomation.gerrit;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class GerritChange extends GerritMore{
    String id;

    @SerializedName("_number")
    Integer number;

    String project;
    String branch;

    @SerializedName("change_id")
    String changeId;
    String subject;
    String status;
    GerritOwner owner;
    String topic;

    Instant created;
    Instant updated;
    Instant submitted;

    @SerializedName("unresolved_comment_count")
    Integer unresolvedCommentCount;
}
