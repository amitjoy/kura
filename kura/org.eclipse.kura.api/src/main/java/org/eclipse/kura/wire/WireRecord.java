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

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.kura.annotation.Immutable;
import org.eclipse.kura.annotation.Nullable;
import org.eclipse.kura.annotation.ThreadSafe;

/**
 * The Class {@link WireRecord} represents a record to be transmitted during wire
 * communication between wire emitter and wire receiver. It represents the actual
 * data that travels through the wires.
 *
 * @noextend This class is not intended to be extended by clients.
 */
@Immutable
@ThreadSafe
public class WireRecord {

    /**
     * {@link WireRecord} Builder class for creation of {@link WireRecord} instance.
     * This is essentially needed for maintaining thread-safety and immutability of
     * every {@link WireRecord} instance.
     */
    public static class Builder {

        /** The contained wire fields. */
        private final Set<WireField> fields;

        /** Constructor */
        public Builder() {
            this.fields = new HashSet<>();
        }

        /**
         * Adds the provided {@link WireField} to the {@link WireRecord} if
         * the provided {@link WireField} is not null
         *
         * @param wireField
         *            the provided {@link WireField}
         * @return the {@link Builder} instance
         */
        public Builder addField(@Nullable final WireField wireField) {
            if (nonNull(wireField)) {
                this.fields.add(wireField);
            }
            return this;
        }

        /**
         * Adds the provided {@link WireField}s to the {@link WireRecord}
         *
         * @param wireFields
         *            the provided {@link WireField}s
         * @return the {@link Builder} instance
         * @throws NullPointerException
         *             if the provided set is null
         */
        public Builder addFields(final Set<WireField> wireFields) {
            requireNonNull(wireFields, "Wire Fields instance cannot be null");
            this.fields.addAll(wireFields);
            return this;
        }

        /**
         * Builds a {@link WireRecord} instance
         *
         * @return {@link WireRecord} instance
         */
        public WireRecord build() {
            return new WireRecord(this);
        }

        /**
         * Gets the associated {@link WireField}s.
         *
         * @return the {@link WireField}s
         */
        public Set<WireField> getFields() {
            return Collections.unmodifiableSet(this.fields);
        }
    }

    /** The contained wire fields. */
    private final Set<WireField> fields;

    /** Constructor */
    private WireRecord(final Builder builder) {
        requireNonNull(builder, "Builder cannot be null");
        this.fields = new HashSet<>(builder.getFields());
    }

    /**
     * Gets the associated {@link WireField}s.
     *
     * @return the {@link WireField}s
     */
    public Set<WireField> getFields() {
        return Collections.unmodifiableSet(this.fields);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "WireRecord [fields=" + this.fields + "]";
    }

}
