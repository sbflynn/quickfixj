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

import java.util.Arrays;
import java.util.List;

import org.quickfixj.FIXGroup;

/**
 * Represents a repeating field group within a message.
 */
public class Group extends AbstractFieldGraph implements FIXGroup {

    /**
     * The serialVersionUID property.
     */
    private static final long serialVersionUID = 1L;

    private int fieldTag;
    private int delim;

    /**
     * Create a group with the specified count and delimiter fields.
     *
     * @param field the count tag number
     * @param delim the delimiter tag number (first group field)
     */
    public Group(int fieldTag, int delim) {
        this(fieldTag, delim, Arrays.asList(delim));
    }

    /**
     * Create a group with the specified count and delimiter fields and
     * field ordering.
     *
     * @param field
     * @param delim
     * @param order
     */
    public Group(int fieldTag, int delim, List<Integer> order) {
        super(order);
        this.fieldTag = fieldTag;
        this.delim = delim;
    }

    /**
     * @return the group delimiter tag number.
     */
    public int delim() {
        return delim;
    }

    /**
     * @return the count field tag.
     */
    @Override
    public int getFieldTag() {
        return fieldTag;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public FIXGroup clone() {

        // careful not to call super.clone() or we end up
        // with a shallow copy of the field storage
        Group clone = (Group) super.clone();

        clone.delim = delim;
        clone.fieldTag = fieldTag;

        return clone;
    }
}
