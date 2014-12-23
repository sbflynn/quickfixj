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

import java.util.ArrayList;
import java.util.List;

import org.quickfixj.FIXMessage;
import org.quickfixj.engine.FIXSession.FIXSessionID;

public class UnitTestApplication implements ApplicationExtended, SessionStateListener {
    public List<FIXMessage> fromAppMessages = new ArrayList<FIXMessage>();
    public List<FIXMessage> toAppMessages = new ArrayList<FIXMessage>();
    public List<FIXMessage> fromAdminMessages = new ArrayList<FIXMessage>();
    public List<FIXMessage> toAdminMessages = new ArrayList<FIXMessage>();
    public List<FIXSessionID> logonSessions = new ArrayList<FIXSessionID>();
    public List<FIXSessionID> logoutSessions = new ArrayList<FIXSessionID>();
    public List<FIXSessionID> createSessions = new ArrayList<FIXSessionID>();
    public int sessionResets = 0;

    @Override
    public boolean canLogon(FIXSessionID sessionID) {
        return true;
    }

    @Override
    public void fromApp(FIXMessage message, FIXSessionID sessionId) throws FieldNotFound,
            IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        System.out.println("from app [" + sessionId + "] " + message);
        fromAppMessages.add(message);
    }

    @Override
    public void toApp(FIXMessage message, FIXSessionID sessionId) throws DoNotSend {
        System.out.println("to app [" + sessionId + "] " + message);
        toAppMessages.add(message);
    }

    @Override
    public void fromAdmin(FIXMessage message, FIXSessionID sessionId) throws FieldNotFound,
            IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        System.out.println("from admin [" + sessionId + "] " + message);
        fromAdminMessages.add(message);
    }

    @Override
    public void toAdmin(FIXMessage message, FIXSessionID sessionId) {
        System.out.println("to admin [" + sessionId + "] " + message);
        toAdminMessages.add(message);
    }

    @Override
    public void onBeforeSessionReset(FIXSessionID sessionId) {
        System.out.println("onBeforeSessionReset [" + sessionId + "]");
    }

    @Override
    public void onLogout(FIXSessionID sessionId) {
        logoutSessions.add(sessionId);
    }

    @Override
    public void onLogon(FIXSessionID sessionId) {
        logonSessions.add(sessionId);
    }

    @Override
    public void onCreate(FIXSessionID sessionId) {
        createSessions.add(sessionId);
    }

    public void clear() {
        fromAppMessages.clear();
        toAppMessages.clear();
        fromAdminMessages.clear();
        toAdminMessages.clear();
        logonSessions.clear();
        logoutSessions.clear();
        createSessions.clear();
    }

    public FIXMessage lastFromAppMessage() {
        if (fromAppMessages.size() == 0)
            return null;
        return fromAppMessages.get(fromAppMessages.size() - 1);
    }

    public FIXMessage lastFromAdminMessage() {
        if (fromAdminMessages.size() == 0)
            return null;
        return fromAdminMessages.get(fromAdminMessages.size() - 1);
    }

    public FIXMessage lastToAppMessage() {
        if (toAppMessages.size() == 0)
            return null;
        return toAppMessages.get(toAppMessages.size() - 1);
    }

    public FIXMessage lastToAdminMessage() {
        if (toAdminMessages.size() == 0)
            return null;
        return toAdminMessages.get(toAdminMessages.size() - 1);
    }

    @Override
    public void onConnect() {
        //no-op
    }

    @Override
    public void onDisconnect() {
        //no-op
    }

    @Override
    public void onLogon() {
        //no-op
    }

    @Override
    public void onLogout() {
        //no-op
    }

    @Override
    public void onReset() {
        sessionResets++;
    }

    @Override
    public void onRefresh() {
        //no-op
    }

    @Override
    public void onMissedHeartBeat() {
        //no-op
    }

    @Override
    public void onHeartBeatTimeout() {
        //no-op
    }
}
