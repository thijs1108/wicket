/*
 * $Id$
 * $Revision$ $Date$
 * 
 * ==============================================================================
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package wicket.util.convert.converters;

import java.text.Format;
import java.text.ParsePosition;
import java.util.Locale;

import wicket.util.convert.ConversionException;
import wicket.util.convert.ITypeConverter;
import wicket.util.convert.converters.AbstractConverter;

/**
 * Base class for locale aware type converters.
 * 
 * @author Eelco Hillenius
 */
public abstract class AbstractConverter implements ITypeConverter
{
	/** The current locale. */
	private Locale locale = Locale.getDefault();

	/**
	 * Constructor
	 */
	public AbstractConverter()
	{
	}

	/**
	 * Constructor
	 * 
	 * @param locale
	 *            The locale for this converter
	 */
	public AbstractConverter(final Locale locale)
	{
		setLocale(locale);
	}

	/**
	 * gets the locale.
	 * 
	 * @return the locale
	 */
	public final Locale getLocale()
	{
		return locale;
	}

	/**
	 * sets the locale.
	 * 
	 * @param locale
	 *            the locale
	 */
	public void setLocale(Locale locale)
	{
		this.locale = locale;
	}

	/**
	 * Parses a value using one of the java.util.text format classes.
	 * 
	 * @param format
	 *            The format to use
	 * @param value
	 *            The object to parse
	 * @return The object
	 * @throws ConversionException
	 *             Thrown if parsing fails
	 */
	protected Object parse(final Format format, final Object value)
	{
		final ParsePosition position = new ParsePosition(0);
		final String stringValue = value.toString();
		final Object result = format.parseObject(stringValue, position);
		if (position.getIndex() != stringValue.length())
		{
			throw newConversionException("Cannot parse '" + value + "' using format " + format,
					value).setFormat(format);
		}
		return result;
	}

	/**
	 * Creates a conversion exception for throwing
	 * 
	 * @param message
	 *            The message
	 * @param value
	 *            The value that didn't convert
	 * @return The ConversionException
	 */
	protected ConversionException newConversionException(final String message, Object value)
	{
		return new ConversionException(message).setSourceValue(value)
				.setTargetType(getTargetType()).setTypeConverter(this);
	}

	/**
	 * @return The target type of this type converter
	 */
	protected abstract Class getTargetType();
}