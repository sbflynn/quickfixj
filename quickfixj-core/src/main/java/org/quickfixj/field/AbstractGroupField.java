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
import java.util.Iterator;
import java.util.List;

import org.quickfixj.FIXGroup;
import org.quickfixj.FIXGroupField;

/**
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public abstract class AbstractGroupField<T extends FIXGroup> extends AbstractField<Integer>
        implements FIXGroupField<T> {

    /**
     * The serialVersionUID property.
     */
    private static final long serialVersionUID = 1L;

    private static final int USE_GROUP_SIZE = -1;

    private int numInGroup;

    private List<T> groups;

    protected AbstractGroupField() {

        this(USE_GROUP_SIZE, new ArrayList<T>());
    }

    protected AbstractGroupField(char[] value, int offset, int count) {

        this.numInGroup = Integer.parseInt(new String(value, offset, count));
        this.groups = new ArrayList<T>(numInGroup);
    }

    protected AbstractGroupField(int numInGroup, List<T> groups) {

        this.groups = groups;
        this.numInGroup = numInGroup;
    }

    /**
     * Get the groups property.
     *
     * @return Returns the groups.
     * @since 2.0
     */
    protected List<T> getGroups() {
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

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public Iterator<T> iterator() {
        return getGroups().iterator();
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public T add(T group) {
        getGroups().add(group);
        return group;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public void remove(T group) {
        getGroups().remove(group);
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public void remove(int index) {
        getGroups().remove(index);
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public T get(int index) {
        return getGroups().get(index);
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public void set(int index, T group) {
        getGroups().set(index, group);
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public int size() {
        return getGroups().size();
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!FIXGroupField.class.isInstance(object)) {
            return false;
        }

        FIXGroupField<?> field = (FIXGroupField<FIXGroup>) object;

        if (getTag() != field.getTag() || size() != field.size()) {
            return false;
        }

        for (int i = 0; i < size(); i++) {
            if (!get(i).equals(field.get(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public AbstractGroupField<T> clone() {

        try {

            // careful not to call super.clone() or we end up
            // with a shallow copy of the field storage
            AbstractGroupField<T> clone = getClass().newInstance();

            clone.numInGroup = numInGroup;
            clone.groups = new ArrayList<T>(size());

            for (T group : this) {
                clone.add((T) group.clone());
            }

            return clone;

        } catch (final InstantiationException e) {
            throw new RuntimeException(e);
        } catch (final IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
