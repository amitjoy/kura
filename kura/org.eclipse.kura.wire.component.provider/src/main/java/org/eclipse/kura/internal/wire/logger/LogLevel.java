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
package org.eclipse.kura.internal.wire.logger;

/**
 * This enumeration represents the allowed log levels as supported by the Logger Wire Component.
 */
enum LogLevel {
    DEBUG, INFO;

    public static LogLevel getLevel(final String levelName) {
        for (final LogLevel level : LogLevel.values()) {
            if (level.name().equalsIgnoreCase(levelName)) {
                return level;
            }
        }
        return null;
    }
}