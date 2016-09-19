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
package org.eclipse.kura.internal.asset.cloudlet;

import static org.eclipse.kura.Preconditions.checkNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.asset.AssetConfiguration;
import org.eclipse.kura.asset.AssetFlag;
import org.eclipse.kura.asset.AssetRecord;
import org.eclipse.kura.asset.AssetService;
import org.eclipse.kura.asset.AssetStatus;
import org.eclipse.kura.asset.Channel;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.cloud.Cloudlet;
import org.eclipse.kura.cloud.CloudletTopic;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.AssetCloudletMessages;
import org.eclipse.kura.message.KuraRequestPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class AssetCloudlet is used to provide MQTT read/write operations on the
 * asset. The application id is configured as {@code ASSET-CLOUDLET}.
 *
 * The available {@code GET} commands are as follows
 * <ul>
 * <li>/assets</li> : to retrieve all the assets
 * <li>/assets/asset_pid</li> : to retrieve all the channels of the provided
 * asset PID
 * <li>/assets/asset_pid/channel_id</li> : to retrieve the value of the
 * specified channel from the provided asset PID
 * <li>/assets/asset_pid/channel_id1,channel_id2,channel_id3</li> : to retrieve
 * the value of the several channels from the provided asset PID. Any number of
 * channels can be provided as well. Also note that {@code ","} delimiter must
 * be used to separate the channel IDs.
 * </ul>
 *
 * The available {@code PUT} commands are as follows
 * <ul>
 * <li>/assets/asset_pid/channel_id</li> : to write the provided {@code value}
 * in the payload to the specified channel of the provided asset PID. The
 * payload must also include the {@code type} of the {@code value} provided.
 * </ul>
 *
 * The {@code type} key in the request payload can be one of the following
 * (case-insensitive)
 * <ul>
 * <li>INTEGER</li>
 * <li>LONG</li>
 * <li>STRING</li>
 * <li>BOOLEAN</li>
 * <li>BYTE</li>
 * <li>SHORT</li>
 * <li>DOUBLE</li>
 * </ul>
 *
 * The {@code value} key in the request payload must contain the value to be
 * written
 *
 * @see Cloudlet
 * @see CloudClient
 * @see AssetCloudlet#doGet(CloudletTopic, KuraRequestPayload,
 *      KuraResponsePayload)
 * @see AssetCloudlet#doPut(CloudletTopic, KuraRequestPayload,
 *      KuraResponsePayload)
 */
public final class AssetCloudlet extends Cloudlet {

	/** Application Identifier for Cloudlet. */
	private static final String APP_ID = "ASSET-CLOUDLET";

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(AssetCloudlet.class);

	/** Localization Resource */
	private static final AssetCloudletMessages s_message = LocalizationAdapter.adapt(AssetCloudletMessages.class);

	/** The map of assets present in the OSGi service registry. */
	private Map<String, Asset> m_assets;

	/** The Asset Service dependency. */
	private volatile AssetService m_assetService;

	/** Asset Tracker Customizer */
	private AssetTrackerCustomizer m_assetTrackerCustomizer;

	/** Asset Tracker. */
	private ServiceTracker<Asset, Asset> m_serviceTracker;

	/**
	 * Instantiates a new asset cloudlet.
	 */
	public AssetCloudlet() {
		super(APP_ID);
	}

	/** {@inheritDoc} */
	@Override
	protected synchronized void activate(final ComponentContext componentContext) {
		s_logger.debug(s_message.activating());
		super.activate(componentContext);
		try {
			this.m_assetTrackerCustomizer = new AssetTrackerCustomizer(componentContext.getBundleContext(),
					this.m_assetService);
			this.m_serviceTracker = new ServiceTracker<Asset, Asset>(componentContext.getBundleContext(),
					Asset.class.getName(), this.m_assetTrackerCustomizer);
			this.m_serviceTracker.open();
		} catch (final InvalidSyntaxException e) {
			s_logger.error(s_message.activationFailed(e));
		}
		s_logger.debug(s_message.activatingDone());
	}

	/**
	 * Asset Service registration callback
	 *
	 * @param assetService
	 *            the asset service dependency
	 */
	protected synchronized void bindAssetService(final AssetService assetService) {
		if (this.m_assetService == null) {
			this.m_assetService = assetService;
		}
	}

	/**
	 * Cloud Service registration callback
	 *
	 * @param cloudService
	 *            the cloud service dependency
	 */
	protected synchronized void bindCloudService(final CloudService cloudService) {
		if (this.getCloudService() == null) {
			super.setCloudService(cloudService);
		}
	}

