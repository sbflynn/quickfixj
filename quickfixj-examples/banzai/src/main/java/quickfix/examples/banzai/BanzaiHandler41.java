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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;

import org.quickfixj.FIXApplication;

import quickfix.FieldNotFound;
import quickfix.FixTags;
import quickfix.Message;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.fix41.ExecutionReport;
import quickfix.fix41.NewOrderSingle;
import quickfix.fix41.OrderCancelReplaceRequest;
import quickfix.fix41.OrderCancelRequest;
import quickfix.fix41.field.AvgPx;
import quickfix.fix41.field.ClOrdID;
import quickfix.fix41.field.CumQty;
import quickfix.fix41.field.ExecID;
import quickfix.fix41.field.HandlInst;
import quickfix.fix41.field.LastShares;
import quickfix.fix41.field.LeavesQty;
import quickfix.fix41.field.LocateReqd;
import quickfix.fix41.field.MsgType;
import quickfix.fix41.field.OrdStatus;
import quickfix.fix41.field.OrdType;
import quickfix.fix41.field.OrderQty;
import quickfix.fix41.field.OrigClOrdID;
import quickfix.fix41.field.Price;
import quickfix.fix41.field.RefSeqNum;
import quickfix.fix41.field.Side;
import quickfix.fix41.field.StopPx;
import quickfix.fix41.field.Symbol;
import quickfix.fix41.field.Text;
import quickfix.fix41.field.TimeInForce;

public class BanzaiHandler41 extends BanzaiHandler {

    static private TwoWayMap sideMap = new TwoWayMap();
    static private TwoWayMap typeMap = new TwoWayMap();
    static private TwoWayMap tifMap = new TwoWayMap();
    static private HashMap<SessionID, HashSet<ExecID>> execIDs = new HashMap<SessionID, HashSet<ExecID>>();

    private final BanzaiApplication application;
    private final OrderTableModel orderTableModel;
    private final ExecutionTableModel executionTableModel;

    public BanzaiHandler41(BanzaiApplication application, OrderTableModel orderTableModel,
            ExecutionTableModel executionTableModel) {
        this.application = application;
        this.orderTableModel = orderTableModel;
        this.executionTableModel = executionTableModel;
    }

