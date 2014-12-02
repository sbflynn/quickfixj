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

package quickfix;

import java.io.Serializable;

import org.quickfixj.CharsetSupport;
import org.quickfixj.FIXField;
import org.quickfixj.field.AbstractField;

/**
 * Base class for FIX message fields. This class should be
 * abstract but that would break compatibility with the QF JNI
 * classes.
 */
public abstract class Field<T extends Serializable> extends AbstractField<T> {
    private static final long serialVersionUID = 7098326013456432197L;
    private int tag;
    private T object;
    private String data;

    protected Field(int field, T object) {
        this.tag = field;
        this.object = object;
    }

    /**
     * Gets the field's tag.
     *
     * @return the tag
     */
    @Override
    public int getTag() {
        return tag;
    }

    public void setTag(int tag) {
        this.tag = tag;
        data = null;
    }

    /**
     * Get the field value
     *
     * @return an object representing the field's value
     */
    @Override
    public T getValue() {
        return object;
    }

    /**
     * Sets the field's value to the given object.
     *
     * @param object
     */
    public void setObject(T object) {
        this.object = object;
        data = null;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public CharSequence getCharacters() {
        return getValue().toString();
    }

    /**
     * Return's the formatted field (tag=value<SOH>)
     *
     * @return the formatted field
     */
    @Override
    public String toString() {
        calculate();
        return data;
    }

    @Deprecated
    public void toString(StringBuilder buffer) {
        buffer.append(getTag()).append('=').append(getCharacters());
    }

    protected String objectAsString() {
        return object.toString();
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
        return object.hashCode();
    }

    /**
     * Returns the length of this field's FIX-encoded bytes (tag=value),
     * including the trailing SOH byte.
     *
     * @return the length of this field's encoded bytes
     */
    @Override
    public int getLength() {
        calculate();
        return MessageUtils.length(CharsetSupport.getCharsetInstance(), data) + 1;
    }

    /**
     * Returns the checksum of this field's FIX-encoded bytes (tag=value),
     * including the trailing SOH byte.
     *
     * @return the checksum of this field's encoded bytes
     */
    @Override
    public int getChecksum() {
        calculate();
        return (MessageUtils.checksum(CharsetSupport.getCharsetInstance(), data, false) + 1) & 0xFF;
    }

    private void calculate() {
        if (data == null) {

            StringBuilder buffer = new StringBuilder();

            buffer.append(getTag());
            buffer.append('=');
            buffer.append(getCharacters());

            data = buffer.toString();
        }
    }
}
