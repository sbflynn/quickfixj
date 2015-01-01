/*******************************************************************************
 * Copyright (c) quickfixengine.org  All rights reserved.
 *
 * This file is part of the QuickFIX FIX Engine
 *
 * This file may be distributed under the terms of the quickfixengine.org
 * license as defined by quickfixengine.org and appearing in the file
 * LICENSE included in the packaging of this file.
 *
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING
 * THE WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE.
 *
 * See http://www.quickfixengine.org/LICENSE for licensing information.
 *
 * Contact ask@quickfixengine.org if any conditions of this licensing
 * are not clear to you.
 ******************************************************************************/

package org.quickfixj.field;

import java.math.BigDecimal;

import org.quickfixj.FIXField;
import org.quickfixj.engine.SessionRejectReason;

public class BigDecimalConverter {

	/**
	 * Converts a double to a string with no padding.
	 *
	 * @param d
	 *            the BigDecimal to convert
	 * @return the formatted String representing the incoming decimal.
	 * @see #convert(BigDecimal, int)
	 */
	public static String convert(BigDecimal d) {
		return d.toPlainString();
	}

	/**
	 * Converts a decimal to a string with padding.
	 *
	 * @param d
	 *            the decimal to convert
	 * @param padding
	 *            the number of zeros to add to end of the formatted decimal
	 * @return the formatted String representing the decimal.
	 */
	public static String convert(BigDecimal d, int padding) {
		return DoubleConverter.getDecimalFormat(padding).format(d);
	}

	/**
	 * Convert a String value to a decimal.
	 *
	 * @param value
	 *            the String value to convert
	 * @return the parsed BigDecimal
	 * @throws FieldConversionException
	 *             if the String is not a valid decimal pattern.
	 */
	public static BigDecimal convert(char[] value, int offset, int count) {

		if (value[offset] == '+') {
			throw new FieldConversionException("invalid double value: "
					+ new String(value, offset, count));
		}

		try {
			return new BigDecimal(value, offset, count);
		} catch (NumberFormatException e) {
			throw new FieldConversionException("invalid double value: "
					+ new String(value, offset, count));
		}
	}

	/**
	 * Converts a field value to an double.
	 *
	 * @param value
	 *            the field to convert
	 * @return If field value is an BigDecimal then the value, otherwise the
	 *         field characters parsed using {@link Double#parseDouble(String)}.
	 * @throws FieldException
	 *             if field value is not a valid format.
	 */
	public static BigDecimal convert(FIXField<?> field) {

		// if stored as an BigDecimal we can take a short cut
		// to save the effort of string parsing
		if (field.getValue() instanceof BigDecimal) {
			return (BigDecimal) field.getValue();
		}

		CharSequence chars = field.getCharacters();

		try {
			return new BigDecimal(chars.toString());
		} catch (NumberFormatException e) {
			throw new FieldException(
					SessionRejectReason.INCORRECT_DATA_FORMAT_FOR_VALUE,
					"invalid double value: " + field.getValue(), field.getTag());
		}
	}
}
