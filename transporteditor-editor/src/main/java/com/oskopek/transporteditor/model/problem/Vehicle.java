package com.oskopek.transporteditor.model.problem;

import com.oskopek.transporteditor.model.domain.action.ActionCost;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a vehicle in the Transport domain's problem instance. Universal for all domain variants.
 */
public class Vehicle extends DefaultLocatable implements Locatable, ActionObject {

    private final ActionCost curCapacity;
    private final ActionCost maxCapacity;
    private final ActionCost curFuelCapacity;
    private final ActionCost maxFuelCapacity;
    private final List<Package> packageList;
    private final boolean readyLoading;

    /**
     * Default constructor for a fuel disabled domain.
     *
     * @param name the name
     * @param location the starting location of the vehicle
     * @param curCapacity the current package capacity (the sum of package sizes that fit into the vehicle)
     * @param maxCapacity the maximum package capacity (the sum of package sizes that fit into the vehicle
     * if it is empty before)
     * @param readyLoading the ready-laoding state
     * @param packageList a list of packages loaded into the vehicle
     */
    public Vehicle(String name, Location location, ActionCost curCapacity, ActionCost maxCapacity, boolean readyLoading,
            List<Package> packageList) {
        this(name, location, curCapacity, maxCapacity, null, null, readyLoading, packageList);
    }

    /**
     * Default constructor for a fuel enabled domain.
     *
     * @param name the name
     * @param location the current location of the vehicle
     * @param curCapacity the current package capacity (the sum of package sizes that fit into the vehicle)
     * @param maxCapacity the maximum package capacity (the sum of package sizes that fit into the vehicle
     * if it is empty before)
     * @param curFuelCapacity the current fuel capacity (the sum of road fuel costs that the vehicle can drive
     * from now)
     * @param maxFuelCapacity the maximum fuel capacity (the sum of road fuel costs that the vehicle can drive
     * from a full tank)
     * @param readyLoading the ready-laoding state
     * @param packageList a list of packages loaded into the vehicle
     */
    public Vehicle(String name, Location location, ActionCost curCapacity, ActionCost maxCapacity,
            ActionCost curFuelCapacity, ActionCost maxFuelCapacity, boolean readyLoading,
            List<Package> packageList) {
        super(name, location);
        if (packageList == null) {
            throw new IllegalArgumentException("Package list cannot be null.");
        }
        this.curCapacity = curCapacity;
        this.maxCapacity = maxCapacity;
        this.curFuelCapacity = curFuelCapacity;
        this.maxFuelCapacity = maxFuelCapacity;
        this.readyLoading = readyLoading;
        this.packageList = packageList;
    }

    /**
     * Get the current package capacity.
     *
     * @return the current package capacity
     */
    public ActionCost getCurCapacity() {
        return curCapacity;
    }

    /**
     * Get the maximum package capacity.
     *
     * @return the maximum package capacity
     */
    public ActionCost getMaxCapacity() {
        return maxCapacity;
    }

    /**
     * Get the current fuel capacity.
     *
     * @return the current fuel capacity
     */
    public ActionCost getCurFuelCapacity() {
        return curFuelCapacity;
    }

    /**
     * Get the maximum fuel capacity.
     *
     * @return the maximum fuel capacity
     */
    public ActionCost getMaxFuelCapacity() {
        return maxFuelCapacity;
    }

    /**
     * Get whether is in ready-loading state.
     *
     * @return the ready-loading state
     */
    public boolean isReadyLoading() {
        return readyLoading;
    }

    /**
     * Get the package list.
     *
     * @return the package list
     */
    public List<Package> getPackageList() {
        return packageList;
    }

    @Override
    public Vehicle updateName(String newName) {
        return new Vehicle(newName, getLocation(), getCurCapacity(), getMaxCapacity(), getCurFuelCapacity(),
                getMaxFuelCapacity(), isReadyLoading(), getPackageList());
    }

    /**
     * Update the vehicle with a new current fuel capacity. Returns a new vehicle instance.
     *
     * @param curFuelCapacity the new current fuel capacity
     * @return the updated vehicle
     */
    public Vehicle updateCurFuelCapacity(ActionCost curFuelCapacity) {
        return new Vehicle(getName(), getLocation(), getCurCapacity(), getMaxCapacity(), curFuelCapacity,
                getMaxFuelCapacity(), isReadyLoading(), getPackageList());
    }

