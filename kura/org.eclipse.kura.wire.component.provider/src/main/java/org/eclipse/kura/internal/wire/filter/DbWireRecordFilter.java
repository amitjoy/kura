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
package org.eclipse.kura.internal.wire.filter;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.wire.Severity.INFO;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.db.DbService;
import org.eclipse.kura.internal.wire.common.DbServiceHelper;
import org.eclipse.kura.internal.wire.store.DbDataTypeMapper;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireField;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class DbWireRecordFilter is responsible for representing a wire component
 * which is mainly used to filter records as received from the wire record
 */
public final class DbWireRecordFilter implements WireEmitter, WireReceiver, ConfigurableComponent {

    /** The Logger instance. */
    private static final Logger logger = LoggerFactory.getLogger(DbWireRecordFilter.class);

    /** Localization Resource */
    private static final WireMessages message = LocalizationAdapter.adapt(WireMessages.class);

    /** Cache container to store values of SQL view wire records */
    private final WireRecordCache cache;

    /** DB Utility Helper */
    private DbServiceHelper dbHelper;

    /** The DB Service dependency. */
    private volatile DbService dbService;

    /** Scheduled Executor Service */
    private final ScheduledExecutorService executorService;

    /** The DB Filter Options. */
    private DbWireRecordFilterOptions options;

    /** The future handle of the thread pool executor service. */
    private ScheduledFuture<?> tickHandle;

    /** The Wire Helper Service. */
    private volatile WireHelperService wireHelperService;

    /** The Wire Supporter component. */
    private WireSupport wireSupport;

    /** Constructor */
    public DbWireRecordFilter() {
        this.executorService = Executors.newSingleThreadScheduledExecutor();
        this.cache = new WireRecordCache(this);
    }

    /**
     * OSGi service component callback for deactivation
     *
     * @param componentContext
     *            the component context
     * @param properties
     *            the properties
     */
    protected synchronized void activate(final ComponentContext componentContext,
            final Map<String, Object> properties) {
        logger.debug(message.activatingFilter());
        this.options = new DbWireRecordFilterOptions(properties);
        this.dbHelper = DbServiceHelper.getInstance(this.dbService);
        this.wireSupport = this.wireHelperService.newWireSupport(this);
        this.scheduleRefresh();
        logger.debug(message.activatingFilterDone());
    }

    /**
     * Binds the DB service.
     *
     * @param dbService
     *            the new DB service
     */
    public synchronized void bindDbService(final DbService dbService) {
        if (this.dbService == null) {
            this.dbService = dbService;
        }
    }

