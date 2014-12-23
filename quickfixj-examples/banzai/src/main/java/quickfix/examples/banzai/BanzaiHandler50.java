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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import org.quickfixj.FIXApplication;
import org.quickfixj.FIXMessage;
import org.quickfixj.engine.FIXSession.FIXSessionID;
import org.quickfixj.engine.FIXTag;
import org.quickfixj.engine.SessionNotFoundException;
import org.quickfixj.field.GenericField;
import org.quickfixj.messages.bd.fix50.ExecutionReport;
import org.quickfixj.messages.bd.fix50.NewOrderSingle;
import org.quickfixj.messages.bd.fix50.OrderCancelReplaceRequest;
import org.quickfixj.messages.bd.fix50.OrderCancelRequest;
import org.quickfixj.messages.bd.fix50.field.BusinessRejectReason;
import org.quickfixj.messages.bd.fix50.field.ClOrdID;
import org.quickfixj.messages.bd.fix50.field.CumQty;
import org.quickfixj.messages.bd.fix50.field.ExecID;
import org.quickfixj.messages.bd.fix50.field.HandlInst;
import org.quickfixj.messages.bd.fix50.field.LeavesQty;
import org.quickfixj.messages.bd.fix50.field.LocateReqd;
import org.quickfixj.messages.bd.fix50.field.MsgType;
import org.quickfixj.messages.bd.fix50.field.OrdStatus;
import org.quickfixj.messages.bd.fix50.field.OrdType;
import org.quickfixj.messages.bd.fix50.field.OrderQty;
import org.quickfixj.messages.bd.fix50.field.OrigClOrdID;
import org.quickfixj.messages.bd.fix50.field.Price;
import org.quickfixj.messages.bd.fix50.field.RefSeqNum;
import org.quickfixj.messages.bd.fix50.field.SessionRejectReason;
import org.quickfixj.messages.bd.fix50.field.Side;
import org.quickfixj.messages.bd.fix50.field.StopPx;
import org.quickfixj.messages.bd.fix50.field.Symbol;
import org.quickfixj.messages.bd.fix50.field.Text;
import org.quickfixj.messages.bd.fix50.field.TimeInForce;
import org.quickfixj.messages.bd.fix50.field.TransactTime;

import quickfix.BusinessRejectReasonText;
import quickfix.FieldNotFound;

public class BanzaiHandler50 extends BanzaiHandler {

    static private TwoWayMap sideMap = new TwoWayMap();
    static private TwoWayMap typeMap = new TwoWayMap();
    static private TwoWayMap tifMap = new TwoWayMap();
    static private HashMap<FIXSessionID, HashSet<ExecID>> execIDs = new HashMap<FIXSessionID, HashSet<ExecID>>();

    public BanzaiHandler50(BanzaiApplication application) {
        super(application);
    }

