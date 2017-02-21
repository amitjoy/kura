/**
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *   Eurotech
 *   Amit Kumar Mondal
 */
package org.eclipse.kura.internal.driver.modbus;

import static com.fazecast.jSerialComm.SerialPort.EVEN_PARITY;
import static com.fazecast.jSerialComm.SerialPort.FLOW_CONTROL_CTS_ENABLED;
import static com.fazecast.jSerialComm.SerialPort.FLOW_CONTROL_DISABLED;
import static com.fazecast.jSerialComm.SerialPort.FLOW_CONTROL_DSR_ENABLED;
import static com.fazecast.jSerialComm.SerialPort.FLOW_CONTROL_DTR_ENABLED;
import static com.fazecast.jSerialComm.SerialPort.FLOW_CONTROL_RTS_ENABLED;
import static com.fazecast.jSerialComm.SerialPort.FLOW_CONTROL_XONXOFF_IN_ENABLED;
import static com.fazecast.jSerialComm.SerialPort.FLOW_CONTROL_XONXOFF_OUT_ENABLED;
import static com.fazecast.jSerialComm.SerialPort.MARK_PARITY;
import static com.fazecast.jSerialComm.SerialPort.NO_PARITY;
import static com.fazecast.jSerialComm.SerialPort.ODD_PARITY;
import static com.fazecast.jSerialComm.SerialPort.ONE_POINT_FIVE_STOP_BITS;
import static com.fazecast.jSerialComm.SerialPort.ONE_STOP_BIT;
import static com.fazecast.jSerialComm.SerialPort.SPACE_PARITY;
import static com.fazecast.jSerialComm.SerialPort.TWO_STOP_BITS;
import static com.ghgande.j2mod.modbus.Modbus.SERIAL_ENCODING_ASCII;
import static com.ghgande.j2mod.modbus.Modbus.SERIAL_ENCODING_BIN;
import static com.ghgande.j2mod.modbus.Modbus.SERIAL_ENCODING_RTU;
import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.internal.driver.modbus.ModbusType.RTU;
import static org.eclipse.kura.internal.driver.modbus.ModbusType.TCP;
import static org.eclipse.kura.internal.driver.modbus.ModbusType.UDP;

import java.util.Map;

import org.eclipse.kura.driver.modbus.localization.ModbusDriverMessages;
import org.eclipse.kura.localization.LocalizationAdapter;

/**
 * The Class ModbusOptions is responsible to provide all the required
 * configurable options for the Modbus Driver. <br/>
 * <br/>
 *
 * The different properties to configure a Modbus Driver are as follows:
 * <ul>
 * <li>modbus.rtu.baudrate</li>
 * <li>modbus.rtu.databits</li>
 * <li>modbus.rtu.encoding</li>
 * <li>modbus.rtu.flowcontrolin</li> must be one of these:
 * FLOW_CONTROL_DISABLED, FLOW_CONTROL_RTS_ENABLED, FLOW_CONTROL_CTS_ENABLED,
 * FLOW_CONTROL_DSR_ENABLED, FLOW_CONTROL_DTR_ENABLED,
 * FLOW_CONTROL_XONXOFF_IN_ENABLED
 * <li>modbus.rtu.flowcontrolout</li> must be one of these:
 * FLOW_CONTROL_DISABLED, FLOW_CONTROL_RTS_ENABLED, FLOW_CONTROL_CTS_ENABLED,
 * FLOW_CONTROL_DSR_ENABLED, FLOW_CONTROL_DTR_ENABLED,
 * FLOW_CONTROL_XONXOFF_OUT_ENABLED
 * <li>modbus.tcp-udp.ip</li>
 * <li>modbus.rtu.parity</li> must be one of these: NO_PARITY, ODD_PARITY,
 * EVEN_PARITY, MARK_PARITY, SPACE_PARITY
 * <li>modbus.tcp-udp.port</li>
 * <li>modbus.rtu.stopbits</li>
 * <li>modbus.transaction.retry</li>
 * <li>modbus.timeout</li> in seconds
 * <li>access.type</li> must be on of these: TCP, UDP, RTU
 * </ul>
 */
final class ModbusOptions {

    /** Modbus Serial (RTU) access type Baudrate */
    private static final String BAUD_RATE = "modbus.rtu.baudrate";

    /** Modbus Serial (RTU) access type Databits */
    private static final String DATABITS = "modbus.rtu.databits";

    /** Modbus Serial (RTU) access type Encoding */
    private static final String ENCODING = "modbus.rtu.encoding";

    /** Modbus Serial (RTU) access type Flow Control In */
    private static final String FLOW_CONTROL_IN = "modbus.rtu.flowcontrolin";

    /** Modbus Serial (RTU) access type Flow Control Out */
    private static final String FLOW_CONTROL_OUT = "modbus.rtu.flowcontrolout";

    /** Modbus TCP or UDP access type configuration IP */
    private static final String IP = "modbus.tcp-udp.ip";

