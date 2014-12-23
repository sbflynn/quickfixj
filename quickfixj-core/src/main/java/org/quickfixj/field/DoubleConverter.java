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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.quickfixj.FIXField;
import org.quickfixj.engine.SessionRejectReason;

/**
 * Converts between a double and a String.
 */
public class DoubleConverter {
    private static final Pattern decimalPattern = Pattern.compile("-?\\d*(\\.\\d*)?");
    private static ThreadLocal<DecimalFormat[]> threadDecimalFormats = new ThreadLocal<DecimalFormat[]>();

    /**
     * Converts a double to a string with no padding.
     *
     * @param d the double to convert
     * @return the formatted String representing the double.
     * @see #convert(double, int)
     */
    public static String convert(double d) {
        return convert(d, 0);
    }

    static DecimalFormat getDecimalFormat(int padding) {
        if (padding > 14) {
            throw new FieldConversionException("maximum padding of 14 zeroes is supported: "
                    + padding);
        }
        DecimalFormat[] decimalFormats = threadDecimalFormats.get();
        if (decimalFormats == null) {
            decimalFormats = new DecimalFormat[14];
            threadDecimalFormats.set(decimalFormats);
        }
        DecimalFormat f = decimalFormats[padding];
        if (f == null) {
            StringBuilder buffer = new StringBuilder("0.");
            for (int i = 0; i < padding; i++) {
                buffer.append('0');
            }
            for (int i = padding; i < 14; i++) {
                buffer.append('#');
            }
            f = new DecimalFormat(buffer.toString());
            f.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
            decimalFormats[padding] = f;
        }
        return f;
    }

    /**
     * Converts a double to a string with padding.
     *
     * @param d the double to convert
     * @param padding the number of zeros to add to end of the formatted double
     * @return the formatted String representing the double.
     */
    public static String convert(double d, int padding) {
        return getDecimalFormat(padding).format(d);
    }

    /**
     * Convert a String value to a double.
     *
     * @param value the String value to convert
     * @return the parsed double
     * @throws FieldConversionException if the String is not a valid double pattern.
     */
    public static double convert(String value) {
        try {
            Matcher matcher = decimalPattern.matcher(value);
            if (!matcher.matches()) {
                throw new NumberFormatException();
            }
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new FieldConversionException("invalid double value: " + value);
        }
    }

    /**
     * Converts a field value to an double.
     *
     * @param value the field to convert
     * @return If field value is an double then the value, otherwise the field 
     * characters parsed using {@link Double#parseDouble(String)}.
     * @throws FieldException raised for any value other than true, false, "Y" or "N".
     */
    public static double convert(FIXField<?> field) {

        // if stored as an integer value we can take a short cut 
        // to save the effort of string parsing
        if (field.getValue() instanceof Double) {
            return (Double) field.getValue();
        }

        CharSequence chars = field.getCharacters();

        try {
            Matcher matcher = decimalPattern.matcher(chars);
            if (!matcher.matches()) {
                throw new NumberFormatException();
            }
            return Double.parseDouble(chars.toString());
        } catch (NumberFormatException e) {
            throw new FieldException(SessionRejectReason.INCORRECT_DATA_FORMAT_FOR_VALUE,
                    "invalid double value: " + field.getValue(), field.getTag());
        }
    }
}
