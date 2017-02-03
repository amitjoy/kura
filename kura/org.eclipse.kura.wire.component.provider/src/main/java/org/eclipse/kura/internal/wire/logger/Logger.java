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
package org.eclipse.kura.internal.wire.logger;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Map;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.LoggerFactory;

/**
 * The Class Logger is the specific Wire Component to log a list of wire records
 * as received in Wire Envelope
 */
public final class Logger implements WireReceiver, ConfigurableComponent {

    /** The Logger instance. */
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Logger.class);

    /** Localization Resource */
    private static final WireMessages message = LocalizationAdapter.adapt(WireMessages.class);

    /** The Wire Helper Service. */
    private volatile WireHelperService wireHelperService;

    /** The wire supporter component. */
    private WireSupport wireSupport;

    /**
     * OSGi Service Component callback for activation.
     *
     * @param componentContext
     *            the component context
     * @param properties
     *            the properties
     */
    protected synchronized void activate(final ComponentContext componentContext,
            final Map<String, Object> properties) {
        logger.debug(message.activatingLogger());
        this.wireSupport = this.wireHelperService.newWireSupport(this);
        logger.debug(message.activatingLoggerDone());
    }

    /**
     * Binds the Wire Helper Service.
     *
     * @param wireHelperService
     *            the new Wire Helper Service
     */
    public synchronized void bindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == null) {
            this.wireHelperService = wireHelperService;
        }
    }

    /**
     * OSGi Service Component callback for deactivation.
     *
     * @param componentContext
     *            the component context
     */
    protected synchronized void deactivate(final ComponentContext componentContext) {
        logger.debug(message.deactivatingLogger());
        // remained for debugging purposes
        logger.debug(message.deactivatingLoggerDone());
    }

    /** {@inheritDoc} */
    @Override
    public void onWireReceive(final WireEnvelope wireEnvelope) {
        requireNonNull(wireEnvelope, message.wireEnvelopeNonNull());
        logger.info(message.wireEnvelopeReceived(wireEnvelope.getEmitterPid()));
        // filtering list of wire records based on the provided severity level
        final List<WireRecord> records = this.wireSupport.filter(wireEnvelope.getRecords());
        logger.info(message.loggerReceive(records.toString()));
    }

    /** {@inheritDoc} */
    @Override
    public void producersConnected(final Wire[] wires) {
        requireNonNull(wires, message.wiresNonNull());
        this.wireSupport.producersConnected(wires);
    }

    /**
     * Unbinds the Wire Helper Service.
     *
     * @param wireHelperService
     *            the new Wire Helper Service
     */
    public synchronized void unbindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == wireHelperService) {
            this.wireHelperService = null;
        }
    }

    /**
     * OSGi Service Component callback for updating.
     *
     * @param properties
     *            the updated properties
     */
    public synchronized void updated(final Map<String, Object> properties) {
        logger.debug(message.updatingLogger());
        // remained for debugging purposes
        logger.debug(message.updatingLoggerDone());
    }

    /** {@inheritDoc} */
    @Override
    public void updated(final Wire wire, final Object value) {
        this.wireSupport.updated(wire, value);
    }

}