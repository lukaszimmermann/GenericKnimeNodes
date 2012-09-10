/**
 * Copyright (c) 2012, Stephan Aiche.
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
package com.genericworkflownodes.knime.config.reader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.genericworkflownodes.knime.parameter.BoolParameter;
import com.genericworkflownodes.knime.parameter.DoubleListParameter;
import com.genericworkflownodes.knime.parameter.DoubleParameter;
import com.genericworkflownodes.knime.parameter.IntegerListParameter;
import com.genericworkflownodes.knime.parameter.IntegerParameter;
import com.genericworkflownodes.knime.parameter.InvalidParameterValueException;
import com.genericworkflownodes.knime.parameter.ListParameter;
import com.genericworkflownodes.knime.parameter.Parameter;
import com.genericworkflownodes.knime.parameter.StringChoiceParameter;
import com.genericworkflownodes.knime.parameter.StringListParameter;
import com.genericworkflownodes.knime.parameter.StringParameter;
import com.genericworkflownodes.util.StringUtils.DoubleRangeExtractor;
import com.genericworkflownodes.util.StringUtils.IntegerRangeExtractor;

/**
 * SAXHandler for the parameters tag in the CTD document.
 * 
 * @author aiche
 */
public class ParamHandler extends DefaultHandler {

	private static String TAG_NODE = "NODE"; // name, description
	private static String TAG_ITEM = "ITEM"; // name, type, value, description,
												// tags, restritions,
												// supported_formats,
												// output_format_source
	private static String TAG_ITEMLIST = "ITEMLIST"; // name, type, description,
														// tags, restrictions
	private static String TAG_LISTITEM = "LISTITEM"; // value
	private static String TAG_PARAMETERS = "PARAMETERS";

	private static String TYPE_INT = "int";
	private static String TYPE_FLOAT = "float";
	private static String TYPE_DOUBLE = "double";
	private static String TYPE_STRING = "string";

	private static String ATTR_NAME = "name";
	private static String ATTR_VALUE = "value";
	private static String ATTR_TYPE = "type";
	private static String ATTR_DESCRIPTION = "description";
	private static String ATTR_TAGS = "tags";
	private static String ATTR_SUPPORTED_FORMATS = "supported_formats";
	private static String ATTR_RESTRICTIONS = "restrictions";
	private static String ATTR_OUTPUT_FORMAT_SOURCE = "output_format_source";

	private static char PATH_SEPARATOR = '.';

	/**
	 * The list of extracted parameters.
	 */
	private LinkedHashMap<String, Parameter<?>> extractedParameters;

	/**
	 * The currently generated parameter.
	 */
	private Parameter<?> currentParameter;

	/**
	 * Store the current list entries to finally add them to the created list
	 * parameter.
	 */
	private List<String> listValues;

	/**
	 * The parent handler that invoked this handler for a sub tree of the XML
	 * document.
	 */
	private CTDHandler parentHandler;

	/**
	 * The {@link XMLReader} that processes the entire document.
	 */
	private XMLReader xmlReader;

	/**
	 * Stores the current path inside the xml tree.
	 */
	private String currentPath;

	public ParamHandler(XMLReader xmlReader, CTDHandler parentHandler) {
		this.xmlReader = xmlReader;
		this.parentHandler = parentHandler;
		currentPath = "";
		extractedParameters = new LinkedHashMap<String, Parameter<?>>();
	}

