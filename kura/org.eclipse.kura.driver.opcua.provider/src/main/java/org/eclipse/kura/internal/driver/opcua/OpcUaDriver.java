/**
 * Copyright (c) 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.internal.driver.opcua;

import static org.eclipse.kura.Preconditions.checkNull;
import static org.eclipse.kura.driver.DriverConstants.CHANNEL_VALUE_TYPE;
import static org.eclipse.kura.driver.DriverFlag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE;
import static org.eclipse.kura.driver.DriverFlag.DRIVER_ERROR_CHANNEL_VALUE_TYPE_CONVERSION_EXCEPTION;
import static org.eclipse.kura.driver.DriverFlag.READ_FAILURE;
import static org.eclipse.kura.driver.DriverFlag.READ_SUCCESSFUL;
import static org.eclipse.kura.driver.DriverFlag.WRITE_FAILURE;
import static org.eclipse.kura.driver.DriverFlag.WRITE_SUCCESSFUL;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.DriverFlag;
import org.eclipse.kura.driver.DriverRecord;
import org.eclipse.kura.driver.DriverStatus;
import org.eclipse.kura.driver.listener.DriverListener;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.OpcUaMessages;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.util.base.ThrowableUtil;
import org.eclipse.kura.util.base.TypeUtil;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.api.nodes.attached.UaVariableNode;
import org.eclipse.milo.opcua.stack.client.UaTcpStackClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class OpcUaDriver is a OPC-UA Driver implementation for Kura Asset-Driver
 * Topology. Currently it only supports reading and writing from/to a specific
 * node. As of now, it doesn't support method execution or history read.<br/>
 * <br/>
 *
 * This OPC-UA Driver can be used in cooperation with Kura Asset Model and in
 * isolation as well. In case of isolation, the properties needs to be provided
 * externally.<br/>
 * <br/>
 *
 * The required properties are enlisted in {@link OpcUaChannelDescriptor} and
 * the driver connection specific properties are enlisted in
 * {@link OpcUaOptions}
 *
 * @see OpcUaChannelDescriptor
 * @see OpcUaOptions
 */
public final class OpcUaDriver implements Driver {

	/** Node Identifier Property */
	private static final String NODE_ID = "node.id";

	/** Node Namespace Index Property */
	private static final String NODE_NAMESPACE_INDEX = "node.namespace.index";

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(OpcUaDriver.class);

	/** Localization Resource. */
	private static final OpcUaMessages s_message = LocalizationAdapter.adapt(OpcUaMessages.class);

	/** OPC-UA Client Connector */
	private OpcUaClient client;

	/** flag to check if the driver is connected. */
	private boolean isConnected;

	/** OPC-UA Configuration Options. */
	private OpcUaOptions options;

	/**
	 * OSGi service component callback while activation.
	 *
	 * @param componentContext
	 *            the component context
	 * @param properties
	 *            the service properties
	 */
	protected synchronized void activate(final ComponentContext componentContext,
			final Map<String, Object> properties) {
		s_logger.debug(s_message.activating());
		this.extractProperties(properties);
		s_logger.debug(s_message.activatingDone());
	}

	/** {@inheritDoc} */
	@Override
	public void connect() throws ConnectionException {
		try {
			s_logger.debug(s_message.connecting());
			final String endPoint = new StringBuilder().append("opc.tcp://").append(this.options.getIp()).append(":")
					.append(this.options.getPort()).append("/").append(this.options.getServerName()).toString();
			final EndpointDescription[] endpoints = UaTcpStackClient.getEndpoints(endPoint).get();
			final EndpointDescription endpoint = Arrays.stream(endpoints).filter(
					e -> e.getSecurityPolicyUri().equals(this.options.getSecurityPolicy().getSecurityPolicyUri()))
					.findFirst().orElseThrow(() -> new ConnectionException(s_message.connectionProblem()));
			final KeyStoreLoader loader = new KeyStoreLoader(this.options.getKeystoreType(),
					this.options.getKeystoreClientAlias(), this.options.getKeystoreServerAlias(),
					this.options.getKeystorePassword(), this.options.getApplicationCertificate());
			final OpcUaClientConfig clientConfig = OpcUaClientConfig.builder().setEndpoint(endpoint)
					.setApplicationName(LocalizedText.english(this.options.getApplicationName()))
					.setApplicationUri(this.options.getApplicationUri())
					.setRequestTimeout(UInteger.valueOf(this.options.getRequestTimeout()))
					.setSessionTimeout(UInteger.valueOf(this.options.getSessionTimeout()))
					.setIdentityProvider(this.options.getIdentityProvider()).setKeyPair(loader.getClientKeyPair())
					.setCertificate(loader.getClientCertificate()).build();
			this.client = new OpcUaClient(clientConfig);
			this.isConnected = true;
			s_logger.debug(s_message.connectingDone());
		} catch (final Exception e) {
			s_logger.debug(s_message.disconnectionProblem() + ThrowableUtil.stackTraceAsString(e));
			throw new ConnectionException(e);
		}

	}

