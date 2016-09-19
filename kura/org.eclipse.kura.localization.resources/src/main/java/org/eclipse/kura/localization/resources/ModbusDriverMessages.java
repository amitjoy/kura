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
package org.eclipse.kura.localization.resources;

import com.github.rodionmoiseev.c10n.annotations.En;

/**
 * ModbusDriverMessages is considered to be a localization resource for
 * {@code Modbus Driver} bundle. It contains all the necessary translations for
 * every string literals mentioned in {@code Modbus Driver} bundle.
 */
public interface ModbusDriverMessages {

	@En("Activating Modbus Driver.....")
	public String activating();

	@En("Activating Modbus Driver.....Done")
	public String activatingDone();

	@En("COILS")
	public String coils();

	@En("Connecting to Modbus RTU...")
	public String connectingRtu();

	@En("Connecting to Modbus RTU...Done")
	public String connectingRtuDone();

	@En("Connecting to Modbus TCP...")
	public String connectingTcp();

	@En("Connecting to Modbus TCP...Done")
	public String connectingTcpDone();

	@En("Connecting to Modbus UDP...")
	public String connectingUdp();

	@En("Connecting to Modbus UDP...Done")
	public String connectingUdpDone();

	@En("Unable to Connect...")
	public String connectionProblem();

	@En("Deactivating Modbus Driver.....")
	public String deactivating();

	@En("Deactivating Modbus Driver.....Done")
	public String deactivatingDone();

	@En("Disconnecting from Modbus RTU...")
	public String disconnectingRtu();

	@En("Disconnecting from Modbus RTU...Done")
	public String disconnectingRtuDone();

	@En("Disconnecting from Modbus TCP...")
	public String disconnectingTcp();

	@En("Disconnecting from Modbus TCP...Done")
	public String disconnectingTcpDone();

	@En("Disconnecting from Modbus UDP...")
	public String disconnectingUdp();

	@En("Disconnecting from Modbus UDP...Done")
	public String disconnectingUdpDone();

	@En("Unable to Disconnect...")
	public String disconnectionProblem();

	@En("DISCRETE_INPUTS")
	public String discreteInputs();

	@En("Error while disconnecting....")
	public String errorDisconnecting();

	@En("Error while retrieving Channel Configuration")
	public String errorRetrievingChannelConfiguration();

	@En("Error while retrieving Function Code")
	public String errorRetrievingFunctionCode();

	@En("Error while retrieving Memory Address")
	public String errorRetrievingMemAddr();

	@En("Error while retrieving Primary Table")
	public String errorRetrievingPrimaryTable();

	@En("Error while retrieving Unit ID")
	public String errorRetrievingUnitId();

	@En("Error while retrieving value type")
	public String errorRetrievingValueType();

	@En("Function Codes not in Range")
	public String functionCodesNotInRange();

	@En("HOLDING_REGISTERS")
	public String holdingRegs();

	@En("INPUT_REGISTERS")
	public String inputRegs();

	@En("memory.address")
	public String memoryAddr();

	@En("Address of the register (as integer value, not HEX)")
	public String memoryAddrDesc();

	@En("primary.table")
	public String primaryTable();

	@En("Modbus Primary Memory Address Space")
	public String primaryTableDesc();

	@En("Primary Table cannot be null")
	public String primaryTableNonNull();

	@En("Properties cannot be null")
	public String propertiesNonNull();

	@En("Driver Record cannot be null")
	public String recordNonNull();

	@En("Request type {0} is not supported")
	public String requestTypeNotSupported(int functionCode);

	@En("Modbus Response cannot be null")
	public String responseNonNull();

	@En("Modbus Transport cannot be null")
	public String transportNonNull();

	@En("unit.id")
	public String unitId();

	@En("Unit ID to connect to")
	public String unitIdDesc();

	@En("Updating Modbus Driver.....")
	public String updating();

	@En("Updating Modbus Driver.....Done")
	public String updatingDone();

	@En("Register address must a positive number greater than 0 but less than or equal to 65536")
	public String wrongRegister();

	@En("Unit ID must a positive number greater than 0 but less than or equal to 247")
	public String wrongUnitId();

}