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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;

import org.junit.Test;
import org.quickfixj.CharsetSupport;
import org.quickfixj.FIXField;
import org.quickfixj.field.BooleanField;
import org.quickfixj.field.CharField;
import org.quickfixj.field.DateField;
import org.quickfixj.field.BigDecimalField;
import org.quickfixj.field.DoubleField;
import org.quickfixj.field.IntField;
import org.quickfixj.field.UtcDateOnlyField;
import org.quickfixj.field.UtcTimeOnlyField;
import org.quickfixj.field.UtcTimestampField;
import org.quickfixj.messages.bd.fix44.field.PossDupFlag;
import org.quickfixj.messages.bd.fix44.field.RawData;
import org.quickfixj.messages.bd.fix44.field.Side;

public class FieldTest {

    public void setUp() throws Exception {

        // no-op
    }

    public void tearDown() throws Exception {

        // no-op
    }

    @Test
    public void testMessageSetGetString() {

        Side side1 = new Side('1');
        Side side2 = new Side('1');
        assertEquals(side1, side2);
        assertTrue(side1.equals(Side.BUY));
        assertNotSame(side1, Side.BUY);
        assertFalse(side1.equals(Side.SELL));

        //        side2 = new Side();
        //        side2.setValue('1');
        //        assertEquals(side1, side2);
    }

    private void testFieldCalcuations(String value, int checksum, int length) {

        FIXField<String> field;

        field = new MockStringField(12, value);

        //   field.setObject(value);
        assertEquals("12=" + value + "\001", field.serialize(new StringBuffer()).toString());
        assertEquals(checksum, MessageUtils.checksum(CharsetSupport.getCharsetInstance(), field));
        assertEquals(length, MessageUtils.length(CharsetSupport.getCharsetInstance(), field));

        value = value.substring(0, value.length() - 1)
                + (char) (value.charAt(value.length() - 1) + 1);
        checksum = (checksum + 1) & 0xFF;

        field = new MockStringField(12, value);
        //   field.setObject(value);
        assertEquals("12=" + value + "\001", field.serialize(new StringBuffer()).toString());
        assertEquals(checksum, MessageUtils.checksum(CharsetSupport.getCharsetInstance(), field));
        assertEquals(length, MessageUtils.length(CharsetSupport.getCharsetInstance(), field));

        field = new MockStringField(13, value);
        checksum = (checksum + 1) & 0xFF;
        assertEquals("13=" + value + "\001", field.serialize(new StringBuffer()).toString());
        assertEquals(checksum, MessageUtils.checksum(CharsetSupport.getCharsetInstance(), field));
        assertEquals(length, MessageUtils.length(CharsetSupport.getCharsetInstance(), field));
    }

    @Test
    public void testFieldCalculationsWithDefaultCharset() {

        testFieldCalcuations("VALUE", 30, 9);
    }

    @Test
    public void testFieldCalculationsWithUTF8Charset() throws IOException {

        CharsetSupport.setCharset("UTF-8");
        try {
            testFieldCalcuations("\u6D4B\u9A8C\u6570\u636E", 50, 16);
        } finally {
            CharsetSupport.setCharset(CharsetSupport.getDefaultCharset());
        }
    }

    @Test
    public void testDateField() {

        DateField field;
        Date date = new Date();

        field = new MockDateField(11, date);

        //        field.setValue(date);
        assertEquals(11, field.getTag());
        assertEquals(date, field.getValue());
        //        field = new DateField(11, date);
        //        assertEquals(11, field.getTag());
        //        assertEquals(date, field.getValue());
    }

    @Test
    public void testUtcDateOnlyField() {

        UtcDateOnlyField field;
        Date date = new Date();

        field = new MockUtcDateOnlyField(11, date);

        //       field.setValue(date);
        assertEquals(11, field.getTag());
        assertEquals(date, field.getValue());
        //        field = new UtcDateOnlyField(11, date);
        //        assertEquals(11, field.getTag());
        //        assertEquals(date, field.getValue());
    }

    @Test
    public void testUtcTimeOnlyField() {

        UtcTimeOnlyField field;
        Date date = new Date();

        field = new MockUtcTimeOnlyField(11, date);

        //        field.setValue(date);
        assertEquals(11, field.getTag());
        assertEquals(date, field.getValue());
        //        field = new UtcTimeOnlyField(11, date);
        //        assertEquals(11, field.getTag());
        //        assertEquals(date, field.getValue());
    }

