/**
 * Copyright (c) 2012, Marc Röttig.
 *
 * This file is part of GenericKnimeNodes.
 * 
 * GenericKnimeNodes is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.genericworkflownodes.knime.parameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The StringChoiceParameter class represents string allowedValues taken from a
 * restricted set.
 * 
 * If the set itself is optional, the empty string is via convention also a
 * valid choice.
 * 
 * @author roettig,aiche
 */
public class StringChoiceParameter extends Parameter<String> {

	/**
	 * The serial version UID.
	 */
	private static final long serialVersionUID = -2717347970783695908L;

	/**
	 * Set of string that is possible for this Parameter.
	 */
	private List<String> allowedValues;

	/**
	 * List of labels for each of the allowed strings.
	 */
	private List<String> labels;

	/**
	 * Constructor.
	 * 
	 * @param key
	 *            The unique key of the parameter.
	 * @param value
	 *            The value of the parameter.
	 */
	public StringChoiceParameter(final String key, final String value) {
		super(key, value);
	}

	/**
	 * Constructor. The first value in the list will be set as current value.
	 * 
	 * @param key
	 *            The unique key of the parameter.
	 * @param values
	 *            The values of the parameter as {@link List}.
	 */
	public StringChoiceParameter(final String key, final List<String> values) {
		super(key, values.get(0));
		this.allowedValues = values;
		this.labels = values;
	}

	/**
	 * Constructor. The first value in the list will be set as current value.
	 * 
	 * @param key
	 *            The unique key of the parameter.
	 * @param values
	 *            The values of the parameter as array of {@link String}s.
	 */
	public StringChoiceParameter(final String key, final String[] values) {
		super(key, values[0]);
		this.allowedValues = Arrays.asList(values);
		this.labels = Arrays.asList(values);
	}

	/**
	 * Constructor. The first value in the list will be set as current value.
	 * 
	 * @param key
	 *            The unique key of the parameter.
	 * @param values
	 *            The values of the parameter.
	 * @param labels
	 *            The labels for the values.
	 */
	public StringChoiceParameter(final String key, final List<String> values,
			final List<String> labels) {
		super(key, values.get(0));
		this.allowedValues = values;
		this.labels = labels;
	}

	/**
	 * Constructor. The first value in the list will be set as current value.
	 * 
	 * @param key
	 *            The unique key of the parameter.
	 * @param values
	 *            The values of the parameter.
	 * @param labels
	 *            The labels for the values.
	 */
	public StringChoiceParameter(final String key, final String[] values,
			final String[] labels) {
		super(key, values[0]);
		this.allowedValues = Arrays.asList(values);
		this.labels = Arrays.asList(labels);
	}

	@Override
	public void setValue(final String value) {
		// the value must either be contained in the allowed values
		// or the empty string if the parameter itself is optional
		if (allowedValues.contains(value) || (isOptional() && "".equals(value))) {
			super.setValue(value);
		}
	}

	/**
	 * Returns the list of allowed string values.
	 * 
	 * @return allowed allowedValues
	 */
	public List<String> getAllowedValues() {
		if (isOptional()) {
			ArrayList<String> tAllowedValues = new ArrayList<String>(
					allowedValues.size() + 1);
			tAllowedValues.add("");
			tAllowedValues.addAll(allowedValues);
			return tAllowedValues;
		} else {
			return allowedValues;
		}
	}

	/**
	 * Returns the list of associated labels for each value.
	 * 
	 * This is mainly for display purposes within GUIs.
	 * 
	 * @return list of labels
	 */
	public List<String> getLabels() {
		if (isOptional()) {
			ArrayList<String> tLabels = new ArrayList<String>(labels.size() + 1);
			tLabels.add("");
			tLabels.addAll(labels);
			return tLabels;
		} else {
			return labels;
		}
	}

	@Override
	public String toString() {
		return getValue();
	}

	@Override
	public void fillFromString(final String s)
			throws InvalidParameterValueException {
		if (s == null) {
			setValue(null);
			return;
		}
		if (!this.getAllowedValues().contains(s)) {
			throw new InvalidParameterValueException("parameter "
					+ this.getKey() + " value is invalid");
		}
		setValue(s);
	}

	@Override
	public boolean validate(final String val) {
		return true;
	}

	@Override
	public String getMnemonic() {
		return "string choice";
	}
}
