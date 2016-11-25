package com.oskopek.transporteditor.model.domain.actionbuilder;

import com.oskopek.transporteditor.model.domain.action.ActionCost;
import com.oskopek.transporteditor.model.domain.action.Refuel;
import com.oskopek.transporteditor.model.domain.action.predicates.Predicate;
import com.oskopek.transporteditor.model.problem.Location;
import com.oskopek.transporteditor.model.problem.PlaceholderActionObject;
import com.oskopek.transporteditor.model.problem.Vehicle;

import java.util.List;

public class RefuelBuilder extends DefaultActionBuilderWithCost<Refuel, Vehicle, PlaceholderActionObject> {

    public RefuelBuilder(List<Predicate> preconditions, List<Predicate> effects, ActionCost cost, ActionCost duration) {
        super(preconditions, effects, cost, duration);
    }

    @Override
    public <Who_ extends Vehicle, What_ extends PlaceholderActionObject> Refuel build(Who_ who, Location where,
            What_ what) {
        return build(who, where);
    }

    public <Who_ extends Vehicle> Refuel build(Who_ who, Location where) {
        return new Refuel(who, where, getPreconditions(), getEffects(), getCost(), getDuration());
    }
}
