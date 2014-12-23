/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 20 Dec 2014 by stephen.flynn@jftechnology.com.
 */
package org.quickfixj.field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.quickfixj.FIXGroup;

import quickfix.Group;

/**
 * AbstractGroupFieldTest - provides...
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public class AbstractGroupFieldTest {

    /**
     * Test method for {@link org.quickfixj.field.AbstractGroupField#AbstractGroupField()}.
     */
    @Test
    public void testAbstractGroupField() {
        AbstractGroupField<FIXGroup> field;
        FIXGroup group;

        field = new GenericGroupField<FIXGroup>(1, 2);

        assertEquals(0, field.size());
        assertEquals(0, (int) field.getValue());

        // add a single empty group
        group = new Group(100, 2);

        field.add(group);

        assertEquals(1, field.size());
        assertEquals(1, (int) field.getValue());

        // add a single empty group
        group = new Group(200, 2);

        field.add(group);

        assertEquals(2, field.size());
        assertEquals(2, (int) field.getValue());

        // remove first group
        field.remove(0);

        assertEquals(1, field.size());
        assertEquals(1, (int) field.getValue());
        assertEquals(200, field.get(0).getFieldTag());
    }

    /**
     * Test method for {@link org.quickfixj.field.AbstractGroupField#AbstractGroupField(char[], int, int)}.
     */
    @Test
    public void testAbstractGroupFieldCharArrayIntInt() {
        AbstractGroupField<FIXGroup> field;
        FIXGroup group;

        field = new GenericGroupField<FIXGroup>(1, 2, "2".toCharArray(), 0, 1);

        assertEquals(0, field.size());
        assertEquals(2, (int) field.getValue());

        // add a single empty group
        group = new Group(100, 2);

        field.add(group);

        assertEquals(1, field.size());
        assertEquals(2, (int) field.getValue());

        // add a single empty group
        group = new Group(200, 2);

        field.add(group);

        assertEquals(2, field.size());
        assertEquals(2, (int) field.getValue());

        // remove first group
        field.remove(0);

        assertEquals(1, field.size());
        assertEquals(2, (int) field.getValue());
        assertEquals(200, field.get(0).getFieldTag());
    }

    /**
     * Test method for {@link org.quickfixj.field.AbstractGroupField#clone()}.
     */
    @Test
    public void testClone() {
        AbstractGroupField<FIXGroup> expected, actual;
        FIXGroup group;

        expected = new GenericGroupField<FIXGroup>(1, 2);
        actual = expected.clone();

        assertEquals(expected, actual);
        assertNotSame(expected, actual);

        // add a group
        group = new Group(3, 2);
        group.setField(new GenericField(100, "test"));

        expected.add(group);
        actual = expected.clone();

        assertEquals(expected, actual);
        // mutable group filed must not be the same
        assertNotSame(expected, actual);

        assertEquals(expected.get(0), actual.get(0));
        // mutable group must not be the same
        assertNotSame(expected.get(0), actual.get(0));

        assertEquals(expected.get(0).getField(100), actual.get(0).getField(100));
        // immutable group member can be the same
        assertSame(expected.get(0).getField(100), actual.get(0).getField(100));
    }

    /**
     * Test method for {@link org.quickfixj.field.AbstractField#equals(java.lang.Object)}.
     */
    @Test
    public void testEqualsObject() {
        AbstractGroupField<FIXGroup> expected, actual;
        FIXGroup groupExpected, groupActual;

        expected = new GenericGroupField<FIXGroup>(1, 2);
        actual = new GenericGroupField<FIXGroup>(1, 2);

        assertEquals(expected, actual);

        // add a single empty group
        groupExpected = new Group(3, 2);
        groupActual = new Group(3, 2);

        actual.add(groupActual);
        assertFalse(expected.equals(actual));
        expected.add(groupExpected);
        assertTrue(expected.equals(actual));

        // then populate it
        groupExpected.setField(new GenericField(5, "yes"));
        assertFalse(expected.equals(actual));
        groupActual.setField(new GenericField(5, "no"));
        assertFalse(expected.equals(actual));
        groupActual.setField(new GenericField(5, "yes"));
        assertTrue(expected.equals(actual));

        // add another empty group
        groupExpected = new Group(3, 2);
        groupActual = new Group(3, 2);

        actual.add(groupActual);
        assertFalse(expected.equals(actual));
        expected.add(groupExpected);
        assertTrue(expected.equals(actual));

        // then populate it
        groupExpected.setField(new GenericField(5, "yes"));
        assertFalse(expected.equals(actual));
        groupActual.setField(new GenericField(5, "no"));
        assertFalse(expected.equals(actual));
        groupActual.setField(new GenericField(5, "yes"));
        assertTrue(expected.equals(actual));
    }
}
