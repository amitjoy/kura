/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.internal.wire;

import static org.eclipse.kura.Preconditions.checkNull;
import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;
import static org.osgi.framework.Constants.SERVICE_PID;

import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.util.service.ServiceUtil;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.EventAdmin;

/**
 * The Class WireHelperServiceImpl is the implementation of
 * {@link WireHelperService}
 */
public final class WireHelperServiceImpl implements WireHelperService {

	/** Localization Resource */
	private static final WireMessages s_message = LocalizationAdapter.adapt(WireMessages.class);

	/** Event Admin Service */
	private volatile EventAdmin m_eventAdmin;

	/**
	 * Binds the Event Admin Service.
	 *
	 * @param eventAdmin
	 *            the new Event Admin Service
	 */
	public synchronized void bindEventAdmin(final EventAdmin eventAdmin) {
		if (this.m_eventAdmin == null) {
			this.m_eventAdmin = eventAdmin;
		}
	}

	/** {@inheritDoc} */
	@Override
	public String getPid(final WireComponent wireComponent) {
		checkNull(wireComponent, s_message.wireComponentNonNull());
		final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
		final ServiceReference<?>[] refs = ServiceUtil.getServiceReferences(context, WireComponent.class, null);
		for (final ServiceReference<?> ref : refs) {
			final WireComponent wc = (WireComponent) context.getService(ref);
			if (wc == wireComponent) {
				return ref.getProperty(KURA_SERVICE_PID).toString();
			}
			context.ungetService(ref);
		}
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public String getServicePid(final String wireComponentPid) {
		checkNull(wireComponentPid, s_message.wireComponentPidNonNull());
		final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
		final ServiceReference<?>[] refs = ServiceUtil.getServiceReferences(context, WireComponent.class, null);
		for (final ServiceReference<?> ref : refs) {
			if (ref.getProperty(KURA_SERVICE_PID).equals(wireComponentPid)) {
				return ref.getProperty(SERVICE_PID).toString();
			}
		}
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public String getServicePid(final WireComponent wireComponent) {
		checkNull(wireComponent, s_message.wireComponentNonNull());
		final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
		final ServiceReference<?>[] refs = ServiceUtil.getServiceReferences(context, WireComponent.class, null);
		for (final ServiceReference<?> ref : refs) {
			final WireComponent wc = (WireComponent) context.getService(ref);
			if (wc == wireComponent) {
				return ref.getProperty(SERVICE_PID).toString();
			}
			context.ungetService(ref);
		}
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public boolean isEmitter(final String wireComponentPid) {
		checkNull(wireComponentPid, s_message.wireComponentPidNonNull());
		final BundleContext context = FrameworkUtil.getBundle(WireHelperServiceImpl.class).getBundleContext();
		final ServiceReference<?>[] refs = ServiceUtil.getServiceReferences(context, WireComponent.class, null);
		for (final ServiceReference<?> ref : refs) {
			WireComponent wc;
			if (ref.getProperty(KURA_SERVICE_PID).equals(wireComponentPid)) {
				wc = (WireComponent) context.getService(ref);
			} else {
				continue;
			}
			context.ungetService(ref);
			if (wc instanceof WireEmitter) {
				return true;
			}
		}
		return false;
	}

	/** {@inheritDoc} */
	@Override
	public boolean isReceiver(final String wireComponentPid) {
		checkNull(wireComponentPid, s_message.wireComponentPidNonNull());
		final BundleContext context = FrameworkUtil.getBundle(WireHelperServiceImpl.class).getBundleContext();
		final ServiceReference<?>[] refs = ServiceUtil.getServiceReferences(context, WireComponent.class, null);
		for (final ServiceReference<?> ref : refs) {
			WireComponent wc;
			if (ref.getProperty(KURA_SERVICE_PID).equals(wireComponentPid)) {
				wc = (WireComponent) context.getService(ref);
			} else {
				continue;
			}
			context.ungetService(ref);
			if (wc instanceof WireReceiver) {
				return true;
			}
		}
		return false;
	}

	/** {@inheritDoc} */
	@Override
	public WireSupport newWireSupport(final WireComponent wireComponent) {
		return new WireSupportImpl(wireComponent, this, this.m_eventAdmin);
	}

	/**
	 * Unbinds the Event Admin Service.
	 *
	 * @param eventAdmin
	 *            the new Event Admin Service
	 */
	public synchronized void unbindEventAdmin(final EventAdmin eventAdmin) {
		if (this.m_eventAdmin == eventAdmin) {
			this.m_eventAdmin = null;
		}
	}

}
