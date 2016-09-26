/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.internal.driver.modbus;

import static com.ghgande.j2mod.modbus.Modbus.READ_COILS;
import static com.ghgande.j2mod.modbus.Modbus.READ_HOLDING_REGISTERS;
import static com.ghgande.j2mod.modbus.Modbus.READ_INPUT_DISCRETES;
import static com.ghgande.j2mod.modbus.Modbus.READ_INPUT_REGISTERS;
import static com.ghgande.j2mod.modbus.Modbus.WRITE_COIL;
import static com.ghgande.j2mod.modbus.Modbus.WRITE_MULTIPLE_COILS;
import static com.ghgande.j2mod.modbus.Modbus.WRITE_MULTIPLE_REGISTERS;
import static com.ghgande.j2mod.modbus.Modbus.WRITE_SINGLE_REGISTER;
import static org.eclipse.kura.Preconditions.checkNull;
import static org.eclipse.kura.driver.DriverConstants.CHANNEL_VALUE_TYPE;
import static org.eclipse.kura.driver.DriverFlag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE;
import static org.eclipse.kura.driver.DriverFlag.DRIVER_ERROR_CHANNEL_VALUE_TYPE_CONVERSION_EXCEPTION;
import static org.eclipse.kura.driver.DriverFlag.READ_FAILURE;
import static org.eclipse.kura.driver.DriverFlag.READ_SUCCESSFUL;
import static org.eclipse.kura.driver.DriverFlag.WRITE_FAILURE;
import static org.eclipse.kura.driver.DriverFlag.WRITE_SUCCESSFUL;
import static org.eclipse.kura.type.DataType.BOOLEAN;
import static org.eclipse.kura.type.DataType.INTEGER;

import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.DriverRecord;
import org.eclipse.kura.driver.DriverStatus;
import org.eclipse.kura.driver.listener.DriverListener;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.ModbusDriverMessages;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.util.base.ThrowableUtil;
import org.eclipse.kura.util.base.TypeUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.facade.AbstractModbusMaster;
import com.ghgande.j2mod.modbus.facade.ModbusSerialMaster;
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;
import com.ghgande.j2mod.modbus.facade.ModbusUDPMaster;
import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import com.ghgande.j2mod.modbus.util.BitVector;
import com.ghgande.j2mod.modbus.util.SerialParameters;

/**
 * The Class ModbusDriver is a Modbus Driver implementation for Kura
 * Asset-Driver Topology. This Modbus Driver needs specific properties to be
 * provided externally. <br/>
 * <br/>
 *
 * This Modbus Driver can be used in cooperation with Kura Asset Model and in
 * isolation as well. In case of isolation, the properties needs to be provided
 * externally.<br/>
 * <br/>
 *
 * The required channel specific properties are enlisted in
 * {@link ModbusChannelDescriptor} and the driver connection specific properties
 * are enlisted in {@link ModbusOptions}
 *
 * @see ModbusChannelDescriptor
 * @see ModbusOptions
 */
public final class ModbusDriver implements Driver {

	/** Modbus Memory Address Property */
	private static final String MEMORY_ADDRESS = "memory.address";

	/** Modbus Memory Address Space Property */
	private static final String PRIMARY_TABLE = "primary.table";

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(ModbusDriver.class);

	/** Localization Resource. */
	private static final ModbusDriverMessages s_message = LocalizationAdapter.adapt(ModbusDriverMessages.class);

	/** Modbus Unit Identifier Property */
	private static final String UNIT_ID = "unit.id";

	/** flag to check if the driver is connected. */
	private boolean isConnected;

	/** Modbus RTU Connection. */
	private AbstractModbusMaster modbusMaster;