	/** {@inheritDoc} */
	@Override
	protected synchronized void deactivate(final ComponentContext componentContext) {
		s_logger.debug(s_message.deactivating());
		super.deactivate(componentContext);
		this.m_serviceTracker.close();
		s_logger.debug(s_message.deactivatingDone());
	}

	/** {@inheritDoc} */
	@Override
	protected void doGet(final CloudletTopic reqTopic, final KuraRequestPayload reqPayload,
			final KuraResponsePayload respPayload) {
		s_logger.info(s_message.cloudGETReqReceiving());
		if ("assets".equals(reqTopic.getResources()[0])) {
			// perform a search operation at the beginning
			this.findAssets();
			if (reqTopic.getResources().length == 1) {
				int i = 0;
				for (final Map.Entry<String, Asset> assetEntry : this.m_assets.entrySet()) {
					respPayload.addMetric("PID " + (++i), assetEntry.getKey());
				}
			}
			// Checks if the name of the asset is provided
			if (reqTopic.getResources().length == 2) {
				final String assetPid = reqTopic.getResources()[1];
				final Asset asset = this.m_assets.get(assetPid);
				final AssetConfiguration configuration = asset.getAssetConfiguration();
				final Map<Long, Channel> assetConfiguredChannels = configuration.getAssetChannels();
				for (final Map.Entry<Long, Channel> entry : assetConfiguredChannels.entrySet()) {
					final Channel channel = entry.getValue();
					respPayload.addMetric(String.valueOf(channel.getId()), channel.getName());
				}
			}
			// Checks if the name of the asset and the name of the channel are
			// provided
			if (reqTopic.getResources().length == 3) {
				final String assetPid = reqTopic.getResources()[1];
				final String channelId = reqTopic.getResources()[2];
				final String channelDelim = ",";
				Set<String> channelIds = null;
				if (channelId.contains(channelDelim)) {
					channelIds = CollectionUtil.newHashSet(Arrays.asList(channelId.split(channelDelim)));
					channelIds.removeAll(Collections.singleton(""));
				}
				final Asset asset = this.m_assets.get(assetPid);
				final AssetConfiguration configuration = asset.getAssetConfiguration();
				final Map<Long, Channel> assetConfiguredChannels = configuration.getAssetChannels();

				final List<Long> channelIdsToRead = CollectionUtil.newArrayList();
				long id;
				if (channelIds == null) {
					id = Long.parseLong(channelId);
					channelIdsToRead.add(id);
				} else {
					for (final String chId : channelIds) {
						id = Long.parseLong(chId);
						channelIdsToRead.add(id);
					}
				}
				if (assetConfiguredChannels != null) {
					List<AssetRecord> assetRecords = null;
					try {
						assetRecords = asset.read(channelIdsToRead);
					} catch (final KuraException e) {
						// if connection exception occurs
						respPayload.addMetric(s_message.errorMessage(), s_message.connectionException());
					}
					if (assetRecords != null) {
						this.prepareResponse(respPayload, assetRecords);
					}
				}
			}
		}
		s_logger.info(s_message.cloudGETReqReceived());
	}

	/** {@inheritDoc} */
	@Override
	protected void doPut(final CloudletTopic reqTopic, final KuraRequestPayload reqPayload,
			final KuraResponsePayload respPayload) {
		s_logger.info(s_message.cloudPUTReqReceiving());
		// Checks if the name of the asset and the name of the channel are
		// provided
		if ("assets".equals(reqTopic.getResources()[0]) && (reqTopic.getResources().length > 2)) {
			// perform a search operation at the beginning
			this.findAssets();
			final String assetPid = reqTopic.getResources()[1];
			final String channelId = reqTopic.getResources()[2];
			final Asset asset = this.m_assets.get(assetPid);
			final AssetConfiguration configuration = asset.getAssetConfiguration();
			final Map<Long, Channel> assetConfiguredChannels = configuration.getAssetChannels();
			final long id = Long.parseLong(channelId);
			if ((assetConfiguredChannels != null) && (id != 0)) {
				final AssetRecord assetRecord = new AssetRecord(id);
				final String userValue = (String) reqPayload.getMetric("value");
				final String userType = (String) reqPayload.getMetric("type");
				this.wrapValue(assetRecord, userValue, userType);

				List<AssetRecord> assetRecords = null;
				try {
					assetRecords = asset.write(Arrays.asList(assetRecord));
				} catch (final KuraException e) {
					// if connection exception occurs
					respPayload.addMetric(s_message.errorMessage(), s_message.connectionException());
				}
				if (assetRecords != null) {
					this.prepareResponse(respPayload, assetRecords);
				}
				if (assetRecords != null) {
					this.prepareResponse(respPayload, assetRecords);
				}
			}
		}
		s_logger.info(s_message.cloudPUTReqReceived());
	}

