package org.marsik.bugautomation.facts;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.ext.JodaDeserializers;
import org.codehaus.jackson.map.ext.JodaSerializers;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(of = "id")
public class TrelloCard {
    String id;

    @NotNull
    TrelloBoard board;
    boolean closed;

    @NotNull
    String status;
    @NotNull
    Double pos;

    String title;
    String description;

    @NotNull
    Set<User> assignedTo;

    @NotNull
    Set<TrelloLabel> labels;

    Bug bug;
    Set<Bug> blocks;

    /**
     * This field holds the importance of the card. Higher numbers are more important.
     *
     * This field can be set from DRL rules.
     */
    Integer score;

    /**
     * Describes the target milestone. Used for non-bug related cards.
     */
    String targetMilestone;

    Instant dueDate;

    Map<String, String> fields;

    public String getCleanDesc() {
        return description.replaceAll("\\{\\{[^}]*\\}\\}", " ");
    }

    public boolean isTargeted() {
        return (targetMilestone != null && !targetMilestone.isEmpty())
                || dueDate != null;
    }

    public boolean isUntargeted() {
        return !isTargeted();
    }

    public boolean above(TrelloCard other) {
        return pos < other.pos;
    }

    public boolean below(TrelloCard other) {
        return pos > other.pos;
    }

    public boolean lessImportant(TrelloCard other) {
        return score != null && other.score != null && score < other.score;
    }

    public boolean moreImportant(TrelloCard other) {
        return score != null && other.score != null && score > other.score;
    }

    public boolean sameScore(TrelloCard other) {
        return score != null && other.score != null && Objects.equals(score, other.score);
    }
}
