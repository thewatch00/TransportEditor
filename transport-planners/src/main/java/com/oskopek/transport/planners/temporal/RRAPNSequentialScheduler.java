package com.oskopek.transport.planners.temporal;

import com.oskopek.transport.model.domain.Domain;
import com.oskopek.transport.model.plan.Plan;
import com.oskopek.transport.model.problem.Problem;
import com.oskopek.transport.planners.AbstractPlanner;
import com.oskopek.transport.planners.sequential.RandomizedRestartAroundPathNearbyPlanner;

import java.util.Optional;
import java.util.function.Function;

/**
 * Scheduler wrapper around {@link RandomizedRestartAroundPathNearbyPlanner}.
 */
public class RRAPNSequentialScheduler extends SequentialScheduler {

    private final AbstractPlanner planner = new RandomizedRestartAroundPathNearbyPlanner();

    @Override
    public Optional<Plan> plan(Domain seqDomain, Problem seqProblem,
            Function<Plan, Plan> planTransformation) {
        return planner.plan(seqDomain, seqProblem, planTransformation);
    }

    @Override
    protected AbstractPlanner getInternalPlanner() {
        return planner;
    }

    @Override
    public RRAPNSequentialScheduler copy() {
        return new RRAPNSequentialScheduler();
    }
}
