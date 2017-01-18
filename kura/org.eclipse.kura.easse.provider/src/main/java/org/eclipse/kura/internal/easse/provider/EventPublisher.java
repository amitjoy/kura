/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.internal.easse.provider;

import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.util.base.ThrowableUtil.stackTraceAsString;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.kura.util.collection.CollectionUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonObject;

public final class EventPublisher {

    /** Logger Instance */
    private static final Logger LOG = LoggerFactory.getLogger(EventPublisher.class);

    /** All registered Event Sources */
    private final Map<SseEventSource, ServiceRegistration<?>> sourcesAndListeners;

    /** Singleton Instance */
    private static final EventPublisher INSTANCE = new EventPublisher();

    /** Bundle Context */
    private static BundleContext bundleContext;

    /** Constructor */
    private EventPublisher() {
        // no need for external access
        this.sourcesAndListeners = CollectionUtil.newConcurrentHashMap();
        bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
    }

    /** Gets Singleton Instance */
    public static EventPublisher getInstance() {
        return INSTANCE;
    }

    /**
     * Registers the provided Event Source instance
     *
     * @param eventSource
     *            the event source
     * @throws NullPointerException
     *             if the Event Source is null
     */
    public void addEventSource(final SseEventSource eventSource) {
        requireNonNull(eventSource, "Event Source instance cannot be null");

        final ServiceRegistration<?> eventHandler = registerEventHandler(eventSource);
        this.sourcesAndListeners.put(eventSource, eventHandler);
    }

    /**
     * Unregisters the provided Event Source instance
     *
     * @param eventSource
     *            the event source
     * @throws NullPointerException
     *             if the Event Source is null
     */
    public void removeEventSource(final SseEventSource eventSource) {
        requireNonNull(eventSource, "Event Source instance cannot be null");

        this.sourcesAndListeners.get(eventSource).unregister();
        this.sourcesAndListeners.remove(eventSource);
    }

    /**
     * Converts the provided Map instance to JSON format
     *
     * @param map
     *            the provided map instance
     * @return the JSON format
     * @throws NullPointerException
     *             if the Map instance is null
     */
    @SuppressWarnings("unchecked")
    private JsonObject getJsonFromMap(final Map<String, Object> map) {
        requireNonNull(map, "Provided Map instance cannot be null");

        final JsonObject jsonData = new JsonObject();
        for (final Map.Entry<String, Object> entry : map.entrySet()) {
            final String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map<?, ?>) {
                value = getJsonFromMap((Map<String, Object>) value);
            }
            jsonData.add(key, String.valueOf(value));
        }
        return jsonData;
    }

    /**
     * Registers an Event Handler for the established Event Source
     *
     * @param eventSource
     *            the established Event Source
     * @return service registration
     * @throws NullPointerException
     *             if the Event Source is null
     */
    private ServiceRegistration<?> registerEventHandler(final SseEventSource eventSource) {
        requireNonNull(eventSource, "The provided Event Source must not be null");

        final Dictionary<String, Object> map = new Hashtable<>();
        map.put(EventConstants.EVENT_TOPIC, eventSource.getTopic());
        return bundleContext.registerService(EventHandler.class, event -> {
            final Map<String, Object> props = CollectionUtil.newHashMap();
            for (final String name : event.getPropertyNames()) {
                props.put(name, event.getProperty(name));
            }
            try {
                eventSource.emitEvent(getJsonFromMap(props).toString());
            } catch (final IOException e) {
                LOG.error("Emit data failed..." + stackTraceAsString(e));
            }
        }, map);
    }
}