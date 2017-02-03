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
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;

/**
 * The {@link WireField} represents an associative data to be contained in {@link WireRecord}.
 * <br/>
 * <br/>
 * Every {@link WireField} represents an association of a name and a value. Every {@link WireField}
 * is also capable of denoting the {@code SeverityLevel} of this association.
 * <br/>
 * <br/>
 * Using such {@link WireField}, any type of primitive data ({@see DataType}) can be represented.
 * For instance, a data retrieved from a measurement point from a device can be represented as the
 * following.
 *
 * <pre>
 * name = LED
 * value = true
 * level = INFO
 * </pre>
 *
 * Clients are hence intended to create instances of {@code WireField} to represent their data.
 * <br/>
 * Usage Example:
 *
 * <pre>
 * WireField field1 = new WireField ("LED", TypedValues.newBooleanValue(true), INFO);
 * WireField field2 = new WireField ("CURRENT", TypedValues.newDoubleValue(543.3), INFO);
 * WireField field3 = new WireField ("PRESSURE", TypedValues.newStringValue("Channel Malconfiguration", ERROR);
 * </pre>
 *
 * @see Severity
 * @see SeverityLevel
 * @see DataType
 * @see TypedValue
 * @see TypedValues
 * @see WireRecord
 * @see WireEnvelope
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
     * @param severityLevel
     *            the severity level
     * @throws NullPointerException
     *             if any of the arguments is null
     */
    public WireField(final String name, final TypedValue<?> value, final SeverityLevel severityLevel) {
        requireNonNull(name, "Wire Field name cannot be null");
        requireNonNull(value, "Wire Field value type cannot be null");
        requireNonNull(severityLevel, "Wire Field severity level cannot be null");

        this.name = name;
        this.value = value;
        this.level = severityLevel;
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
