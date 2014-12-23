/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 14 Dec 2014 by stephen.flynn@jftechnology.com.
 */
package org.quickfixj.engine;

import java.util.List;

import org.quickfixj.FIXBeginString;
import org.quickfixj.FIXMessage;
import org.quickfixj.engine.FIXSession.FIXSessionID;

/**
 * FIXEngine - provides...
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public interface FIXEngine {

    /**
     * @param beginString
     * @param namespace
     * @since 2.0
     */
    FIXMessageDictionaryFactory getMessageDictionaryFactory(FIXBeginString beginString,
            String namespace);

    /**
     * @param beginString
     * @param namespace
     * @since 2.0
     */
    FIXMessageBuilderFactory getMessageBuilderFactory(FIXBeginString beginString, String namespace);

    /**
     * Registration of named factories for a begin string.
     * @param beginString
     * @param namespace
     * @param messageDictionaryFactory
     * @since 2.0
     */
    void registerMessageDictionaryFactory(FIXBeginString beginString, String namespace,
            FIXMessageDictionaryFactory messageDictionaryFactory);

    /**
     * Registration of named factories for a begin string.
     * @param beginString
     * @param namespace
     * @param messageBuilderFactory
     * @since 2.0
     */
    void registerMessageBuilderFactory(FIXBeginString beginString, String namespace,
            FIXMessageBuilderFactory messageBuilderFactory);

    /**
     * @param session
     * @since 2.0
     */
    void registerSession(FIXSession session);

    FIXSession lookupSession(FIXSessionID sessionID);

    void unregisterSessions(List<FIXSessionID> sessionIds);

    /**
     * Send a message to the session specified by the provided session ID.
     *
     * @param message a FIX message
     * @param sessionID the target SessionID
     * @return true is send was successful, false otherwise
     * @throws SessionNotFoundException if session could not be located
     * @since 2.0
     */
    boolean send(FIXMessage message, FIXSessionID sessionID) throws SessionNotFoundException;
}
