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
package org.eclipse.kura.driver.modbus.localization;

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

    @En("Channel Configuration cannot be null")
    public String channelConfigNonNull();

    @En("COILS")
    public String coils();

    @En("Connecting to Modbus...")
    public String connectingModbus();

    @En("Connecting to Modbus...Done")
    public String connectingModbusDone();

    @En("Unable to Connect...")
    public String connectionProblem();

    @En("Modbus Connector cannot be null")
    public String connectorNonNull();

    @En("Deactivating Modbus Driver.....")
    public String deactivating();

    @En("Deactivating Modbus Driver.....Done")
    public String deactivatingDone();

    @En("Disconnecting from Modbus...")
    public String disconnectingModbus();

    @En("Disconnecting from Modbus...Done")
    public String disconnectingModbusDone();

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

    @En("Provided Value Type is erroneous")
    public String wrongValueType();

}