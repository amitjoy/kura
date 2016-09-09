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
package org.eclipse.kura.internal.wire.publisher;

import static org.eclipse.kura.Preconditions.checkCondition;
import static org.eclipse.kura.Preconditions.checkNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.util.base.ThrowableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class CloudPublisherDisconnectManager manages the disconnection with
 * Cloud Publisher
 */
final class CloudPublisherDisconnectManager {

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(CloudPublisherDisconnectManager.class);

	/** Localization Resource */
	private static final WireMessages s_message = LocalizationAdapter.adapt(WireMessages.class);

	/** The data service dependency. */
	private final DataService m_dataService;

	/** Schedule Executor Service **/
	private ScheduledExecutorService m_executorService;

	/** The quiesce timeout. */
	private long m_quiesceTimeout;

	/** The future handle of the thread pool executor service. */
	private ScheduledFuture<?> m_tickHandle;

	/**
	 * Instantiates a new cloud publisher disconnect manager.
	 *
	 * @param dataService
	 *            the data service
	 * @param quiesceTimeout
	 *            the quiesce timeout
	 * @throws KuraRuntimeException
	 *             if data service dependency is null
	 */
	CloudPublisherDisconnectManager(final DataService dataService, final long quiesceTimeout) {
		checkNull(dataService, s_message.dataServiceNonNull());
		this.m_dataService = dataService;
		this.m_quiesceTimeout = quiesceTimeout;
		this.m_executorService = Executors.newScheduledThreadPool(5);
	}

	/**
	 * Disconnect in minutes.
	 *
	 * @param minutes
	 *            the minutes
	 * @param isForceUpdate
	 *            checks if the scheduling needs to be updated forcefully
	 * @throws KuraRuntimeException
	 *             if argument passed is negative
	 */
	synchronized void disconnectInMinutes(final int minutes, final boolean isForceUpdate) {
		checkCondition(minutes < 0, s_message.minutesNonNegative());
		final long requiredDelay = (long) minutes * 60 * 1000;
		this.schedule(requiredDelay, isForceUpdate);
	}

	/**
	 * Gets the quiesce timeout.
	 *
	 * @return the quiesce timeout
	 */
	long getQuiesceTimeout() {
		return this.m_quiesceTimeout;
	}

	/**
	 * Schedule new schedule thread pool executor with the specified delay
	 *
	 * @param delay
	 *            the delay
	 * @param isForceUpdate
	 *            checks if the scheduling needs to be updated forcefully
	 * @throws KuraRuntimeException
	 *             if delay provided is negative
	 */
	private void schedule(final long delay, final boolean isForceUpdate) {
		checkCondition(delay < 0, s_message.delayNonNegative());
		// cancel existing timer
		if (this.m_tickHandle != null) {
			// if it is a force update then cancel existing scheduler
			if (isForceUpdate) {
				this.m_tickHandle.cancel(true);
			} else {
				return;
			}
		}
		this.m_tickHandle = this.m_executorService.scheduleAtFixedRate(new Runnable() {
			/** {@inheritDoc} */
			@Override
			public void run() {
				try {
					m_dataService.disconnect(m_quiesceTimeout);
				} catch (final Exception exception) {
					s_logger.error(
							s_message.errorDisconnectingCloudPublisher() + ThrowableUtil.stackTraceAsString(exception));
				}
			}
		}, 0, delay, TimeUnit.MILLISECONDS);

	}

	/**
	 * Sets the quiesce timeout.
	 *
	 * @param quiesceTimeout
	 *            the new quiesce timeout
	 */
	void setQuiesceTimeout(final long quiesceTimeout) {
		this.m_quiesceTimeout = quiesceTimeout;
	}

	/**
	 * Stops the scheduler thread pool
	 */
	synchronized void stop() {
		s_logger.info(s_message.schedulerStopping());
		if (this.m_tickHandle != null) {
			this.m_tickHandle.cancel(true);
		}
		if (this.m_executorService != null) {
			this.m_executorService.shutdown();
		}
		this.m_executorService = null;
		s_logger.info(s_message.schedulerStopped());
	}
}
