package com.oskopek.transporteditor.model.state;

import com.oskopek.transporteditor.model.domain.action.Action;
import com.oskopek.transporteditor.model.problem.Problem;

/**
 * Simple interface for mutating a problem's state by applying actions on top of it.
 */
public interface PlanState extends Problem {

    /**
     * Changes the internal state of the problem by applying the specified action.
     *
     * @param action the action to apply
     */
    default void apply(Action action) {
        applyPreconditions(action);
        applyEffects(action);
    }

    /**
     * Changes the internal state of the problem by applying effects specified as "at start".
     *
     * @param action the action whose parts to apply
     */
    void applyPreconditions(Action action);

    /**
     * Changes the internal state of the problem by applying effects specified as "at end".
     *
     * @param action the action whose parts to apply
     */
    void applyEffects(Action action);

    default boolean isGoalState() {
        return getAllPackages().stream().allMatch(p -> p.getTarget().equals(p.getLocation()));
    }

}
