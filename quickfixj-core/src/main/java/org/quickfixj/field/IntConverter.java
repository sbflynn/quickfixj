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

import org.quickfixj.FIXField;
import org.quickfixj.engine.SessionRejectReason;

/**
 * Convert between an integer and a String
 */
public final class IntConverter {

    /**
     * Convert and integer to a String
     *
     * @param i the integer to convert
     * @return the String representing the integer
     * @see java.lang.Long#toString(long)
     */
    public static String convert(int i) {
        return Long.toString(i);
    }

    /**
     * Convert a String to an integer.
     *
     * @param value the String to convert
     * @return the converted integer
     * @throws FieldConversionException raised if the String does not represent a valid integer
     * @see java.lang.Integer#parseInt(String)
     */
    public static int convert(String value) {
        try {
            for (int i = 0; i < value.length(); i++) {
                if (!Character.isDigit(value.charAt(i)) && !(i == 0 && value.charAt(i) == '-')) {
                    throw new FieldConversionException("invalid integral value: " + value);
                }
            }
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new FieldConversionException("invalid integral value: " + value + ": " + e);
        }
    }

    /**
     * Converts a field value to an int.
     *
     * @param value the field to convert
     * @return If field value is an int then the value, otherwise the field 
     * characters parsed .
     * @throws FieldException raised for any value other than true, false, "Y" or "N".
     */
    public static int convert(FIXField<?> field) {

        // if stored as an integer value we can take a short cut 
        // to save the effort of string parsing
        if (field.getValue() instanceof Integer) {
            return (Integer) field.getValue();
        }

        CharSequence chars = field.getCharacters();

        try {
            for (int i = 0; i < chars.length(); i++) {
                if (!Character.isDigit(chars.charAt(i)) && !(i == 0 && chars.charAt(i) == '-')) {
                    throw new FieldException(SessionRejectReason.INCORRECT_DATA_FORMAT_FOR_VALUE,
                            "invalid integral value: " + field.getValue(), field.getTag());
                }
            }
            return Integer.parseInt(chars.toString());
        } catch (NumberFormatException e) {
            throw new FieldException(SessionRejectReason.INCORRECT_DATA_FORMAT_FOR_VALUE,
                    "invalid integral value: " + field.getValue(), field.getTag());
        }
    }
}
