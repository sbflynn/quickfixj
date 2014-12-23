/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 17 Dec 2014 by stephen.flynn@jftechnology.com.
 */
package quickfix.examples.executor;

import org.quickfixj.messages.bd.fix40.ExecutionReport;
import org.quickfixj.messages.bd.fix40.NewOrderSingle;
import org.quickfixj.messages.bd.fix40.field.AvgPx;
import org.quickfixj.messages.bd.fix40.field.CumQty;
import org.quickfixj.messages.bd.fix40.field.ExecID;
import org.quickfixj.messages.bd.fix40.field.ExecTransType;
import org.quickfixj.messages.bd.fix40.field.LastPx;
import org.quickfixj.messages.bd.fix40.field.LastShares;
import org.quickfixj.messages.bd.fix40.field.OrdStatus;
import org.quickfixj.messages.bd.fix40.field.OrdType;
import org.quickfixj.messages.bd.fix40.field.OrderID;
import org.quickfixj.messages.bd.fix40.field.OrderQty;
import org.quickfixj.messages.bd.fix40.field.Side;

import quickfix.FieldNotFound;
import quickfix.IncorrectTagValue;
import quickfix.LogUtil;
import quickfix.MessageCracker;
import quickfix.SessionID;

/**
 * ExecutorHandler40 - provides...
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public class ExecutorHandler40 extends MessageCracker {

    private final Application application;

    ExecutorHandler40(Application application) {
        this.application = application;
    }

    public void onMessage(NewOrderSingle order, SessionID sessionID) {
        try {

            validateOrder(order.getOrdType());

            double price = getPrice(order);

            OrderQty orderQty = order.getOrderQty();

            ExecutionReport accept = new ExecutionReport(new OrderID(application.genOrderID()),
                    new ExecID(application.genExecIDAsInt()), ExecTransType.NEW, OrdStatus.NEW,
                    order.getSymbol(), order.getSide(), orderQty, new LastShares(0), new LastPx(0),
                    new CumQty(0), new AvgPx(0));

            accept.setClOrdID(order.getClOrdID());
            application.sendMessage(sessionID, accept);

            if (isOrderExecutable(order, price)) {
                ExecutionReport fill = new ExecutionReport(new OrderID(application.genOrderID()),
                        new ExecID(application.genExecIDAsInt()), ExecTransType.NEW,
                        OrdStatus.FILLED, order.getSymbol(), order.getSide(), orderQty,
                        new LastShares(orderQty.getValue()), new LastPx(price), new CumQty(
                                orderQty.getValue()), new AvgPx(price));

                fill.setClOrdID(order.getClOrdID());

                application.sendMessage(sessionID, fill);
            }
        } catch (RuntimeException e) {
            LogUtil.logThrowable(sessionID, e.getMessage(), e);
        }
    }

    private boolean isOrderExecutable(NewOrderSingle order, double price) throws FieldNotFound {
        if (OrdType.LIMIT.equals(order.getOrdType())) {
            double limitPrice = order.getPrice().getValue();
            Side side = order.getSide();

            return (Side.BUY.equals(side) && price <= limitPrice)
                    || ((Side.SELL.equals(side) || Side.SELL_SHORT.equals(side)) && price >= limitPrice);
        }
        return true;
    }

    private void validateOrder(OrdType ordType) throws IncorrectTagValue, FieldNotFound {

        if (!application.getValidOrderTypes().contains(ordType.getValue())) {
            application.getLog().error("Order type not in ValidOrderTypes setting");
            throw new IncorrectTagValue(ordType.getTag());
        }

        if (OrdType.MARKET.equals(ordType) && application.getMarketDataProvider() == null) {
            application.getLog().error("DefaultMarketPrice setting not specified for market order");
            throw new IncorrectTagValue(ordType.getTag());
        }
    }

    private double getPrice(NewOrderSingle message) throws FieldNotFound {

        if (OrdType.LIMIT.equals(message.getOrdType()) && application.isAlwaysFillLimitOrders()) {
            return message.getPrice().getValue();
        }

        if (Side.BUY.equals(message.getSide())) {
            return application.getAsk(message.getSymbol().getValue());
        }

        if (Side.SELL.equals(message.getSide()) || Side.SELL_SHORT.equals(message.getSide())) {
            return application.getBid(message.getSymbol().getValue());
        }

        throw new RuntimeException("Invalid order side: " + message.getSide());
    }
}
