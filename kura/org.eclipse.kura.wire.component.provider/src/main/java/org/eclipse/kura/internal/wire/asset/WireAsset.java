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
package org.eclipse.kura.internal.wire.asset;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.asset.AssetFlag.FAILURE;
import static org.eclipse.kura.asset.ChannelType.READ;
import static org.eclipse.kura.asset.ChannelType.READ_WRITE;
import static org.eclipse.kura.asset.ChannelType.WRITE;
import static org.eclipse.kura.wire.SeverityLevel.ERROR;
import static org.eclipse.kura.wire.SeverityLevel.INFO;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.asset.AssetFlag;
import org.eclipse.kura.asset.AssetRecord;
import org.eclipse.kura.asset.AssetStatus;
import org.eclipse.kura.asset.Channel;
import org.eclipse.kura.asset.provider.BaseAsset;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.util.base.ThrowableUtil;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.eclipse.kura.wire.SeverityLevel;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireField;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class WireAsset is a wire component which provides all necessary higher
 * level abstractions of a Kura asset. This wire asset is an integral wire
 * component in Kura Wires topology as it represents an industrial device with a
 * field protocol driver associated to it.<br/>
 * <br/>
 *
 * The WireRecord to be emitted by every wire asset comprises the following keys
 *
 * <ul>
 * <li>{@code <channelName>}</li> comprises proper value if present
 * <li>{@code <channelName>_assetName}</li> comprises asset name
 * <li>{@code <channelName>_channelId}</li> comprises channel ID
 * <li>{@code <channelName>_timestamp}</li> comprises timestamp
 * <li>{@code <channelName>_error}</li> (This will be present if and only if there
 * exists any error {@link AssetRecord} )
 * </ul>
 * <br/>
 * Note that, {@code <channelName>} will be replaced by the actual name of the channel.
 * <br/>
 * Also note that, if the channel name is equal to the received value of the
 * channel wire field name, then it would be considered as a WRITE wire field
 * value to the specific channel. <br/>
 * <br/>
 * For instance, {@code A} asset sends a Wire Record to {@code B} asset and the
 * received Wire Record contains list of Wire Fields. If there exists a Wire
 * Field which signifies the channel name and if this channel name also exists
 * in {@code B}'s list of configured channels, then the Wire Field which
 * contains the typed value of this channel in the received Wire Record will be
 * considered as a WRITE Value in that specific channel in B and this value will
 * be written to {@code B}'s channel
 *
 * @see Asset
 */
public final class WireAsset extends BaseAsset implements WireEmitter, WireReceiver {

    /** Configuration PID Property. */
    private static final String CONF_PID = "org.eclipse.kura.wire.WireAsset";

    /** The Logger instance. */
    private static final Logger logger = LoggerFactory.getLogger(WireAsset.class);

    /** Localization Resource. */
    private static final WireMessages message = LocalizationAdapter.adapt(WireMessages.class);

    /** Asset Name Property for Wire Fields */
    private static final String PROP_ASSET_NAME = "assetName";

    /** Channel ID Property for Wire Fields */
    private static final String PROP_CHANNEL_ID = "channelId";

    /** Error Property for Wire Fields */
    public static final String PROP_ERROR = "error";

    /** Timestamp Property for Wire Fields */
    private static final String PROP_TIMESTAMP = "timestamp";

    /** Property Separator for Wire Fields */
    private static final String SEPARATOR = "_";

    /** The Wire Helper Service. */
    private volatile WireHelperService wireHelperService;

    /** Wire Supporter Component. */
    private WireSupport wireSupport;

