package com.oskopek.transport.model.domain.action.predicates;

import com.oskopek.transport.model.domain.action.Action;
import com.oskopek.transport.model.domain.action.Drive;
import com.oskopek.transport.model.problem.*;

/**
 * Assert that a vehicle (who) has a current fuel capacity (current fuel level) to drive to the given location (what).
 * Only validated in a fuel domain.
 * @see Drive#isFuelDomain()
 */
public class HasFuelCapacityForDrive extends DefaultPredicate {

    @Override
    public boolean isValidInternal(Problem state, Action action) {
        if (action instanceof Drive) {
            Drive drive = (Drive) action;
            if (drive.isFuelDomain()) {
                Vehicle vehicle = state.getVehicle(action.getWho().getName());
                FuelRoad road = (FuelRoad) drive.getRoad();
                return vehicle.getCurFuelCapacity().subtract(road.getFuelCost()).getCost() >= 0;
            }
        }
        return true;
    }
}
