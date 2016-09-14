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
package org.eclipse.kura.internal.wire.subscriber;

import static org.eclipse.kura.Preconditions.checkNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.data.listener.DataServiceListener;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.util.base.ThrowableUtil;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.eclipse.kura.wire.SeverityLevel;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireField;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class CloudSubscriber is the specific Wire Component to subscribe a list
 * of wire records as received in Wire Envelope from the configured cloud
 * platform.<br/>
 * <br/>
 *
 * For every Wire Record as found in Wire Envelope will be wrapped inside a Kura
 * Payload and will be sent to the Cloud Platform. Unlike Cloud Publisher Wire
 * Component, the user can only avail to wrap every Wire Record in the default
 * Google Protobuf Payload.
 */
public final class CloudSubscriber implements WireEmitter, DataServiceListener, ConfigurableComponent {

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(CloudSubscriber.class);

	/** Localization Resource */
	private static final WireMessages s_message = LocalizationAdapter.adapt(WireMessages.class);

	/** The cloud client. */
	private CloudClient m_cloudClient;

	/** The cloud service. */
	private volatile CloudService m_cloudService;

	/** The cloud subscriber options. */
	private CloudSubscriberOptions m_options;

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
		s_logger.debug(s_message.activatingCloudSubscriber());
		this.m_wireSupport = this.m_wireHelperService.newWireSupport(this);
		// Update properties
		this.m_options = new CloudSubscriberOptions(properties);
		// recreate the CloudClient
		try {
			this.setupCloudClient();
		} catch (final KuraException e) {
			s_logger.error(s_message.cloudClientSetupProblem() + ThrowableUtil.stackTraceAsString(e));
		}
		s_logger.debug(s_message.activatingCloudSubscriberDone());
	}

	/**
	 * Binds the cloud service.
	 *
	 * @param cloudService
	 *            the new cloud service
	 */
	public synchronized void bindCloudService(final CloudService cloudService) {
		if (this.m_cloudService == null) {
			this.m_cloudService = cloudService;
		}
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
	 * Closes cloud client.
	 */
	private void closeCloudClient() {
		if (this.m_cloudClient != null) {
			this.m_cloudClient.release();
			this.m_cloudClient = null;
		}
	}

	/** {@inheritDoc} */
	@Override
	public void consumersConnected(final Wire[] wires) {
		this.m_wireSupport.consumersConnected(wires);
	}

	/**
	 * OSGi Service Component callback for deactivation.
	 *
	 * @param componentContext
	 *            the component context
	 */
	protected synchronized void deactivate(final ComponentContext componentContext) {
		s_logger.debug(s_message.deactivatingCloudSubscriber());
		// close the client
		this.closeCloudClient();
		// close the disconnect manager
		s_logger.debug(s_message.deactivatingCloudSubscriberDone());
	}

	/** {@inheritDoc} */
	@Override
	public Object polled(final Wire wires) {
		return this.m_wireSupport.polled(wires);
	}

	/**
	 * Setup cloud client.
	 *
	 * @throws KuraException
	 *             the kura exception
	 */
	private void setupCloudClient() throws KuraException {
		this.closeCloudClient();
		// create the new CloudClient for the specified application
		final String appId = this.m_options.getSubscribeApplication();
		this.m_cloudClient = this.m_cloudService.newCloudClient(appId);
		this.m_cloudClient.subscribe(this.m_options.getSubscribingTopic(), this.m_options.getSubscribingQos());
	}

	/**
	 * Unbinds cloud service.
	 *
	 * @param cloudService
	 *            the cloud service
	 */
	public synchronized void unbindCloudService(final CloudService cloudService) {
		if (this.m_cloudService == cloudService) {
			this.m_cloudService = null;
		}
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
		s_logger.debug(s_message.updatingCloudSubscriber());
		// Update properties
		this.m_options = new CloudSubscriberOptions(properties);
		// recreate the Cloud Client
		try {
			this.setupCloudClient();
		} catch (final KuraException e) {
			s_logger.error(s_message.cloudClientSetupProblem() + ThrowableUtil.stackTraceAsString(e));
		}
		s_logger.debug(s_message.updatingCloudSubscriberDone());
	}

	/** {@inheritDoc} */
	@Override
	public void onConnectionEstablished() {
		// Not required
	}

	/** {@inheritDoc} */
	@Override
	public void onDisconnecting() {
		// Not required
	}

	/** {@inheritDoc} */
	@Override
	public void onDisconnected() {
		// Not required
	}

	/** {@inheritDoc} */
	@Override
	public void onConnectionLost(Throwable cause) {
		// Not required
	}

	/** {@inheritDoc} */
	@Override
	public void onMessageArrived(String topic, byte[] payload, int qos, boolean retained) {
		WireRecord record = new WireRecord(new WireField("SUBSCRIBER", TypedValues.newStringValue("SUBSCRIBER"), SeverityLevel.CONFIG));
		m_wireSupport.emit(Arrays.asList(record));
	}
	
	/** {@inheritDoc} */
	@Override
	public void onMessagePublished(int messageId, String topic) {
		// Not required
	}

	/** {@inheritDoc} */
	@Override
	public void onMessageConfirmed(int messageId, String topic) {
		// Not required
	}

}