    @Override
    public void process(quickfix.Message message, SessionID sessionID) {
        try {
            if (application.isAvailable()) {
                if (application.isMissingField()) {
                    // For OpenFIX certification testing
                    sendBusinessReject(sessionID, message, -1,
                            "Conditionally required field missing");
                } else if (message.getHeader().isFieldSet(FixTags.DELIVER_TO_COMP_ID)) {
                    // This is here to support OpenFIX certification
                    sendSessionReject(message, sessionID);
                } else if (message.getHeader().getField(FixTags.MSG_TYPE).getValue().equals("8")) {
                    executionReport((ExecutionReport) message, sessionID);
                } else if (message.getHeader().getField(FixTags.MSG_TYPE).getValue().equals("9")) {
                    cancelReject(message, sessionID);
                } else {
                    sendBusinessReject(sessionID, message, -1, "Unsupported Message Type");
                }
            } else {
                sendBusinessReject(sessionID, message, -1, "Application not available");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendSessionReject(Message message, SessionID sessionID) throws FieldNotFound,
            SessionNotFound {
        Message reply = createMessage(sessionID, MsgType.REJECT);
        reverseRoute(message, reply);
        String refSeqNum = message.getHeader().getString(FixTags.MSG_SEQ_NUM);
        reply.setString(RefSeqNum.TAG, refSeqNum);
        Session.sendToTarget(reply);
    }

    @Override
    public void sendBusinessReject(SessionID sessionID, Message message, int rejectReason,
            String rejectText) {
        //        Message reply = createMessage(sessionID, MsgType.BUSINESS_MESSAGE_REJECT);
        //        reverseRoute(message, reply);
        //        String refSeqNum = message.getHeader().getString(FixTags.MSG_SEQ_NUM);
        //        reply.setString(RefSeqNum.TAG, refSeqNum);
        //        reply.setString(Text.TAG, rejectText);
        //        Session.sendToTarget(reply);
    }

    private Message createMessage(SessionID sessionID, MsgType msgType) throws FieldNotFound {
        return application
                .getMessageFactory()
                .getMessageBuilder(sessionID.getBeginString(), FIXApplication.FIX41,
                        msgType.getValue()).create();
    }

    private void reverseRoute(Message message, Message reply) throws FieldNotFound {
        reply.getHeader().setString(FixTags.SENDER_COMP_ID,
                message.getHeader().getString(FixTags.TARGET_COMP_ID));
        reply.getHeader().setString(FixTags.TARGET_COMP_ID,
                message.getHeader().getString(FixTags.SENDER_COMP_ID));
    }

    private void executionReport(ExecutionReport message, SessionID sessionID) throws FieldNotFound {

        ExecID execID = message.getExecID();

        if (alreadyProcessed(execID, sessionID))
            return;

        Order order = orderTableModel.getOrder(message.getClOrdID().getValue());
        if (order == null) {
            return;
        }

        BigDecimal fillSize;

        if (message.getLastShares() != null) {
            LastShares lastShares = message.getLastShares();
            fillSize = new BigDecimal(lastShares.getValue());
        } else {
            // > FIX 4.1
            LeavesQty leavesQty = message.getLeavesQty();
            fillSize = new BigDecimal(order.getQuantity()).subtract(new BigDecimal(leavesQty
                    .getValue()));
        }

        if (fillSize.compareTo(BigDecimal.ZERO) > 0) {
            order.setOpen(order.getOpen() - (int) Double.parseDouble(fillSize.toPlainString()));
            order.setExecuted(new Integer(message.getString(CumQty.TAG)));
            order.setAvgPx(new Double(message.getString(AvgPx.TAG)));
        }

        OrdStatus ordStatus = message.getOrdStatus();

        if (ordStatus.equals(OrdStatus.REJECTED)) {
            order.setRejected(true);
            order.setOpen(0);
        } else if (ordStatus.equals(OrdStatus.CANCELED) || ordStatus.equals(OrdStatus.DONE_FOR_DAY)) {
            order.setCanceled(true);
            order.setOpen(0);
        } else if (ordStatus.equals(OrdStatus.NEW)) {
            if (order.isNew()) {
                order.setNew(false);
            }
        }

        try {
            order.setMessage(message.getText().getValue());
        } catch (FieldNotFound e) {
            // ignore
        }

        orderTableModel.updateOrder(order, message.getClOrdID().getValue());
        //    observableOrder.update(order);

        if (fillSize.compareTo(BigDecimal.ZERO) > 0) {
            Execution execution = new Execution();
            execution.setExchangeID(sessionID + message.getExecID().getValue());

            execution.setSymbol(message.getSymbol().getValue());
            execution.setQuantity(fillSize.intValue());
            if (message.getLastPx() != null) {
                execution.setPrice(message.getLastPx().getValue());
            }
            Side side = message.getSide();
            execution.setSide(FIXSideToSide(side));
            executionTableModel.addExecution(execution);
        }
    }

    private void cancelReject(Message message, SessionID sessionID) throws FieldNotFound {

        String id = message.getField(ClOrdID.TAG).getCharacters().toString();
        Order order = orderTableModel.getOrder(id);
        if (order == null)
            return;
        if (order.getOriginalID() != null)
            order = orderTableModel.getOrder(order.getOriginalID());

        try {
            order.setMessage(message.getField(Text.TAG).getCharacters().toString());
        } catch (FieldNotFound e) {
            // ignore
        }

        orderTableModel.updateOrder(order, message.getField(OrigClOrdID.TAG).getCharacters()
                .toString());
    }

    private boolean alreadyProcessed(ExecID execID, SessionID sessionID) {
        HashSet<ExecID> set = execIDs.get(sessionID);
        if (set == null) {
            set = new HashSet<ExecID>();
            set.add(execID);
            execIDs.put(sessionID, set);
            return false;
        }

        if (set.contains(execID)) {
            return true;
        }

        set.add(execID);
        return false;
    }

    @Override
    public void send(Order order) {
        NewOrderSingle newOrderSingle = new NewOrderSingle(new ClOrdID(order.getID()),
                HandlInst.AUTOMATED_EXECUTION_ORDER_PRIVATE_NO_BROKER_INTERVENTION, new Symbol(
                        order.getSymbol()), sideToFIXSide(order.getSide()),
                typeToFIXType(order.getType()));

        newOrderSingle.setOrderQty(new OrderQty(order.getQuantity()));

        send(populateOrder(order, newOrderSingle), order.getSessionID());
    }

    public quickfix.Message populateOrder(Order order, quickfix.Message newOrderSingle) {

        OrderType type = order.getType();

        if (type == OrderType.LIMIT)
            newOrderSingle.setField(new Price(order.getLimit().doubleValue()));
        else if (type == OrderType.STOP) {
            newOrderSingle.setField(new StopPx(order.getStop().doubleValue()));
        } else if (type == OrderType.STOP_LIMIT) {
            newOrderSingle.setField(new Price(order.getLimit().doubleValue()));
            newOrderSingle.setField(new StopPx(order.getStop().doubleValue()));
        }

        if (order.getSide() == OrderSide.SHORT_SELL
                || order.getSide() == OrderSide.SHORT_SELL_EXEMPT) {
            newOrderSingle.setField(new LocateReqd("N"));
        }

        newOrderSingle.setField(tifToFIXTif(order.getTIF()));
        return newOrderSingle;
    }

    @Override
    public void cancel(Order order) {

        String id = order.generateID();
        OrderCancelRequest message = new OrderCancelRequest(new OrigClOrdID(order.getID()),
                new ClOrdID(id), new Symbol(order.getSymbol()), sideToFIXSide(order.getSide()));
        message.setOrderQty(new OrderQty(order.getQuantity()));

        orderTableModel.addID(order, id);
        send(message, order.getSessionID());
    }

    @Override
    public void replace(Order order, Order newOrder) {

        OrderCancelReplaceRequest message = new OrderCancelReplaceRequest(new OrigClOrdID(
                order.getID()), new ClOrdID(newOrder.getID()),
                HandlInst.AUTOMATED_EXECUTION_ORDER_PRIVATE_NO_BROKER_INTERVENTION, new Symbol(
                        order.getSymbol()), sideToFIXSide(order.getSide()),
                typeToFIXType(order.getType()));

        orderTableModel.addID(order, newOrder.getID());
        send(populateCancelReplace(order, newOrder, message), order.getSessionID());
    }

    Message populateCancelReplace(Order order, Order newOrder, quickfix.Message message) {

        if (order.getQuantity() != newOrder.getQuantity())
            message.setField(new OrderQty(newOrder.getQuantity()));
        if (!order.getLimit().equals(newOrder.getLimit()))
            message.setField(new Price(newOrder.getLimit().doubleValue()));
        return message;
    }

    private Side sideToFIXSide(OrderSide side) {
        return (Side) sideMap.getFirst(side);
    }

    private OrderSide FIXSideToSide(Side side) {
        return (OrderSide) sideMap.getSecond(side);
    }

    private OrdType typeToFIXType(OrderType type) {
        return (OrdType) typeMap.getFirst(type);
    }

    private TimeInForce tifToFIXTif(OrderTIF tif) {
        return (TimeInForce) tifMap.getFirst(tif);
    }

    static {
        sideMap.put(OrderSide.BUY, Side.BUY);
        sideMap.put(OrderSide.SELL, Side.SELL);
        sideMap.put(OrderSide.SHORT_SELL, Side.SELL_SHORT);
        sideMap.put(OrderSide.SHORT_SELL_EXEMPT, Side.SELL_SHORT_EXEMPT);
        sideMap.put(OrderSide.CROSS, Side.CROSS);

        typeMap.put(OrderType.MARKET, OrdType.MARKET);
        typeMap.put(OrderType.LIMIT, OrdType.LIMIT);
        typeMap.put(OrderType.STOP, OrdType.STOP);
        typeMap.put(OrderType.STOP_LIMIT, OrdType.STOP_LIMIT);

        tifMap.put(OrderTIF.DAY, TimeInForce.DAY);
        tifMap.put(OrderTIF.IOC, TimeInForce.IMMEDIATE_OR_CANCEL);
        tifMap.put(OrderTIF.OPG, TimeInForce.AT_THE_OPENING);
        tifMap.put(OrderTIF.GTC, TimeInForce.GOOD_TILL_CANCEL);
        tifMap.put(OrderTIF.GTX, TimeInForce.GOOD_TILL_CROSSING);
    }
}
