package org.eclipse.kura.web.client.ui.wires;

/**
 * The ViewData used by {@link ValidatableInputCell}.
 */
public class ValidationData {
	private boolean invalid;
	private String value;

	public String getValue() {
		return this.value;
	}

	public boolean isInvalid() {
		return this.invalid;
	}

	public void setInvalid(final boolean invalid) {
		this.invalid = invalid;
	}

	public void setValue(final String value) {
		this.value = value;
	}
}