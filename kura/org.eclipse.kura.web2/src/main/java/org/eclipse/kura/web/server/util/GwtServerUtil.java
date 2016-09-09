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
package org.eclipse.kura.web.server.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;
import org.eclipse.kura.web.shared.model.GwtConfigParameter.GwtConfigParameterType;

/**
 * The Class GwtServerUtil is an utility class required for Kura Server
 * Components in GWT
 */
public final class GwtServerUtil {

	/**
	 * Instantiates a new gwt server util.
	 */
	private GwtServerUtil() {
		// No need to instantiate
	}

	/**
	 * Gets the object value.
	 *
	 * @param gwtConfigParam
	 *            the gwt config param
	 * @param strValue
	 *            the str value
	 * @return the object value
	 */
	public static Object getObjectValue(final GwtConfigParameter gwtConfigParam, final String strValue) {
		Object objValue = null;
		if (strValue != null) {
			final GwtConfigParameterType gwtType = gwtConfigParam.getType();
			switch (gwtType) {
			case LONG:
				objValue = Long.parseLong(strValue);
				break;
			case DOUBLE:
				objValue = Double.parseDouble(strValue);
				break;
			case FLOAT:
				objValue = Float.parseFloat(strValue);
				break;
			case INTEGER:
				objValue = Integer.parseInt(strValue);
				break;
			case SHORT:
				objValue = Short.parseShort(strValue);
				break;
			case BYTE:
				objValue = Byte.parseByte(strValue);
				break;

			case BOOLEAN:
				objValue = Boolean.parseBoolean(strValue);
				break;

			case PASSWORD:
				objValue = new Password(strValue);
				break;

			case CHAR:
				objValue = Character.valueOf(strValue.charAt(0));
				break;

			case STRING:
				objValue = strValue;
				break;
			}
		}
		return objValue;
	}

	/**
	 * Gets the object value.
	 *
	 * @param gwtConfigParam
	 *            the gwt config param
	 * @param defaultValues
	 *            the default values
	 * @return the object value
	 */
	public static Object[] getObjectValue(final GwtConfigParameter gwtConfigParam, final String[] defaultValues) {
		final List<Object> values = new ArrayList<Object>();
		final GwtConfigParameterType type = gwtConfigParam.getType();
		switch (type) {
		case BOOLEAN:
			for (final String value : defaultValues) {
				if (!value.trim().isEmpty()) {
					values.add(Boolean.valueOf(value));
				}
			}
			return values.toArray(new Boolean[] {});

		case BYTE:
			for (final String value : defaultValues) {
				if (!value.trim().isEmpty()) {
					values.add(Byte.valueOf(value));
				}
			}
			return values.toArray(new Byte[] {});

		case CHAR:
			for (final String value : defaultValues) {
				if (!value.trim().isEmpty()) {
					values.add(new Character(value.charAt(0)));
				}
			}
			return values.toArray(new Character[] {});

		case DOUBLE:
			for (final String value : defaultValues) {
				if (!value.trim().isEmpty()) {
					values.add(Double.valueOf(value));
				}
			}
			return values.toArray(new Double[] {});

		case FLOAT:
			for (final String value : defaultValues) {
				if (!value.trim().isEmpty()) {
					values.add(Float.valueOf(value));
				}
			}
			return values.toArray(new Float[] {});

		case INTEGER:
			for (final String value : defaultValues) {
				if (!value.trim().isEmpty()) {
					values.add(Integer.valueOf(value));
				}
			}
			return values.toArray(new Integer[] {});

		case LONG:
			for (final String value : defaultValues) {
				if (!value.trim().isEmpty()) {
					values.add(Long.valueOf(value));
				}
			}
			return values.toArray(new Long[] {});

		case SHORT:
			for (final String value : defaultValues) {
				if (!value.trim().isEmpty()) {
					values.add(Short.valueOf(value));
				}
			}
			return values.toArray(new Short[] {});

		case PASSWORD:
			for (final String value : defaultValues) {
				if (!value.trim().isEmpty()) {
					values.add(new Password(value));
				}
			}
			return values.toArray(new Password[] {});

		case STRING:
			for (final String value : defaultValues) {
				if (!value.trim().isEmpty()) {
					values.add(value);
				}
			}
			return values.toArray(new String[] {});
		}

		return null;
	}

	/**
	 * Strip pid prefix.
	 *
	 * @param pid
	 *            the pid
	 * @return the string
	 */
	public static String stripPidPrefix(final String pid) {
		final int start = pid.lastIndexOf('.');
		if (start < 0) {
			return pid;
		} else {
			final int begin = start + 1;
			if (begin < pid.length()) {
				return pid.substring(begin);
			} else {
				return pid;
			}
		}
	}

}