    /**
     * OSGi service component callback while activation.
     *
     * @param componentContext
     *            the component context
     * @param properties
     *            the service properties
     */
    @Override
    protected synchronized void activate(final ComponentContext componentContext,
            final Map<String, Object> properties) {
        logger.debug(message.activatingWireAsset());
        super.activate(componentContext, properties);
        this.wireSupport = this.wireHelperService.newWireSupport(this);
        logger.debug(message.activatingWireAssetDone());
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

    /** {@inheritDoc} */
    @Override
    public void consumersConnected(final Wire[] wires) {
        this.wireSupport.consumersConnected(wires);
    }

    /**
     * OSGi service component callback while deactivation.
     *
     * @param context
     *            the context
     */
    @Override
    protected synchronized void deactivate(final ComponentContext context) {
        logger.debug(message.deactivatingWireAsset());
        super.deactivate(context);
        logger.debug(message.deactivatingWireAssetDone());
    }

    /**
     * Determines the channels to read from the list of provided Wire Records
     *
     * @param records
     *            the list of Wire Records
     * @return the list of channel IDs
     * @throws NullPointerException
     *             if argument is null
     * @throws IllegalArgumentException
     *             if argument is empty
     */
    private List<Long> determineReadingChannels(final List<WireRecord> records) {
        requireNonNull(records, message.wireRecordsNonNull());
        if (records.isEmpty()) {
            throw new IllegalArgumentException(message.wireRecordsNonEmpty());
        }

        final List<Long> channelsToRead = CollectionUtil.newArrayList();
        final Map<Long, Channel> channels = this.assetConfiguration.getAssetChannels();
        for (final Map.Entry<Long, Channel> channelEntry : channels.entrySet()) {
            final Channel channel = channelEntry.getValue();
            if ((channel.getType() == READ) || (channel.getType() == READ_WRITE)) {
                channelsToRead.add(channel.getId());
            }
        }
        return channelsToRead;
    }

    /**
     * Determine the channels to write
     *
     * @param records
     *            the list of Wire Records to parse
     * @return list of Asset Records containing the values to be written
     * @throws NullPointerException
     *             if argument is null
     * @throws IllegalArgumentException
     *             if argument is empty
     */
    private List<AssetRecord> determineWritingChannels(final List<WireRecord> records) {
        requireNonNull(records, message.wireRecordsNonNull());
        if (records.isEmpty()) {
            throw new IllegalArgumentException(message.wireRecordsNonEmpty());
        }

        final List<AssetRecord> assetRecordsToWriteChannels = CollectionUtil.newArrayList();
        final Map<Long, Channel> channels = this.assetConfiguration.getAssetChannels();
        for (final WireRecord wireRecord : records) {
            String channelNameWireField = null;
            for (final WireField wireField : wireRecord.getFields()) {
                for (final Map.Entry<Long, Channel> channelEntry : channels.entrySet()) {
                    final Channel channel = channelEntry.getValue();
                    if ((channel.getType() == WRITE) || (channel.getType() == READ_WRITE)) {
                        final String wireFieldName = wireField.getName();
                        if (message.channelName().equalsIgnoreCase(wireFieldName)) {
                            channelNameWireField = String.valueOf(wireField.getValue().getValue());
                        }
                        if ((channelNameWireField != null) && channel.getName().equalsIgnoreCase(channelNameWireField)
                                && message.typedValue().equalsIgnoreCase(wireFieldName)
                                && (wireField.getSeverityLevel() == INFO)) {
                            assetRecordsToWriteChannels.add(this.prepareAssetRecord(channel, wireField.getValue()));
                        }
                    }
                }
            }
        }
        return assetRecordsToWriteChannels;
    }

    /**
     * Emit the provided list of asset records to the associated wires.
     *
     * @param assetRecords
     *            the list of asset records conforming to the aforementioned
     *            specification
     * @throws NullPointerException
     *             if provided records list is null
     * @throws IllegalArgumentException
     *             if provided records list is empty
     */
    private void emitAssetRecords(final List<AssetRecord> assetRecords) {
        requireNonNull(assetRecords, message.assetRecordsNonNull());
        if (assetRecords.isEmpty()) {
            throw new IllegalArgumentException(message.assetRecordsNonEmpty());
        }

        WireRecord wireRecord = null;
        for (final AssetRecord assetRecord : assetRecords) {
            final AssetStatus assetStatus = assetRecord.getAssetStatus();
            final AssetFlag assetFlag = assetStatus.getAssetFlag();
            final long channelId = assetRecord.getChannelId();
            final SeverityLevel level = (assetFlag == FAILURE) ? ERROR : INFO;
            final String channelName = this.assetConfiguration.getAssetChannels().get(channelId).getName();

            final Throwable exception = assetStatus.getException();
            final String exceptionMessage = assetStatus.getExceptionMessage();
            final String exceptionString = nonNull(exception) ? exception.getMessage()
                    : (nonNull(exceptionMessage) ? exceptionMessage : level.toString());

            WireField valueField;
            if (level == ERROR) {
                valueField = new WireField(channelName + SEPARATOR + PROP_ERROR,
                        TypedValues.newStringValue(exceptionString), ERROR);
            } else {
                valueField = new WireField(channelName, assetRecord.getValue(), INFO);
            }

            WireField assetProp = null;
            try {
                assetProp = new WireField(channelName + SEPARATOR + PROP_ASSET_NAME,
                        TypedValues.newStringValue(getConfiguration().getPid()), level);
            } catch (final KuraException e) {
                logger.error(ThrowableUtil.stackTraceAsString(e));
            }
            final WireField channelIdProp = new WireField(channelName + SEPARATOR + PROP_CHANNEL_ID,
                    TypedValues.newLongValue(channelId), level);
            final WireField timestampProp = new WireField(channelName + SEPARATOR + PROP_TIMESTAMP,
                    TypedValues.newLongValue(assetRecord.getTimestamp()), level);

            wireRecord = new WireRecord.Builder().addField(assetProp).addField(channelIdProp).addField(timestampProp)
                    .addField(valueField).build();
        }
        if (nonNull(wireRecord)) {
            this.wireSupport.emit(Arrays.asList(wireRecord));
        }
    }

    /** {@inheritDoc} */
    @Override
    protected String getFactoryPid() {
        return CONF_PID;
    }

    /**
     * This method is triggered as soon as the wire component receives a Wire
     * Envelope. After it receives a Wire Envelope, it checks for all associated
     * channels to read and write and perform the operations accordingly. The
     * order of executions are performed the following way:
     *
     * <ul>
     * <li>Perform all read operations on associated reading channels</li>
     * <li>Perform all write operations on associated writing channels</li>
     * <ul>
     *
     * Both of the aforementioned operations are performed as soon as it timer
     * wire component is also triggered.
     *
     * @param wireEnvelope
     *            the received wire envelope
     * @throws NullPointerException
     *             if Wire Envelope is null
     */
    @Override
    public void onWireReceive(final WireEnvelope wireEnvelope) {
        requireNonNull(wireEnvelope, message.wireEnvelopeNonNull());
        logger.debug(message.wireEnvelopeReceived() + this.wireSupport);

        // filtering list of wire records based on the provided severity level
        final List<WireRecord> records = this.wireSupport.filter(wireEnvelope.getRecords());
        final List<Long> channelIds = this.determineReadingChannels(records);
        final List<AssetRecord> assetRecordsToWriteChannels = this.determineWritingChannels(records);

        // perform the operations
        this.writeChannels(assetRecordsToWriteChannels);
        this.readChannels(channelIds);
    }

    /** {@inheritDoc} */
    @Override
    public Object polled(final Wire wire) {
        return this.wireSupport.polled(wire);
    }

    /**
     * Create an asset record from the provided channel information.
     *
     * @param channel
     *            the channel to get the values from
     * @param value
     *            the value
     * @return the asset record
     * @throws NullPointerException
     *             if any of the provided arguments is null
     */
    private AssetRecord prepareAssetRecord(final Channel channel, final TypedValue<?> value) {
        requireNonNull(channel, message.channelNonNull());
        requireNonNull(value, message.valueNonNull());

        final AssetRecord assetRecord = new AssetRecord(channel.getId());
        assetRecord.setValue(value);
        return assetRecord;
    }

    /** {@inheritDoc} */
    @Override
    public void producersConnected(final Wire[] wires) {
        this.wireSupport.producersConnected(wires);
    }

    /**
     * Perform Channel Read and Emit operations
     *
     * @param channelsToRead
     *            the list of {@link Channel} IDs
     * @throws NullPointerException
     *             if the provided list is null
     */
    private void readChannels(final List<Long> channelsToRead) {
        requireNonNull(channelsToRead, message.channelIdsNonNull());
        try {
            List<AssetRecord> recentlyReadRecords = null;
            if (!channelsToRead.isEmpty()) {
                recentlyReadRecords = this.read(channelsToRead);
            }
            if (recentlyReadRecords != null) {
                this.emitAssetRecords(recentlyReadRecords);
            }
        } catch (final KuraException e) {
            logger.error(message.errorPerformingRead() + ThrowableUtil.stackTraceAsString(e));
        }
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
     * OSGi service component callback while updation.
     *
     * @param properties
     *            the service properties
     */
    @Override
    public synchronized void updated(final Map<String, Object> properties) {
        logger.debug(message.updatingWireAsset());
        super.updated(properties);
        logger.debug(message.updatingWireAssetDone());
    }

    /** {@inheritDoc} */
    @Override
    public void updated(final Wire wire, final Object value) {
        this.wireSupport.updated(wire, value);
    }

    /**
     * Perform Channel Write operation
     *
     * @param assetRecordsToWriteChannels
     *            the list of {@link AssetRecord}s
     * @throws NullPointerException
     *             if the provided list is null
     */
    private void writeChannels(final List<AssetRecord> assetRecordsToWriteChannels) {
        requireNonNull(assetRecordsToWriteChannels, message.assetRecordsNonNull());
        try {
            if (!assetRecordsToWriteChannels.isEmpty()) {
                this.write(assetRecordsToWriteChannels);
            }
        } catch (final KuraException e) {
            logger.error(message.errorPerformingWrite() + ThrowableUtil.stackTraceAsString(e));
        }
    }

}
