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
package org.eclipse.kura.internal.wire.timer;

import static org.eclipse.kura.Preconditions.checkCondition;
import static org.eclipse.kura.Preconditions.checkNull;

import java.util.Map;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.util.base.ThrowableUtil;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class Timer represents a Wire Component which triggers a ticking event on
 * every interval as configured. It fires the event on every tick.
 */
public final class Timer implements WireEmitter, ConfigurableComponent {

	/** Group Identifier for Quartz Job and Triggers */
	private static final String GROUP_ID = "wires";

	/** This is required to generate unique ID for the Quartz Trigger and Job */
	private static int id = 0;

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(Timer.class);

	/** Localization Resource */
	private static final WireMessages s_message = LocalizationAdapter.adapt(WireMessages.class);

	/** Job Key for Quartz Scheduling */
	private JobKey m_jobKey;

	/** Scheduler instance */
	private Scheduler m_scheduler;

	/** The configured options */
	private TimerOptions m_timerOptions;

	/** The Wire Helper Service. */
	private volatile WireHelperService m_wireHelperService;

	/** The wire supporter component. */
	private WireSupport m_wireSupport;

	/**
	 * OSGi service component activation callback
	 *
	 * @param ctx
	 *            the component context
	 * @param properties
	 *            the configured properties
	 */
	protected synchronized void activate(final ComponentContext ctx, final Map<String, Object> properties) {
		s_logger.debug(s_message.activatingTimer());
		this.m_wireSupport = this.m_wireHelperService.newWireSupport(this);
		this.m_timerOptions = new TimerOptions(properties);
		try {
			this.m_scheduler = new StdSchedulerFactory().getScheduler();
			this.doUpdate();
		} catch (final SchedulerException e) {
			s_logger.error(ThrowableUtil.stackTraceAsString(e));
		}
		s_logger.debug(s_message.activatingTimerDone());
	}

	/**
	 * Binds the Wire Helper Service.
	 *
	 * @param wireHelperService
	 *            the new Wire Helper Service
	 */
	public synchronized void bindWireHelperService(final WireHelperService wireHelperService) {
		if (this.m_wireHelperService == null) {
			this.m_wireHelperService = wireHelperService;
		}
	}

	/** {@inheritDoc} */
	@Override
	public void consumersConnected(final Wire[] wires) {
		this.m_wireSupport.consumersConnected(wires);
	}

	/**
	 * OSGi service component deactivation callback
	 *
	 * @param ctx
	 *            the component context
	 */
	protected synchronized void deactivate(final ComponentContext ctx) {
		s_logger.debug(s_message.deactivatingTimer());
		if (this.m_jobKey != null) {
			try {
				this.m_scheduler.deleteJob(this.m_jobKey);
			} catch (final SchedulerException e) {
				s_logger.error(ThrowableUtil.stackTraceAsString(e));
			}
		}
		s_logger.debug(s_message.deactivatingTimerDone());
	}

	/**
	 * Perform update operation which internally emits a Wire Record every
	 * interval
	 *
	 * @throws SchedulerException
	 *             if job scheduling fails
	 */
	private void doUpdate() throws SchedulerException {
		int interval;
		if ("SIMPLE".equalsIgnoreCase(this.m_timerOptions.getType())) {
			interval = this.m_timerOptions.getSimpleInterval();
			this.scheduleSimpleInterval(interval);
			return;
		}
		final String cronExpression = this.m_timerOptions.getCronExpression();
		this.scheduleCronInterval(cronExpression);
	}

	/** {@inheritDoc} */
	@Override
	protected void finalize() throws Throwable {
		if (this.m_scheduler != null) {
			this.m_scheduler = null;
		}
	}

	/** {@inheritDoc} */
	@Override
	public Object polled(final Wire wire) {
		return this.m_wireSupport.polled(wire);
	}

	/**
	 * Creates a cron trigger based on the provided interval
	 *
	 * @param expression
	 *            the CRON expression
	 * @throws SchedulerException
	 *             if scheduling fails
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	private void scheduleCronInterval(final String expression) throws SchedulerException {
		checkNull(expression, s_message.cronExpressionNonNull());
		++id;
		if (this.m_jobKey != null) {
			this.m_scheduler.deleteJob(this.m_jobKey);
		}
		this.m_jobKey = new JobKey("emitJob" + id, GROUP_ID);
		final Trigger trigger = TriggerBuilder.newTrigger().withIdentity("emitTrigger" + id, GROUP_ID)
				.withSchedule(CronScheduleBuilder.cronSchedule(expression)).build();

		final TimerJobDataMap jobDataMap = new TimerJobDataMap();
		jobDataMap.putWireSupport(this.m_wireSupport);
		final JobDetail job = JobBuilder.newJob(EmitJob.class).withIdentity(this.m_jobKey).setJobData(jobDataMap)
				.build();

		this.m_scheduler.getContext().put("wireSupport", this.m_wireSupport);
		this.m_scheduler.start();

		this.m_scheduler.scheduleJob(job, trigger);
	}

	/**
	 * Creates a trigger based on the provided interval
	 *
	 * @param interval
	 *            the interval
	 * @throws SchedulerException
	 *             if scheduling fails
	 * @throws KuraRuntimeException
	 *             if the interval is less than or equal to zero
	 */
	private void scheduleSimpleInterval(final int interval) throws SchedulerException {
		checkCondition(interval <= 0, s_message.intervalNonLessThanEqualToZero());
		++id;
		if (this.m_jobKey != null) {
			this.m_scheduler.deleteJob(this.m_jobKey);
		}
		this.m_jobKey = new JobKey("emitJob" + id, GROUP_ID);
		final Trigger trigger = TriggerBuilder.newTrigger().withIdentity("emitTrigger" + id, GROUP_ID)
				.withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(interval).repeatForever())
				.build();

		final TimerJobDataMap jobDataMap = new TimerJobDataMap();
		jobDataMap.putWireSupport(this.m_wireSupport);
		final JobDetail job = JobBuilder.newJob(EmitJob.class).withIdentity(this.m_jobKey).setJobData(jobDataMap)
				.build();

		this.m_scheduler.start();
		this.m_scheduler.scheduleJob(job, trigger);
	}

	/**
	 * Unbinds the Wire Helper Service.
	 *
	 * @param wireHelperService
	 *            the new Wire Helper Service
	 */
	public synchronized void unbindWireHelperService(final WireHelperService wireHelperService) {
		if (this.m_wireHelperService == wireHelperService) {
			this.m_wireHelperService = null;
		}
	}

	/**
	 * OSGi service component modification callback
	 *
	 * @param properties
	 *            the updated properties
	 */
	protected synchronized void updated(final Map<String, Object> properties) {
		s_logger.debug(s_message.updatingTimer());
		this.m_timerOptions = new TimerOptions(properties);
		try {
			this.m_scheduler = new StdSchedulerFactory().getScheduler();
			this.doUpdate();
		} catch (final SchedulerException e) {
			s_logger.error(ThrowableUtil.stackTraceAsString(e));
		}
		s_logger.debug(s_message.updatingTimerDone());
	}
}
