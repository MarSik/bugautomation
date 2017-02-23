package org.marsik.bugautomation.gerrit;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GerritMore {
    @SerializedName("_more_changes")
    boolean moreChanges;
}
