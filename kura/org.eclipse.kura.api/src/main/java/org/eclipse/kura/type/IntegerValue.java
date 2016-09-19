/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.type;

import static org.eclipse.kura.Preconditions.checkNull;
import static org.eclipse.kura.type.DataType.INTEGER;

import org.eclipse.kura.annotation.Immutable;
import org.eclipse.kura.annotation.ThreadSafe;

/**
 * This class represents a {@link Integer} value as a {@link TypedValue}.
 */
@Immutable
@ThreadSafe
public final class IntegerValue implements TypedValue<Integer> {

	/**
	 * The actual contained value that will be represented as
	 * {@link TypedValue}.
	 */
	private final int value;

	/**
	 * Instantiates a new integer value.
	 *
	 * @param value
	 *            the value
	 */
	public IntegerValue(final int value) {
		this.value = value;
	}

	/** {@inheritDoc} */
	@Override
	public int compareTo(final TypedValue<Integer> otherTypedValue) {
		checkNull(otherTypedValue, "Typed Value cannot be null");
		return Integer.compare(this.value, otherTypedValue.getValue());
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
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		final IntegerValue other = (IntegerValue) obj;
		if (this.value != other.value) {
			return false;
		}
		return true;
	}

	/** {@inheritDoc} */
	@Override
	public DataType getType() {
		return INTEGER;
	}

	/** {@inheritDoc} */
	@Override
	public Integer getValue() {
		return this.value;
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + this.value;
		return result;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return "IntegerValue [value=" + this.value + "]";
	}
}