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

import java.io.IOException;
import java.io.Serializable;

import org.quickfixj.FIXField;

/**
 * Base class for FIX message fields. This class should be
 * abstract but that would break compatibility with the QF JNI
 * classes.
 */
public abstract class AbstractField<T extends Serializable> implements FIXField<T>, Serializable,
        Cloneable {

    /**
     * The serialVersionUID property.
     */
    private static final long serialVersionUID = 1L;

    private static final char FIELD_SEPARATOR = '\001';

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public CharSequence getCharacters() {
        return getValue().toString();
    }

    @Override
    public boolean equals(Object object) {

        if (this == object) {

            return true;
        }

        if (object instanceof FIXField<?>) {

            FIXField<?> field = (FIXField<?>) object;

            if (getTag() != field.getTag()) {

                return false;
            }

            return getCharacters().equals(field.getCharacters());
        }

        return false;
    }

    @Override
    public int hashCode() {

        // REVIEW : Should include the tag ?
        return getValue().hashCode();
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public Appendable serialize(Appendable appendable) {

        try {

            // subclasses might overwrite to improve performance
            // may be able to rework integer serialization as well
            appendable.append(getTagCharacters());
            appendable.append('=');
            appendable.append(getCharacters());
            appendable.append(FIELD_SEPARATOR);

            return appendable;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected CharSequence getTagCharacters() {
        // subclasses might overwrite to improve performance
        return String.valueOf(getTag());
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public FIXField<T> clone() {
        return this;
    }
}
