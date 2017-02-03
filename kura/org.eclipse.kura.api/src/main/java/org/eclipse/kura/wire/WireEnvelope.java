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

import java.util.Collections;
import java.util.List;

import org.eclipse.kura.annotation.Immutable;
import org.eclipse.kura.annotation.ThreadSafe;
import org.osgi.service.wireadmin.BasicEnvelope;
import org.osgi.service.wireadmin.Envelope;

/**
 * The Class {@link WireEnvelope} represents a composite envelope to be used as an
 * abstract data to be transmitted between the wire emitter and the wire
 * receiver
 *
 * @see Envelope
 * @see BasicEnvelope
 * @see WireRecord
 * @see WireField
 *
 * @noextend This class is not intended to be extended by clients.
 */
@Immutable
@ThreadSafe
public class WireEnvelope extends BasicEnvelope {

    /**
     * The scope as agreed by the composite producer and consumer. This remains
     * same for all the Kura Wires communications.
     */
    private static final String SCOPE = "WIRES";

    /**
     * Instantiates a new {@link WireEnvelope}.
     *
     * @param emitterPid
     *            the wire emitter PID
     * @param wireRecords
     *            the {@link WireRecord}s
     */
    public WireEnvelope(final String emitterPid, final List<WireRecord> wireRecords) {
        super(wireRecords, emitterPid, SCOPE);
    }

    /**
     * Gets the wire emitter PID.
     *
     * @return the wire emitter PID
     */
    public String getEmitterPid() {
        return (String) this.getIdentification();
    }

    /**
     * Gets the {@link WireRecord}s.
     *
     * @return the {@link WireRecord}s
     */
    @SuppressWarnings("unchecked")
    public List<WireRecord> getRecords() {
        return Collections.unmodifiableList((List<WireRecord>) this.getValue());
    }

}
