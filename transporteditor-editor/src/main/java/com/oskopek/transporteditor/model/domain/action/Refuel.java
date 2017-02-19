package com.oskopek.transporteditor.model.domain.action;

import com.oskopek.transporteditor.model.domain.Domain;
import com.oskopek.transporteditor.model.domain.action.predicates.Predicate;
import com.oskopek.transporteditor.model.problem.Location;
import com.oskopek.transporteditor.model.problem.PlaceholderActionObject;
import com.oskopek.transporteditor.model.problem.Problem;
import com.oskopek.transporteditor.model.problem.Vehicle;

import java.util.List;

/**
 * The refuel action. Doesn't use the what argument. Semantics: Refuel vehicle (who) at location (where) to the maximum.
 */
public class Refuel extends DefaultAction<Vehicle, PlaceholderActionObject> {

    /**
     * Default constructor.
     *
     * @param vehicle the vehicle to refuel
     * @param location where to refuel
     * @param preconditions applicable preconditions
     * @param effects applicable effects
     * @param cost cost of the action
     * @param duration duration of the action
     */
    public Refuel(Vehicle vehicle, Location location, List<Predicate> preconditions, List<Predicate> effects,
            ActionCost cost, ActionCost duration) {
        super("refuel", vehicle, location, new PlaceholderActionObject(), preconditions, effects, cost, duration);
    }

    @Override
    public Problem apply(Domain domain, Problem problemState) {
        String name = this.getWho().getName();
        Vehicle vehicle = problemState.getVehicle(name);
        return problemState.putVehicle(name, vehicle.updateCurFuelCapacity(vehicle.getMaxFuelCapacity()));
    }
}
