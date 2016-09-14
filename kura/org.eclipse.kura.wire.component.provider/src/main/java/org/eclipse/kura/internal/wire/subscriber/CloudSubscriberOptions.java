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
package org.eclipse.kura.internal.wire.subscriber;

import java.util.Map;

import org.eclipse.kura.wire.SeverityLevel;

/**
 * The Class CloudSubscriberOptions is responsible to provide all the required
 * options for the Cloud Subscriber Wire Component
 */
final class CloudSubscriberOptions {

	/** The Constant denoting the publisher application. */
	private static final String CONF_APPLICATION = "subscribe.application";

	/** The Constant denoting QoS. */
	private static final String CONF_QOS = "subscribe.qos";

	/** The Constant denoting MQTT topic. */
	private static final String CONF_TOPIC = "subscribe.topic";

	/** The Constant application to perform (either publish or subscribe). */
	private static final String DEFAULT_APPLICATION = "SUB";

	/** The Constant denoting default QoS. */
	private static final int DEFAULT_QOS = 0;

	/** The Constant denoting default MQTT topic. */
	private static final String DEFAULT_TOPIC = "EVENT";

	/** The Constant denoting severity level. */
	private static final String SEVERITY_LEVEL = "severity.level";

	/** The properties as associated */
	private final Map<String, Object> m_properties;

	/**
	 * Instantiates a new cloud subscriber options.
	 *
	 * @param properties
	 *            the properties
	 */
	CloudSubscriberOptions(final Map<String, Object> properties) {
		this.m_properties = properties;
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
	 * Returns the topic to be used for message subscription.
	 *
	 * @return the subscribing application topic
	 */
	String getSubscribeApplication() {
		String subscribeApp = DEFAULT_APPLICATION;
		final Object app = this.m_properties.get(CONF_APPLICATION);
		if ((this.m_properties != null) && this.m_properties.containsKey(CONF_APPLICATION) && (app != null)
				&& (app instanceof String)) {
			subscribeApp = String.valueOf(app);
		}
		return subscribeApp;
	}

	/**
	 * Returns the QoS to be used for message subscription.
	 *
	 * @return the subscribing QoS
	 */
	int getSubscribingQos() {
		int subscribingQos = DEFAULT_QOS;
		final Object qos = this.m_properties.get(CONF_QOS);
		if ((this.m_properties != null) && this.m_properties.containsKey(CONF_QOS) && (qos != null)
				&& (qos instanceof Integer)) {
			subscribingQos = (Integer) qos;
		}
		return subscribingQos;
	}

	/**
	 * Returns the topic to be used for message subscription.
	 *
	 * @return the subscribing topic
	 */
	String getSubscribingTopic() {
		String subscribingTopic = DEFAULT_TOPIC;
		final Object topic = this.m_properties.get(CONF_TOPIC);
		if ((this.m_properties != null) && this.m_properties.containsKey(CONF_TOPIC) && (topic != null)
				&& (topic instanceof String)) {
			subscribingTopic = String.valueOf(topic);
		}
		return subscribingTopic;
	}

}