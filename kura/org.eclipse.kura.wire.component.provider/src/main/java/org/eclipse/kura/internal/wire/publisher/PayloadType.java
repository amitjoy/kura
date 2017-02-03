/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.internal.wire.publisher;

enum PayloadType {
    JSON(2), KURA_PAYLOAD(1);

    private int value;

    private PayloadType(final int value) {
        this.value = value;
    }

    public static PayloadType getPayloadType(final int payloadTypeInt) {
        for (final PayloadType tempPayloadType : PayloadType.values()) {
            if (tempPayloadType.getValue() == payloadTypeInt) {
                return tempPayloadType;
            }
        }
        return null;
    }

    public int getValue() {
        return this.value;
    }
}