    @Test
    public void testUtcTimeStampField() {

        UtcTimestampField field;
        Date date = new Date();

        field = new MockUtcTimeStampField(11, date);

        //        field.setValue(date);
        assertEquals(11, field.getTag());
        assertEquals(date, field.getValue());
        //        field = new UtcTimeStampField(11, date);
        //        assertEquals(11, field.getTag());
        //        assertEquals(date, field.getValue());
    }

    @Test
    public void testBooleanField() {

        PossDupFlag field;

        field = new PossDupFlag(true);
        // field.setValue(true);
        assertEquals(PossDupFlag.TAG, field.getTag());
        assertEquals(Boolean.TRUE, field.getValue());
        //   assertEquals(PossDupFlag.POSSIBLE_DUPLICATE, field);

        field = new PossDupFlag(false);
        // field.setValue(Boolean.FALSE);
        assertEquals(PossDupFlag.TAG, field.getTag());
        assertEquals(Boolean.FALSE, field.getValue());
        //     assertEquals(PossDupFlag.ORIGINAL_TRANSMISSION, field);

        field = new PossDupFlag("Y".toCharArray(), 0, 1);
        // field.setValue(true);
        assertEquals(PossDupFlag.TAG, field.getTag());
        assertEquals(Boolean.TRUE, field.getValue());
        //     assertEquals(PossDupFlag.POSSIBLE_DUPLICATE, field);

        field = new PossDupFlag("N".toCharArray(), 0, 1);
        // field.setValue(Boolean.FALSE);
        assertEquals(PossDupFlag.TAG, field.getTag());
        assertEquals(Boolean.FALSE, field.getValue());
        //     assertEquals(PossDupFlag.ORIGINAL_TRANSMISSION, field);

        // field = new BooleanField(22, true);
        // assertEquals(22, field.getTag());
        // assertEquals(true, field.getValue());
        // field = new BooleanField(33, Boolean.TRUE);
        // assertEquals(33, field.getTag());
        // assertEquals(true, field.getValue());
    }

    @Test
    public void testDoubleField() {

        DoubleField field;

        field = new MockDoubleField(11, 12.3);

        //      field.setValue(12.3);
        assertEquals(11, field.getTag());
        assertEquals(12.3, field.getValue(), 0);

        field = new MockDoubleField(11, "12.3");

        //      field.setValue(12.3);
        assertEquals(11, field.getTag());
        assertEquals(12.3, field.getValue(), 0);

        //        field.setValue(new Double(23.4));
        //        assertEquals(11, field.getTag());
        //        assertEquals(23.4, field.getValue(), 0);
        //        field = new DoubleField(22, 34.5);
        //        assertEquals(22, field.getTag());
        //        assertEquals(34.5, field.getValue(), 0);
        //        field = new DoubleField(33, new Double(45.6));
        //        assertEquals(33, field.getTag());
        //        assertEquals(45.6, field.getValue(), 0);
    }

    @SuppressWarnings("unused")
    @Test(expected = NumberFormatException.class)
    public void testDoubleFieldException() {
        new MockDoubleField(11, Double.NaN);
    }

    @Test
    public void testDecimalField() {

        BigDecimalField field;
        field = new MockDecimalField(11, new BigDecimal("12.3"));
        //   field.setValue(12.3);
        assertEquals(11, field.getTag());
        assertEquals(BigDecimal.valueOf(12.3), field.getValue());

        field = new MockDecimalField(11, "12.3");
        //   field.setValue(12.3);
        assertEquals(11, field.getTag());
        assertEquals(BigDecimal.valueOf(12.3), field.getValue());

        //        field.setValue(23.4);
        //        assertEquals(11, field.getTag());
        //        assertEquals(BigDecimal.valueOf(23.4), field.getValue());
        //        field = new DecimalField(22, 34.5);
        //        assertEquals(22, field.getTag());
        //        assertEquals(BigDecimal.valueOf(34.5), field.getValue());
        //        field = new DecimalField(33, new Double(45.6));
        //        assertEquals(33, field.getTag());
        //        assertEquals(BigDecimal.valueOf(45.6), field.getValue());
    }

