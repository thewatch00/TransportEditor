package com.oskopek.transport.model.domain.action;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Wrapper around {@link Action} containing start and end times.
 */
public class TemporalPlanAction implements Comparable<TemporalPlanAction> {

    private final Action action;

    private final Double startTimestamp;
    private final Double endTimestamp;

    /**
     * Default constructor.
     *
     * @param action the action
     * @param startTimestamp the start time
     * @param endTimestamp the end time
     */
    public TemporalPlanAction(Action action, Double startTimestamp, Double endTimestamp) {
        this.action = action;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
    }

    /**
     * Updater method for the start time. Changes the end time appropriately using the action's duration.
     *
     * @param startTimestamp the new start time
     * @return the updated temporal action
     * @see Action#getDuration()
     */
    public TemporalPlanAction updateStartTimestampSmart(Double startTimestamp) {
        return new TemporalPlanAction(action, startTimestamp,
                startTimestamp + action.getDuration().getCost());
    }

    /**
     * Get the start time.
     *
     * @return the start time
     */
    public Double getStartTimestamp() {
        return startTimestamp;
    }

    /**
     * Get the end time.
     *
     * @return the end time
     */
    public Double getEndTimestamp() {
        return endTimestamp;
    }

    /**
     * Get the action.
     *
     * @return the action
     */
    public Action getAction() {
        return action;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(action)
                .append(startTimestamp)
                .append(endTimestamp)
                .toHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TemporalPlanAction)) {
            return false;
        }
        TemporalPlanAction that = (TemporalPlanAction) o;
        return new EqualsBuilder()
                .append(action, that.action)
                .append(startTimestamp, that.startTimestamp)
                .append(endTimestamp, that.endTimestamp)
                .isEquals();
    }

    @Override
    public int compareTo(TemporalPlanAction other) {
        return new CompareToBuilder().append(startTimestamp, other.startTimestamp)
                .append(endTimestamp, other.endTimestamp).toComparison();
    }

    @Override
    public String toString() {
        return "TemporalPlanAction{" + "action=" + action + ", startTimestamp=" + startTimestamp + ", endTimestamp="
                + endTimestamp + '}';
    }
}
