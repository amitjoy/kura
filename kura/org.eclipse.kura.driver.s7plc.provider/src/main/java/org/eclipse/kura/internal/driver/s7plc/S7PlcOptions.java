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
package org.eclipse.kura.internal.driver.s7plc;

import static org.eclipse.kura.Preconditions.checkNull;

import java.util.Map;

import org.eclipse.kura.driver.s7plc.localization.S7PlcMessages;
import org.eclipse.kura.localization.LocalizationAdapter;

/**
 * The Class S7PlcOptions is responsible to provide all the required
 * configurable options for the S7 PLC Driver.<br/>
 * <br/>
 *
 * The different properties to configure a S7 PLC Driver are as follows:
 * <ul>
 * <li>host.ip</li>
 * <li>host.port</li>
 * <li>rack</li>
 * <li>slot</li>
 * </ul>
 */
final class S7PlcOptions {

    /** S7 PLC IP */
    private static final String IP = "host.ip";

    /** S7 PLC Port */
    private static final String PORT = "host.port";

    /** S7 PLC Rack */
    private static final String RACK = "rack";

    /** Localization Resource. */
    private static final S7PlcMessages s_message = LocalizationAdapter.adapt(S7PlcMessages.class);

    /** S7 PLC Slot */
    private static final String SLOT = "slot";

    /** The properties as associated */
    private final Map<String, Object> properties;

    /**
     * Instantiates a new S7 PLC options.
     *
     * @param properties
     *            the properties
     */
    S7PlcOptions(final Map<String, Object> properties) {
        checkNull(properties, s_message.propertiesNonNull());
        this.properties = properties;
    }

    /**
     * Returns the S7 PLC Host IP
     *
     * @return the S7 PLC Host IP
     */
    String getIp() {
        String ipAddress = null;
        if ((this.properties != null) && this.properties.containsKey(IP) && (this.properties.get(IP) != null)) {
            ipAddress = this.properties.get(IP).toString();
        }
        return ipAddress;
    }

    /**
     * Returns S7 PLC Port
     *
     * @return the S7 PLC Port
     */
    int getPort() {
        int port = 0;
        if ((this.properties != null) && this.properties.containsKey(PORT) && (this.properties.get(PORT) != null)) {
            port = Integer.valueOf(this.properties.get(PORT).toString());
        }
        return port;
    }

    /**
     * Returns the S7 PLC Rack
     *
     * @return the S7 PLC Rack
     */
    int getRack() {
        int rack = 0;
        if ((this.properties != null) && this.properties.containsKey(RACK) && (this.properties.get(RACK) != null)) {
            rack = Integer.valueOf(this.properties.get(RACK).toString());
        }
        return rack;
    }

    /**
     * Returns the S7 PLC Slot
     *
     * @return the S7 PLC Slot
     */
    int getSlot() {
        int slot = 0;
        if ((this.properties != null) && this.properties.containsKey(SLOT) && (this.properties.get(SLOT) != null)) {
            slot = Integer.valueOf(this.properties.get(SLOT).toString());
        }
        return slot;
    }

}
