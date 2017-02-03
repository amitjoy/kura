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
import org.eclipse.kura.wire.WireField;
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

    /** Default Log Level */
    private static final String DEFAULT_LOG_LEVEL = "INFO";

    /** The Logger instance. */
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Logger.class);

    /** Localization Resource */
    private static final WireMessages message = LocalizationAdapter.adapt(WireMessages.class);

    /** Constant denoting log level property */
    private static final String PROP_LOG_LEVEL = "log.level";

    /** Component Configured Properties */
    private Map<String, Object> properties;

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
        this.properties = properties;
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

    /**
     * Returns the log level as configured
     *
     * @return the configured log level
     */
    private LogLevel getLoggingLevel() {
        String logLevel = DEFAULT_LOG_LEVEL;
        final Object configuredLogLevel = this.properties.get(PROP_LOG_LEVEL);
        if ((configuredLogLevel != null) && (configuredLogLevel instanceof String)) {
            logLevel = String.valueOf(configuredLogLevel);
        }
        return LogLevel.getLevel(logLevel);
    }

    /**
     * Log the provided list of {@link WireRecord} in the log file based on the provided
     * log level
     *
     * @param wireRecords
     *            the records to log
     * @param logLevel
     *            the logging level
     * @throws NullPointerException
     *             if any of the provided arguments is null
     */
    private void log(final List<WireRecord> wireRecords, final LogLevel logLevel) {
        requireNonNull(wireRecords, message.wireRecordsNonNull());
        requireNonNull(logLevel, message.logLevelNonNull());

        switch (logLevel) {
        case DEBUG:
            logger.debug("-------------------------------------------------------------");

            int i = 0;
            for (final WireRecord record : wireRecords) {
                logger.debug("Wire Record " + ++i);
                int j = 0;
                for (final WireField field : record.getFields()) {
                    logger.debug("       " + "Wire Field " + ++j);
                    logger.debug("                " + "Name: " + field.getName());
                    logger.debug("                " + "Value: " + field.getValue().getValue());
                    logger.debug("                " + "Severity: " + field.getSeverityLevel());
                }
            }

            logger.debug("-------------------------------------------------------------");
            break;
        case INFO:
            logger.info("-------------------------------------------------------------");

            int k = 0;
            for (final WireRecord record : wireRecords) {
                logger.info("Wire Record " + ++k);
                int j = 0;
                for (final WireField field : record.getFields()) {
                    logger.info("       " + "Wire Field " + ++j);
                    logger.info("                " + "Name: " + field.getName());
                    logger.info("                " + "Value: " + field.getValue().getValue());
                    logger.info("                " + "Severity: " + field.getSeverityLevel());
                }
            }

            logger.info("-------------------------------------------------------------");
            break;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onWireReceive(final WireEnvelope wireEnvelope) {
        requireNonNull(wireEnvelope, message.wireEnvelopeNonNull());
        logger.info(message.wireEnvelopeReceived(wireEnvelope.getEmitterPid()));
        // filtering list of wire records based on the provided severity level
        final List<WireRecord> records = this.wireSupport.filter(wireEnvelope.getRecords());
        log(records, getLoggingLevel());
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