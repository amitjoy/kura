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
package org.eclipse.kura.internal.wire.store;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.internal.wire.store.DbWireRecordStore.PREFIX;

import java.util.Map;

import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;

/**
 * The Class DbWireRecordStoreOptions is responsible to contain all the DB Wire
 * Record Store related options
 */
final class DbWireRecordStoreOptions {

    /** Localization Resource */
    private static final WireMessages message = LocalizationAdapter.adapt(WireMessages.class);

    /** The Constant denotes the period as configured for periodic cleanup. */
    private static final String PERIODIC_CLEANUP_ID = "periodic.cleanup";

    /** The Constant denotes the number of records in the table to keep. */
    private static final String PERIODIC_CLEANUP_RECORDS_ID = "periodic.cleanup.records.keep";

    /** The Constant denotes the name of the table to perform operations on. */
    private static final String TABLE_NAME = "table.name";

    /** The properties as associated */
    private final Map<String, Object> properties;

    /**
     * Instantiates a new DB wire record store options.
     *
     * @param properties
     *            the configured properties
     */
    DbWireRecordStoreOptions(final Map<String, Object> properties) {
        requireNonNull(properties, message.propertiesNonNull());
        this.properties = properties;
    }

    /**
     * Returns the number of records to keep as configured .
     *
     * @return the number of records
     */
    int getNoOfRecordsToKeep() {
        int noOfRecords = 0;
        final Object cleanUp = this.properties.get(PERIODIC_CLEANUP_RECORDS_ID);
        if (nonNull(cleanUp) && (cleanUp instanceof Integer)) {
            noOfRecords = (Integer) cleanUp;
        }
        return noOfRecords;
    }

    /**
     * Returns the period as configured for the periodic cleanup.
     *
     * @return the period
     */
    int getPeriodicCleanupRate() {
        int period = 0;
        final Object rate = this.properties.get(PERIODIC_CLEANUP_ID);
        if (nonNull(rate) && (rate instanceof Integer)) {
            period = (Integer) rate;
        }
        return period;
    }

    /**
     * Returns the name of the table as configured.
     *
     * @return the name of the table
     */
    String getTableName() {
        String tableName = null;
        final Object name = this.properties.get(TABLE_NAME);
        if (nonNull(tableName) && (name instanceof String)) {
            tableName = name.toString();
        }
        return PREFIX + tableName;
    }

}