    /** Modbus Serial (RTU) access type Parity */
    private static final String PARITY = "modbus.rtu.parity";

    /** Modbus TCP or UDP access type configuration Port */
    private static final String PORT = "modbus.tcp-udp.port";

    /** Modbus Transaction no of retries */
    private static final String RETRY = "modbus.transaction.retry";

    /** Localization Resource. */
    private static final ModbusDriverMessages s_message = LocalizationAdapter.adapt(ModbusDriverMessages.class);

    /** Modbus RTU Serial configuration Port Name */
    private static final String SERIAL_PORT = "modbus.rtu.port.name";

    /** Modbus Serial (RTU) access type Stopbits */
    private static final String STOPBITS = "modbus.rtu.stopbits";

    /** Modbus Timeout */
    private static final String TIMEOUT = "modbus.timeout";

    /** Modbus TCP or UDP or RTU access type */
    private static final String TYPE = "access.type";

    /** The properties as associated */
    private final Map<String, Object> properties;

    /**
     * Instantiates a new Modbus options.
     *
     * @param properties
     *            the properties
     * @throws NullPointerException
     *             if the argument is null
     */
    ModbusOptions(final Map<String, Object> properties) {
        requireNonNull(properties, s_message.propertiesNonNull());
        this.properties = properties;
    }

    /**
     * Returns the Modbus RTU Baudrate
     *
     * @return the Modbus RTU Baudrate
     */
    int getBaudrate() {
        int baudRate = 0;
        final Object rate = this.properties.get(BAUD_RATE);
        if ((rate != null) && (rate instanceof Integer)) {
            baudRate = Integer.valueOf(rate.toString());
        }
        return baudRate;
    }

    /**
     * Returns the Modbus RTU Databits
     *
     * @return the Modbus RTU Databits
     */
    int getDatabits() {
        int databits = 0;
        final Object bits = this.properties.get(DATABITS);
        if ((bits != null) && (bits instanceof Integer)) {
            databits = Integer.valueOf(bits.toString());
        }
        return databits;
    }

    /**
     * Returns the Modbus RTU Encoding
     *
     * @return the Modbus RTU Encoding
     */
    String getEncoding() {
        String encoding = null;
        final Object enc = this.properties.get(ENCODING);
        if (enc != null) {
            encoding = enc.toString();
        }
        if (encoding != null) {
            if ("SERIAL_ENCODING_ASCII".equalsIgnoreCase(encoding)) {
                return SERIAL_ENCODING_ASCII;
            }
            if ("SERIAL_ENCODING_RTU".equalsIgnoreCase(encoding)) {
                return SERIAL_ENCODING_RTU;
            }
            if ("SERIAL_ENCODING_BIN".equalsIgnoreCase(encoding)) {
                return SERIAL_ENCODING_BIN;
            }
        }
        return null;
    }

    /**
     * Returns the Modbus RTU Flow Control In
     *
     * @return the Modbus RTU Flow Control In
     */
    int getFlowControlIn() {
        String flowControlIn = null;
        final Object controlIn = this.properties.get(FLOW_CONTROL_IN);
        if (controlIn != null) {
            flowControlIn = controlIn.toString();
        }
        if (flowControlIn != null) {
            if ("FLOW_CONTROL_DISABLED".equalsIgnoreCase(flowControlIn)) {
                return FLOW_CONTROL_DISABLED;
            }
            if ("FLOW_CONTROL_RTS_ENABLED".equalsIgnoreCase(flowControlIn)) {
                return FLOW_CONTROL_RTS_ENABLED;
            }
            if ("FLOW_CONTROL_CTS_ENABLED".equalsIgnoreCase(flowControlIn)) {
                return FLOW_CONTROL_CTS_ENABLED;
            }
            if ("FLOW_CONTROL_DSR_ENABLED".equalsIgnoreCase(flowControlIn)) {
                return FLOW_CONTROL_DSR_ENABLED;
            }
            if ("FLOW_CONTROL_DTR_ENABLED".equalsIgnoreCase(flowControlIn)) {
                return FLOW_CONTROL_DTR_ENABLED;
            }
            if ("FLOW_CONTROL_XONXOFF_IN_ENABLED".equalsIgnoreCase(flowControlIn)) {
                return FLOW_CONTROL_XONXOFF_IN_ENABLED;
            }
        }
        return 0;
    }

