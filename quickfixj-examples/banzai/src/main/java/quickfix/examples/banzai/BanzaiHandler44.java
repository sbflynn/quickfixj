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

import quickfix.BusinessRejectReasonText;
import quickfix.FieldNotFound;
import quickfix.FixTags;
import quickfix.Message;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.fix44.ExecutionReport;
import quickfix.fix44.NewOrderSingle;
import quickfix.fix44.OrderCancelReplaceRequest;
import quickfix.fix44.OrderCancelRequest;
import quickfix.fix44.field.AvgPx;
import quickfix.fix44.field.BusinessRejectReason;
import quickfix.fix44.field.ClOrdID;
import quickfix.fix44.field.CumQty;
import quickfix.fix44.field.ExecID;
import quickfix.fix44.field.HandlInst;
import quickfix.fix44.field.LeavesQty;
import quickfix.fix44.field.LocateReqd;
import quickfix.fix44.field.MsgType;
import quickfix.fix44.field.OrdStatus;
import quickfix.fix44.field.OrdType;
import quickfix.fix44.field.OrderQty;
import quickfix.fix44.field.OrigClOrdID;
import quickfix.fix44.field.Price;
import quickfix.fix44.field.RefSeqNum;
import quickfix.fix44.field.SessionRejectReason;
import quickfix.fix44.field.Side;
import quickfix.fix44.field.StopPx;
import quickfix.fix44.field.Symbol;
import quickfix.fix44.field.Text;
import quickfix.fix44.field.TimeInForce;
import quickfix.fix44.field.TransactTime;

public class BanzaiHandler44 extends BanzaiHandler {

    static private TwoWayMap sideMap = new TwoWayMap();
    static private TwoWayMap typeMap = new TwoWayMap();
    static private TwoWayMap tifMap = new TwoWayMap();
    static private HashMap<SessionID, HashSet<ExecID>> execIDs = new HashMap<SessionID, HashSet<ExecID>>();

    private final BanzaiApplication application;
    private final OrderTableModel orderTableModel;
    private final ExecutionTableModel executionTableModel;

    public BanzaiHandler44(BanzaiApplication application, OrderTableModel orderTableModel,
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
                    sendBusinessReject(sessionID, message,
                            BusinessRejectReasonText.CONDITIONALLY_REQUIRED_FIELD_MISSING,
                            "Conditionally required field missing");
                } else if (message.getHeader().isFieldSet(FixTags.DELIVER_TO_COMP_ID)) {
                    // This is here to support OpenFIX certification
                    sendSessionReject(sessionID, message, SessionRejectReason.COMPID_PROBLEM);
                } else if (message.getHeader().getField(FixTags.MSG_TYPE).getValue().equals("8")) {
                    executionReport((ExecutionReport) message, sessionID);
                } else if (message.getHeader().getField(FixTags.MSG_TYPE).getValue().equals("9")) {
                    cancelReject(message, sessionID);
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

    private void sendSessionReject(SessionID sessionID, Message message,
            SessionRejectReason rejectReason) throws FieldNotFound, SessionNotFound {
        Message reply = createMessage(sessionID, MsgType.REJECT);
        reverseRoute(message, reply);
        String refSeqNum = message.getHeader().getString(FixTags.MSG_SEQ_NUM);
        reply.setString(RefSeqNum.TAG, refSeqNum);
        reply.setString(FixTags.REF_MSG_TYPE, message.getHeader().getString(FixTags.MSG_TYPE));
        reply.setField(rejectReason);
        Session.sendToTarget(reply);
    }

    @Override
    public void sendBusinessReject(SessionID sessionID, Message message, int rejectReason,
            String rejectText) throws FieldNotFound, SessionNotFound {
        Message reply = createMessage(sessionID, MsgType.BUSINESS_MESSAGE_REJECT);
        reverseRoute(message, reply);
        String refSeqNum = message.getHeader().getString(FixTags.MSG_SEQ_NUM);
        reply.setString(RefSeqNum.TAG, refSeqNum);
        reply.setString(FixTags.REF_MSG_TYPE, message.getHeader().getString(FixTags.MSG_TYPE));
        reply.setInt(BusinessRejectReason.TAG, rejectReason);
        reply.setString(Text.TAG, rejectText);
        Session.sendToTarget(reply);
    }

    private Message createMessage(SessionID sessionID, MsgType msgType) throws FieldNotFound {
        return application
                .getMessageFactory()
                .getMessageBuilder(sessionID.getBeginString(), FIXApplication.FIX44,
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

        LeavesQty leavesQty = message.getLeavesQty();
        fillSize = new BigDecimal(order.getQuantity())
                .subtract(new BigDecimal(leavesQty.getValue()));

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
            execution.setSide(getSide(side));
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
                getSide(order.getSide()), new TransactTime(new Date()),
                getOrderType(order.getType()));

        newOrderSingle.setOrderQty(new OrderQty(order.getQuantity()));
        newOrderSingle.setSymbol(new Symbol(order.getSymbol()));
        newOrderSingle.setHandlInst(HandlInst.AUTOMATED_EXECUTION_ORDER_PRIVATE);
        newOrderSingle.setTimeInForce(getTimeInForce(order.getTIF()));

        switch (order.getType()) {
        case LIMIT:
            newOrderSingle.setField(new Price(order.getLimit().doubleValue()));
            break;
        case STOP:
            newOrderSingle.setField(new StopPx(order.getStop().doubleValue()));
            break;
        case STOP_LIMIT:
            newOrderSingle.setField(new Price(order.getLimit().doubleValue()));
            newOrderSingle.setField(new StopPx(order.getStop().doubleValue()));
            break;
        default:
            break;
        }

        switch (order.getSide()) {
        case SHORT_SELL:
        case SHORT_SELL_EXEMPT:
            newOrderSingle.setField(new LocateReqd(false));
            break;

        default:
            break;
        }

        send(newOrderSingle, order.getSessionID());
    }

    @Override
    public void cancel(Order order) {
        String id = order.generateID();
        OrderCancelRequest message = new OrderCancelRequest(new OrigClOrdID(order.getID()),
                new ClOrdID(id), getSide(order.getSide()), new TransactTime(new Date()));

        message.setSymbol(new Symbol(order.getSymbol()));
        message.setOrderQty(new OrderQty(order.getQuantity()));

        orderTableModel.addID(order, id);
        send(message, order.getSessionID());
    }

    @Override
    public void replace(Order order, Order newOrder) {

        OrderCancelReplaceRequest message = new OrderCancelReplaceRequest(new OrigClOrdID(
                order.getID()), new ClOrdID(newOrder.getID()), getSide(order.getSide()),
                new TransactTime(new Date()), getOrderType(order.getType()));

        message.setSymbol(new Symbol(order.getSymbol()));

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

    private Side getSide(OrderSide side) {
        return (Side) sideMap.getFirst(side);
    }

    private OrderSide getSide(Side side) {
        return (OrderSide) sideMap.getSecond(side);
    }

    private OrdType getOrderType(OrderType type) {
        return (OrdType) typeMap.getFirst(type);
    }

    private TimeInForce getTimeInForce(OrderTIF tif) {
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
