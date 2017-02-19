package com.oskopek.transporteditor.model.domain.action;

import com.oskopek.transporteditor.model.domain.Domain;
import com.oskopek.transporteditor.model.domain.action.predicates.Predicate;
import com.oskopek.transporteditor.model.problem.Location;
import com.oskopek.transporteditor.model.problem.Package;
import com.oskopek.transporteditor.model.problem.Problem;
import com.oskopek.transporteditor.model.problem.Vehicle;

import java.util.List;

/**
 * The pick-up action. Semantics: Vehicle (who) picks up package (what) at location (where).
 */
public class PickUp extends DefaultAction<Vehicle, Package> {

    /**
     * Default constructor.
     *
     * @param vehicle the vehicle
     * @param location where to pick up
     * @param aPackage the package to pick up
     * @param preconditions applicable preconditions
     * @param effects applicable effects
     * @param cost cost of the action
     * @param duration duration of the action
     */
    public PickUp(Vehicle vehicle, Location location, Package aPackage, List<Predicate> preconditions,
            List<Predicate> effects, ActionCost cost, ActionCost duration) {
        super("pick-up", vehicle, location, aPackage, preconditions, effects, cost, duration);
    }

    @Override
    public Problem apply(Domain domain, Problem problemState) {
        String vehicleName = this.getWho().getName();
        String packageName = this.getWhat().getName();
        Vehicle vehicle = problemState.getVehicle(vehicleName);
        Package pkg = problemState.getPackage(packageName);
        Package newPackage = pkg.updateLocation(null);
        return problemState.putVehicle(vehicleName, vehicle.addPackage(newPackage))
                .putPackage(packageName, newPackage);
    }
}
