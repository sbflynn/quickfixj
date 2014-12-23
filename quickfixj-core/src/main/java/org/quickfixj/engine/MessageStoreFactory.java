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

package org.quickfixj.engine;

import org.quickfixj.engine.FIXSession.FIXSessionID;

/**
 * Used by a Session to create a message store implementation.
 *
 * @see FIXSession
 * @see FIXSessionID
 */
public interface MessageStoreFactory {

    /**
     * Creates a message store implementation.
     *
     * @param sessionID the session ID, often used to access session configurations
     * @return the message store implementation
     */
    MessageStore create(FIXSessionID sessionID);
}
