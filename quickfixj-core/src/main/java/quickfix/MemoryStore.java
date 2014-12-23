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

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import org.quickfixj.engine.FIXSession;
import org.quickfixj.engine.MessageStore;
import org.slf4j.LoggerFactory;

/**
 * In-memory message store implementation.
 *
 * @see quickfix.MemoryStoreFactory
 */
public class MemoryStore implements MessageStore {
    private HashMap<Integer, String> messages = new HashMap<Integer, String>();
    private int nextSenderMsgSeqNum;
    private int nextTargetMsgSeqNum;
    private SessionID sessionID;
    private Calendar creationTime = SystemTime.getUtcCalendar();

    public MemoryStore() throws IOException {
        reset();
    }

    public MemoryStore(SessionID sessionID) {
        this.sessionID = sessionID;
    }

    @Override
    public void get(int startSequence, int endSequence, Collection<String> messages)
            throws IOException {
        for (int i = startSequence; i <= endSequence; i++) {
            String message = this.messages.get(i);
            if (message != null) {
                messages.add(message);
            }
        }
    }

    @Override
    public Date getCreationTime() throws IOException {
        return creationTime.getTime();
    }

    /* package */void setCreationTime(Calendar creationTime) {
        this.creationTime = creationTime;
    }

    @Override
    public int getNextSenderMsgSeqNum() throws IOException {
        return nextSenderMsgSeqNum;
    }

    @Override
    public int getNextTargetMsgSeqNum() throws IOException {
        return nextTargetMsgSeqNum;
    }

    @Override
    public void incrNextSenderMsgSeqNum() throws IOException {
        setNextSenderMsgSeqNum(getNextSenderMsgSeqNum() + 1);
    }

    @Override
    public void incrNextTargetMsgSeqNum() throws IOException {
        setNextTargetMsgSeqNum(getNextTargetMsgSeqNum() + 1);
    }

    @Override
    public void reset() throws IOException {
        setNextSenderMsgSeqNum(1);
        setNextTargetMsgSeqNum(1);
        messages.clear();
        creationTime = SystemTime.getUtcCalendar();
    }

    @Override
    public boolean set(int sequence, String message) throws IOException {
        return messages.put(sequence, message) == null;
    }

    @Override
    public void setNextSenderMsgSeqNum(int next) throws IOException {
        nextSenderMsgSeqNum = next;
    }

    @Override
    public void setNextTargetMsgSeqNum(int next) throws IOException {
        nextTargetMsgSeqNum = next;
    }

    @Override
    public void refresh() throws IOException {
        // IOException is declared to maintain strict compatibility with QF JNI
        final String text = "memory store does not support refresh!";
        if (sessionID != null) {
            FIXSession session = Session.lookupSession(sessionID);
            session.getLog().onErrorEvent("ERROR: " + text);
        } else {
            LoggerFactory.getLogger(MemoryStore.class).error(text);
        }
    }

}
