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
package org.eclipse.kura.internal.wire.logger;

import static org.eclipse.kura.Preconditions.checkNull;

import java.util.List;
import java.util.Map;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.wire.SeverityLevel;
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
	private static final org.slf4j.Logger s_logger = LoggerFactory.getLogger(Logger.class);

	/** Localization Resource */
	private static final WireMessages s_message = LocalizationAdapter.adapt(WireMessages.class);

	/** The Constant denoting severity level. */
	private static final String SEVERITY_LEVEL = "severity.level";

	/** The properties as associated */
	private Map<String, Object> m_properties;

	/** The Wire Helper Service. */
	private volatile WireHelperService m_wireHelperService;

	/** The wire supporter component. */
	private WireSupport m_wireSupport;

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
		s_logger.debug(s_message.activatingLogger());
		this.m_wireSupport = this.m_wireHelperService.newWireSupport(this);
		this.m_properties = properties;
		s_logger.debug(s_message.activatingLoggerDone());
	}

	/**
	 * Binds the Wire Helper Service.
	 *
	 * @param wireHelperService
	 *            the new Wire Helper Service
	 */
	public synchronized void bindWireHelperService(final WireHelperService wireHelperService) {
		if (this.m_wireHelperService == null) {
			this.m_wireHelperService = wireHelperService;
		}
	}

	/**
	 * OSGi Service Component callback for deactivation.
	 *
	 * @param componentContext
	 *            the component context
	 */
	protected synchronized void deactivate(final ComponentContext componentContext) {
		s_logger.debug(s_message.deactivatingLogger());

		s_logger.debug(s_message.deactivatingLoggerDone());
	}

	/**
	 * Returns the severity level of accepted wire fields.
	 *
	 * @return the severity level
	 */
	SeverityLevel getSeverityLevel() {
		String severityLevel = "ERROR";
		final Object level = this.m_properties.get(SEVERITY_LEVEL);
		if ((this.m_properties != null) && this.m_properties.containsKey(SEVERITY_LEVEL) && (level != null)
				&& (level instanceof String)) {
			severityLevel = String.valueOf(level);
		}
		if ("ERROR".equalsIgnoreCase(severityLevel)) {
			return SeverityLevel.ERROR;
		}
		if ("INFO".equalsIgnoreCase(severityLevel)) {
			return SeverityLevel.INFO;
		}
		if ("CONFIG".equalsIgnoreCase(severityLevel)) {
			return SeverityLevel.CONFIG;
		}
		return SeverityLevel.ERROR;
	}

	/** {@inheritDoc} */
	@Override
	public void onWireReceive(final WireEnvelope wireEnvelope) {
		checkNull(wireEnvelope, s_message.wireEnvelopeNonNull());
		s_logger.info(s_message.wireEnvelopeReceived(wireEnvelope.getEmitterPid()));
		// filtering list of wire records based on the provided severity level
		final List<WireRecord> records = this.m_wireSupport.filter(wireEnvelope.getRecords());
		s_logger.info("Filtered Wire Envelope ==>" + records);
	}

	/** {@inheritDoc} */
	@Override
	public void producersConnected(final Wire[] wires) {
		checkNull(wires, s_message.wiresNonNull());
		this.m_wireSupport.producersConnected(wires);
	}

	/**
	 * Unbinds the Wire Helper Service.
	 *
	 * @param wireHelperService
	 *            the new Wire Helper Service
	 */
	public synchronized void unbindWireHelperService(final WireHelperService wireHelperService) {
		if (this.m_wireHelperService == wireHelperService) {
			this.m_wireHelperService = null;
		}
	}

	/**
	 * OSGi Service Component callback for updating.
	 *
	 * @param properties
	 *            the updated properties
	 */
	public synchronized void updated(final Map<String, Object> properties) {
		s_logger.debug(s_message.updatingLogger());
		this.m_properties = properties;
		s_logger.debug(s_message.updatingLoggerDone());
	}

	/** {@inheritDoc} */
	@Override
	public void updated(final Wire wire, final Object value) {
		this.m_wireSupport.updated(wire, value);
	}

}