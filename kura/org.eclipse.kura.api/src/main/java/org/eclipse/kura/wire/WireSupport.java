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

import java.util.List;

import org.osgi.service.wireadmin.Consumer;
import org.osgi.service.wireadmin.Producer;

/**
 * The interface WireSupport is responsible for managing incoming as well as
 * outgoing wires of the contained Wire Component. This is also used to perform
 * wire related operations for instance, emit and receive wire records.
 *
 * @see WireField
 * @see WireRecord
 * @see Severity
 * @see SeverityLevel
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface WireSupport extends Producer, Consumer {

    /**
     * The topic to be used for publishing and receiving the emit trigger events
     */
    public static final String EMIT_EVENT_TOPIC = "org/eclipse/kura/wires/emit";

    /**
     * Emit the provided list of {@link WireRecord}s
     *
     * @param wireRecords
     *            the list of {@link WireRecord}s to emit
     * @throws NullPointerException
     *             if the argument is null
     */
    public void emit(List<WireRecord> wireRecords);

    /**
     * Filters the provided list of {@link WireRecord}s based on the configured
     * {@link SeverityLevel} for the provided wire component. It filters out all
     * the {@link WireRecord}s that do not belong to the {@link SeverityLevel}
     * as required by the Wire Component.
     *
     * @param wireRecords
     *            the list of {@link WireRecord}s to filter
     * @throws NullPointerException
     *             if the argument is null
     * @return the list of filtered {@link WireRecord}s
     */
    public List<WireRecord> filter(List<WireRecord> wireRecords);
}
