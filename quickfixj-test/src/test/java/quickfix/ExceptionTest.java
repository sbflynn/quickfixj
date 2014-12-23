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

import org.junit.Test;
import org.quickfixj.engine.SessionNotFoundException;

public class ExceptionTest {

    @SuppressWarnings("unused")
    @Test
    public void testDoNotSend() {
        new DoNotSend();
    }

    @Test
    public void testIncorrectDataFormat() {
        IncorrectDataFormat e = new IncorrectDataFormat(5, "test");
        assertEquals(5, e.field);
        assertEquals("test", e.data);
    }

    @SuppressWarnings("unused")
    @Test
    public void testIncorrectTagValue() {
        new IncorrectTagValue(5);
        IncorrectTagValue e = new IncorrectTagValue("test");
        e.field = 5;
    }

    @SuppressWarnings("unused")
    @Test
    public void testRejectLogon() {
        new RejectLogon();
    }

    @SuppressWarnings("unused")
    @Test
    public void testRuntimeError() {
        new RuntimeError();
        new RuntimeError("test");
        new RuntimeError(new Exception());
    }

    @SuppressWarnings("unused")
    @Test
    public void testSessionNotFound() {
        new SessionNotFoundException();
        new SessionNotFoundException("test");
    }

    @SuppressWarnings("unused")
    @Test
    public void testSessionException() {
        new SessionException();
        new SessionException("test");
        new SessionException(new Exception());
    }
}
