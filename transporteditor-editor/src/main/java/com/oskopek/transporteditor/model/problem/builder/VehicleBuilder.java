package com.oskopek.transporteditor.model.problem.builder;

import com.oskopek.transporteditor.model.domain.action.ActionCost;
import com.oskopek.transporteditor.model.problem.Package;
import com.oskopek.transporteditor.model.problem.Vehicle;

import java.util.List;

/**
 * Action object builder for {@link Vehicle}s.
 */
public class VehicleBuilder extends LocatableBuilder<Vehicle> {

    private ActionCost curCapacity;
    private ActionCost maxCapacity;
    private ActionCost curFuelCapacity;
    private ActionCost maxFuelCapacity;
    private List<Package> packageList;

    /**
     * Default constructor.
     */
    public VehicleBuilder() {
        // intentionally empty
    }

    /**
     * Get the current capacity.
     *
     * @return the current capacity
     */
    @FieldLocalization(key = "vehicle.curcapacity", priority = 3, editable = false)
    public ActionCost getCurCapacity() { // TODO: Set automatically maxFuelCapacity - packageSizeSum
        return curCapacity;
    }

    /**
     * Set the current capacity.
     *
     * @param curCapacity the current capacity to set
     */
    public void setCurCapacity(ActionCost curCapacity) {
        this.curCapacity = curCapacity;
    }

    /**
     * Get the maximum capacity.
     *
     * @return the maximum capacity
     */
    @FieldLocalization(key = "vehicle.maxcapacity", priority = 3)
    public ActionCost getMaxCapacity() {
        return maxCapacity;
    }

    /**
     * Set the maximum capacity.
     *
     * @param maxCapacity the maximum capacity to set
     */
    public void setMaxCapacity(ActionCost maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    /**
     * Get the current fuel capacity.
     *
     * @return the current fuel capacity
     */
    @FieldLocalization(key = "vehicle.curfuelcapacity", priority = 4) // TODO: Validate to be max maxFuelCapacity
    public ActionCost getCurFuelCapacity() {
        return curFuelCapacity;
    }

    /**
     * Set the current fuel capacity.
     *
     * @param curFuelCapacity the current fuel capacity to set
     */
    public void setCurFuelCapacity(ActionCost curFuelCapacity) {
        this.curFuelCapacity = curFuelCapacity;
    }

    /**
     * Get the maximum fuel capacity.
     *
     * @return the maximum fuel capacity
     */
    @FieldLocalization(key = "vehicle.maxfuelcapacity", priority = 4)
    public ActionCost getMaxFuelCapacity() {
        return maxFuelCapacity;
    }

    /**
     * Set the maximum fuel capacity.
     *
     * @param maxFuelCapacity the maximum fuel capacity to set
     */
    public void setMaxFuelCapacity(ActionCost maxFuelCapacity) {
        this.maxFuelCapacity = maxFuelCapacity;
    }

    /**
     * Get the package list.
     *
     * @return the package list
     */
    @FieldLocalization(key = "vehicle.packagelist", editable = false)
    public List<Package> getPackageList() {
        return packageList;
    }

    /**
     * Set the package list.
     *
     * @param packageList the package list to set
     */
    public void setPackageList(List<Package> packageList) {
        this.packageList = packageList;
    }

    @Override
    public Vehicle build() {
        return new Vehicle(getName(), getLocation(), getCurCapacity(), getMaxCapacity(), getCurFuelCapacity(),
                getMaxFuelCapacity(), getPackageList());
    }

    @Override
    public void from(Vehicle instance) {
        super.from(instance);
        setCurCapacity(instance.getCurCapacity());
        setMaxCapacity(instance.getMaxCapacity());
        setCurFuelCapacity(instance.getCurFuelCapacity());
        setMaxFuelCapacity(instance.getMaxFuelCapacity());
        setPackageList(instance.getPackageList());
    }
}