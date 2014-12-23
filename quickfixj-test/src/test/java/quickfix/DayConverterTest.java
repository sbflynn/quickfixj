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

import java.util.Locale;

import org.junit.Test;

public class DayConverterTest {

    private Locale defaultLocale;

    protected void setUp() throws Exception {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    protected void tearDown() throws Exception {
        Locale.setDefault(defaultLocale);
        defaultLocale = null;
    }

    @Test
    public void testConversionToInt() throws Exception {
        assertEquals(1, DayConverter.toInteger("sU"));
        assertEquals(4, DayConverter.toInteger("WEDnes"));

        // check that day values are US days either default local is set to France
        Locale.setDefault(Locale.FRANCE);
        assertEquals(1, DayConverter.toInteger("Su"));
        assertEquals(2, DayConverter.toInteger("Mo"));
    }

    @Test(expected = ConfigError.class)
    public void testConversionToInt_FAIL() throws Exception {
        DayConverter.toInteger("bogus");
    }

    @Test
    public void testConversionToString() throws Exception {
        Locale.setDefault(Locale.US);
        assertEquals("sunday", DayConverter.toString(1));
        assertEquals("wednesday", DayConverter.toString(4));

        // check that day values are US days either default local is set to France
        Locale.setDefault(Locale.FRANCE);
        assertEquals("sunday", DayConverter.toString(1));
        assertEquals("wednesday", DayConverter.toString(4));
    }

    @Test(expected = ConfigError.class)
    public void testConversionToString_FAIL() throws Exception {
        DayConverter.toString(999);
    }
}
