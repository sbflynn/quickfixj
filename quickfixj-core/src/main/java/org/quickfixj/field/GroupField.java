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

import java.util.ArrayList;
import java.util.List;

import org.quickfixj.FIXGroup;
import org.quickfixj.FIXGroupField;

/**
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public abstract class GroupField<T extends FIXGroup> extends AbstractField<Integer> implements
        FIXGroupField<T> {

    /**
     * The serialVersionUID property.
     */
    private static final long serialVersionUID = 1L;

    private static final int USE_GROUP_SIZE = -1;

    private final List<T> groups;

    private int numInGroup;

    public GroupField(int numInGroup) {

        this.groups = new ArrayList<T>();
        this.numInGroup = numInGroup;
    }

    public GroupField(CharSequence characters) {

        this.groups = new ArrayList<T>();
        this.numInGroup = Integer.parseInt(characters.toString());
    }

    /**
     * Get the groups property.
     *
     * @return Returns the groups.
     * @since 2.0
     */
    @Override
    public List<T> getGroups() {
        return groups;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public Integer getValue() {

        return numInGroup != USE_GROUP_SIZE ? numInGroup : getGroups().size();
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public CharSequence getCharacters() {

        return String.valueOf(getValue());
    }
}