    /**
     * Binds the Wire Helper Service.
     *
     * @param wireHelperService
     *            the new Wire Helper Service
     */
    public synchronized void bindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == null) {
            this.wireHelperService = wireHelperService;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void consumersConnected(final Wire[] wires) {
        this.wireSupport.consumersConnected(wires);
    }

    /**
     * OSGi service component callback for deactivation
     *
     * @param componentContext
     *            the component context
     */
    protected synchronized void deactivate(final ComponentContext componentContext) {
        logger.debug(message.deactivatingFilter());
        if (this.tickHandle != null) {
            this.tickHandle.cancel(true);
        }
        this.executorService.shutdown();
        logger.debug(message.deactivatingFilterDone());
    }

    /**
     * Filters the database records based on the provided query
     *
     * @return the filtered records
     */
    synchronized List<WireRecord> filter() {
        logger.debug(message.filteringStarted());
        try {
            return this.refreshSQLView();
        } catch (final SQLException e) {
            logger.error(message.errorFiltering(), e);
        }
        return Collections.emptyList();
    }

    /**
     * Trigger emitting data as soon as new wire envelope is received. This
     * retrieves the last updated value from the cache if the time difference
     * between the current time and the last cache updated time is less than the
     * configured cache interval. If it is more than the aforementioned time
     * difference, then retrieve the value from the cache using current time as
     * a key. This will actually result in a cache miss. Every cache miss will
     * internally be handled by {@link WireRecordCache} in such a way that
     * whenever a cache miss occurs it will load the value from the DB.
     */
    @Override
    public synchronized void onWireReceive(final WireEnvelope wireEnvelope) {
        requireNonNull(wireEnvelope, message.wireEnvelopeNonNull());
        logger.debug(message.wireEnvelopeReceived(), wireEnvelope);
        this.wireSupport.emit(this.cache.get(this.cache.getLastRefreshedTime().getTimeInMillis()));
    }

    /** {@inheritDoc} */
    @Override
    public Object polled(final Wire wire) {
        return this.wireSupport.polled(wire);
    }

    /** {@inheritDoc} */
    @Override
    public void producersConnected(final Wire[] wires) {
        this.wireSupport.producersConnected(wires);
    }

    /**
     * Refreshes the SQL view
     */
    private List<WireRecord> refreshSQLView() throws SQLException {
        final List<WireRecord> wireRecords = CollectionUtil.newArrayList();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rset = null;
        final String sqlView = this.options.getSqlView();

        try {
            conn = this.dbHelper.getConnection();
            stmt = conn.createStatement();
            rset = stmt.executeQuery(sqlView);

            if (nonNull(rset)) {
                while (rset.next()) {
                    final Set<WireField> wireFields = CollectionUtil.newHashSet();
                    final ResultSetMetaData rmet = rset.getMetaData();
                    for (int i = 1; i <= rmet.getColumnCount(); i++) {
                        String fieldName = rmet.getColumnLabel(i);

                        if (isNull(fieldName)) {
                            fieldName = rmet.getColumnName(i);
                        }

                        WireField wireField = null;
                        final int jdbcType = rmet.getColumnType(i);
                        final DataType dataType = DbDataTypeMapper.getDataType(jdbcType);

                        switch (dataType) {
                        case BOOLEAN:
                            final boolean boolValue = rset.getBoolean(i);
                            wireField = new WireField(fieldName, TypedValues.newBooleanValue(boolValue), INFO);
                            break;
                        case BYTE:
                            final byte byteValue = rset.getByte(i);
                            wireField = new WireField(fieldName, TypedValues.newByteValue(byteValue), INFO);
                            break;
                        case DOUBLE:
                            final double doubleValue = rset.getDouble(i);
                            wireField = new WireField(fieldName, TypedValues.newDoubleValue(doubleValue), INFO);
                            break;
                        case INTEGER:
                            final int intValue = rset.getInt(i);
                            wireField = new WireField(fieldName, TypedValues.newIntegerValue(intValue), INFO);
                            break;
                        case LONG:
                            final long longValue = rset.getLong(i);
                            wireField = new WireField(fieldName, TypedValues.newLongValue(longValue), INFO);
                            break;
                        case BYTE_ARRAY:
                            final byte[] bytesValue = rset.getBytes(i);
                            wireField = new WireField(fieldName, TypedValues.newByteArrayValue(bytesValue), INFO);
                            break;
                        case SHORT:
                            final short shortValue = rset.getShort(i);
                            wireField = new WireField(fieldName, TypedValues.newShortValue(shortValue), INFO);
                            break;
                        case STRING:
                            final String stringValue = rset.getString(i);
                            wireField = new WireField(fieldName, TypedValues.newStringValue(stringValue), INFO);
                            break;
                        default:
                            break;
                        }
                        wireFields.add(wireField);
                    }
                    final WireRecord wireRecord = new WireRecord.Builder().addFields(wireFields).build();
                    wireRecords.add(wireRecord);
                }
            }
            logger.info(message.refreshed());
        } catch (final SQLException e) {
            throw e;
        } finally {
            this.dbHelper.close(rset);
            this.dbHelper.close(stmt);
            this.dbHelper.close(conn);
        }
        return wireRecords;
    }

    /**
     * Schedule refresh of SQL view operation
     */
    private void scheduleRefresh() {
        final int refreshRate = this.options.getRefreshRate();
        this.cache.setRefreshDuration(refreshRate);
        this.cache.setCapacity(this.options.getCacheCapacity());
        // Cancel the current refresh view handle
        if (this.tickHandle != null) {
            this.tickHandle.cancel(true);
        }
        // schedule the new refresh view
        if (refreshRate != 0) {
            this.tickHandle = this.executorService.schedule(() -> DbWireRecordFilter.this.cache
                    .put(System.currentTimeMillis(), DbWireRecordFilter.this.filter()), refreshRate, TimeUnit.SECONDS);
        }
    }

    /**
     * Unbinds DB service.
     *
     * @param dbService
     *            the DB service
     */
    public synchronized void unbindDbService(final DbService dbService) {
        if (this.dbService == dbService) {
            this.dbService = null;
        }
    }

    /**
     * Unbinds the Wire Helper Service.
     *
     * @param wireHelperService
     *            the new Wire Helper Service
     */
    public synchronized void unbindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == wireHelperService) {
            this.wireHelperService = null;
        }
    }

    /**
     * OSGi service component callback for updating
     *
     * @param properties
     *            the updated properties
     */
    public synchronized void updated(final Map<String, Object> properties) {
        logger.debug(message.updatingFilter(), properties);
        this.options = new DbWireRecordFilterOptions(properties);
        this.scheduleRefresh();
        logger.debug(message.updatingFilterDone());
    }

    /** {@inheritDoc} */
    @Override
    public void updated(final Wire wire, final Object value) {
        this.wireSupport.updated(wire, value);
    }
}