	@Override
	public void startElement(String uri, String localName, String name,
			Attributes attributes) throws SAXException {
		if (TAG_NODE.equals(name)) {
			currentPath += attributes.getValue(ATTR_NAME);
			currentPath += PATH_SEPARATOR;
			System.out.println("Prefix is: " + currentPath);
		} else if (TAG_ITEM.equals(name)) {
			String type = attributes.getValue(ATTR_TYPE);
			String paramName = attributes.getValue(ATTR_NAME);
			String paramValue = attributes.getValue(ATTR_VALUE);

			if (TYPE_INT.equals(type)) {
				handleIntType(paramName, paramValue, attributes);
			} else if (TYPE_DOUBLE.equals(type) || TYPE_FLOAT.equals(type)) {
				handleDoubleType(paramName, paramValue, attributes);
			} else if (TYPE_STRING.equals(type)) {
				handleStringType(paramName, paramValue, attributes);
			}

			// did we create a parameter
			if (currentParameter != null) {
				setCommonParameters(attributes);

				extractedParameters.put(
						currentPath + currentParameter.getKey(),
						currentParameter);

				// reset for the next iteration
				currentParameter = null;
			}
		} else if (TAG_ITEMLIST.equals(name)) {
			// start the list parameter
			String type = attributes.getValue(ATTR_TYPE);
			String paramName = attributes.getValue(ATTR_NAME);

			if (TYPE_INT.equals(type)) {
				handleIntList(paramName, attributes);
			} else if (TYPE_DOUBLE.equals(type) || TYPE_FLOAT.equals(type)) {
				handleDoubleList(paramName, attributes);
			} else if (TYPE_STRING.equals(type)) {
				handleStringList(paramName, attributes);
			}
			// initialize list for storing the list values
			listValues = new ArrayList<String>();

			// set extra values for this parameter
			setCommonParameters(attributes);
		} else if (TAG_LISTITEM.equals(name)) {
			String listValue = attributes.getValue(ATTR_VALUE);
			listValues.add(listValue);
		}
	}

	/**
	 * Extracts common parameters like isAdvanced, isOptional, and description
	 * from the {@link Attributes} and passes them to the current parameter.
	 * 
	 * @param attributes
	 *            The {@link Attributes} containing the necessary values.
	 */
	private void setCommonParameters(Attributes attributes) {
		// set flags for parameter
		currentParameter.setAdvanced(isAdvanced(attributes));
		currentParameter.setIsOptional(isOptional(attributes));

		// extract the description
		String description = attributes.getValue(ATTR_DESCRIPTION);
		currentParameter.setDescription(description);
	}

	private void handleStringList(String paramName, Attributes attributes) {
		if (isPort(attributes)) {
			createListPort(paramName, attributes);
		} else {
			currentParameter = new StringListParameter(paramName,
					new ArrayList<String>());
			String restrictions = attributes.getValue(ATTR_RESTRICTIONS);
			if (restrictions != null) {
				((StringListParameter) currentParameter).setRestrictions(Arrays
						.asList(restrictions.split(",")));
			}
		}
	}

	private void createListPort(String paramName, Attributes attributes) {
		// TODO Auto-generated method stub

	}

	private void handleDoubleList(String paramName, Attributes attributes) {
		currentParameter = new DoubleListParameter(paramName,
				new ArrayList<Double>());

		// check for restrictions
		String restrs = attributes.getValue(ATTR_RESTRICTIONS);
		if (restrs != null) {
			((DoubleListParameter) currentParameter)
					.setLowerBound(new DoubleRangeExtractor()
							.getLowerBound(restrs));
			((DoubleListParameter) currentParameter)
					.setUpperBound(new DoubleRangeExtractor()
							.getUpperBound(restrs));
		}
	}

	private void handleIntList(String paramName, Attributes attributes) {
		currentParameter = new IntegerListParameter(paramName,
				new ArrayList<Integer>());

		// check for restrictions
		String restrs = attributes.getValue(ATTR_RESTRICTIONS);
		if (restrs != null) {
			((IntegerListParameter) currentParameter)
					.setLowerBound(new IntegerRangeExtractor()
							.getLowerBound(restrs));
			((IntegerListParameter) currentParameter)
					.setUpperBound(new IntegerRangeExtractor()
							.getUpperBound(restrs));
		}
	}

	private void handleStringType(String paramName, String paramValue,
			Attributes attributes) {
		if (isPort(attributes)) {
			createPort(paramName, paramValue, attributes);
		}

		// check if we have a boolean
		String restrictions = attributes.getValue(ATTR_RESTRICTIONS);
		if (isBooleanParameter(restrictions)) {
			currentParameter = new BoolParameter(paramName, paramValue);
		} else {
			if (restrictions != null && restrictions.length() > 0) {
				currentParameter = new StringChoiceParameter(paramName,
						restrictions.split(","));
				((StringChoiceParameter) currentParameter).setValue(paramValue);
			} else {
				currentParameter = new StringParameter(paramName, paramValue);
			}
		}
	}

	private void createPort(String paramName, String paramValue,
			Attributes attributes) {
		// TODO Auto-generated method stub

	}

