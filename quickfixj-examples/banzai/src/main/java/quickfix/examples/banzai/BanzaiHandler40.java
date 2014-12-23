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
import org.quickfixj.FIXMessage;
import org.quickfixj.engine.FIXSession.FIXSessionID;
import org.quickfixj.engine.FIXTag;
import org.quickfixj.engine.SessionNotFoundException;
import org.quickfixj.field.GenericField;
import org.quickfixj.messages.bd.fix40.ExecutionReport;
import org.quickfixj.messages.bd.fix40.NewOrderSingle;
import org.quickfixj.messages.bd.fix40.OrderCancelReplaceRequest;
import org.quickfixj.messages.bd.fix40.OrderCancelRequest;
import org.quickfixj.messages.bd.fix40.field.AvgPx;
import org.quickfixj.messages.bd.fix40.field.ClOrdID;
import org.quickfixj.messages.bd.fix40.field.CumQty;
import org.quickfixj.messages.bd.fix40.field.CxlType;
import org.quickfixj.messages.bd.fix40.field.ExecID;
import org.quickfixj.messages.bd.fix40.field.HandlInst;
import org.quickfixj.messages.bd.fix40.field.LastShares;
import org.quickfixj.messages.bd.fix40.field.LocateReqd;
import org.quickfixj.messages.bd.fix40.field.MsgType;
import org.quickfixj.messages.bd.fix40.field.OrdStatus;
import org.quickfixj.messages.bd.fix40.field.OrdType;
import org.quickfixj.messages.bd.fix40.field.OrderQty;
import org.quickfixj.messages.bd.fix40.field.OrigClOrdID;
import org.quickfixj.messages.bd.fix40.field.Price;
import org.quickfixj.messages.bd.fix40.field.RefSeqNum;
import org.quickfixj.messages.bd.fix40.field.Side;
import org.quickfixj.messages.bd.fix40.field.StopPx;
import org.quickfixj.messages.bd.fix40.field.Symbol;
import org.quickfixj.messages.bd.fix40.field.Text;
import org.quickfixj.messages.bd.fix40.field.TimeInForce;

import quickfix.FieldNotFound;

public class BanzaiHandler40 extends BanzaiHandler {

    private static final TwoWayMap sideMap = new TwoWayMap();
    private static final TwoWayMap typeMap = new TwoWayMap();
    private static final TwoWayMap tifMap = new TwoWayMap();
    private static final HashMap<FIXSessionID, HashSet<ExecID>> execIDs = new HashMap<FIXSessionID, HashSet<ExecID>>();

    public BanzaiHandler40(BanzaiApplication application) {

        super(application);
    }

    @Override
    public void process(FIXMessage message, FIXSessionID sessionID) {
        try {
            if (getApplication().isAvailable()) {
                if (getApplication().isMissingField()) {
                    // For OpenFIX certification testing
                    sendBusinessReject(sessionID, message, -1,
                            "Conditionally required field missing");
                } else if (message.getHeader().isFieldSet(FIXTag.DELIVER_TO_COMP_ID)) {
                    // This is here to support OpenFIX certification
                    sendSessionReject(sessionID, message);
                } else if (message.getHeader().getField(FIXTag.MSG_TYPE).getValue().equals("8")) {
                    executionReport((ExecutionReport) message, sessionID);
                } else if (message.getHeader().getField(FIXTag.MSG_TYPE).getValue().equals("9")) {
                    cancelReject(message);
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

    private void sendSessionReject(FIXSessionID sessionID, FIXMessage message)
            throws FieldNotFound, SessionNotFoundException {
        FIXMessage reply = getApplication().createMessage(sessionID, FIXApplication.FIX40,
                MsgType.REJECT.getValue());
        reverseRoute(message, reply);
        String refSeqNum = message.getHeader().getFieldValue(FIXTag.MSG_SEQ_NUM);
        reply.setField(new GenericField(RefSeqNum.TAG, refSeqNum));
        getApplication().getEngine().send(reply, sessionID);
    }

    @Override
    public void sendBusinessReject(FIXSessionID sessionID, FIXMessage message, int rejectReason,
            String rejectText) {
        //        Message reply = createMessage(sessionID, MsgType.BUSINESS_MESSAGE_REJECT);
        //        reverseRoute(message, reply);
        //        String refSeqNum = message.getHeader().getString(FixTags.MSG_SEQ_NUM);
        //        reply.setField(new GenericField(RefSeqNum.TAG, refSeqNum));
        //        reply.setField(new GenericField(Text.TAG, rejectText));
        //        Session.sendToTarget(reply);
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

        LastShares lastShares = message.getLastShares();
        fillSize = new BigDecimal(lastShares.getValue());

        if (fillSize.compareTo(BigDecimal.ZERO) > 0) {
            order.setOpen(order.getOpen() - (int) Double.parseDouble(fillSize.toPlainString()));
            order.setExecuted(new Integer(message.getFieldValue(CumQty.TAG)));
            order.setAvgPx(new Double(message.getFieldValue(AvgPx.TAG)));
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
            execution.setExchangeID(sessionID + message.getExecID().getCharacters().toString());

            execution.setSymbol(message.getSymbol().getValue());
            execution.setQuantity(fillSize.intValue());
            if (message.getLastPx() != null) {
                execution.setPrice(message.getLastPx().getValue());
            }
            Side side = message.getSide();
            execution.setSide(FIXSideToSide(side));
            getExecutionTableModel().addExecution(execution);
        }
    }

    public void cancelReject(FIXMessage message) throws FieldNotFound {

        String id = message.getFieldValue(ClOrdID.TAG);
        Order order = getOrderTableModel().getOrder(id);

        if (order == null) {
            return;
        }

        if (order.getOriginalID() != null) {
            order = getOrderTableModel().getOrder(order.getOriginalID());
        }

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
                HandlInst.AUTOMATED_EXECUTION_ORDER_PRIVATE_NO_BROKER_INTERVENTION, new Symbol(
                        order.getSymbol()), sideToFIXSide(order.getSide()), new OrderQty(
                        order.getQuantity()), typeToFIXType(order.getType()));

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
            newOrderSingle.setField(new LocateReqd("N"));
        }

        newOrderSingle.setField(tifToFIXTif(order.getTIF()));
        return newOrderSingle;
    }

    @Override
    public void cancel(Order order) {
        String id = order.generateID();
        OrderCancelRequest message = new OrderCancelRequest(new OrigClOrdID(order.getID()),
                new ClOrdID(id), CxlType.FULL_REMAINING_QUANTITY, new Symbol(order.getSymbol()),
                sideToFIXSide(order.getSide()), new OrderQty(order.getQuantity()));

        getOrderTableModel().addID(order, id);
        send(message, order.getSessionID());
    }

    @Override
    public void replace(Order order, Order newOrder) {

        OrderCancelReplaceRequest message = new OrderCancelReplaceRequest(new OrigClOrdID(
                order.getID()), new ClOrdID(newOrder.getID()),
                HandlInst.AUTOMATED_EXECUTION_ORDER_PRIVATE_NO_BROKER_INTERVENTION, new Symbol(
                        order.getSymbol()), sideToFIXSide(order.getSide()), new OrderQty(
                        newOrder.getQuantity()), typeToFIXType(order.getType()));

        if (order.getQuantity() != newOrder.getQuantity()) {
            message.setField(new OrderQty(newOrder.getQuantity()));
        }

        if (!order.getLimit().equals(newOrder.getLimit())) {
            message.setField(new Price(newOrder.getLimit().doubleValue()));
        }

        getOrderTableModel().addID(order, newOrder.getID());

        send(message, order.getSessionID());
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
