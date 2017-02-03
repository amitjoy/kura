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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.kura.annotation.Immutable;
import org.eclipse.kura.annotation.Nullable;
import org.eclipse.kura.annotation.ThreadSafe;
import org.eclipse.kura.type.TypedValue;

/**
 * The Class {@link WireRecord} represents a record to be transmitted during wire
 * communication between wire emitter and wire receiver. It represents the actual
 * data that travels through the wires.
 * <br/>
 * <br/>
 * Note that, due to optimization, a {@link WireRecord} instance cannot contain no pair
 * of {@link WireField}s {@code w1} and {@code w2} such that {@code w1.equals(w2)} results
 * in {@code true}. In addition, {@link WireRecord} cannot contain any {@code null} element
 * as well. Hence, any endeavor to add any {@code null} {@link WireField} instance will not
 * impact {@link WireRecord} instance as the {@code null} instance will never be added to the
 * {@link WireRecord}.
 * <br/>
 * Usage Example:
 *
 * <pre>
 * WireField field1 = new WireField("LED", TypedValues.newBooleanValue(true), INFO);
 * WireField field2 = new WireField("CURRENT", TypedValues.newDoubleValue(543.3), INFO);
 *
 * WireRecord record = new WireRecord.Builder().addField(field1).addField(field2).build();
 * </pre>
 *
 * or,
 *
 * <pre>
 * Set{@code <WireField>} wireFields = ....;
 *
 * WireRecord record = new WireRecord.Builder().addFields(wireFields).build();
 * </pre>
 *
 * @see WireField
 * @see WireEnvelope
 * @see Severity
 * @see SeverityLevel
 *
 * @noextend This class is not intended to be extended by clients.
 */
@Immutable
@ThreadSafe
public class WireRecord {
    
    /** The associated wire fields. */
    private final Set<WireField> fields;

    /** Constructor */
    private WireRecord(final Builder builder) {
        requireNonNull(builder, "Builder cannot be null");
        this.fields = new HashSet<>(builder.getFields());
    }

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
            requireNonNull(wireFields, "Set of Wire Fields cannot be null");
            wireFields.stream().forEach(this::addField);
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
        private Set<WireField> getFields() {
            return Collections.unmodifiableSet(this.fields);
        }
    }

    /**
     * Gets the unmodifiable flat map representation of associated {@link WireField}s.
     *
     * @return the unmodifiable flat map representation of associated {@link WireField}s.
     *         The returned map will contain the {@link WireField} name as
     *         the key and {@link WireField} value as the value in th map.
     */
    public Map<String, TypedValue<?>> flatMapFields() {
        final Map<String, TypedValue<?>> flatMap = this.fields.stream()
                .collect(Collectors.toMap(WireField::getName, WireField::getValue));
        return Collections.unmodifiableMap(flatMap);
    }

    /**
     * Gets the unmodifiable view of associated {@link WireField}s.
     *
     * @return the unmodifiable view of v{@link WireField}s
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
