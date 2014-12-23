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

import java.math.BigDecimal;
import java.util.ArrayList;

import org.quickfixj.FIXMessage;
import org.quickfixj.engine.FIXSession.FIXSessionID;
import org.quickfixj.engine.FIXTag;
import org.quickfixj.engine.SessionNotFoundException;
import org.quickfixj.messages.bd.fix42.ExecutionReport;
import org.quickfixj.messages.bd.fix42.MarketDataRequest;
import org.quickfixj.messages.bd.fix42.MarketDataRequest.NoRelatedSymGroup;
import org.quickfixj.messages.bd.fix42.NewOrderSingle;
import org.quickfixj.messages.bd.fix42.OrderCancelRequest;
import org.quickfixj.messages.bd.fix42.field.AvgPx;
import org.quickfixj.messages.bd.fix42.field.ClOrdID;
import org.quickfixj.messages.bd.fix42.field.CumQty;
import org.quickfixj.messages.bd.fix42.field.ExecID;
import org.quickfixj.messages.bd.fix42.field.ExecTransType;
import org.quickfixj.messages.bd.fix42.field.ExecType;
import org.quickfixj.messages.bd.fix42.field.LastPx;
import org.quickfixj.messages.bd.fix42.field.LastShares;
import org.quickfixj.messages.bd.fix42.field.LeavesQty;
import org.quickfixj.messages.bd.fix42.field.OrdStatus;
import org.quickfixj.messages.bd.fix42.field.OrdType;
import org.quickfixj.messages.bd.fix42.field.OrderID;
import org.quickfixj.messages.bd.fix42.field.OrderQty;
import org.quickfixj.messages.bd.fix42.field.Side;
import org.quickfixj.messages.bd.fix42.field.SubscriptionRequestType;
import org.quickfixj.messages.bd.fix42.field.Symbol;
import org.quickfixj.messages.bd.fix42.field.Text;
import org.quickfixj.messages.bd.fix42.field.TimeInForce;

import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.IncorrectTagValue;
import quickfix.MessageCracker;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.UnsupportedMessageType;

public class Application extends MessageCracker implements quickfix.Application {

    private OrderMatcher orderMatcher = new OrderMatcher();

    private IdGenerator generator = new IdGenerator();

    @Override
    public void fromAdmin(FIXMessage message, FIXSessionID sessionId) {

        // no-op
    }

    @Override
    public void fromApp(FIXMessage message, FIXSessionID sessionId) {

        crack(message, sessionId);
    }

    public void onMessage(NewOrderSingle message, FIXSessionID sessionID) {

        String senderCompId = message.getHeader().getFieldValue(FIXTag.SENDER_COMP_ID);
        String targetCompId = message.getHeader().getFieldValue(FIXTag.TARGET_COMP_ID);
        ClOrdID clOrdId = message.getClOrdID();
        Symbol symbol = message.getSymbol();
        Side side = message.getSide();
        OrdType ordType = message.getOrdType();

        BigDecimal price = BigDecimal.ZERO;
        if (OrdType.LIMIT.equals(ordType)) {
            price = message.getPrice().getValue();
        }

        BigDecimal qty = message.getOrderQty().getValue();
        TimeInForce timeInForce = TimeInForce.DAY;
        if (message.isFieldSet(TimeInForce.TAG)) {
            timeInForce = message.getTimeInForce();
        }

        try {
            if (timeInForce != TimeInForce.DAY) {
                throw new RuntimeException("Unsupported TIF, use Day");
            }

            Order order = new Order(clOrdId, symbol, senderCompId, targetCompId, side, ordType,
                    price.doubleValue(), qty.intValue());

            processOrder(order);
        } catch (Exception e) {
            rejectOrder(senderCompId, targetCompId, clOrdId, symbol, side, e.getMessage());
        }
    }

    private void rejectOrder(String senderCompId, String targetCompId, ClOrdID clOrdId,
            Symbol symbol, Side side, String message) {

        ExecutionReport fixOrder = new ExecutionReport(new OrderID(clOrdId.getValue()), new ExecID(
                generator.genExecutionID()), ExecTransType.NEW, ExecType.REJECTED,
                OrdStatus.REJECTED, symbol, side, new LeavesQty(0), new CumQty(0), new AvgPx(0));

        fixOrder.setText(new Text(message));

        try {
            Session.sendToTarget(fixOrder, senderCompId, targetCompId);
        } catch (SessionNotFoundException e) {
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

        ExecutionReport fixOrder = new ExecutionReport(new OrderID(order.getClientOrderId()
                .getValue()), new ExecID(generator.genExecutionID()), ExecTransType.NEW, execType,
                status, order.getSymbol(), order.getSide(), new LeavesQty(order.getOpenQuantity()),
                new CumQty(order.getExecutedQuantity()), new AvgPx(order.getAvgExecutedPrice()));

        fixOrder.setOrderQty(new OrderQty(order.getQuantity()));

        if (status == OrdStatus.FILLED || status == OrdStatus.PARTIALLY_FILLED) {
            fixOrder.setLastShares(new LastShares(order.getLastExecutedQuantity()));
            fixOrder.setLastPx(new LastPx(order.getPrice()));
        }

        try {
            Session.sendToTarget(fixOrder, senderCompId, targetCompId);
        } catch (SessionNotFoundException e) {
            // ignore
        }
    }

    private void fillOrder(Order order) {

        if (order.isFilled()) {
            updateOrder(order, ExecType.FILL, OrdStatus.FILLED);
        } else {
            updateOrder(order, ExecType.PARTIAL_FILL, OrdStatus.PARTIALLY_FILLED);
        }
    }

    public void onMessage(OrderCancelRequest message, SessionID sessionID) throws FieldNotFound,
            UnsupportedMessageType {

        Symbol symbol = message.getSymbol();
        Side side = message.getSide();
        String id = message.getOrigClOrdID().getValue();
        Order order = orderMatcher.find(symbol, side, id);
        order.cancel();
        cancelOrder(order);
        orderMatcher.erase(order);
    }

    public void onMessage(MarketDataRequest message, SessionID sessionID) throws FieldNotFound,
            UnsupportedMessageType, IncorrectTagValue {

        SubscriptionRequestType subscriptionRequestType = message.getSubscriptionRequestType();

        if (SubscriptionRequestType.SNAPSHOT.equals(subscriptionRequestType)) {
            throw new IncorrectTagValue(SubscriptionRequestType.TAG);
        }

        for (NoRelatedSymGroup group : message.getNoRelatedSym()) {

            Symbol symbol = group.getSymbol();
            System.err.println("*** market data: " + symbol);
        }
    }

    @Override
    public void onCreate(FIXSessionID sessionId) {
        // no-op
    }

    @Override
    public void onLogon(FIXSessionID sessionId) {
        System.out.println("Logon - " + sessionId);
    }

    @Override
    public void onLogout(FIXSessionID sessionId) {
        System.out.println("Logout - " + sessionId);
    }

    @Override
    public void toAdmin(FIXMessage message, FIXSessionID sessionId) {
        // no-op
    }

    @Override
    public void toApp(FIXMessage message, FIXSessionID sessionId) throws DoNotSend {
        // no-op
    }

    public OrderMatcher orderMatcher() {
        return orderMatcher;
    }
}