	/**
	 * OSGi service component callback while deactivation.
	 *
	 * @param componentContext
	 *            the component context
	 */
	protected synchronized void deactivate(final ComponentContext componentContext) {
		s_logger.debug(s_message.deactivating());
		try {
			this.disconnect();
		} catch (final ConnectionException e) {
			s_logger.error(s_message.errorDisconnecting() + ThrowableUtil.stackTraceAsString(e));
		}
		this.client = null;
		s_logger.debug(s_message.deactivatingDone());
	}

	/** {@inheritDoc} */
	@Override
	public void disconnect() throws ConnectionException {
		if (this.isConnected) {
			s_logger.debug(s_message.disconnecting());
			this.client.disconnect();
			this.isConnected = false;
			s_logger.debug(s_message.disconnectingDone());
		}
	}

	/**
	 * Extract the OPC-UA specific configurations from the provided properties.
	 *
	 * @param properties
	 *            the provided properties to parse
	 */
	private void extractProperties(final Map<String, Object> properties) {
		checkNull(properties, s_message.propertiesNonNull());
		this.options = new OpcUaOptions(properties);
	}

	/** {@inheritDoc} */
	@Override
	public ChannelDescriptor getChannelDescriptor() {
		return new OpcUaChannelDescriptor();
	}

	/**
	 * Returns the wrapped Typed Value instance based on the provided value
	 *
	 * @param value
	 *            the provided value to wrap
	 * @param record
	 *            the driver record to check the expected value type
	 * @return the TypedValue instance
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null
	 */
	private TypedValue<?> getTypedValue(final Object value, final DriverRecord record) {
		checkNull(value, s_message.valueNonNull());
		checkNull(record, s_message.recordNonNull());

		final DataType expectedValueType = (DataType) record.getChannelConfig().get(CHANNEL_VALUE_TYPE.value());

		try {
			switch (expectedValueType) {
			case LONG:
				return TypedValues.newLongValue(Long.parseLong(value.toString()));
			case SHORT:
				return TypedValues.newShortValue(Short.parseShort(value.toString()));
			case DOUBLE:
				return TypedValues.newDoubleValue(Double.parseDouble(value.toString()));
			case INTEGER:
				return TypedValues.newIntegerValue(Integer.parseInt(value.toString()));
			case BYTE:
				return TypedValues.newByteValue(Byte.parseByte(value.toString()));
			case BOOLEAN:
				return TypedValues.newBooleanValue(Boolean.parseBoolean(value.toString()));
			case STRING:
				return TypedValues.newStringValue(value.toString());
			case BYTE_ARRAY:
				try {
					return TypedValues.newByteArrayValue(TypeUtil.objectToByteArray(value));
				} catch (final IOException e) {
					s_logger.error(ThrowableUtil.stackTraceAsString(e));
					return null;
				}
			default:
				return null;
			}
		} catch (final NumberFormatException nfe) {
			return null;
		}
	}

