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

package quickfix.test.acceptance;

import java.util.HashSet;

import org.quickfixj.FIXField;
import org.quickfixj.FIXMessage;
import org.quickfixj.engine.FIXTag;
import org.quickfixj.engine.SessionNotFoundException;
import org.quickfixj.messages.bd.fix44.field.ClOrdID;
import org.quickfixj.messages.bd.fix44.field.PossResend;

import quickfix.FieldNotFound;
import quickfix.Session;
import quickfix.SessionID;

class ATMessageCracker extends quickfix.MessageCracker {

    private HashSet<Pair> orderIDs = new HashSet<Pair>();

    public void reset() {

        orderIDs.clear();
    }

    public void process(FIXMessage message, SessionID sessionID) throws FieldNotFound {

        FIXMessage echo = (FIXMessage) message.clone();
        PossResend possResend = new PossResend(false);
        if (message.getHeader().isFieldSet(FIXTag.POSS_RESEND)) {
            message.getHeader().getField(FIXTag.POSS_RESEND);
        }

        Pair pair = new Pair(message.getField(ClOrdID.TAG), sessionID);

        if (possResend.getValue() && orderIDs.contains(pair)) {
            return;
        }

        orderIDs.add(pair);
        try {
            Session.sendToTarget(echo, sessionID);
        } catch (SessionNotFoundException snf) {
            // ignore
        }
    }

    public void onMessage(org.quickfixj.messages.bd.fix50.NewOrderSingle message,
            SessionID sessionID) throws FieldNotFound {

        process(message, sessionID);
    }

    public void onMessage(org.quickfixj.messages.bd.fix50.SecurityDefinition message,
            SessionID sessionID) {

        try {
            Session.sendToTarget(message, sessionID);
        } catch (SessionNotFoundException snf) {
            snf.printStackTrace();
        }
    }

    public void onMessage(org.quickfixj.messages.bd.fix44.NewOrderSingle message,
            SessionID sessionID) throws FieldNotFound {

        process(message, sessionID);
    }

    public void onMessage(org.quickfixj.messages.bd.fix44.SecurityDefinition message,
            SessionID sessionID) {

        try {
            Session.sendToTarget(message, sessionID);
        } catch (SessionNotFoundException snf) {
            snf.printStackTrace();
        }
    }

    public void onMessage(org.quickfixj.messages.bd.fix43.NewOrderSingle message,
            SessionID sessionID) throws FieldNotFound {

        process(message, sessionID);
    }

    public void onMessage(org.quickfixj.messages.bd.fix43.SecurityDefinition message,
            SessionID sessionID) {

        try {
            Session.sendToTarget(message, sessionID);
        } catch (SessionNotFoundException snf) {
            snf.printStackTrace();
        }
    }

    public void onMessage(org.quickfixj.messages.bd.fix42.NewOrderSingle message,
            SessionID sessionID) throws FieldNotFound {

        process(message, sessionID);
    }

    public void onMessage(org.quickfixj.messages.bd.fix42.SecurityDefinition message,
            SessionID sessionID) {

        try {
            Session.sendToTarget(message, sessionID);
        } catch (SessionNotFoundException snf) {
            snf.printStackTrace();
        }
    }

    public void onMessage(org.quickfixj.messages.bd.fix41.NewOrderSingle message,
            SessionID sessionID) throws FieldNotFound {

        process(message, sessionID);
    }

    public void onMessage(org.quickfixj.messages.bd.fix40.NewOrderSingle message,
            SessionID sessionID) throws FieldNotFound {

        process(message, sessionID);
    }

    private static class Pair {

        private FIXField<?> clOrdID;

        private SessionID sessionID;

        private int hashCode = 0;

        public Pair(FIXField<?> clOrdID, SessionID sessionID) {

            this.clOrdID = clOrdID;
            this.sessionID = sessionID;
            hashCode = ("C:" + clOrdID.toString() + "S:" + sessionID.toString()).hashCode();
        }

        @Override
        public boolean equals(Object object) {

            if (object == null) {
                return false;
            }
            if (!(object instanceof Pair)) {
                return false;
            }
            Pair pair = (Pair) object;

            return clOrdID.equals(pair.clOrdID) && sessionID.equals(pair.sessionID);
        }

        @Override
        public int hashCode() {

            return hashCode;
        }
    }
}
