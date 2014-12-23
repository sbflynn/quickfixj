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

import org.quickfixj.FIXApplication;
import org.quickfixj.FIXBeginString;
import org.quickfixj.FIXMessage;
import org.quickfixj.engine.FIXEngine;
import org.quickfixj.engine.FIXMessageBuilderFactory;
import org.quickfixj.engine.FIXSession;
import org.quickfixj.engine.FIXSession.FIXSessionID;
import org.quickfixj.engine.FIXTag;

import quickfix.Application;
import quickfix.DoNotSend;

public class BanzaiApplication implements Application {

    private final FIXEngine engine;
    private final OrderTableModel orderTableModel;
    private final ExecutionTableModel executionTableModel;

    private ObservableOrder observableOrder = new ObservableOrder();
    private ObservableLogon observableLogon = new ObservableLogon();
    private boolean isAvailable = true;
    private boolean isMissingField;

    static private Map<FIXBeginString, BanzaiHandler> handlers = new HashMap<FIXBeginString, BanzaiHandler>();

    public BanzaiApplication(FIXEngine engine) {

        this.engine = engine;

        this.orderTableModel = new OrderTableModel();
        this.executionTableModel = new ExecutionTableModel();

        handlers.put(FIXBeginString.FIX40, new BanzaiHandler40(this));
        handlers.put(FIXBeginString.FIX41, new BanzaiHandler41(this));
        handlers.put(FIXBeginString.FIX42, new BanzaiHandler42(this));
        handlers.put(FIXBeginString.FIX43, new BanzaiHandler43(this));
        handlers.put(FIXBeginString.FIX44, new BanzaiHandler44(this));
        handlers.put(FIXBeginString.FIXT11, new BanzaiHandler50(this));
    }

    @Override
    public void onCreate(FIXSessionID sessionID) {
        //no-op
    }

    @Override
    public void onLogon(FIXSessionID sessionID) {
        observableLogon.logon(sessionID);
    }

    @Override
    public void onLogout(FIXSessionID sessionID) {
        observableLogon.logoff(sessionID);
    }

    @Override
    public void toAdmin(FIXMessage message, FIXSessionID sessionID) {
        //no-op
    }

    @Override
    public void toApp(FIXMessage message, FIXSessionID sessionID) throws DoNotSend {
        //no-op
    }

    @Override
    public void fromAdmin(FIXMessage message, FIXSessionID sessionID) {
        //no-op
    }

    @Override
    public void fromApp(FIXMessage message, FIXSessionID sessionID) {
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
        private HashSet<FIXSessionID> set = new HashSet<FIXSessionID>();

        public void logon(FIXSessionID sessionID) {
            set.add(sessionID);
            setChanged();
            notifyObservers(new LogonEvent(sessionID, true));
            clearChanged();
        }

        public void logoff(FIXSessionID sessionID) {
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
     * Get the engine property.
     *
     * @return Returns the engine.
     * @since 2.0
     */
    public FIXEngine getEngine() {
        return engine;
    }

    /**
     * Get the orderTableModel property.
     *
     * @return Returns the orderTableModel.
     * @since 2.0
     */
    public OrderTableModel getOrderTableModel() {
        return orderTableModel;
    }

    /**
     * Get the executionTableModel property.
     *
     * @return Returns the executionTableModel.
     * @since 2.0
     */
    public ExecutionTableModel getExecutionTableModel() {
        return executionTableModel;
    }

    /**
     * Get the messageFactory property.
     *
     * @return Returns the messageFactory.
     * @since 2.0
     */
    public FIXMessageBuilderFactory getMessageFactory(FIXSessionID sessionID) {
        FIXSession session = engine.lookupSession(sessionID);
        return session.getMessageFactory();
    }

    public FIXMessage createMessage(FIXSessionID sessionID, FIXApplication application,
            String msgType) {
        return getMessageFactory(sessionID).getMessageBuilder(application, msgType).create();
    }

    public class MessageProcessor implements Runnable {
        private FIXMessage message;
        private FIXSessionID sessionID;

        public MessageProcessor(FIXMessage message, FIXSessionID sessionID) {
            this.message = message;
            this.sessionID = sessionID;
        }

        @Override
        public void run() {
            try {

                FIXBeginString beginString = FIXBeginString.parse(message.getHeader()
                        .getFieldValue(FIXTag.BEGIN_STRING));

                if (handlers.containsKey(beginString)) {
                    handlers.get(beginString).process(message, sessionID);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