    @SuppressWarnings("unused")
    @Test(expected = NumberFormatException.class)
    public void testDecimalFieldException() {
        new MockDecimalField(11, BigDecimal.valueOf(Double.POSITIVE_INFINITY));
    }

    @Test
    public void testCharField() {

        MockCharField field;

        field = new MockCharField(11, 'x');
        //        field.setValue('x');
        assertEquals(11, field.getTag());
        assertEquals(Character.valueOf('x'), field.getValue());

        field = new MockCharField(11, "X");
        // field.setValue(Character.valueOf('X'));
        assertEquals(11, field.getTag());
        assertEquals(Character.valueOf('X'), field.getValue());
        //        field = new CharField(22, 'a');
        //        assertEquals(22, field.getTag());
        //        assertEquals('a', field.getValue());
        //        field = new CharField(33, Character.valueOf('A'));
        //        assertEquals(33, field.getTag());
        //        assertEquals('A', field.getValue());
    }

    @Test
    public void testIntField() {

        IntField field;

        field = new MockIntField(11, 12);
        //      field.setValue(12);
        assertEquals(11, field.getTag());
        assertEquals(Integer.valueOf(12), field.getValue());
        assertEquals("12", field.getCharacters().toString());

        field = new MockIntField(11, "23");
        //    field.setValue(Integer.valueOf(23));
        assertEquals(11, field.getTag());
        assertEquals(Integer.valueOf(23), field.getValue());
        assertEquals("23", field.getCharacters().toString());
        //        field = new IntField(22, 23);
        //        assertEquals(22, field.getTag());
        //        assertEquals(23, field.getValue());
        //        field = new IntField(33, Integer.valueOf(44));
        //        assertEquals(33, field.getTag());
        //        assertEquals(44, field.getValue());
    }

    @Test
    public void testBytesField() {

        byte[] data = "rawdata".getBytes();

        RawData field = new RawData(data);
        //     field.setValue(data);
        assertEquals(RawData.TAG, field.getTag());
        assertTrue(Arrays.equals(data, field.getValue()));
        assertEquals("rawdata", field.getCharacters());

        StringBuilder sb = new StringBuilder();
        field.serialize(sb);
        assertEquals("96=rawdata\001", sb.toString());
    }

    @Test
    public void testFieldhashCode() throws Exception {

        assertEqualsAndHash(new MockIntField(11, 100), new MockIntField(11, 100));
        assertEqualsAndHash(new MockIntField(11, "100"), new MockIntField(11, 100));

        assertEqualsAndHash(new MockDoubleField(11, 100.0), new MockDoubleField(11, 100.0));
        assertEqualsAndHash(new MockDoubleField(11, "100.0"), new MockDoubleField(11, 100.0));

        assertEqualsAndHash(new MockStringField(11, "foo"), new MockStringField(11, "foo"));

        assertEqualsAndHash(new MockBooleanField(11, true), new MockBooleanField(11, true));
        assertEqualsAndHash(new MockBooleanField(11, true), new MockBooleanField(11, "Y"));
        assertEqualsAndHash(new MockBooleanField(11, false), new MockBooleanField(11, "N"));

        assertEqualsAndHash(new MockCharField(11, 'x'), new MockCharField(11, 'x'));
        assertEqualsAndHash(new MockCharField(11, "x"), new MockCharField(11, 'x'));

        Date date = new Date();
        assertEqualsAndHash(new MockUtcDateOnlyField(11, date), new MockUtcDateOnlyField(11, date));
        assertEqualsAndHash(new MockUtcTimeOnlyField(11, date), new MockUtcTimeOnlyField(11, date));
        assertEqualsAndHash(new MockUtcTimeStampField(11, date),
                new MockUtcTimeStampField(11, date));
    }

    private void assertEqualsAndHash(FIXField<?> field1, FIXField<?> field2) {

        assertEquals("fields not equal", field1, field2);
        assertEquals("fields hashcode not equal", field1.hashCode(), field2.hashCode());
    }

    private class MockStringField extends org.quickfixj.field.StringField {

        /**
         * The serialVersionUID property.
         */
        private static final long serialVersionUID = 1L;

        private final int tag;

