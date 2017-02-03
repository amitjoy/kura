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
package org.eclipse.kura.wire;

/**
 * The SeverityLevel Enumeration signifies a severity level of a {@link WireField}.
 * Depending on the configuration as provided by different Wire Components, the
 * Wire Components will choose the specific Wire Fields that will be processed.
 * <br/>
 * <br/>
 * Also note that, every {@link WireField} contains a severity level that signifies
 * the type of the data that the {@link WireField} contains.
 */
public enum SeverityLevel {

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
     * Typically {@link WireField}s of {@code INFO} severity level will be delegated to the connected
     * wire components
     */
    INFO

}