    /**
     * Returns the Modbus RTU Flow Control Out
     *
     * @return the Modbus RTU Flow Control Out
     */
    int getFlowControlOut() {
        String flowControlOut = null;
        final Object controlOut = this.properties.get(FLOW_CONTROL_OUT);
        if (controlOut != null) {
            flowControlOut = controlOut.toString();
        }
        if (flowControlOut != null) {
            if ("FLOW_CONTROL_DISABLED".equalsIgnoreCase(flowControlOut)) {
                return FLOW_CONTROL_DISABLED;
            }
            if ("FLOW_CONTROL_RTS_ENABLED".equalsIgnoreCase(flowControlOut)) {
                return FLOW_CONTROL_RTS_ENABLED;
            }
            if ("FLOW_CONTROL_CTS_ENABLED".equalsIgnoreCase(flowControlOut)) {
                return FLOW_CONTROL_CTS_ENABLED;
            }
            if ("FLOW_CONTROL_DSR_ENABLED".equalsIgnoreCase(flowControlOut)) {
                return FLOW_CONTROL_DSR_ENABLED;
            }
            if ("FLOW_CONTROL_DTR_ENABLED".equalsIgnoreCase(flowControlOut)) {
                return FLOW_CONTROL_DTR_ENABLED;
            }
            if ("FLOW_CONTROL_XONXOFF_OUT_ENABLED".equalsIgnoreCase(flowControlOut)) {
                return FLOW_CONTROL_XONXOFF_OUT_ENABLED;
            }
        }
        return 0;
    }

    /**
     * Returns the Modbus TCP or UDP IP
     *
     * @return the Modbus TCP or UDP IP
     */
    String getIp() {
        String ipAddress = null;
        final Object ip = this.properties.get(IP);
        if (ip != null) {
            ipAddress = ip.toString();
        }
        return ipAddress;
    }

    /**
     * Returns Modbus Transaction No of Retries
     *
     * @return the Modbus Transaction No of Retries
     */
    int getNoOfRetry() {
        int retry = 0;
        final Object retryNo = this.properties.get(RETRY);
        if ((retryNo != null) && (retryNo instanceof Integer)) {
            retry = Integer.valueOf(retryNo.toString());
        }
        return retry;
    }

    /**
     * Returns the Modbus RTU Parity
     *
     * @return the Modbus RTU Parity
     */
    int getParity() {
        String parity = null;
        final Object parityCheck = this.properties.get(PARITY);
        if (parityCheck != null) {
            parity = parityCheck.toString();
        }
        if (parity != null) {
            if ("NO_PARITY".equalsIgnoreCase(parity)) {
                return NO_PARITY;
            }
            if ("ODD_PARITY".equalsIgnoreCase(parity)) {
                return ODD_PARITY;
            }
            if ("EVEN_PARITY".equalsIgnoreCase(parity)) {
                return EVEN_PARITY;
            }
            if ("MARK_PARITY".equalsIgnoreCase(parity)) {
                return MARK_PARITY;
            }
            if ("SPACE_PARITY".equalsIgnoreCase(parity)) {
                return SPACE_PARITY;
            }
        }
        return 0;
    }

    /**
     * Returns Modbus TCP or UDP Port
     *
     * @return the Modbus TCP or UDP Port
     */
    int getPort() {
        int port = 0;
        final Object endpointPort = this.properties.get(PORT);
        if ((endpointPort != null) && (endpointPort instanceof Integer)) {
            port = Integer.valueOf(endpointPort.toString());
        }
        return port;

    }

    /**
     * Returns Modbus RTU Port Name
     *
     * @return the Modbus RTU Port Name
     */
    String getRtuPortName() {
        String port = null;
        final Object endpointPort = this.properties.get(SERIAL_PORT);
        if (endpointPort != null) {
            port = endpointPort.toString();
        }
        return port;
    }

    /**
     * Returns the Modbus RTU Stopbits
     *
     * @return the Modbus RTU Stopbits
     */
    int getStopbits() {
        String stopbits = null;
        final Object stopbit = this.properties.get(STOPBITS);
        if (stopbit != null) {
            stopbits = stopbit.toString();
        }
        if (stopbits != null) {
            if ("ONE_STOP_BIT".equalsIgnoreCase(stopbits)) {
                return ONE_STOP_BIT;
            }
            if ("ONE_POINT_FIVE_STOP_BITS".equalsIgnoreCase(stopbits)) {
                return ONE_POINT_FIVE_STOP_BITS;
            }
            if ("TWO_STOP_BITS".equalsIgnoreCase(stopbits)) {
                return TWO_STOP_BITS;
            }
        }
        return 0;
    }

    /**
     * Returns Modbus Timeout In Milliseconds
     *
     * @return the Modbus Timeout in Milliseconds
     */
    int getTimeout() {
        int timeout = 0;
        final Object timeoutSec = this.properties.get(TIMEOUT);
        if ((timeoutSec != null) && (timeoutSec instanceof Integer)) {
            timeout = Integer.valueOf(timeoutSec.toString());
        }
        return timeout * 1000;
    }

    /**
     * Returns the type of the Modbus Access
     *
     * @return the Modbus Access Type
     */
    ModbusType getType() {
        String messageType = null;
        final Object type = this.properties.get(TYPE);
        if ((type != null) && (type instanceof String)) {
            messageType = (String) type;
        }
        if (messageType != null) {
            if ("TCP".equalsIgnoreCase(messageType)) {
                return TCP;
            }
            if ("UDP".equalsIgnoreCase(messageType)) {
                return UDP;
            }
            if ("RTU".equalsIgnoreCase(messageType)) {
                return RTU;
            }
        }
        return null;
    }

}