        protected MockStringField(int tag, String value) {
            super(value);
            this.tag = tag;
        }

        @Override
        public int getTag() {
            return tag;
        }
    }

    private class MockIntField extends IntField {

        /**
         * The serialVersionUID property.
         */
        private static final long serialVersionUID = 1L;

        private final int tag;

        protected MockIntField(int tag, int value) {
            super(value);
            this.tag = tag;
        }

        protected MockIntField(int tag, String value) {
            super(value.toCharArray(), 0, value.length());
            this.tag = tag;
        }

        @Override
        public int getTag() {
            return tag;
        }
    }

    private class MockBooleanField extends BooleanField {

        /**
         * The serialVersionUID property.
         */
        private static final long serialVersionUID = 1L;

        private final int tag;

        protected MockBooleanField(int tag, boolean value) {
            super(value);
            this.tag = tag;
        }

        protected MockBooleanField(int tag, String value) {
            super(value.toCharArray(), 0, value.length());
            this.tag = tag;
        }

        @Override
        public int getTag() {
            return tag;
        }
    }

    private class MockCharField extends CharField {

        /**
         * The serialVersionUID property.
         */
        private static final long serialVersionUID = 1L;

        private final int tag;

        protected MockCharField(int tag, char value) {
            super(value);
            this.tag = tag;
        }

        protected MockCharField(int tag, String value) {
            super(value.toCharArray(), 0, value.length());
            this.tag = tag;
        }

        @Override
        public int getTag() {
            return tag;
        }
    }

    private class MockDoubleField extends DoubleField {

        /**
         * The serialVersionUID property.
         */
        private static final long serialVersionUID = 1L;

        private final int tag;

        protected MockDoubleField(int tag, double value) {
            super(value);
            this.tag = tag;
        }

        protected MockDoubleField(int tag, String value) {
            super(value.toCharArray(), 0, value.length());
            this.tag = tag;
        }

        @Override
        public int getTag() {
            return tag;
        }
    }

    private class MockDecimalField extends BigDecimalField {

        /**
         * The serialVersionUID property.
         */
        private static final long serialVersionUID = 1L;

        private final int tag;

        protected MockDecimalField(int tag, BigDecimal value) {
            super(value);
            this.tag = tag;
        }

        protected MockDecimalField(int tag, String value) {
            super(value.toCharArray(), 0, value.length());
            this.tag = tag;
        }

        @Override
        public int getTag() {
            return tag;
        }
    }

    private class MockDateField extends DateField {

        /**
         * The serialVersionUID property.
         */
        private static final long serialVersionUID = 1L;

        private final int tag;

        protected MockDateField(int tag, Date value) {
            super(value);
            this.tag = tag;
        }

        @Override
        public int getTag() {
            return tag;
        }
    }

    private class MockUtcDateOnlyField extends UtcDateOnlyField {

        /**
         * The serialVersionUID property.
         */
        private static final long serialVersionUID = 1L;

        private final int tag;

        protected MockUtcDateOnlyField(int tag, Date value) {
            super(value);
            this.tag = tag;
        }

        protected MockUtcDateOnlyField(int tag, String value) {
            super(value.toCharArray(), 0, value.length());
            this.tag = tag;
        }

        @Override
        public int getTag() {
            return tag;
        }
    }

    private class MockUtcTimeOnlyField extends UtcTimeOnlyField {

        /**
         * The serialVersionUID property.
         */
        private static final long serialVersionUID = 1L;

        private final int tag;

        protected MockUtcTimeOnlyField(int tag, Date value) {
            super(value);
            this.tag = tag;
        }

        protected MockUtcTimeOnlyField(int tag, String value) {
            super(value.toCharArray(), 0, value.length());
            this.tag = tag;
        }

        @Override
        public int getTag() {
            return tag;
        }
    }

    private class MockUtcTimeStampField extends UtcTimestampField {

        /**
         * The serialVersionUID property.
         */
        private static final long serialVersionUID = 1L;

        private final int tag;

        protected MockUtcTimeStampField(int tag, Date value) {
            super(value);
            this.tag = tag;
        }

        protected MockUtcTimeStampField(int tag, String value) {
            super(value.toCharArray(), 0, value.length());
            this.tag = tag;
        }

        @Override
        public int getTag() {
            return tag;
        }
    }
}
