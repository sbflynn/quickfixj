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

public abstract class BooleanField extends AbstractField<Boolean> {

    /**
     * The serialVersionUID property.
     */
    private static final long serialVersionUID = 1L;

    private final boolean value;

    protected BooleanField(boolean value) {

        this.value = value;
    }

    protected BooleanField(char[] value, int offset, int count) {

        switch (value[offset]) {
        case 'Y':
            this.value = true;
            break;
        case 'N':
            this.value = false;
            break;

        default:
            // TODO : throw a more meaningful exception 
            throw new FieldConversionException("BAD BOOLEAN VALUE "
                    + new String(value, offset, count));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public CharSequence getCharacters() {
        return value ? "Y" : "N";
    }

    @Override
    public Boolean getValue() {
        return value;
    }
}
