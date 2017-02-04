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

import static java.util.Objects.requireNonNull;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.wire.WireRecord;

/**
 * The Class WireRecordCache is responsible to contain the {@link WireRecord} cached
 * values.
 */
final class WireRecordCache {

    /** Map that is the cache. */
    private static final Map<Long, List<WireRecord>> cacheMap = new WeakHashMap<>();

    /** Localization Resource. */
    private static final WireMessages message = LocalizationAdapter.adapt(WireMessages.class);

    /** Cache Maximum Capacity as configured. */
    private int capacity;

    /** Last refreshed time. */
    private Calendar lastRefreshedTime;

    /** DB Wire Record Filter instance. */
    private final DbWireRecordFilter recordFilter;

    /** Refresh duration in seconds. */
    private int refreshDuration;

    /**
     * Instantiates a new wire record cache.
     *
     * @param filter
     *            the DB Wire Record filter
     * @throws NullPointerException
     *             if argument is null
     */
    WireRecordCache(final DbWireRecordFilter filter) {
        requireNonNull(filter, message.dbFilterNonNull());
        this.recordFilter = filter;
    }

    /**
     * Returns the object in the map based on input key.
     *
     * @param key
     *            - key to get from cache map
     * @return object for the particular key
     */
    List<WireRecord> get(final long key) {
        if (this.refreshCache()) {
            cacheMap.put(this.lastRefreshedTime.getTimeInMillis(), this.recordFilter.filter());
        }
        return cacheMap.get(key);
    }

    /**
     * Gets the capacity.
     *
     * @return the capacity
     */
    public int getCapacity() {
        return this.capacity;
    }

    /**
     * Gets the last refreshed time.
     *
     * @return the last refreshed time
     */
    Calendar getLastRefreshedTime() {
        if (this.lastRefreshedTime == null) {
            this.lastRefreshedTime = Calendar.getInstance();
            return this.lastRefreshedTime;
        }
        return this.lastRefreshedTime;
    }

    /**
     * Gets the refresh duration.
     *
     * @return the refresh duration
     */
    int getRefreshDuration() {
        return this.refreshDuration;
    }

    /**
     * Puts the object to the key provided in the cache map.
     *
     * @param key
     *            - key to put in cache map
     * @param value
     *            - object for the key
     */
    void put(final long key, final List<WireRecord> value) {
        // clears the map if the size of the map is as same as the max size
        // expected
        if (cacheMap.size() == this.capacity) {
            cacheMap.clear();
        }
        cacheMap.put(key, value);
        this.lastRefreshedTime = Calendar.getInstance();
    }

    /**
     * Refreshes the Cache as per the cache duration set.
     *
     * @return true or false if cache is expired or not
     */
    private boolean refreshCache() {
        final Calendar now = Calendar.getInstance();
        final Calendar lastRefreshedTime = Calendar.getInstance(this.lastRefreshedTime.getTimeZone());
        lastRefreshedTime.setTime(this.lastRefreshedTime.getTime());
        lastRefreshedTime.add(Calendar.SECOND, this.refreshDuration);

        if (lastRefreshedTime.after(now)) {
            return false;
        } else {
            // Cache expired hence refresh it
            this.lastRefreshedTime = Calendar.getInstance();
            return true;
        }
    }

    /**
     * Sets the capacity.
     *
     * @param capacity
     *            the new capacity
     */
    public void setCapacity(final int capacity) {
        this.capacity = capacity;
    }

    /**
     * Sets the refresh duration.
     *
     * @param refreshDuration
     *            the new refresh duration
     */
    void setRefreshDuration(final int refreshDuration) {
        this.refreshDuration = refreshDuration;
    }

}