    @Override
    public void process(FIXMessage message, FIXSessionID sessionID) {

        try {
            if (getApplication().isAvailable()) {
                if (getApplication().isMissingField()) {
                    // For OpenFIX certification testing
                    sendBusinessReject(sessionID, message,
                            BusinessRejectReasonText.CONDITIONALLY_REQUIRED_FIELD_MISSING,
                            "Conditionally required field missing");
                } else if (message.getHeader().isFieldSet(FIXTag.DELIVER_TO_COMP_ID)) {
                    // This is here to support OpenFIX certification
                    sendSessionReject(sessionID, message, SessionRejectReason.COMPID_PROBLEM);
                } else if (message.getHeader().getField(FIXTag.MSG_TYPE).getValue().equals("8")) {
                    executionReport((ExecutionReport) message, sessionID);
                } else if (message.getHeader().getField(FIXTag.MSG_TYPE).getValue().equals("9")) {
                    cancelReject(message);
                } else {
                    sendBusinessReject(sessionID, message,
                            BusinessRejectReasonText.UNSUPPORTED_MESSAGE_TYPE,
                            "Unsupported Message Type");
                }
            } else {
                sendBusinessReject(sessionID, message,
                        BusinessRejectReasonText.APPLICATION_NOT_AVAILABLE,
                        "Application not available");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendSessionReject(FIXSessionID sessionID, FIXMessage message,
            SessionRejectReason rejectReason) throws SessionNotFoundException {
        FIXMessage reply = getApplication().createMessage(sessionID, FIXApplication.FIX50,
                MsgType.REJECT.getValue());
        reverseRoute(message, reply);
        String refSeqNum = message.getHeader().getFieldValue(FIXTag.MSG_SEQ_NUM);
        reply.setField(new GenericField(RefSeqNum.TAG, refSeqNum));
        reply.setField(new GenericField(FIXTag.REF_MSG_TYPE, message.getHeader().getFieldValue(
                FIXTag.MSG_TYPE)));
        reply.setField(rejectReason);
        getApplication().getEngine().send(reply, sessionID);
    }

    @Override
    public void sendBusinessReject(FIXSessionID sessionID, FIXMessage message, int rejectReason,
            String rejectText) throws SessionNotFoundException {
        FIXMessage reply = getApplication().createMessage(sessionID, FIXApplication.FIX50,
                MsgType.BUSINESS_MESSAGE_REJECT.getValue());
        reverseRoute(message, reply);
        reply.setField(new GenericField(RefSeqNum.TAG, message.getHeader().getFieldValue(
                FIXTag.MSG_SEQ_NUM)));
        reply.setField(new GenericField(FIXTag.REF_MSG_TYPE, message.getHeader().getFieldValue(
                FIXTag.MSG_TYPE)));
        reply.setField(new GenericField(BusinessRejectReason.TAG, rejectReason));
        reply.setField(new GenericField(Text.TAG, rejectText));
        getApplication().getEngine().send(reply, sessionID);
    }

    private void executionReport(ExecutionReport message, FIXSessionID sessionID)
            throws FieldNotFound {

        ExecID execID = message.getExecID();

        if (alreadyProcessed(execID, sessionID))
            return;

        Order order = getOrderTableModel().getOrder(message.getClOrdID().getValue());
        if (order == null) {
            return;
        }

        BigDecimal fillSize;

        LeavesQty leavesQty = message.getLeavesQty();
        fillSize = new BigDecimal(order.getQuantity()).subtract(leavesQty.getValue());

        if (fillSize.compareTo(BigDecimal.ZERO) > 0) {
            order.setOpen(order.getOpen() - (int) Double.parseDouble(fillSize.toPlainString()));
            order.setExecuted(new Integer(message.getFieldValue(CumQty.TAG)));
            order.setAvgPx(message.getAvgPx().getValue().doubleValue());
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

        getOrderTableModel().updateOrder(order, message.getClOrdID().getValue());
        //    observableOrder.update(order);

        if (fillSize.compareTo(BigDecimal.ZERO) > 0) {
            Execution execution = new Execution();
            execution.setExchangeID(sessionID + message.getExecID().getValue());

            execution.setSymbol(message.getSymbol().getValue());
            execution.setQuantity(fillSize.intValue());
            if (message.getLastPx() != null) {
                execution.setPrice(message.getLastPx().getValue().doubleValue());
            }
            Side side = message.getSide();
            execution.setSide(FIXSideToSide(side));
            getExecutionTableModel().addExecution(execution);
        }
    }

    private void cancelReject(FIXMessage message) throws FieldNotFound {

        String id = message.getFieldValue(ClOrdID.TAG);
        Order order = getOrderTableModel().getOrder(id);
        if (order == null)
            return;
        if (order.getOriginalID() != null)
            order = getOrderTableModel().getOrder(order.getOriginalID());

        try {
            order.setMessage(message.getFieldValue(Text.TAG));
        } catch (FieldNotFound e) {
            // ignore
        }

        getOrderTableModel().updateOrder(order, message.getFieldValue(OrigClOrdID.TAG));
    }

    private boolean alreadyProcessed(ExecID execID, FIXSessionID sessionID) {
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
                sideToFIXSide(order.getSide()), new TransactTime(new Date()),
                typeToFIXType(order.getType()));

        newOrderSingle.setOrderQty(new OrderQty(order.getQuantity()));
        newOrderSingle.setSymbol(new Symbol(order.getSymbol()));
        newOrderSingle.setHandlInst(HandlInst.AUTOMATED_EXECUTION_ORDER_PRIVATE);

        send(populateOrder(order, newOrderSingle), order.getSessionID());
    }

    public FIXMessage populateOrder(Order order, FIXMessage newOrderSingle) {

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
            newOrderSingle.setField(new LocateReqd(false));
        }

        newOrderSingle.setField(tifToFIXTif(order.getTIF()));
        return newOrderSingle;
    }

    @Override
    public void cancel(Order order) {
        String id = order.generateID();
        OrderCancelRequest message = new OrderCancelRequest(new OrigClOrdID(order.getID()),
                new ClOrdID(id), sideToFIXSide(order.getSide()), new TransactTime(new Date()));

        message.setSymbol(new Symbol(order.getSymbol()));
        message.setOrderQty(new OrderQty(order.getQuantity()));

        getOrderTableModel().addID(order, id);
        send(message, order.getSessionID());
    }

    @Override
    public void replace(Order order, Order newOrder) {

        OrderCancelReplaceRequest message = new OrderCancelReplaceRequest(new OrigClOrdID(
                order.getID()), new ClOrdID(newOrder.getID()), sideToFIXSide(order.getSide()),
                new TransactTime(new Date()), typeToFIXType(order.getType()));

        message.setSymbol(new Symbol(order.getSymbol()));

        getOrderTableModel().addID(order, newOrder.getID());
        send(populateCancelReplace(order, newOrder, message), order.getSessionID());
    }

    FIXMessage populateCancelReplace(Order order, Order newOrder, FIXMessage message) {

        if (order.getQuantity() != newOrder.getQuantity()) {
            message.setField(new OrderQty(newOrder.getQuantity()));
        }

        if (!order.getLimit().equals(newOrder.getLimit())) {
            message.setField(new Price(newOrder.getLimit().doubleValue()));
        }

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
        sideMap.put(OrderSide.CROSS_SHORT, Side.CROSS_SHORT);

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
