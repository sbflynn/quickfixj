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
 * Converts between a character and a String.
 */
public class CharConverter {

    /**
     * Converts a character to a String
     *
     * @param c the character to convert
     * @return a single character String based on the converted character
     * @see java.lang.Character#toString(char)
     */
    public static String convert(char c) {
        return Character.toString(c);
    }

    /**
     * Convert a String value to a character.
     *
     * @param value
     * @return the converted character
     * @throws FieldConversionException if String length != 1
     */
    public static char convert(String value) {
        if (value.length() != 1) {
            throw new FieldConversionException("invalid character value: " + value);
        }
        return value.charAt(0);
    }

    /**
     * Converts a field value to a character.
     *
     * @param value the field to convert
     * @return If field value is a character then the value, otherwise if the field 
     * characters length is 1 the first character.
     * @throws FieldException raised for any value other than true, false, "Y" or "N".
     */
    public static char convert(FIXField<?> field) {

        // if stored as an character value we can take a short cut 
        // to save the effort of string parsing
        if (field.getValue() instanceof Character) {
            return (Character) field.getValue();
        }

        CharSequence chars = field.getCharacters();

        if (chars.length() != 1) {
            throw new FieldException(SessionRejectReason.INCORRECT_DATA_FORMAT_FOR_VALUE,
                    "invalid character value: " + field.getValue(), field.getTag());
        }

        return chars.charAt(0);
    }
}
