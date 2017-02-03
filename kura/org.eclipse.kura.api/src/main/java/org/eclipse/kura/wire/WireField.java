/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.wire;

import static java.util.Objects.requireNonNull;

import org.eclipse.kura.annotation.Immutable;
import org.eclipse.kura.annotation.ThreadSafe;
import org.eclipse.kura.type.TypedValue;

/**
 * The {@link WireField} represents a data type to be contained in {@link WireRecord}.
 * <br/>
 * <br/>
 * Note that, due to optimization, a {@link WireRecord} instance cannot contain no pair
 * of {@link WireField}s <code>w1</code> and <code>w2</code> such that
 * <code>w1.equals(w2)</code> results in true. In addition, {@link WireRecord} cannot
 * contain any null element as well. Hence, any endeavor to add any null {@link WireField}
 * instance will not impact {@link WireRecord} instance as the null instance will never be
 * added to the {@link WireRecord}.
 *
 * @noextend This class is not intended to be extended by clients.
 */
@Immutable
@ThreadSafe
public class WireField {

    /** The severity level of the field */
    private final SeverityLevel level;

    /** The name of the field */
    private final String name;

    /** The value as contained */
    private final TypedValue<?> value;

    /**
     * Instantiates a new wire field.
     *
     * @param name
     *            the name
     * @param value
     *            the value
     * @param level
     *            the severity level
     * @throws NullPointerException
     *             if any of the arguments is null
     */
    public WireField(final String name, final TypedValue<?> value, final SeverityLevel level) {
        requireNonNull(name, "Wire Field name cannot be null");
        requireNonNull(value, "Wire Field value type cannot be null");
        requireNonNull(level, "Wire Field severity level cannot be null");

        this.name = name;
        this.value = value;
        this.level = level;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final WireField other = (WireField) obj;
        if (this.name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!this.name.equals(other.name)) {
            return false;
        }
        return true;
    }

    /**
     * Gets the name of the field
     *
     * @return the name of the field
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the severity level of the field
     *
     * @return the severity level of the field
     */
    public SeverityLevel getSeverityLevel() {
        return this.level;
    }

    /**
     * Gets the contained value
     *
     * @return the contained value
     */
    public TypedValue<?> getValue() {
        return this.value;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((this.name == null) ? 0 : this.name.hashCode());
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "WireField [level=" + this.level + ", name=" + this.name + ", value=" + this.value + "]";
    }

}
