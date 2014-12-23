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

import org.quickfixj.FIXGroup;

/**
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public final class GenericGroupField<T extends FIXGroup> extends AbstractGroupField<T> {

    /**
     * The serialVersionUID property.
     */
    private static final long serialVersionUID = 1L;

    private int tag;

    private int delimiterField;

    public GenericGroupField(int tag, int delimiterField) {

        super();

        this.tag = tag;
        this.delimiterField = delimiterField;
    }

    public GenericGroupField(int tag, int delimiterField, char[] value, int offset, int count) {

        super(value, offset, count);

        this.tag = tag;
        this.delimiterField = delimiterField;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public int getTag() {
        return tag;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public int getDelimiterField() {
        return delimiterField;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public GenericGroupField<T> clone() {

        // careful not to call super.clone() or we end up
        // with a shallow copy of the field storage
        GenericGroupField<T> clone = new GenericGroupField<T>(getTag(), getDelimiterField());

        for (T group : this) {
            clone.add((T) group.clone());
        }

        return clone;
    }
}
