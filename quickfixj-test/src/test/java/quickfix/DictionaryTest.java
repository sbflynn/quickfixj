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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.quickfixj.field.FieldConversionException;

public class DictionaryTest {

    private Dictionary dictionary;
    private Locale defaultLocale;

    @Before
    public void setUp() throws Exception {
        dictionary = new Dictionary();
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @After
    public void tearDown() throws Exception {
        Locale.setDefault(defaultLocale);
        defaultLocale = null;
        dictionary = null;
    }

    @Test
    public void testDay() throws ConfigError {
        assertFalse(dictionary.has("DAY"));
        dictionary.setString("DAY", "monday");
        assertTrue(dictionary.has("DAY"));
        assertEquals(2, dictionary.getDay("DAY"));

        dictionary.setString("DAY", "tue");
        assertEquals(3, dictionary.getDay("DAY"));

        dictionary.setString("DAY", "we");
        assertEquals(4, dictionary.getDay("DAY"));
    }

    @Test(expected = ConfigError.class)
    @Ignore
    // SBF : doesn't throw an exception - original test 
    // was meaningless
    public void testDayTooShort() throws ConfigError {
        dictionary.setString("DAY", "t");
        dictionary.getDay("DAY");
    }

    @Test(expected = ConfigError.class)
    public void testDayUnknown() throws ConfigError {
        dictionary.setString("DAY", "xyz");
        dictionary.getDay("DAY");
    }

    @Test
    public void testBoolean() throws Exception {
        dictionary.setBool("B", true);
        assertTrue(dictionary.getBool("B"));

        dictionary.setBool("B", false);
        assertFalse(dictionary.getBool("B"));
    }

    @Test(expected = FieldConversionException.class)
    public void testBooleanError() throws ConfigError {
        dictionary.setString("B", "XYZ");
        dictionary.getBool("B");
    }

    @Test(expected = ConfigError.class)
    public void testBooleanMissing() throws ConfigError {
        dictionary.getBool("B");
    }

    @Test
    public void testString() throws ConfigError {
        dictionary.setString("B", "X");
        assertEquals("X", dictionary.getString("B"));
    }

    @Test(expected = ConfigError.class)
    public void testStringMissing() throws ConfigError {
        dictionary.getString("X");
    }

    @Test
    public void testDouble() throws ConfigError {
        dictionary.setDouble("B", 1.1);
        assertEquals(1.1, dictionary.getDouble("B"), 0);
    }

    @Test(expected = FieldConversionException.class)
    public void testDoubleError() throws ConfigError {
        dictionary.setString("B", "XYZ");
        dictionary.getDouble("B");
    }

    @Test(expected = ConfigError.class)
    public void testDoubleMissing() throws ConfigError {
        dictionary.getDouble("B");
    }

    @Test
    public void testLong() throws Exception {
        dictionary.setLong("B", 1);
        assertEquals(1, dictionary.getLong("B"));
    }

    @Test(expected = FieldConversionException.class)
    public void testLongError() throws Exception {
        dictionary.setString("B", "XYZ");
        dictionary.getLong("B");
    }

    @Test(expected = ConfigError.class)
    public void testLongMissing() throws Exception {
        dictionary.getLong("B");
    }

    @Test
    public void testMerge() throws ConfigError {
        Dictionary d2 = new Dictionary("ABC");
        d2.setString("XYZ", "123");

        dictionary.setString("ABC", "DEF");
        assertFalse(dictionary.has("XYZ"));
        dictionary.merge(d2);
        assertTrue(dictionary.has("XYZ"));
        assertEquals("123", dictionary.getString("XYZ"));
        assertEquals(2, dictionary.toMap().size());

        assertEquals(1, d2.toMap().size());
    }

    @Test
    public void testName() throws Exception {
        assertNull(dictionary.getName());

        Dictionary d = new Dictionary("NAME");
        assertEquals("NAME", d.getName());
    }

    @Test
    public void testConstructors() throws Exception {
        Dictionary dw = new Dictionary();
        assertNull(dw.getName());
        assertTrue(dw.toMap().isEmpty());

        // Tested dictionary("name") already
        Dictionary dx = new Dictionary("NAME");
        dx.setString("FOO", "BAR");

        Dictionary dy = new Dictionary(dx);
        assertEquals("NAME", dy.getName());
        assertEquals(1, dy.toMap().size());
        assertEquals("BAR", dy.getString("FOO"));

        Dictionary dz = new Dictionary("XYZ", dx.toMap());
        assertEquals("XYZ", dz.getName());
        assertEquals(1, dz.toMap().size());
        assertEquals("BAR", dz.getString("FOO"));
    }

    // From C++ tests
    @Test
    public void testGetDay() throws Exception {
        Dictionary object = new Dictionary();

        object.setString("DAY1", "SU");
        object.setString("DAY2", "MO");
        object.setString("DAY3", "TU");
        object.setString("DAY4", "WE");
        object.setString("DAY5", "TH");
        object.setString("DAY6", "FR");
        object.setString("DAY7", "SA");

        assertEquals(1, object.getDay("DAY1"));
        assertEquals(2, object.getDay("DAY2"));
        assertEquals(3, object.getDay("DAY3"));
        assertEquals(4, object.getDay("DAY4"));
        assertEquals(5, object.getDay("DAY5"));
        assertEquals(6, object.getDay("DAY6"));
        assertEquals(7, object.getDay("DAY7"));

        object.setDay("NEXTDAY1", 1);
        object.setDay("NEXTDAY2", 2);
        object.setDay("NEXTDAY3", 3);
        object.setDay("NEXTDAY4", 4);
        object.setDay("NEXTDAY5", 5);
        object.setDay("NEXTDAY6", 6);
        object.setDay("NEXTDAY7", 7);

        assertEquals(1, object.getDay("NEXTDAY1"));
        assertEquals(2, object.getDay("NEXTDAY2"));
        assertEquals(3, object.getDay("NEXTDAY3"));
        assertEquals(4, object.getDay("NEXTDAY4"));
        assertEquals(5, object.getDay("NEXTDAY5"));
        assertEquals(6, object.getDay("NEXTDAY6"));
        assertEquals(7, object.getDay("NEXTDAY7"));
    }
}
