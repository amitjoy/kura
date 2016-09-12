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

import java.util.Arrays;

import org.eclipse.kura.wire.TimerWireField;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * The Class EmitJob is responsible for emitting Wire Record every specified
 * interval (or specified CRON job interval)
 */
@DisallowConcurrentExecution
public final class EmitJob implements Job {

	/**
	 * Emits a Wire Record every specified interval.
	 *
	 * @param context
	 *            the Job Execution context
	 * @throws JobExecutionException
	 *             the job execution exception
	 */
	@Override
	public void execute(final JobExecutionContext context) throws JobExecutionException {
		final TimerJobDataMap dataMap = (TimerJobDataMap) context.getJobDetail().getJobDataMap();
		final WireSupport wireSupport = dataMap.getWireSupport();
		wireSupport.emit(Arrays.asList(new WireRecord(new TimerWireField())));
	}

}