	/** Modbus Configuration Options. */
	private ModbusOptions options;

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
		s_logger.debug(s_message.connectingModbus());
		try {
			this.modbusMaster.connect();
		} catch (final Exception e) {
			s_logger.error(s_message.connectionProblem() + ThrowableUtil.stackTraceAsString(e));
			throw new ConnectionException(s_message.connectionProblem() + ThrowableUtil.stackTraceAsString(e));
		}
		s_logger.debug(s_message.connectingModbusDone());
		this.isConnected = true;
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
		s_logger.debug(s_message.deactivatingDone());
	}

	/** {@inheritDoc} */
	@Override
	public void disconnect() throws ConnectionException {
		this.options.getType();
		if (this.isConnected) {
			s_logger.debug(s_message.disconnectingModbus());
			try {
				this.modbusMaster.disconnect();
			} catch (final Exception e) {
				s_logger.error(s_message.disconnectionProblem() + ThrowableUtil.stackTraceAsString(e));
				throw new ConnectionException(s_message.disconnectionProblem() + ThrowableUtil.stackTraceAsString(e));
			}
			s_logger.debug(s_message.disconnectingModbusDone());
			this.isConnected = false;
		}
	}

	/**
	 * Extract the Modbus specific configurations from the provided properties.
	 *
	 * @param properties
	 *            the provided properties to parse
	 */
	private void extractProperties(final Map<String, Object> properties) {
		checkNull(properties, s_message.propertiesNonNull());
		this.options = new ModbusOptions(properties);
		switch (this.options.getType()) {
		case TCP:
			this.modbusMaster = new ModbusTCPMaster(this.options.getIp(), this.options.getPort());
			break;
		case UDP:
			this.modbusMaster = new ModbusUDPMaster(this.options.getIp(), this.options.getPort());
			break;
		case RTU:
			final SerialParameters parameters = new SerialParameters(this.options.getRtuPortName(),
					this.options.getBaudrate(), this.options.getFlowControlIn(), this.options.getFlowControlOut(),
					this.options.getDatabits(), this.options.getStopbits(), this.options.getParity(), false);
			parameters.setEncoding(this.options.getEncoding());
			this.modbusMaster = new ModbusSerialMaster(parameters);
			break;
		default:
			break;
		}
	}

	/** {@inheritDoc} */
	@Override
	public ChannelDescriptor getChannelDescriptor() {
		return new ModbusChannelDescriptor();
	}

	/**
	 * Get the integer value of the provided read function code.
	 *
	 * @param primaryTable
	 *            the string representation of the address space
	 * @return the function code
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	private int getReadFunctionCode(final String primaryTable) {
		checkNull(primaryTable, s_message.primaryTableNonNull());
		if ("COILS".equalsIgnoreCase(primaryTable)) {
			return READ_COILS;
		}
		if ("DISCRETE_INPUTS".equalsIgnoreCase(primaryTable)) {
			return READ_INPUT_DISCRETES;
		}
		if ("INPUT_REGISTERS".equalsIgnoreCase(primaryTable)) {
			return READ_INPUT_REGISTERS;
		}
		if ("HOLDING_REGISTERS".equalsIgnoreCase(primaryTable)) {
			return READ_HOLDING_REGISTERS;
		}
		return 0;
	}

	/**
	 * Gets the Typed value as found in the provided response
	 *
	 * @param response
	 *            the provided Modbus response
	 * @param record
	 *            the driver record to check the expected value type
	 * @return the value
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null
	 */
	private TypedValue<?> getValue(final Object response, final DriverRecord record) {
		checkNull(response, s_message.responseNonNull());
		checkNull(record, s_message.recordNonNull());

		final Map<String, Object> channelConfig = record.getChannelConfig();
		final DataType expectedValueType = (DataType) channelConfig.get(CHANNEL_VALUE_TYPE.value());
		if (response instanceof BitVector) {
			final boolean registerValue = ((BitVector) response).getBit(0);
			return TypedValues.newBooleanValue(registerValue);
		}
		if (response instanceof InputRegister[]) {
			final int registerValue = ((InputRegister[]) response)[0].getValue();
			switch (expectedValueType) {
			case LONG:
				return TypedValues.newLongValue(registerValue);
			case DOUBLE:
				return TypedValues.newDoubleValue(registerValue);
			case INTEGER:
				return TypedValues.newIntegerValue(registerValue);
			case STRING:
				return TypedValues.newStringValue(Integer.toString(registerValue));
			case BYTE_ARRAY:
				return TypedValues.newByteArrayValue(TypeUtil.intToBytes(registerValue));
			default:
				return null;
			}
		}
		return null;
	}

	/**
	 * Get the integer value of the provided write function code.
	 *
	 * @param primaryTable
	 *            the string representation of the address space
	 * @param isMultipleWrite
	 *            the flag denoting if multiple write operations are allowed
	 * @return the function code
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	private int getWriteFunctionCode(final String primaryTable, final boolean isMultipleWrite) {
		checkNull(primaryTable, s_message.primaryTableNonNull());
		if ("COILS".equalsIgnoreCase(primaryTable)) {
			return isMultipleWrite ? WRITE_MULTIPLE_COILS : WRITE_COIL;
		}
		if ("HOLDING_REGISTERS".equalsIgnoreCase(primaryTable)) {
			return isMultipleWrite ? WRITE_MULTIPLE_REGISTERS : WRITE_SINGLE_REGISTER;
		}
		return 0;
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
			// check if the unit ID configuration is provided
			if (!channelConfig.containsKey(UNIT_ID)) {
				record.setDriverStatus(
						new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE, s_message.errorRetrievingUnitId(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			// check if the primary table configuration is provided
			if (!channelConfig.containsKey(PRIMARY_TABLE)) {
				record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE,
						s_message.errorRetrievingPrimaryTable(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			// check if the memory address configuration is provided
			if (!channelConfig.containsKey(MEMORY_ADDRESS)) {
				record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE,
						s_message.errorRetrievingMemAddr(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			int unitId;
			int memoryAddr;
			try {
				unitId = Integer.parseInt(channelConfig.get(UNIT_ID).toString());
				memoryAddr = Integer.parseInt(channelConfig.get(MEMORY_ADDRESS).toString()) - 1;
			} catch (final NumberFormatException nfe) {
				record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE,
						s_message.errorRetrievingMemAddr(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			final String primaryTable = channelConfig.get(PRIMARY_TABLE).toString();
			final int functionCode = this.getReadFunctionCode(primaryTable);

			// check if the function code is correct
			if (functionCode == 0) {
				record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE,
						s_message.errorRetrievingFunctionCode(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			// check if the unit id is correct
			if ((unitId < 1) || (unitId > 247)) {
				record.setDriverStatus(
						new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE, s_message.wrongUnitId(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			// check if the memory address is correct
			if ((memoryAddr < 0) || (memoryAddr > 65535)) {
				record.setDriverStatus(
						new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE, s_message.wrongRegister(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			try {
				// always read single register
				final Object response = this.readRequest(unitId, this.modbusMaster, functionCode, memoryAddr, 1);
				final TypedValue<?> value = this.getValue(response, record);
				if (value == null) {
					record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE,
							s_message.errorRetrievingValueType(), null));
					record.setTimestamp(System.currentTimeMillis());
					continue;
				}
				record.setValue(value);
				record.setDriverStatus(new DriverStatus(READ_SUCCESSFUL));
			} catch (final ModbusException e) {
				record.setDriverStatus(new DriverStatus(READ_FAILURE, null, e));
			} catch (final KuraRuntimeException e) {
				record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE,
						s_message.connectorNonNull() + " OR " + s_message.wrongUnitId(), e));
			}
			record.setTimestamp(System.currentTimeMillis());
		}
		this.disconnect();
		return records;
	}

	/**
	 * Executes a read transaction using the function code, register and count.
	 *
	 * @param unitId
	 *            the Unit ID to connect
	 * @param modbusMaster
	 *            the Modbus Master instance
	 * @param functionCode
	 *            Function code to use
	 * @param register
	 *            Register number
	 * @param count
	 *            Number of registers
	 * @return Response object
	 * @throws ModbusException
	 *             the Modbus exception
	 * @throws KuraRuntimeException
	 *             if the transport is null
	 */
	private synchronized Object readRequest(final int unitId, final AbstractModbusMaster modbusMaster,
			final int functionCode, final int register, final int count) throws ModbusException {
		checkNull(modbusMaster, s_message.connectorNonNull());
		switch (functionCode) {
		case READ_COILS:
			BitVector coilResponseVector = null;
			if (modbusMaster instanceof ModbusSerialMaster) {
				coilResponseVector = ((ModbusSerialMaster) modbusMaster).readCoils(unitId, register, count);
			}
			if (modbusMaster instanceof ModbusTCPMaster) {
				coilResponseVector = ((ModbusTCPMaster) modbusMaster).readCoils(unitId, register, count);
			}
			if (modbusMaster instanceof ModbusUDPMaster) {
				coilResponseVector = ((ModbusUDPMaster) modbusMaster).readCoils(unitId, register, count);
			}
			return coilResponseVector;
		case READ_INPUT_DISCRETES:
			BitVector discreteInputResponseVector = null;
			if (modbusMaster instanceof ModbusSerialMaster) {
				discreteInputResponseVector = ((ModbusSerialMaster) modbusMaster).readInputDiscretes(unitId, register,
						count);
			}
			if (modbusMaster instanceof ModbusTCPMaster) {
				discreteInputResponseVector = ((ModbusTCPMaster) modbusMaster).readInputDiscretes(unitId, register,
						count);
			}
			if (modbusMaster instanceof ModbusUDPMaster) {
				discreteInputResponseVector = ((ModbusUDPMaster) modbusMaster).readInputDiscretes(unitId, register,
						count);
			}
			return discreteInputResponseVector;
		case READ_INPUT_REGISTERS:
			InputRegister[] inputRegisters = null;
			if (modbusMaster instanceof ModbusSerialMaster) {
				inputRegisters = ((ModbusSerialMaster) modbusMaster).readInputRegisters(unitId, register, count);
			}
			if (modbusMaster instanceof ModbusTCPMaster) {
				inputRegisters = ((ModbusTCPMaster) modbusMaster).readInputRegisters(unitId, register, count);
			}
			if (modbusMaster instanceof ModbusUDPMaster) {
				inputRegisters = ((ModbusUDPMaster) modbusMaster).readInputRegisters(unitId, register, count);
			}
			return inputRegisters;
		case READ_HOLDING_REGISTERS:
			InputRegister[] multipleRegisters = null;
			if (modbusMaster instanceof ModbusSerialMaster) {
				multipleRegisters = ((ModbusSerialMaster) modbusMaster).readMultipleRegisters(unitId, register, count);
			}
			if (modbusMaster instanceof ModbusTCPMaster) {
				multipleRegisters = ((ModbusTCPMaster) modbusMaster).readMultipleRegisters(unitId, register, count);
			}
			if (modbusMaster instanceof ModbusUDPMaster) {
				multipleRegisters = ((ModbusUDPMaster) modbusMaster).readMultipleRegisters(unitId, register, count);
			}
			return multipleRegisters;
		default:
			throw new ModbusException(s_message.requestTypeNotSupported(functionCode));
		}
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
			if (!channelConfig.containsKey(UNIT_ID)) {
				record.setDriverStatus(
						new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE, s_message.errorRetrievingUnitId(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			// check if the primary table configuration is provided
			if (!channelConfig.containsKey(PRIMARY_TABLE)) {
				record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE,
						s_message.errorRetrievingPrimaryTable(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			// check if the memory address configuration is provided
			if (!channelConfig.containsKey(MEMORY_ADDRESS)) {
				record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE,
						s_message.errorRetrievingMemAddr(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			int unitId;
			int memoryAddr;
			try {
				unitId = Integer.parseInt(channelConfig.get(UNIT_ID).toString());
				memoryAddr = Integer.parseInt(channelConfig.get(MEMORY_ADDRESS).toString()) - 1;
			} catch (final NumberFormatException nfe) {
				record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE,
						s_message.errorRetrievingMemAddr(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			final String primaryTable = channelConfig.get(PRIMARY_TABLE).toString();
			final int functionCode = this.getWriteFunctionCode(primaryTable, false);
			// check if the function code is correct
			if (functionCode == 0) {
				record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE,
						s_message.errorRetrievingFunctionCode(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			// check if the unit id is correct
			if ((unitId < 1) || (unitId > 247)) {
				record.setDriverStatus(
						new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE, s_message.wrongUnitId(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			// check if the memory address is correct
			if ((memoryAddr < 0) || (memoryAddr > 65535)) {
				record.setDriverStatus(
						new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE, s_message.wrongRegister(), null));
				record.setTimestamp(System.currentTimeMillis());
				continue;
			}
			try {
				final DataType expectedValueType = (DataType) channelConfig.get(CHANNEL_VALUE_TYPE.value());
				Object valueToWrite = null;
				if ((expectedValueType != INTEGER) && (expectedValueType != BOOLEAN)) {
					record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_VALUE_TYPE_CONVERSION_EXCEPTION,
							s_message.errorRetrievingValueType(), null));
					record.setTimestamp(System.currentTimeMillis());
					continue;
				}
				if (expectedValueType == INTEGER) {
					try {
						valueToWrite = Integer.parseInt(record.getValue().getValue().toString());
					} catch (final NumberFormatException nfe) {
						record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_VALUE_TYPE_CONVERSION_EXCEPTION,
								s_message.errorRetrievingValueType(), null));
						record.setTimestamp(System.currentTimeMillis());
						continue;
					}
				}
				if (expectedValueType == BOOLEAN) {
					try {
						valueToWrite = Boolean.parseBoolean(record.getValue().getValue().toString());
					} catch (final NumberFormatException nfe) {
						record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_VALUE_TYPE_CONVERSION_EXCEPTION,
								s_message.errorRetrievingValueType(), null));
						record.setTimestamp(System.currentTimeMillis());
						continue;
					}
				}
				this.writeRequest(unitId, this.modbusMaster, functionCode, memoryAddr, valueToWrite);
				record.setDriverStatus(new DriverStatus(WRITE_SUCCESSFUL));
			} catch (final ModbusException e) {
				record.setDriverStatus(new DriverStatus(WRITE_FAILURE, null, e));
			} catch (final KuraRuntimeException e) {
				record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE,
						s_message.connectorNonNull() + " OR " + s_message.wrongUnitId(), e));
			} catch (final NumberFormatException nfe) {
				record.setDriverStatus(new DriverStatus(DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE,
						s_message.errorRetrievingValueType(), nfe));
			}
			record.setTimestamp(System.currentTimeMillis());
		}
		this.disconnect();
		return records;
	}

	/**
	 * Executes a write transaction using the function code, register and value
	 *
	 * @param unitId
	 *            the Unit ID to connect
	 * @param modbusMaster
	 *            the Modbus Master instance
	 * @param functionCode
	 *            Function code to use
	 * @param register
	 *            Register number
	 * @param values
	 *            Values to apply
	 * @throws KuraRuntimeException
	 *             if the transport is null
	 * @throws ModbusException
	 *             if the function code provided is not supported for write
	 *             operation
	 */
	private synchronized void writeRequest(final int unitId, final AbstractModbusMaster modbusMaster,
			final int functionCode, final int register, final Object... values) throws ModbusException {
		checkNull(modbusMaster, s_message.connectorNonNull());
		switch (functionCode) {
		case WRITE_COIL:
			if (modbusMaster instanceof ModbusSerialMaster) {
				((ModbusSerialMaster) modbusMaster).writeCoil(unitId, register, Boolean.valueOf(values[0].toString()));
			}
			if (modbusMaster instanceof ModbusTCPMaster) {
				((ModbusTCPMaster) modbusMaster).writeCoil(unitId, register, Boolean.valueOf(values[0].toString()));
			}
			if (modbusMaster instanceof ModbusUDPMaster) {
				((ModbusUDPMaster) modbusMaster).writeCoil(unitId, register, Boolean.valueOf(values[0].toString()));
			}
			break;
		case WRITE_SINGLE_REGISTER:
			if (modbusMaster instanceof ModbusSerialMaster) {
				((ModbusSerialMaster) modbusMaster).writeSingleRegister(unitId, register,
						new SimpleRegister(Integer.valueOf(values[0].toString())));
			}
			if (modbusMaster instanceof ModbusTCPMaster) {
				((ModbusTCPMaster) modbusMaster).writeSingleRegister(unitId, register,
						new SimpleRegister(Integer.valueOf(values[0].toString())));
			}
			if (modbusMaster instanceof ModbusUDPMaster) {
				((ModbusUDPMaster) modbusMaster).writeSingleRegister(unitId, register,
						new SimpleRegister(Integer.valueOf(values[0].toString())));
			}
			break;
		case WRITE_MULTIPLE_COILS:
			final BitVector bitVectorMultipleCoil = new BitVector(values.length);
			for (int i = 0; i < bitVectorMultipleCoil.size(); i++) {
				bitVectorMultipleCoil.setBit(i, Boolean.valueOf(values[i].toString()));
			}
			if (modbusMaster instanceof ModbusSerialMaster) {
				((ModbusSerialMaster) modbusMaster).writeMultipleCoils(unitId, register, bitVectorMultipleCoil);
			}
			if (modbusMaster instanceof ModbusTCPMaster) {
				((ModbusTCPMaster) modbusMaster).writeMultipleCoils(unitId, register, bitVectorMultipleCoil);
			}
			if (modbusMaster instanceof ModbusUDPMaster) {
				((ModbusUDPMaster) modbusMaster).writeMultipleCoils(unitId, register, bitVectorMultipleCoil);
			}
			break;
		case WRITE_MULTIPLE_REGISTERS:
			final Register[] registers = new Register[values.length];
			for (int i = 0; i < registers.length; i++) {
				registers[i] = new SimpleRegister(Integer.valueOf(values[i].toString()));
			}
			if (modbusMaster instanceof ModbusSerialMaster) {
				((ModbusSerialMaster) modbusMaster).writeMultipleRegisters(unitId, register, registers);
			}
			if (modbusMaster instanceof ModbusTCPMaster) {
				((ModbusTCPMaster) modbusMaster).writeMultipleRegisters(unitId, register, registers);
			}
			if (modbusMaster instanceof ModbusUDPMaster) {
				((ModbusUDPMaster) modbusMaster).writeMultipleRegisters(unitId, register, registers);
			}
			break;
		default:
			throw new ModbusException(s_message.requestTypeNotSupported(functionCode));
		}
	}

}