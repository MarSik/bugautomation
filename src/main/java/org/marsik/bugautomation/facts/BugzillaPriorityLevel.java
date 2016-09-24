package org.marsik.bugautomation.facts;

public enum BugzillaPriorityLevel {
    UNSPECIFIED("?"),
    LOW("L"),
    MEDIUM("M"),
    HIGH("H"),
    URGENT("U");

    private final String symbol;

    BugzillaPriorityLevel(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
