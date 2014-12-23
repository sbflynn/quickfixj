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
 * Converts between a boolean and a string.
 */
public class BooleanConverter {
    private static final String NO = "N";
    private static final String YES = "Y";

    /**
     * Converts a boolean to a String.
     *
     * @param b the boolean value
     * @return "Y" for true and "N" for false.
     */
    public static String convert(boolean b) {
        return b ? YES : NO;
    }

    /**
     * Converts a String value to a boolean.
     *
     * @param value the String value to convert
     * @return true if "Y" and false if "N"
     * @throws FieldConversionException raised for any value other than "Y" or "N".
     */
    public static boolean convert(String value) {
        if (YES.equals(value)) {
            return true;
        } else if (NO.equals(value)) {
            return false;
        } else {
            throw new FieldConversionException("invalid boolean value: " + value);
        }
    }

    /**
     * Converts a field value to a boolean.
     *
     * @param value the field to convert
     * @return If field value is a boolean then the value, otherwise if the field 
     * characters is "Y" then true, if "N" then false.
     * @throws FieldException raised for any value other than true, false, "Y" or "N".
     */
    public static boolean convert(FIXField<?> field) {

        // if stored as an boolean value we can take a short cut 
        // to save the effort of string parsing
        if (field.getValue() instanceof Boolean) {
            return (Boolean) field.getValue();
        }

        CharSequence chars = field.getCharacters();

        if (chars.length() != 1) {
            throw new FieldException(SessionRejectReason.INCORRECT_DATA_FORMAT_FOR_VALUE,
                    "invalid boolean value: " + field.getValue(), field.getTag());
        }

        switch (chars.charAt(0)) {
        case 'Y':
            return true;
        case 'N':
            return false;
        default:
            throw new FieldException(SessionRejectReason.INCORRECT_DATA_FORMAT_FOR_VALUE,
                    "invalid boolean value: " + field.getValue(), field.getTag());
        }
    }
}
