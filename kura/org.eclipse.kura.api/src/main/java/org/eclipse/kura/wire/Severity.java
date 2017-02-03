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
package org.eclipse.kura.wire;

/**
 * The {@link Severity} enumeration signifies an association with every {@link WireField}
 * that denotes the type of the data that the {@link WireField} contains.
 * <br/>
 * <br/>
 * For example, a recently read data from a measurement point of a device can be represented
 * as {@code INFO} data if the channel read operation becomes successful and hence there
 * exists a proper associated value that denotes the measurement.
 * <br/>
 * <br/>
 * And there also exists situations when such channel operations do not succeed due to several
 * reasons, such as channel malconfiguration, operation timeout, interface busy etc. Such
 * channel operations result in data to be represented as {@code ERROR}.
 * <br/>
 * <br/>
 * Also note that, depending on the configuration as provided by different Wire Components,
 * the Wire Components can choose the specific type of {@link WireField} that can be processed by
 * the Wire Component. That is, every Wire Component can choose whether it intends to process
 * only {@code INFO} data or only {@code ERROR} data or all data including {@code INFO}
 * and {@code ERROR}.
 *
 * @see WireField
 * @see WireRecord
 * @see WireEnvelope
 */
public enum Severity implements SeverityLevel {

    /**
     * {@code ERROR} is a severity level indicating a serious failure or exception that is <b>CRITICAL</b>.
     * <p>
     * In general {@link WireField}s of {@code ERROR} severity level should describe data that are
     * of most importance and which will prevent normal program execution. They should
     * be reasonably intelligible to end users and to system administrators.
     * <p>
     * {@link WireField}s of {@code ERROR} severity level also signifies that critical condition has been
     * occurred and needs proper attention.
     */
    ERROR,

    /**
     * {@code INFO} is a severity level for informational messages and hence only denotes proper value.
     * <p>
     * Typically {@link WireField}s of {@code INFO} severity level should be used for significant messages
     * that will make sense to end users and system administrators and also associate a legible value.
     */
    INFO

}
