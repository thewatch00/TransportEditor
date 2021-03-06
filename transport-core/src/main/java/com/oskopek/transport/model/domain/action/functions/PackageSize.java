package com.oskopek.transport.model.domain.action.functions;

import com.oskopek.transport.model.domain.action.ActionCost;
import com.oskopek.transport.model.problem.Package;
import com.oskopek.transport.model.problem.*;

/**
 * The package-size function. Returns the the size that the {@link Package} has.
 */
public class PackageSize extends DefaultFunction {

    @Override
    public ActionCost apply(ActionObject... actionObjects) {
        if (actionObjects == null || actionObjects.length != 1 || !Package.class.isInstance(actionObjects[0])) {
            throw new IllegalArgumentException("PackageSize can only be applied to one argument of type Package");
        }
        return apply((Package) actionObjects[0]);
    }

    /**
     * Get the size of the {@link Package}.
     *
     * @param aPackage the package
     * @return the size of the package
     */
    public ActionCost apply(Package aPackage) {
        if (aPackage == null) {
            throw new IllegalArgumentException("Package cannot be null.");
        }
        return aPackage.getSize();
    }

}
