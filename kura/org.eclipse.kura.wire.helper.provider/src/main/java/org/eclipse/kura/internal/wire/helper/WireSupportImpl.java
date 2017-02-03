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
package org.eclipse.kura.internal.wire.helper;

import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.wire.Severity.ERROR;
import static org.eclipse.kura.wire.Severity.INFO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.eclipse.kura.util.service.ServiceUtil;
import org.eclipse.kura.wire.SeverityLevel;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireField;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.wireadmin.Wire;

/**
 * The Class WireSupportImpl implements {@link WireSupport}
 */
final class WireSupportImpl implements WireSupport {

    /** Localization Resource */
    private static final WireMessages message = LocalizationAdapter.adapt(WireMessages.class);

    /** Event Admin Service */
    private final EventAdmin eventAdmin;

    /** The incoming wires. */
    private List<Wire> incomingWires;

    /** The outgoing wires. */
    private List<Wire> outgoingWires;

    /** The Wire Helper Service. */
    private final WireHelperService wireHelperService;

    /** The supported Wire Component. */
    private final WireComponent wireSupporter;

    /**
     * Instantiates a new wire support implementation.
     *
     * @param wireSupporter
     *            the wire supporter
     * @param wireHelperService
     *            the Wire Helper service
     * @param eventAdmin
     *            the Event Admin service
     * @throws NullPointerException
     *             if any of the provided arguments is null
     */
    WireSupportImpl(final WireComponent wireSupporter, final WireHelperService wireHelperService,
            final EventAdmin eventAdmin) {
        requireNonNull(wireSupporter, message.wireSupportedComponentNonNull());
        requireNonNull(wireHelperService, message.wireHelperServiceNonNull());
        requireNonNull(eventAdmin, message.eventAdminNonNull());

        this.wireSupporter = wireSupporter;
        this.wireHelperService = wireHelperService;
        this.eventAdmin = eventAdmin;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void consumersConnected(final Wire[] wires) {
        this.outgoingWires = Arrays.asList(wires);
    }

    /** {@inheritDoc} */
    @Override
    public void emit(final List<WireRecord> wireRecords) {
        requireNonNull(wireRecords, message.wireRecordsNonNull());
        if (this.wireSupporter instanceof WireEmitter) {
            final String emitterPid = this.wireHelperService.getServicePid(this.wireSupporter);
            final String pid = this.wireHelperService.getPid(this.wireSupporter);
            final WireEnvelope wireEnvelope = new WireEnvelope(emitterPid, wireRecords);
            for (final Wire wire : this.outgoingWires) {
                wire.update(wireEnvelope);
            }
            final Map<String, Object> properties = CollectionUtil.newHashMap();
            properties.put("emitter", pid);
            this.eventAdmin.postEvent(new Event(EMIT_EVENT_TOPIC, properties));
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<WireRecord> filter(final List<WireRecord> wireRecords) {
        requireNonNull(wireRecords, message.wireRecordsNonNull());
        final SeverityLevel level = this.getSeverityLevel();
        // If the severity level is not set or set to ALL, then all wire fields remain
        if (level == null) {
            return wireRecords;
        }
        final List<WireRecord> newRecords = new ArrayList<>();
        final Set<WireField> newFields = CollectionUtil.newHashSet();
        for (final WireRecord wireRecord : wireRecords) {
            for (final WireField wireField : wireRecord.getFields()) {
                final SeverityLevel wireFieldLevel = wireField.getSeverityLevel();

                // If the severity level is INFO, then only info wire fields will remain
                if ((wireFieldLevel == INFO) && (level == INFO)) {
                    newFields.add(wireField);
                }

                // If the severity level is ERROR, then only ERROR wire fields will remain
                if ((wireFieldLevel == ERROR) && (level == ERROR)) {
                    newFields.add(wireField);
                }
            }
            final WireRecord newWireRecord = new WireRecord.Builder().addFields(newFields).build();
            newRecords.add(newWireRecord);
        }
        return newRecords;
    }

    /**
     * Returns the severity level of the wire component
     *
     * @return the severity level
     */
    private SeverityLevel getSeverityLevel() {
        final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        final ServiceReference<WireComponent>[] refs = ServiceUtil.getServiceReferences(context, WireComponent.class,
                null);
        String property = null;
        final String severityLevel = "severity.level";
        try {
            for (final ServiceReference<WireComponent> ref : refs) {
                final WireComponent component = context.getService(ref);
                if (component == this.wireSupporter) {
                    property = ref.getProperty(severityLevel).toString();
                    break;
                }
            }
            if ("INFO".equalsIgnoreCase(property)) {
                return INFO;
            } else if ("ERROR".equalsIgnoreCase(property)) {
                return ERROR;
            }
        } finally {
            ServiceUtil.ungetServiceReferences(context, refs);
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized Object polled(final Wire wire) {
        return wire.getLastValue();
    }

    /** {@inheritDoc} */
    @Override
    public void producersConnected(final Wire[] wires) {
        this.outgoingWires = Arrays.asList(wires);
    }

    /** {@inheritDoc} */
    @Override
    public void updated(final Wire wire, final Object value) {
        requireNonNull(wire, message.wireNonNull());
        if ((value instanceof WireEnvelope) && (this.wireSupporter instanceof WireReceiver)) {
            ((WireReceiver) this.wireSupporter).onWireReceive((WireEnvelope) value);
        }
    }
}
