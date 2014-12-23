/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 6 Dec 2014 by stephen.flynn@jftechnology.com.
 */
package quickfix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * MessageTokenizerTest - provides...
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public class MessageTokenIteratorTest {

    @Test
    public void testMessageTokenIterator() {

        MessageTokenIterator iterator = new MessageTokenIterator("8=FIX.4.4\0019=0\00135=DA\001");

        assertEquals(0, iterator.getCurrentStart());
        assertEquals(1, iterator.getCurrentSeparator());
        assertEquals(9, iterator.getCurrentEnd());
        assertEquals(8, iterator.getTag());
        assertFalse(iterator.finished());
        assertEquals("FIX.4.4", iterator.toString());
        assertEquals(7, iterator.length());
        assertEquals("IX.", iterator.subSequence(1, 4));
        assertTrue(iterator.hasNext());

        assertEquals(9, iterator.next());
        assertEquals(10, iterator.getCurrentStart());
        assertEquals(11, iterator.getCurrentSeparator());
        assertEquals(13, iterator.getCurrentEnd());
        assertEquals(9, iterator.getTag());
        assertFalse(iterator.finished());
        assertEquals("0", iterator.toString());
        assertEquals(1, iterator.length());
        assertTrue(iterator.hasNext());

        assertEquals(35, iterator.next());
        assertEquals(14, iterator.getCurrentStart());
        assertEquals(16, iterator.getCurrentSeparator());
        assertEquals(19, iterator.getCurrentEnd());
        assertEquals(35, iterator.getTag());
        assertFalse(iterator.finished());
        assertEquals("DA", iterator.toString());
        assertEquals(2, iterator.length());
        assertEquals('D', iterator.charAt(0));
        assertEquals('A', iterator.charAt(1));
        assertFalse(iterator.hasNext());

        assertEquals(35, iterator.next());
        assertTrue(iterator.finished());

    }

    @SuppressWarnings("unused")
    @Test(expected = InvalidMessage.class)
    public void testMessageTokenIterator_FAIL1() {

        new MessageTokenIterator("8S=FIX.4.4\0019=0\00135=DA\001");
    }

    @Test(expected = InvalidMessage.class)
    public void testMessageTokenIterator_FAIL2() {

        MessageTokenIterator iterator = new MessageTokenIterator("8=FIX.4.4\001X=0\00135=DA\001");
        iterator.next();
    }
}
