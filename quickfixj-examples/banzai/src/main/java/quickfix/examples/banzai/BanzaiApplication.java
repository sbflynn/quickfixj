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

package quickfix.examples.banzai;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.swing.SwingUtilities;

import org.quickfixj.FIXBeginString;
import org.quickfixj.MessageBuilderFactory;
import org.quickfixj.spi.MessageBuilderServiceLoader;

import quickfix.Application;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.FixTags;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.RejectLogon;
import quickfix.SessionID;
import quickfix.UnsupportedMessageType;

public class BanzaiApplication implements Application {
    private MessageBuilderFactory messageFactory = MessageBuilderServiceLoader
            .getMessageBuilderFactory();
    private ObservableOrder observableOrder = new ObservableOrder();
    private ObservableLogon observableLogon = new ObservableLogon();
    private boolean isAvailable = true;
    private boolean isMissingField;

    static private Map<FIXBeginString, BanzaiHandler> handlers = new HashMap<FIXBeginString, BanzaiHandler>();

    public BanzaiApplication(OrderTableModel orderTableModel,
            ExecutionTableModel executionTableModel) {

        handlers.put(FIXBeginString.FIX40, new BanzaiHandler40(this, orderTableModel,
                executionTableModel));
        handlers.put(FIXBeginString.FIX41, new BanzaiHandler41(this, orderTableModel,
                executionTableModel));
        handlers.put(FIXBeginString.FIX42, new BanzaiHandler42(this, orderTableModel,
                executionTableModel));
        handlers.put(FIXBeginString.FIX43, new BanzaiHandler43(this, orderTableModel,
                executionTableModel));
        handlers.put(FIXBeginString.FIX44, new BanzaiHandler44(this, orderTableModel,
                executionTableModel));
        handlers.put(FIXBeginString.FIXT11, new BanzaiHandler50(this, orderTableModel,
                executionTableModel));
    }

    @Override
    public void onCreate(SessionID sessionID) {
        //no-op
    }

    @Override
    public void onLogon(SessionID sessionID) {
        observableLogon.logon(sessionID);
    }

    @Override
    public void onLogout(SessionID sessionID) {
        observableLogon.logoff(sessionID);
    }

    @Override
    public void toAdmin(quickfix.Message message, SessionID sessionID) {
        //no-op
    }

    @Override
    public void toApp(quickfix.Message message, SessionID sessionID) throws DoNotSend {
        //no-op
    }

    @Override
    public void fromAdmin(quickfix.Message message, SessionID sessionID) throws FieldNotFound,
            IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        //no-op
    }

    @Override
    public void fromApp(quickfix.Message message, SessionID sessionID) throws FieldNotFound,
            IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        SwingUtilities.invokeLater(new MessageProcessor(message, sessionID));
    }

    public void send(Order order) {
        FIXBeginString beginString = order.getSessionID().getBeginString();
        if (handlers.containsKey(beginString)) {
            handlers.get(beginString).send(order);
        }
    }

    public void cancel(Order order) {
        FIXBeginString beginString = order.getSessionID().getBeginString();
        if (handlers.containsKey(beginString)) {
            handlers.get(beginString).cancel(order);
        }
    }

    public void replace(Order order, Order newOrder) {
        FIXBeginString beginString = order.getSessionID().getBeginString();
        if (handlers.containsKey(beginString)) {
            handlers.get(beginString).replace(order, newOrder);
        }
    }

    public void addLogonObserver(Observer observer) {
        observableLogon.addObserver(observer);
    }

    public void deleteLogonObserver(Observer observer) {
        observableLogon.deleteObserver(observer);
    }

    public void addOrderObserver(Observer observer) {
        observableOrder.addObserver(observer);
    }

    public void deleteOrderObserver(Observer observer) {
        observableOrder.deleteObserver(observer);
    }

    private static class ObservableOrder extends Observable {
        public void update(Order order) {
            setChanged();
            notifyObservers(order);
            clearChanged();
        }
    }

    private static class ObservableLogon extends Observable {
        private HashSet<SessionID> set = new HashSet<SessionID>();

        public void logon(SessionID sessionID) {
            set.add(sessionID);
            setChanged();
            notifyObservers(new LogonEvent(sessionID, true));
            clearChanged();
        }

        public void logoff(SessionID sessionID) {
            set.remove(sessionID);
            setChanged();
            notifyObservers(new LogonEvent(sessionID, false));
            clearChanged();
        }
    }

    public boolean isMissingField() {
        return isMissingField;
    }

    public void setMissingField(boolean isMissingField) {
        this.isMissingField = isMissingField;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    /**
     * Get the messageFactory property.
     *
     * @return Returns the messageFactory.
     * @since 2.0
     */
    public MessageBuilderFactory getMessageFactory() {
        return messageFactory;
    }

    public class MessageProcessor implements Runnable {
        private quickfix.Message message;
        private SessionID sessionID;

        public MessageProcessor(quickfix.Message message, SessionID sessionID) {
            this.message = message;
            this.sessionID = sessionID;
        }

        @Override
        public void run() {
            try {

                FIXBeginString beginString = FIXBeginString.parse(message.getHeader().getString(
                        FixTags.BEGIN_STRING));

                if (handlers.containsKey(beginString)) {
                    handlers.get(beginString).process(message, sessionID);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
