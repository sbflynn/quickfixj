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

package quickfix.mina.message;

import java.io.UnsupportedEncodingException;

import org.apache.mina.filter.codec.ProtocolCodecException;
import org.quickfixj.CharsetSupport;
import org.quickfixj.FIXMessage;
import org.quickfixj.messages.bd.fix44.News;
import org.quickfixj.messages.bd.fix44.field.Headline;

import junit.framework.ComparisonFailure;
import junit.framework.TestCase;

public class FIXMessageEncoderTest extends TestCase {

    @Override
    public void tearDown() throws UnsupportedEncodingException {

        // reset charset after every test
        CharsetSupport.setCharset(CharsetSupport.getDefaultCharset());
    }

    public void testEncoding() throws Exception {

        FIXMessageEncoder encoder = new FIXMessageEncoder();

        FIXMessage message = new org.quickfixj.messages.bd.fix40.Logon();

        message.getHeader().setField(new org.quickfixj.messages.bd.fix40.field.SenderCompID("TW"));
        message.getHeader()
                .setField(new org.quickfixj.messages.bd.fix40.field.TargetCompID("ISLD"));

        ProtocolEncoderOutputForTest protocolEncoderOutputForTest = new ProtocolEncoderOutputForTest();
        encoder.encode(null, message, protocolEncoderOutputForTest);
        assertTrue(protocolEncoderOutputForTest.buffer.limit() > 0);
    }

    public void testWesternEuropeanEncoding() throws Exception {

        String headline = "\u00E4bcf\u00F6d\u00E7\u00E9";

        // Default encoding, should work
        doEncodingTest(headline);

        try {
            // This will break because of European characters
            CharsetSupport.setCharset("US-ASCII");
            doEncodingTest(headline);
            fail("wrong charset should fail");
        } catch (ComparisonFailure e) {
            // expected
        }
    }

    public void testChineseEncoding() throws Exception {

        String headline = "\u6D4B\u9A8C\u6570\u636E";

        try {
            // This will break because of Chinese characters
            doEncodingTest(headline);
            fail("wrong charset should fail");
        } catch (ComparisonFailure e) {
            // expected
        }

        // This should work
        CharsetSupport.setCharset("UTF-8");
        doEncodingTest(headline);
    }

    private void doEncodingTest(String headline) throws ProtocolCodecException,
            UnsupportedEncodingException {

        // äbcfödçé
        News news = new News();
        news.setHeadline(new Headline(headline));
        FIXMessageEncoder encoder = new FIXMessageEncoder();
        ProtocolEncoderOutputForTest encoderOut = new ProtocolEncoderOutputForTest();
        encoder.encode(null, news, encoderOut);
        byte[] bytes = new byte[encoderOut.buffer.limit() - encoderOut.buffer.position()];
        encoderOut.buffer.get(bytes);

        assertEquals("wrong encoding", new String(bytes, CharsetSupport.getCharset()),
                news.toString());
    }

    public void testEncodingBadType() throws Exception {

        FIXMessageEncoder encoder = new FIXMessageEncoder();
        try {
            encoder.encode(null, new Object(), new ProtocolEncoderOutputForTest());
            fail("expected exception");
        } catch (ProtocolCodecException e) {
            // expected
        }
    }

    public void testEncodingStringEnglish() throws Exception {

        FIXMessageEncoder encoder = new FIXMessageEncoder();
        ProtocolEncoderOutputForTest protocolEncoderOutputForTest = new ProtocolEncoderOutputForTest();
        encoder.encode(null, "abcd", protocolEncoderOutputForTest);
        assertEquals(4, protocolEncoderOutputForTest.buffer.limit());
    }

    public void testEncodingStringChinese() throws Exception {

        CharsetSupport.setCharset("UTF-8");
        FIXMessageEncoder encoder = new FIXMessageEncoder();
        ProtocolEncoderOutputForTest protocolEncoderOutputForTest = new ProtocolEncoderOutputForTest();
        encoder.encode(null, "\u6D4B\u9A8C\u6570\u636E", protocolEncoderOutputForTest);
        assertEquals(12, protocolEncoderOutputForTest.buffer.limit());
    }

}