	/** {@inheritDoc} */
	@Override
	public List<DriverRecord> read(final List<DriverRecord> records) throws ConnectionException {
		if (!this.isConnected) {
			this.connect();
		}
		for (final DriverRecord record : records) {
			// check if the channel type configuration is provided
			final Map<String, Object> channelConfig = record.getChannelConfig();
			if (!channelConfig.containsKey(CHANNEL_VALUE_TYPE.value())) {
				record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_VALUE_TYPE_CONVERSION_EXCEPTION,
						s_message.errorRetrievingValueType(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			// check if the node ID configuration is provided
			if (!channelConfig.containsKey(NODE_ID)) {
				record.setDriverStatus(
						new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE, s_message.errorRetrievingNodeId(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			// check if the node namespace index configuration is provided
			if (!channelConfig.containsKey(NODE_NAMESPACE_INDEX)) {
				record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE,
						s_message.errorRetrievingNodeNamespace(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			int nodeNamespaceIndex;
			try {
				nodeNamespaceIndex = Integer.parseInt(channelConfig.get(NODE_NAMESPACE_INDEX).toString());
			} catch (final NumberFormatException nfe) {
				record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE,
						s_message.errorRetrievingNodeNamespace(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			final NodeId nodeId = new NodeId(nodeNamespaceIndex, channelConfig.get(NODE_ID).toString());
			final UaVariableNode node = this.client.getAddressSpace().getVariableNode(nodeId);
			Object value = null;
			try {
				value = node.readValueAttribute().get();
			} catch (final Exception e) {
				record.setDriverStatus(new DriverStatus(READ_FAILURE, s_message.readFailed(), e));
				record.setTimestamp(System.currentTimeMillis());
			}
			if (value != null) {
				final TypedValue<?> typedValue = this.getTypedValue(value, record);
				if (typedValue == null) {
					record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_VALUE_TYPE_CONVERSION_EXCEPTION,
							s_message.errorValueTypeConversion(), null));
					record.setTimestamp(System.currentTimeMillis());
					continue;
				}
				record.setValue(typedValue);
				record.setDriverStatus(new DriverStatus(READ_SUCCESSFUL));
				record.setTimestamp(System.currentTimeMillis());
			} else {
				record.setDriverStatus(new DriverStatus(DriverFlag.CUSTOM_ERROR_0, s_message.valueNull(), null));
				record.setTimestamp(System.currentTimeMillis());
			}
		}
		return records;
	}

	/** {@inheritDoc} */
	@Override
	public void registerDriverListener(final Map<String, Object> channelConfig, final DriverListener listener)
			throws ConnectionException {
		throw new KuraRuntimeException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
	}

	/** {@inheritDoc} */
	@Override
	public void unregisterDriverListener(final DriverListener listener) throws ConnectionException {
		throw new KuraRuntimeException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
	}

	/**
	 * OSGi service component callback while updating.
	 *
	 * @param properties
	 *            the properties
	 */
	public synchronized void updated(final Map<String, Object> properties) {
		s_logger.debug(s_message.updating());
		this.extractProperties(properties);
		s_logger.debug(s_message.updatingDone());
	}

	/** {@inheritDoc} */
	@Override
	public List<DriverRecord> write(final List<DriverRecord> records) throws ConnectionException {
		if (!this.isConnected) {
			this.connect();
		}
		for (final DriverRecord record : records) {
			// check if the channel type configuration is provided
			final Map<String, Object> channelConfig = record.getChannelConfig();
			if (!channelConfig.containsKey(CHANNEL_VALUE_TYPE.value())) {
				record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_VALUE_TYPE_CONVERSION_EXCEPTION,
						s_message.errorRetrievingValueType(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			// check if the unit ID configuration is provided
			if (!channelConfig.containsKey(NODE_ID)) {
				record.setDriverStatus(
						new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE, s_message.errorRetrievingNodeId(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			// check if the node namespace index configuration is provided
			if (!channelConfig.containsKey(NODE_NAMESPACE_INDEX)) {
				record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE,
						s_message.errorRetrievingNodeNamespace(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			int nodeNamespaceIndex;
			try {
				nodeNamespaceIndex = Integer.parseInt(channelConfig.get(NODE_NAMESPACE_INDEX).toString());
			} catch (final NumberFormatException nfe) {
				record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE,
						s_message.errorRetrievingNodeNamespace(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			final TypedValue<?> value = record.getValue();
			final NodeId nodeId = new NodeId(nodeNamespaceIndex, channelConfig.get(NODE_ID).toString());
			final UaVariableNode node = this.client.getAddressSpace().getVariableNode(nodeId);
			final DataValue newValue = new DataValue(new Variant(value.getValue()));
			try {
				node.writeValue(newValue).get();
				record.setDriverStatus(new DriverStatus(WRITE_SUCCESSFUL));
			} catch (final Exception e) {
				record.setDriverStatus(new DriverStatus(WRITE_FAILURE, s_message.writeFailed(), e));
			}
			record.setTimestamp(System.currentTimeMillis());
		}
		return records;
	}

}
