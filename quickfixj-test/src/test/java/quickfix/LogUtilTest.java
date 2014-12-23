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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

import org.quickfixj.FIXBeginString;
import org.quickfixj.engine.Log;
import org.quickfixj.engine.LogFactory;
import org.quickfixj.engine.MessageStore;
import org.quickfixj.engine.MessageStoreFactory;
import org.quickfixj.engine.FIXSession.FIXSessionID;
import org.quickfixj.field.FieldConversionException;

import junit.framework.TestCase;

public class LogUtilTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SystemTime.setTimeSource(new MockSystemTimeSource(System.currentTimeMillis()));
    }

    public void testLogThrowable() throws ConfigError, FieldConversionException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        LogFactory mockLogFactory = createLogFactory(data);
        createSessionAndGenerateException(mockLogFactory);

        String message = new String(data.toByteArray());
        assertTrue(message.contains("IOException"));
    }

    private void createSessionAndGenerateException(LogFactory mockLogFactory) throws ConfigError,
            FieldConversionException {
        SessionSettings settings = new SessionSettings();
        settings.setString(Session.SETTING_START_TIME, "16:00:00");
        settings.setString(Session.SETTING_END_TIME, "13:00:00");
        SessionID sessionID = new SessionID(FIXBeginString.FIX42, "SENDER", "TARGET");
        SessionSchedule schedule = new SessionSchedule(settings, sessionID);
        Session session = new Session(null, new MessageStoreFactory() {
            @Override
            public MessageStore create(FIXSessionID sessionID) {
                try {
                    return new MemoryStore() {
                        @Override
                        public Date getCreationTime() throws IOException {
                            throw new IOException("test");
                        }
                    };
                } catch (IOException e) {
                    // ignore
                    return null;
                }
            }
        }, sessionID, null, schedule, mockLogFactory, null, new DefaultValidator(
                FIXBeginString.FIX42), 0);
        try {
            session.close();
        } catch (IOException e) {
            // ignore
        }
    }

    private LogFactory createLogFactory(ByteArrayOutputStream data) {
        final ScreenLog log = new ScreenLog(true, true, true, true, false, null, new PrintStream(
                data));
        return new LocationAwareLogFactory() {
            @Override
            public Log create(FIXSessionID sessionID) {
                return create(sessionID, log.getClass().getName());
            }

            @Override
            public Log create(FIXSessionID sessionID, String callerFQCN) {
                return log;
            }
        };
    }

}
