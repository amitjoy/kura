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
package org.eclipse.kura.web.client.ui.wires;

import java.util.logging.Logger;

import com.google.gwt.cell.client.AbstractInputCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safecss.shared.SafeStyles;
import com.google.gwt.safecss.shared.SafeStylesUtils;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.client.SafeHtmlTemplates.Template;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;

public class ValidatableInputCell extends AbstractInputCell<String, ValidationData> {

	interface Template extends SafeHtmlTemplates {
		@Template("<input type=\"text\" value=\"{0}\" style=\"{1}\" class=\"{2}\" tabindex=\"-1\"/>")
		SafeHtml input(String value, SafeStyles color, String cssClassName);
	}

	private static final Logger logger = Logger.getLogger(ValidatableInputCell.class.getSimpleName());
	private final SafeHtml errorMessage;
	private Template template;

	public ValidatableInputCell(final String errorMessage) {
		super("change");
		if (this.template == null) {
			this.template = GWT.create(Template.class);
		}
		this.errorMessage = SimpleHtmlSanitizer.sanitizeHtml(errorMessage);
	}

	@Override
	public void onBrowserEvent(final Context context, final Element parent, final String value, final NativeEvent event,
			final ValueUpdater<String> valueUpdater) {
		super.onBrowserEvent(context, parent, value, event, valueUpdater);

		// Ignore events that don't target the input.
		final Element target = event.getEventTarget().cast();
		if (!parent.getFirstChildElement().isOrHasChild(target)) {
			return;
		}

		final Object key = context.getKey();
		ValidationData viewData = this.getViewData(key);
		final String eventType = event.getType();
		if ("change".equals(eventType)) {
			final InputElement input = parent.getFirstChild().cast();

			// Mark cell as containing a pending change
			input.getStyle().setColor("blue");

			// Save the new value in the view data.
			if (viewData == null) {
				viewData = new ValidationData();
				this.setViewData(key, viewData);
			}
			final String newValue = input.getValue();
			viewData.setValue(newValue);
			this.finishEditing(parent, newValue, key, valueUpdater);

			// Update the value updater, which updates the field updater.
			if (valueUpdater != null) {
				valueUpdater.update(newValue);
			}
		}
	}

	@Override
	protected void onEnterKeyDown(final Context context, final Element parent, final String value,
			final NativeEvent event, final ValueUpdater<String> valueUpdater) {
		final Element target = event.getEventTarget().cast();
		if (this.getInputElement(parent).isOrHasChild(target)) {
			this.finishEditing(parent, value, context.getKey(), valueUpdater);
		} else {
			super.onEnterKeyDown(context, parent, value, event, valueUpdater);
		}
	}

	@Override
	public void render(final Context context, final String value, final SafeHtmlBuilder sb) {
		// Get the view data.
		final Object key = context.getKey();
		ValidationData viewData = this.getViewData(key);
		if ((viewData != null) && viewData.getValue().equals(value)) {
			// Clear the view data if the value is the same as the current
			// value.
			this.clearViewData(key);
			viewData = null;
		}

		/*
		 * If viewData is null, just paint the contents black. If it is
		 * non-null, show the pending value and paint the contents red if they
		 * are known to be invalid.
		 */
		final String pendingValue = (viewData == null) ? null : viewData.getValue();
		final boolean invalid = (viewData == null) ? false : viewData.isInvalid();

		final String color = pendingValue != null ? (invalid ? "red" : "blue") : "black";
		final SafeStyles safeColor = SafeStylesUtils.fromTrustedString("color: " + color + ";");
		sb.append(this.template.input(pendingValue != null ? pendingValue : value, safeColor,
				invalid ? "error-text-box" : "noerror-text-box"));

		/**
		 * if (invalid) {
		 * sb.appendHtmlConstant("&nbsp;<span class='tooltiptext'>");
		 * sb.append(this.errorMessage); sb.appendHtmlConstant("</span>"); }
		 */
	}
}