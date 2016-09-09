/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.internal.wire.filter;

import java.util.Map;

import org.eclipse.kura.wire.SeverityLevel;

/**
 * The Class DbWireRecordFilterOptions is responsible to contain all the Db Wire
 * Record related filter options
 */
final class DbWireRecordFilterOptions {

	/** The Constant denotes the cache max capacity. */
	private static final String CONF_CACHE_CAPACITY = "cache.max.capacity";

	/** The Constant denotes the cache update interval. */
	private static final String CONF_CACHE_INTERVAL = "cache.update.interval";

	/** The Constant denotes the refresh rate. */
	private static final String CONF_REFRESH_RATE = "refresh.rate";

	/** The Constant denotes SQL view. */
	private static final String CONF_SQL_VIEW = "sql.view";

	/** The Constant denoting severity level. */
	private static final String SEVERITY_LEVEL = "severity.level";

	/** The properties as associated */
	private final Map<String, Object> m_properties;

	/**
	 * Instantiates a new DB wire record filter options.
	 *
	 * @param properties
	 *            the provided properties
	 */
	DbWireRecordFilterOptions(final Map<String, Object> properties) {
		this.m_properties = properties;
	}

	/**
	 * Returns the cache max capacity as configured.
	 *
	 * @return the configured cache max capacity
	 */
	int getCacheCapacity() {
		int cacheSize = 0;
		final Object cacheCapacity = this.m_properties.get(CONF_CACHE_CAPACITY);
		if ((this.m_properties != null) && (this.m_properties.containsKey(CONF_CACHE_CAPACITY))
				&& (cacheCapacity instanceof String)) {
			cacheSize = (Integer) cacheCapacity;
		}
		return cacheSize;
	}

	/**
	 * Returns the cache interval as configured.
	 *
	 * @return the configured cache interval
	 */
	int getCacheInterval() {
		int cacheInterval = 0;
		final Object cacheInt = this.m_properties.get(CONF_CACHE_INTERVAL);
		if ((this.m_properties != null) && (this.m_properties.containsKey(CONF_CACHE_INTERVAL))
				&& (cacheInt instanceof String)) {
			cacheInterval = (Integer) cacheInt;
		}
		return cacheInterval;
	}

	/**
	 * Returns the rate of refresh for this view.
	 *
	 * @return the refresh rate
	 */
	int getRefreshRate() {
		int refreshRate = 0;
		final Object cacheRefreshRate = this.m_properties.get(CONF_REFRESH_RATE);
		if ((this.m_properties != null) && (this.m_properties.containsKey(CONF_REFRESH_RATE))
				&& (cacheRefreshRate instanceof Integer)) {
			refreshRate = (Integer) cacheRefreshRate;
		}
		return refreshRate;
	}

	/**
	 * Returns the severity level of accepted wire fields.
	 *
	 * @return the severity level
	 */
	SeverityLevel getSeverityLevel() {
		String severityLevel = "ERROR";
		final Object level = this.m_properties.get(SEVERITY_LEVEL);
		if ((this.m_properties != null) && this.m_properties.containsKey(SEVERITY_LEVEL) && (level != null)
				&& (level instanceof String)) {
			severityLevel = String.valueOf(level);
		}
		if ("ERROR".equalsIgnoreCase(severityLevel)) {
			return SeverityLevel.ERROR;
		}
		if ("INFO".equalsIgnoreCase(severityLevel)) {
			return SeverityLevel.INFO;
		}
		if ("CONFIG".equalsIgnoreCase(severityLevel)) {
			return SeverityLevel.CONFIG;
		}
		return SeverityLevel.ERROR;
	}

	/**
	 * Returns the SQL to be executed for this view.
	 *
	 * @return the configured SQL view
	 */
	String getSqlView() {
		String sqlView = null;
		final Object view = this.m_properties.get(CONF_SQL_VIEW);
		if ((this.m_properties != null) && (this.m_properties.containsKey(CONF_SQL_VIEW)) && (view instanceof String)) {
			sqlView = String.valueOf(view);
		}
		return sqlView;
	}

}