	private boolean isBooleanParameter(final String restrictions) {
		if (restrictions == null || restrictions.trim().length() == 0)
			return false;
		else {
			// tokenize restrictions
			String[] tokens = restrictions.split(",");
			if (tokens.length != 2) {
				return false;
			} else
				return (("true".equals(tokens[0]) && "false".equals(tokens[1])) || ("false"
						.equals(tokens[0]) && "true".equals(tokens[1])));
		}
	}

	private boolean isPort(final Attributes attributes) {
		Set<String> tagSet = getTags(attributes);
		return (tagSet.contains("input file") || tagSet.contains("output file"));
	}

	private void handleIntType(final String paramName, final String paramValue,
			Attributes attributes) {
		currentParameter = new IntegerParameter(paramName, paramValue);

		// check for restrictions
		String restrictions = attributes.getValue(ATTR_RESTRICTIONS);
		if (restrictions != null) {
			((IntegerParameter) currentParameter)
					.setLowerBound(new IntegerRangeExtractor()
							.getLowerBound(restrictions));
			((IntegerParameter) currentParameter)
					.setUpperBound(new IntegerRangeExtractor()
							.getUpperBound(restrictions));
		}
	}

	private void handleDoubleType(final String paramName,
			final String paramValue, Attributes attributes) {
		currentParameter = new DoubleParameter(paramName, paramValue);

		// check for restrictions
		String restrs = attributes.getValue(ATTR_RESTRICTIONS);
		if (restrs != null) {
			((DoubleParameter) currentParameter)
					.setLowerBound(new DoubleRangeExtractor()
							.getLowerBound(restrs));
			((DoubleParameter) currentParameter)
					.setUpperBound(new DoubleRangeExtractor()
							.getUpperBound(restrs));
		}
	}

	private boolean isOptional(final Attributes attributes) {
		Set<String> tagSet = getTags(attributes);
		return !(tagSet.contains("mandatory") || tagSet.contains("required"));
	}

	private boolean isAdvanced(final Attributes attributes) {
		Set<String> tagSet = getTags(attributes);
		return tagSet.contains("advanced");
	}

	/**
	 * Creates a list of all tags that were given in the attributes.
	 * 
	 * @param attributes
	 *            The attributes to check for the tags attribute.
	 * @return A {@link Set} of all tags. The {@link Set} is empty if no tags
	 *         were given or the tags attribute was not given at all.
	 */
	private Set<String> getTags(final Attributes attributes) {
		String tags = attributes.getValue(ATTR_TAGS);
		if (tags != null) {
			String[] tokens = tags.split(",");
			Set<String> tokenSet = new HashSet<String>();
			Collections.addAll(tokenSet, tokens);
			return tokenSet;
		} else {
			return new HashSet<String>();
		}
	}

	@Override
	public void endElement(String uri, String localName, String name)
			throws SAXException {
		if (TAG_NODE.equals(name)) {
			// reduce prefix
			removeSuffix();
			System.out.println("Prefix is: " + currentPath);
		} else if (TAG_ITEM.equals(name)) {

		} else if (TAG_ITEMLIST.equals(name)) {
			try {
				if (listValues.size() > 0) {
					String[] values = new String[listValues.size()];
					int i = 0;
					for (String v : listValues) {
						values[i++] = v;
					}
					((ListParameter) currentParameter).fillFromStrings(values);
				}
			} catch (InvalidParameterValueException e) {
				// should not happen
				e.printStackTrace();
			}
			extractedParameters.put(currentPath + currentParameter.getKey(),
					currentParameter);

			// reset for the next iteration
			currentParameter = null;
		} else if (TAG_LISTITEM.equals(name)) {

		} else if (TAG_PARAMETERS.equals(name)) {
			// TODO: return values to surrounding handler
			parentHandler.setParameters(extractedParameters);
			xmlReader.setContentHandler(parentHandler);
		}
	}

	private void removeSuffix() {
		// find suffix border
		int i = currentPath.length() - 2;
		for (; i > 0; --i) {
			if (currentPath.charAt(i) == PATH_SEPARATOR)
				break;
		}

		// i should point to the prefix position
		if (i != 0)
			currentPath = currentPath.substring(0, i + 1);
		else
			currentPath = ""; // reset prefix if we reached the top level
	}
}
