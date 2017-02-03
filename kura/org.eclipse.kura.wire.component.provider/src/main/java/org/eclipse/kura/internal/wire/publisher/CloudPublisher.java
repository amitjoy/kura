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
package org.eclipse.kura.internal.wire.publisher;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.internal.wire.publisher.PayloadType.JSON;
import static org.eclipse.kura.internal.wire.publisher.PayloadType.KURA_PAYLOAD;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireField;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

/**
 * The Class CloudPublisher is the specific Wire Component to publish a list of
 * wire records as received in Wire Envelope to the configured cloud
 * platform.<br/>
 * <br/>
 *
 * For every Wire Record as found in Wire Envelope will be wrapped inside a Kura
 * Payload and will be sent to the Cloud Platform. In addition, the user can
 * avail to wrap every Wire Record as a JSON object as well.
 */
public final class CloudPublisher implements WireReceiver, CloudClientListener, ConfigurableComponent {

    /**
     * Inner class defined to track the CloudServices as they get added, modified or removed.
     * Specific methods can refresh the cloudService definition and setup again the Cloud Client.
     *
     */
    private final class CloudPublisherServiceTrackerCustomizer
            implements ServiceTrackerCustomizer<CloudService, CloudService> {

        @Override
        public CloudService addingService(final ServiceReference<CloudService> reference) {
            CloudPublisher.this.cloudService = CloudPublisher.this.bundleContext.getService(reference);
            try {
                // recreate the Cloud Client
                setupCloudClient();
            } catch (final KuraException e) {
                logger.error(message.cloudClientSetupProblem(), e);
            }
            return CloudPublisher.this.cloudService;
        }

        @Override
        public void modifiedService(final ServiceReference<CloudService> reference, final CloudService service) {
            CloudPublisher.this.cloudService = CloudPublisher.this.bundleContext.getService(reference);
            try {
                // recreate the Cloud Client
                setupCloudClient();
            } catch (final KuraException e) {
                logger.error(message.cloudClientSetupProblem(), e);
            }
        }

        @Override
        public void removedService(final ServiceReference<CloudService> reference, final CloudService service) {
            CloudPublisher.this.cloudService = null;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(CloudPublisher.class);

    private static final WireMessages message = LocalizationAdapter.adapt(WireMessages.class);

    private BundleContext bundleContext;

    private CloudClient cloudClient;

    private CloudPublisherOptions cloudPublisherOptions;

    private volatile CloudService cloudService;

    private ServiceTracker<CloudService, CloudService> cloudServiceTracker;

    private ServiceTrackerCustomizer<CloudService, CloudService> cloudServiceTrackerCustomizer;

    private volatile WireHelperService wireHelperService;

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
        logger.debug(message.activatingCloudPublisher());
        this.wireSupport = this.wireHelperService.newWireSupport(this);
        this.bundleContext = componentContext.getBundleContext();

        // Update properties
        this.cloudPublisherOptions = new CloudPublisherOptions(properties);

        this.cloudServiceTrackerCustomizer = new CloudPublisherServiceTrackerCustomizer();
        initCloudServiceTracking();

        logger.debug(message.activatingCloudPublisherDone());
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
     * Builds the provided JSON payload instance from the provided wire record.
     *
     * @param wireRecord
     *            the wire record
     * @param payload
     *            the JSON payload to build
     * @throws NullPointerException
     *             if the provided wire record or JSON payload is null
     */
    private void buildJsonPayload(final WireRecord wireRecord, final JsonObject payload) {
        requireNonNull(wireRecord, message.wireRecordNonNull());
        requireNonNull(payload, message.payloadNonNull());

        for (final WireField wireField : wireRecord.getFields()) {
            final Object wrappedValue = wireField.getValue().getValue();
            payload.add(wireField.getName(), wrappedValue.toString());
        }
    }

    /**
     * Builds the provided Kura payload instance from the provided wire record.
     *
     * @param wireRecord
     *            the wire record
     * @param payload
     *            the Kura Payload to build
     * @throws NullPointerException
     *             if the provided wire record or Kura Payload is null
     */
    private void buildKuraPayload(final WireRecord wireRecord, final KuraPayload payload) {
        requireNonNull(wireRecord, message.wireRecordNonNull());
        requireNonNull(payload, message.payloadNonNull());

        for (final WireField wireField : wireRecord.getFields()) {
            final Object wrappedValue = wireField.getValue().getValue();
            payload.addMetric(wireField.getName(), wrappedValue);
        }
    }

    /**
     * Builds the provided payload instance from the provided wire record.
     *
     * @param wireRecord
     *            the wire record
     * @param payload
     *            the payload to build
     * @throws NullPointerException
     *             if the provided wire record or payload is null
     */
    private void buildPayload(final WireRecord wireRecord, final Object payload) {
        requireNonNull(wireRecord, message.wireRecordNonNull());
        requireNonNull(payload, message.payloadNonNull());

        if (payload instanceof KuraPayload) {
            final KuraPayload kuraPayload = (KuraPayload) payload;
            buildKuraPayload(wireRecord, kuraPayload);
        }
        if (payload instanceof JsonObject) {
            final JsonObject jsonObject = (JsonObject) payload;
            buildJsonPayload(wireRecord, jsonObject);
        }

    }

    /**
     * Closes the cloud client.
     */
    private void closeCloudClient() {
        if (this.cloudClient != null) {
            this.cloudClient.removeCloudClientListener(this);
            this.cloudClient.release();
            this.cloudClient = null;
        }
    }

    /**
     * OSGi Service Component callback for deactivation.
     *
     * @param componentContext
     *            the component context
     */
    protected synchronized void deactivate(final ComponentContext componentContext) {
        logger.debug(message.deactivatingCloudPublisher());
        // close the client
        closeCloudClient();

        if (this.cloudServiceTracker != null) {
            this.cloudServiceTracker.close();
        }
        logger.debug(message.deactivatingCloudPublisherDone());
    }

    /**
     * Service tracker to manage Cloud Services
     */
    private void initCloudServiceTracking() {
        final String selectedCloudServicePid = this.cloudPublisherOptions.getCloudServicePid();
        final String filterString = String.format("(&(%s=%s)(kura.service.pid=%s))", Constants.OBJECTCLASS,
                CloudService.class.getName(), selectedCloudServicePid);
        Filter filter = null;
        try {
            filter = this.bundleContext.createFilter(filterString);
        } catch (final InvalidSyntaxException e) {
            logger.error(message.filterSetupException(), e);
        }
        this.cloudServiceTracker = new ServiceTracker<>(this.bundleContext, filter, this.cloudServiceTrackerCustomizer);
        this.cloudServiceTracker.open();
    }

    /** {@inheritDoc} */
    @Override
    public void onConnectionEstablished() {
        // Not required
    }

    /** {@inheritDoc} */
    @Override
    public void onConnectionLost() {
        // Not required
    }

    /** {@inheritDoc} */
    @Override
    public void onControlMessageArrived(final String deviceId, final String appTopic, final KuraPayload msg,
            final int qos, final boolean retain) {
        // Not required
    }

    /** {@inheritDoc} */
    @Override
    public void onMessageArrived(final String deviceId, final String appTopic, final KuraPayload msg, final int qos,
            final boolean retain) {
        // Not required
    }

    /** {@inheritDoc} */
    @Override
    public void onMessageConfirmed(final int messageId, final String topic) {
        // Not required
    }

    /** {@inheritDoc} */
    @Override
    public void onMessagePublished(final int messageId, final String topic) {
        // Not required
    }

    /** {@inheritDoc} */
    @Override
    public void onWireReceive(final WireEnvelope wireEnvelope) {
        requireNonNull(wireEnvelope, message.wireEnvelopeNonNull());
        logger.info(message.wireEnvelopeReceived(wireEnvelope.getEmitterPid()));
        // filtering list of wire records based on the provided severity level
        final List<WireRecord> records = this.wireSupport.filter(wireEnvelope.getRecords());

        if ((this.cloudService != null) && (this.cloudClient != null)) {
            publish(records);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void producersConnected(final Wire[] wires) {
        requireNonNull(wires, message.wiresNonNull());
        this.wireSupport.producersConnected(wires);
    }

    /**
     * Publishes the list of provided Wire Records
     *
     * @param wireRecords
     *            the provided list of Wire Records
     * @throws NullPointerException
     *             if the argument is null or the cloud client is null
     */
    private void publish(final List<WireRecord> wireRecords) {
        requireNonNull(this.cloudClient, message.cloudClientNonNull());
        requireNonNull(wireRecords, message.wireRecordsNonNull());

        final String appTopic = this.cloudPublisherOptions.getPublishingTopic();
        final PayloadType messageType = this.cloudPublisherOptions.getPayloadType();
        final Timestamp timestamp = new Timestamp(new Date().getTime());
        Object payload = null;

        if (messageType == KURA_PAYLOAD) {
            payload = new KuraPayload();
            final KuraPayload kuraPayload = (KuraPayload) payload;
            kuraPayload.setTimestamp(timestamp);
        } else if (messageType == JSON) {
            payload = Json.object();
            final JsonObject jsonPayload = (JsonObject) payload;
            jsonPayload.add(message.timestamp(), timestamp.toString());
        }

        try {
            for (final WireRecord wireRecord : wireRecords) {
                buildPayload(wireRecord, payload);
            }
            if (nonNull(payload)) {
                if (messageType == KURA_PAYLOAD) {
                    final KuraPayload kuraPayload = (KuraPayload) payload;
                    publishKuraPayload(kuraPayload, appTopic);
                } else if (messageType == JSON) {
                    final JsonObject jsonPayload = (JsonObject) payload;
                    publishJson(jsonPayload, appTopic);
                }
            }
        } catch (final KuraException e) {
            logger.error(message.errorPublishingWireRecords(), e);
        }
    }

    /**
     * Publishes the provided {@link JsonObject} to the provided application topic
     *
     * @param jsonObject
     *            the {@link JsonObject} instance to publish
     * @param appTopic
     *            the application topic
     * @throws KuraException
     *             if publish fails
     * @throws NullPointerException
     *             if any of the arguments is null
     */
    private void publishJson(final JsonObject jsonObject, final String appTopic) throws KuraException {
        requireNonNull(jsonObject, message.payloadNonNull());
        requireNonNull(appTopic, message.appTopicNonNull());

        this.cloudClient.publish(appTopic, jsonObject.toString().getBytes(),
                this.cloudPublisherOptions.getPublishingQos(), this.cloudPublisherOptions.getPublishingRetain(),
                this.cloudPublisherOptions.getPublishingPriority());
    }

    /**
     * Publishes the provided {@link KuraPayload} to the provided application topic
     *
     * @param kuraPayload
     *            the {@link KuraPayload} instance to publish
     * @param appTopic
     *            the application topic
     * @throws KuraException
     *             if publish fails
     * @throws NullPointerException
     *             if any of the arguments if null
     */
    private void publishKuraPayload(final KuraPayload kuraPayload, final String appTopic) throws KuraException {
        requireNonNull(kuraPayload, message.payloadNonNull());
        requireNonNull(appTopic, message.appTopicNonNull());

        if (this.cloudPublisherOptions.isControlMessage()) {
            this.cloudClient.controlPublish(appTopic, kuraPayload, this.cloudPublisherOptions.getPublishingQos(),
                    this.cloudPublisherOptions.getPublishingRetain(),
                    this.cloudPublisherOptions.getPublishingPriority());
        } else {
            this.cloudClient.publish(appTopic, kuraPayload, this.cloudPublisherOptions.getPublishingQos(),
                    this.cloudPublisherOptions.getPublishingRetain(),
                    this.cloudPublisherOptions.getPublishingPriority());
        }
    }

    /**
     * Setup cloud client.
     *
     * @throws KuraException
     *             the kura exception
     */
    private void setupCloudClient() throws KuraException {
        closeCloudClient();
        // create the new CloudClient for the specified application
        final String appId = this.cloudPublisherOptions.getPublishingApplication();
        this.cloudClient = this.cloudService.newCloudClient(appId);
        this.cloudClient.addCloudClientListener(this);
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
        logger.debug(message.updatingCloudPublisher());
        // Update properties
        this.cloudPublisherOptions = new CloudPublisherOptions(properties);

        if (this.cloudServiceTracker != null) {
            this.cloudServiceTracker.close();
        }
        initCloudServiceTracking();
        logger.debug(message.updatingCloudPublisherDone());
    }

    /** {@inheritDoc} */
    @Override
    public void updated(final Wire wire, final Object value) {
        this.wireSupport.updated(wire, value);
    }
}