    /**
     * Update the vehicle with a new location. Returns a new vehicle instance.
     *
     * @param location the new location
     * @return the updated vehicle
     */
    public Vehicle updateLocation(Location location) {
        return new Vehicle(getName(), location, getCurCapacity(), getMaxCapacity(), getCurFuelCapacity(),
                getMaxFuelCapacity(), isReadyLoading(), getPackageList());
    }

    /**
     * Update the vehicle with a new ready loading state. Returns a new vehicle instance.
     *
     * @param readyLoading the new ready-loading state
     * @return the updated vehicle
     */
    public Vehicle updateReadyLoading(boolean readyLoading) {
        return new Vehicle(getName(), getLocation(), getCurCapacity(), getMaxCapacity(), getCurFuelCapacity(),
                getMaxFuelCapacity(), readyLoading, getPackageList());
    }

    /**
     * Update the vehicle with a new package. Returns a new vehicle instance.
     *
     * @param oldPackage the old package
     * @param newPackage the new package
     * @return the updated vehicle
     */
    public Vehicle changePackage(Package oldPackage, Package newPackage) {
        return removePackage(oldPackage).addPackage(newPackage);
    }

    /**
     * Adds a package to the vehicle. Returns a new vehicle instance.
     *
     * @param pkg the package to add
     * @return the updated vehicle
     */
    public Vehicle addPackage(Package pkg) {
        if (getPackageList().contains(pkg)) {
            throw new IllegalArgumentException("Package " + pkg + " already is in vehicle " + this + ".");
        }
        if (pkg.getLocation() != null) {
            throw new IllegalStateException(
                    "Package " + pkg + " is in vehicle and somewhere else at the same time " + pkg.getLocation() + ".");
        }
        List<Package> newPackageList = new ArrayList<>(getPackageList());
        newPackageList.add(pkg);
        return new Vehicle(getName(), getLocation(), getCurCapacity().subtract(pkg.getSize()), getMaxCapacity(),
                getCurFuelCapacity(), getMaxFuelCapacity(), isReadyLoading(), newPackageList);
    }

    /**
     * Removes a package from the vehicle. Returns a new vehicle instance.
     *
     * @param pkg the package to remove
     * @return the updated vehicle
     */
    public Vehicle removePackage(Package pkg) {
        if (!getPackageList().contains(pkg)) {
            throw new IllegalArgumentException("Package " + pkg + " is not in vehicle " + this + ".");
        }
        if (pkg.getLocation() != null) {
            throw new IllegalStateException(
                    "Package " + pkg + " is in vehicle and somewhere else at the same time " + pkg.getLocation() + ".");
        }
        List<Package> newPackageList = new ArrayList<>(getPackageList());
        newPackageList.remove(pkg);
        return new Vehicle(getName(), getLocation(), getCurCapacity().add(pkg.getSize()), getMaxCapacity(),
                getCurFuelCapacity(), getMaxFuelCapacity(), isReadyLoading(), newPackageList);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).appendSuper(super.hashCode()).append(getCurCapacity()).append(
                getMaxCapacity()).append(getCurFuelCapacity()).append(getMaxFuelCapacity()).append(isReadyLoading())
                .append(getPackageList()).toHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Vehicle)) {
            return false;
        }
        Vehicle vehicle = (Vehicle) o;
        return new EqualsBuilder().appendSuper(super.equals(o)).append(getCurCapacity(), vehicle.getCurCapacity())
                .append(getMaxCapacity(), vehicle.getMaxCapacity())
                .append(getCurFuelCapacity(), vehicle.getCurFuelCapacity())
                .append(getMaxFuelCapacity(), vehicle.getMaxFuelCapacity())
                .append(isReadyLoading(), vehicle.isReadyLoading())
                .append(getPackageList(), vehicle.getPackageList())
                .isEquals();
    }

    @Override
    public String toString() {
        return "Vehicle[" + getName() + ", at=" + getLocation() + ", capacity=" + getCurCapacity() + "/"
                + getMaxCapacity() + ", fuel=" + getCurFuelCapacity() + "/" + getMaxFuelCapacity() + ", readyLoading="
                + isReadyLoading() + ", packages=" + getPackageList() + "]";
    }
}
