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
package org.eclipse.kura.internal.wire.timer;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.util.Map;

import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;

/**
 * The Class TimerOptions is responsible to contain all the Timer related
 * configurable options
 */
final class TimerOptions {

    /** Localization Resource */
    private static final WireMessages message = LocalizationAdapter.adapt(WireMessages.class);

    /** The Constant denoting the interval property for the CRON expression */
    private static final String PROP_CRON_INTERVAL = "cron.interval";

    /** The Constant denoting the simple interval property from the metatype */
    private static final String PROP_SIMPLE_INTERVAL = "simple.interval";

    /** The Constant denoting the type of the interval */
    private static final String PROP_TYPE = "type";

    /** The properties as associated */
    private final Map<String, Object> properties;

    /**
     * Instantiates a new Timer options.
     *
     * @param properties
     *            the provided properties
     */
    TimerOptions(final Map<String, Object> properties) {
        requireNonNull(properties, message.propertiesNonNull());
        this.properties = properties;
    }

    /**
     * Returns the rate of refresh for this view.
     *
     * @return the CRON expression
     */
    String getCronExpression() {
        String expression = null;
        final Object interval = this.properties.get(PROP_CRON_INTERVAL);
        if (nonNull(interval) && (interval instanceof String)) {
            expression = (String) interval;
        }
        return expression;
    }

    /**
     * Returns the simple interval as configured.
     *
     * @return the simple interval
     */
    int getSimpleInterval() {
        int interval = 0;
        final Object simpleInterval = this.properties.get(PROP_SIMPLE_INTERVAL);
        if (nonNull(simpleInterval) && (simpleInterval instanceof Integer)) {
            interval = (Integer) simpleInterval;
        }
        return interval;
    }

    /**
     * Returns type as configured.
     *
     * @return the configured type
     */
    String getType() {
        String type = null;
        final Object timerType = this.properties.get(PROP_TYPE);
        if (nonNull(timerType) && (timerType instanceof String)) {
            type = (String) timerType;
        }
        return type;
    }

}