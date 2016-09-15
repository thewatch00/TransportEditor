/*
 * Copyright (c) 2016 Ondrej Škopek <oskopek@oskopek.com>. All rights reserved.
 */

package com.oskopek.transporteditor.model.problem;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class DefaultActionObject implements ActionObject {

    private final String name;

    public DefaultActionObject(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DefaultActionObject)) {
            return false;
        }
        DefaultActionObject that = (DefaultActionObject) o;
        return new EqualsBuilder().append(getName(), that.getName()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(getName()).toHashCode();
    }

    @Override
    public String toString() {
        return "DefaultActionObject{" + "name=" + getName() + '}';
    }
}