	/**
	 * Searches for all the currently available assets in the service registry
	 */
	private void findAssets() {
		this.m_assets = this.m_assetTrackerCustomizer.getRegisteredAssets();
	}

	/**
	 * Prepares the response payload based on the asset records as provided
	 *
	 * @param respPayload
	 *            the response payload to prepare
	 * @param assetRecords
	 *            the list of asset records
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null
	 */
	private void prepareResponse(final KuraResponsePayload respPayload, final List<AssetRecord> assetRecords) {
		checkNull(respPayload, s_message.respPayloadNonNull());
		checkNull(assetRecords, s_message.assetRecordsNonNull());

		for (final AssetRecord assetRecord : assetRecords) {
			final TypedValue<?> assetValue = assetRecord.getValue();
			final String value = (assetValue != null) ? String.valueOf(assetValue.getValue()) : "ERROR";
			String errorText;
			final AssetStatus assetStatus = assetRecord.getAssetStatus();
			final AssetFlag assetFlag = assetStatus.getAssetFlag();

			final String prefix = assetRecord.getChannelId() + ".";
			respPayload.addMetric(prefix + s_message.flag(), assetFlag.toString());
			respPayload.addMetric(prefix + s_message.channel(), assetRecord.getChannelId());
			respPayload.addMetric(prefix + s_message.timestamp(), assetRecord.getTimestamp());
			respPayload.addMetric(prefix + s_message.value(), value);

			if (assetFlag == AssetFlag.FAILURE) {
				final String exceptionMessage = assetStatus.getExceptionMessage();
				errorText = (exceptionMessage != null) ? exceptionMessage : "";
				respPayload.addMetric(prefix + s_message.errorMessage(), errorText);
			}
		}
	}

	/**
	 * Asset Service deregistration callback
	 *
	 * @param assetService
	 *            the asset service dependency
	 */
	protected synchronized void unbindAssetService(final AssetService assetService) {
		if (this.m_assetService == assetService) {
			this.m_assetService = null;
		}
	}

	/**
	 * Cloud Service deregistration callback
	 *
	 * @param cloudService
	 *            the cloud service dependency
	 */
	protected synchronized void unbindCloudService(final CloudService cloudService) {
		if (this.getCloudService() == cloudService) {
			super.unsetCloudService(cloudService);
		}
	}

	/**
	 * Wraps the provided user provided value to the an instance of
	 * {@link TypedValue} in the asset record
	 *
	 * @param assetRecord
	 *            the asset record to contain the typed value
	 * @param userValue
	 *            the value to wrap
	 * @param userType
	 *            the type to use
	 * @throws KuraRuntimeException
	 *             if any of the provided arguments is null
	 */
	private void wrapValue(final AssetRecord assetRecord, final String userValue, final String userType) {
		checkNull(assetRecord, s_message.assetRecordNonNull());
		checkNull(userValue, s_message.valueNonNull());
		checkNull(userType, s_message.typeNonNull());

		TypedValue<?> value = null;
		if ("INTEGER".equalsIgnoreCase(userType)) {
			value = TypedValues.newIntegerValue(Integer.parseInt(userValue));
		}
		if ("BOOLEAN".equalsIgnoreCase(userType)) {
			value = TypedValues.newBooleanValue(Boolean.parseBoolean(userValue));
		}
		if ("BYTE".equalsIgnoreCase(userType)) {
			value = TypedValues.newByteValue(Byte.parseByte(userValue));
		}
		if ("DOUBLE".equalsIgnoreCase(userType)) {
			value = TypedValues.newDoubleValue(Double.parseDouble(userValue));
		}
		if ("LONG".equalsIgnoreCase(userType)) {
			value = TypedValues.newLongValue(Long.parseLong(userValue));
		}
		if ("SHORT".equalsIgnoreCase(userType)) {
			value = TypedValues.newShortValue(Short.parseShort(userValue));
		}
		if ("STRING".equalsIgnoreCase(userType)) {
			value = TypedValues.newStringValue(userValue);
		}
		if (userValue != null) {
			assetRecord.setValue(value);
		}
	}

}