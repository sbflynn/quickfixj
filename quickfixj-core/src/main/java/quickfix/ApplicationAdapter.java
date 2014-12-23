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

import org.quickfixj.FIXMessage;
import org.quickfixj.engine.FIXSession.FIXSessionID;

/**
 * Convenience class with default implementation for application methods
 */
public class ApplicationAdapter implements Application {

    /* (non-Javadoc)
     * @see quickfix.Application#fromAdmin(quickfix.Message, quickfix.SessionID)
     */
    @Override
    public void fromAdmin(FIXMessage message, FIXSessionID sessionId) throws FieldNotFound,
            IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        // EMPTY
    }

    /* (non-Javadoc)
     * @see quickfix.Application#fromApp(quickfix.Message, quickfix.SessionID)
     */
    @Override
    public void fromApp(FIXMessage message, FIXSessionID sessionId) throws FieldNotFound,
            IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        // EMPTY
    }

    /* (non-Javadoc)
     * @see quickfix.Application#onCreate(quickfix.SessionID)
     */
    @Override
    public void onCreate(FIXSessionID sessionId) {
        // EMPTY
    }

    /* (non-Javadoc)
     * @see quickfix.Application#onLogon(quickfix.SessionID)
     */
    @Override
    public void onLogon(FIXSessionID sessionId) {
        // EMPTY
    }

    /* (non-Javadoc)
     * @see quickfix.Application#onLogout(quickfix.SessionID)
     */
    @Override
    public void onLogout(FIXSessionID sessionId) {
        // EMPTY
    }

    /* (non-Javadoc)
     * @see quickfix.Application#toAdmin(quickfix.Message, quickfix.SessionID)
     */
    @Override
    public void toAdmin(FIXMessage message, FIXSessionID sessionId) {
        // EMPTY
    }

    /* (non-Javadoc)
     * @see quickfix.Application#toApp(quickfix.Message, quickfix.SessionID)
     */
    @Override
    public void toApp(FIXMessage message, FIXSessionID sessionId) throws DoNotSend {
        // EMPTY
    }
}
