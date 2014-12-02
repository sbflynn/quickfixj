/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 25 Nov 2014 by stephen.flynn@jftechnology.com.
 */
package quickfix.examples.banzai;

import quickfix.FixTags;
import quickfix.Message;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;

/**
 * BanzaiHandler - provides...
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public abstract class BanzaiHandler {

    protected final void send(quickfix.Message message, SessionID sessionID) {
        try {

            message.getHeader().setString(FixTags.MSG_TYPE, message.getMsgType());

            Session.sendToTarget(message, sessionID);
        } catch (SessionNotFound e) {
            System.out.println(e);
        }
    }

    protected abstract void send(Order order);

    protected abstract void cancel(Order order);

    protected abstract void replace(Order order, Order newOrder);

    protected abstract void process(quickfix.Message message, SessionID sessionID);

    protected abstract void sendBusinessReject(SessionID sessionID, Message message,
            int rejectReason, String rejectText) throws SessionNotFound;
}
