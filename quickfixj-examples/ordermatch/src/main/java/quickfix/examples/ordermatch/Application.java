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

package quickfix.examples.ordermatch;

import java.util.ArrayList;

import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.FixTags;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.MessageCracker;
import quickfix.RejectLogon;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.UnsupportedMessageType;
import quickfix.fix42.ExecutionReport;
import quickfix.fix42.MarketDataRequest;
import quickfix.fix42.MarketDataRequest.NoRelatedSymGroup;
import quickfix.fix42.NewOrderSingle;
import quickfix.fix42.OrderCancelRequest;
import quickfix.fix42.field.AvgPx;
import quickfix.fix42.field.ClOrdID;
import quickfix.fix42.field.CumQty;
import quickfix.fix42.field.ExecID;
import quickfix.fix42.field.ExecTransType;
import quickfix.fix42.field.ExecType;
import quickfix.fix42.field.LastPx;
import quickfix.fix42.field.LastShares;
import quickfix.fix42.field.LeavesQty;
import quickfix.fix42.field.OrdStatus;
import quickfix.fix42.field.OrdType;
import quickfix.fix42.field.OrderID;
import quickfix.fix42.field.OrderQty;
import quickfix.fix42.field.Side;
import quickfix.fix42.field.SubscriptionRequestType;
import quickfix.fix42.field.Symbol;
import quickfix.fix42.field.Text;
import quickfix.fix42.field.TimeInForce;

public class Application extends MessageCracker implements quickfix.Application {

    private OrderMatcher orderMatcher = new OrderMatcher();

    private IdGenerator generator = new IdGenerator();

    @Override
    public void fromAdmin(Message message, SessionID sessionId)
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue,
            RejectLogon {

        // no-op
    }

    @Override
    public void fromApp(Message message, SessionID sessionId)
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue,
            UnsupportedMessageType {

        crack(message, sessionId);
    }

    public void onMessage(NewOrderSingle message, SessionID sessionID)
            throws FieldNotFound, UnsupportedMessageType {

        String senderCompId = message.getHeader().getString(
                FixTags.SENDER_COMP_ID);
        String targetCompId = message.getHeader().getString(
                FixTags.TARGET_COMP_ID);
        ClOrdID clOrdId = message.getClOrdID();
        Symbol symbol = message.getSymbol();
        Side side = message.getSide();
        OrdType ordType = message.getOrdType();

        double price = 0;
        if (OrdType.LIMIT.equals(ordType)) {
            price = message.getPrice().getValue();
        }

        double qty = message.getOrderQty().getValue();
        TimeInForce timeInForce = TimeInForce.DAY;
        if (message.isFieldSet(TimeInForce.TAG)) {
            timeInForce = message.getTimeInForce();
        }

        try {
            if (timeInForce != TimeInForce.DAY) {
                throw new RuntimeException("Unsupported TIF, use Day");
            }

            Order order = new Order(clOrdId, symbol, senderCompId,
                    targetCompId, side, ordType, price, (int) qty);

            processOrder(order);
        } catch (Exception e) {
            rejectOrder(senderCompId, targetCompId, clOrdId, symbol, side,
                    e.getMessage());
        }
    }

    private void rejectOrder(String senderCompId, String targetCompId,
            ClOrdID clOrdId, Symbol symbol, Side side, String message) {

        ExecutionReport fixOrder = new ExecutionReport(new OrderID(
                clOrdId.getCharacters()),
                new ExecID(generator.genExecutionID()), ExecTransType.NEW,
                ExecType.REJECTED, OrdStatus.REJECTED, symbol, side,
                new LeavesQty(0), new CumQty(0), new AvgPx(0));

        fixOrder.setText(new Text(message));

        try {
            Session.sendToTarget(fixOrder, senderCompId, targetCompId);
        } catch (SessionNotFound e) {
            e.printStackTrace();
        }
    }

    private void processOrder(Order order) {

        if (orderMatcher.insert(order)) {
            acceptOrder(order);

            ArrayList<Order> orders = new ArrayList<Order>();
            orderMatcher.match(order.getSymbol(), orders);

            while (orders.size() > 0) {
                fillOrder(orders.remove(0));
            }
            orderMatcher.display(order.getSymbol());
        } else {
            rejectOrder(order);
        }
    }

    private void rejectOrder(Order order) {

        updateOrder(order, ExecType.REJECTED, OrdStatus.REJECTED);
    }

    private void acceptOrder(Order order) {

        updateOrder(order, ExecType.NEW, OrdStatus.NEW);
    }

    private void cancelOrder(Order order) {

        updateOrder(order, ExecType.CANCELED, OrdStatus.CANCELED);
    }

    private void updateOrder(Order order, ExecType execType, OrdStatus status) {

        String targetCompId = order.getOwner();
        String senderCompId = order.getTarget();

        ExecutionReport fixOrder = new ExecutionReport(new OrderID(order
                .getClientOrderId().getCharacters()), new ExecID(
                generator.genExecutionID()), ExecTransType.NEW, execType,
                status, order.getSymbol(), order.getSide(), new LeavesQty(
                        order.getOpenQuantity()), new CumQty(
                        order.getExecutedQuantity()), new AvgPx(
                        order.getAvgExecutedPrice()));

        fixOrder.setDouble(OrderQty.TAG, order.getQuantity());

        if (status == OrdStatus.FILLED || status == OrdStatus.PARTIALLY_FILLED) {
            fixOrder.setDouble(LastShares.TAG, order.getLastExecutedQuantity());
            fixOrder.setDouble(LastPx.TAG, order.getPrice());
        }

        try {
            Session.sendToTarget(fixOrder, senderCompId, targetCompId);
        } catch (SessionNotFound e) {
            // ignore
        }
    }

    private void fillOrder(Order order) {

        if (order.isFilled()) {
            updateOrder(order, ExecType.FILL, OrdStatus.FILLED);
        } else {
            updateOrder(order, ExecType.PARTIAL_FILL,
                    OrdStatus.PARTIALLY_FILLED);
        }
    }

    public void onMessage(OrderCancelRequest message, SessionID sessionID)
            throws FieldNotFound, UnsupportedMessageType {

        Symbol symbol = message.getSymbol();
        Side side = message.getSide();
        String id = message.getOrigClOrdID().getValue();
        Order order = orderMatcher.find(symbol, side, id);
        order.cancel();
        cancelOrder(order);
        orderMatcher.erase(order);
    }

    public void onMessage(MarketDataRequest message, SessionID sessionID)
            throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {

        SubscriptionRequestType subscriptionRequestType = message
                .getSubscriptionRequestType();

        if (SubscriptionRequestType.SNAPSHOT.equals(subscriptionRequestType)) {
            throw new IncorrectTagValue(SubscriptionRequestType.TAG);
        }

        for (NoRelatedSymGroup group : message.getNoRelatedSym().getGroups()) {

            Symbol symbol = group.getSymbol();
            System.err.println("*** market data: " + symbol);
        }
    }

    @Override
    public void onCreate(SessionID sessionId) {

        // no-op
    }

    @Override
    public void onLogon(SessionID sessionId) {

        System.out.println("Logon - " + sessionId);
    }

    @Override
    public void onLogout(SessionID sessionId) {

        System.out.println("Logout - " + sessionId);
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {

        // no-op
    }

    @Override
    public void toApp(Message message, SessionID sessionId) throws DoNotSend {

        // no-op
    }

    public OrderMatcher orderMatcher() {

        return orderMatcher;
    }
}
